package bingboost;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BingBoost {
	// How much value we place on words in the title and description
	final int title_weight = 3;
	final int desc_weight = 1;
	
	// Bonus multiplier for terms close to query words
	final float bonus_multiplier = 1.2f;

	// Initial parameters
	String origQuery;
	Result[] results;
	int n_relevant_terms;
	int n_irrelevant_terms;
	Set<String> stopwords;

	// Normalized frequency for terms
	Map<String, Float> matches;
	Map<String, Float> misses;

	public BingBoost() throws FileNotFoundException, IOException {
		stopwords = readStopwords();
	}

	/*
	 * Create a map of the stopwords to quickly prune documents
	 */
	private Set<String> readStopwords() throws FileNotFoundException, IOException {
		Set<String> set = new HashSet<String>();

		try (BufferedReader br = new BufferedReader(new InputStreamReader(BingBoost.class.getResourceAsStream("stopwords.txt")))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() > 0 && line.charAt(0) != '#')
					set.add(line);
			}
		}

		return set;
	}

	/*
	 * Populate map with scores for each word in words based on the specified pts. Also use the relevant
	 * int (0 for irrelevant, 1 for relevant) to help us keep track of the number of terms for each.
	 */
	private void mapTermCounts(Map<String, Float> map, String[] words, int relevant, float pts) {
		for (String s : words) {
			if (stopwords.contains(s))
				continue;

			// Update total number of relevant or irrelevant words
			if (relevant > 0) 
				n_relevant_terms += pts; 
			else 
				n_irrelevant_terms += pts;

			if (map.get(s) != null)
				map.put(s, map.get(s) + pts);
			else
				map.put(s, pts);
		}
	}

	/*
	 * Add the terms in the result to the correct map (matches/misses) based on
	 * whether the Result is relevant or not. Keep track of the number of terms going
	 * into each.
	 */
	private void mapTermCounts(Result r) {
		Map<String, Float> map = (r.relevant > 0) ? matches : misses;

		// Build up map with absolute counts
		mapTermCounts(map, r.description.split("\\W+"), r.relevant, desc_weight);
		mapTermCounts(map, r.title.split("\\W+"), r.relevant, title_weight);
	}

	/*
	 * This function will set the matches and misses maps to contain average frequencies
	 * for each word over each set of documents (relevant and irrelevant). As an example, if
	 * there are two relevant documents, the absolute count that non-stop words appear in
	 * either will be obtained. The absolute count for each term will then be normalized by
	 * dividing it by the total number of non-stop words in each of the two documents.
	 */
	private void createNormalizedMaps() {
		for (Result r : results) {
			mapTermCounts(r);
		}

		if (n_relevant_terms > 0) {
			for (String s : matches.keySet())
				matches.put(s, matches.get(s) / n_relevant_terms);
		}

		if (n_irrelevant_terms > 0) {
			for (String s : misses.keySet())
				misses.put(s, misses.get(s) / n_irrelevant_terms);
		}
	}

	/*
	 * Used to add the scores for terms in the src map to the dest map give a mult that should be applied
	 * to each value in src before the addition takes place. multi is made negative to perform subtraction
	 */
	private void addTermFrequencies(Map<String, Float> dest, Map<String, Float> src, float mult) {
		for (String s : src.keySet()) {
			float val = src.get(s) * mult;
			if (dest.get(s) != null)
				dest.put(s, dest.get(s) + val);
			else
				dest.put(s, val);
		}
	}

	/*
	 * Creates the difference map given the matches and misses maps built up from processes the 
	 * relevant and irrelevant results
	 */
	private Map<String, Float> subtractMaps() {
		Map<String, Float> diff_map = new HashMap<String, Float>();

		// Add frequencies from matches
		addTermFrequencies(diff_map, matches, 1);

		// Subtract frequencies from misses
		addTermFrequencies(diff_map, misses, -1);

		return diff_map;
	}

	/*
	 * Concatenates result titles
	 */
	private String extractData(boolean getRelevant) {
		StringBuilder sb = new StringBuilder("");
		for (Result r : results) {
			if (r.relevant == 1 && getRelevant)
				sb.append(r.description);
			if (r.relevant == 0 && !getRelevant)
				sb.append(r.description);
		}
		return sb.toString();
	}

	/*
	 * Bonus terms that are k or less chars away. Tunable parameters are k, bonus.
	 */
	public void queryProximityBonus() {
		int k = 25;
		for (String term : matches.keySet()) {
			if (Utils.neighboringTerms(extractData(true), origQuery, term, k)) {
				matches.put(term, (matches.get(term) * bonus_multiplier));
			}
		}
		for (String term : misses.keySet()) {
			if (Utils.neighboringTerms(extractData(false), origQuery, term, k)) {
				misses.put(term, (misses.get(term) * bonus_multiplier));
			}
		}
	}

	/*
	 * Suggest word to be added to the query.
	 */
	private String suggestedWordToEnhanceQuery(Map<String, Float> diffMap) {
		float highestScore = -1;
		String word = "";

		for (String s : diffMap.keySet()) {
			if (!origQuery.contains(s) && diffMap.get(s) > highestScore) {
				highestScore = diffMap.get(s);
				word = s;
			}
		}

		return word;
	}

	/*
	 * Add suggested word to query according to most frequent order.
	 * Count occurrences of suggested word appearing before and after query in description, check majority vote.
	 * By default, concatenate word to end of query. Very crude, no semantic parsing.
	 */
	private String updateQueryWithSuggestion(String word) {
		int before = 0;
		int after = 0;
		for (Result result : results) {
			if (result.relevant == 0)
				continue;

			// Note both if clauses are necessary as query may not exist in description.
			if (result.description.indexOf(word) < result.description.indexOf(origQuery)) 
				before += desc_weight;
			if (result.description.indexOf(word) > result.description.indexOf(origQuery))
				after += desc_weight;
			if (result.title.indexOf(word) < result.title.indexOf(origQuery))
				before += title_weight;
			if (result.title.indexOf(word) > result.title.indexOf(origQuery))
				after += title_weight;
		}

		String updated = (before > after) ? word + " " + origQuery : origQuery + " " + word;
		return updated;
	}

	/*
	 * Outside of the constructor, the only public function for this class. Should reset
	 * all instance variable to process the new query and results.
	 * 
	 * Avoid reconstructing this object as you have to read in the stopwords each time.
	 */
	public String updatedQueryForFeedback(String query, Result[] results) {
		// Initialize instance variables
		n_relevant_terms = n_irrelevant_terms = 0;
		matches = new HashMap<String, Float>();
		misses = new HashMap<String, Float>();
		this.results = results;
		this.origQuery = query;
		// Process the results
		createNormalizedMaps();
		// Increase weight of terms near original query
		queryProximityBonus();
		Map<String, Float> scores = subtractMaps();

		String word = suggestedWordToEnhanceQuery(scores);
		String enhancedQuery = updateQueryWithSuggestion(word);
		System.out.println("Augmenting by: " + word);

		return enhancedQuery;
	}

}
