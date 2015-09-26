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
		
		// Print out the frequencies for testing purposes
		/*System.out.println("RELEVANT TERM FREQUENCIES");
		printFrequencies(matches);
		
		System.out.println("IRRELEVANT TERM FREQUENCIES");
		printFrequencies(misses);*/
	}
	
	private void addTermFrequencies(Map<String, Float> dest, Map<String, Float> src, float mult) {
		for (String s : src.keySet()) {
			float val = src.get(s) * mult;
			//System.out.println("Value: " + val);
			if (dest.get(s) != null)
				dest.put(s, dest.get(s) + val);
			else
				dest.put(s, val);
		}
	}
	
	private Map<String, Float> subtractMaps() {
		Map<String, Float> diff_map = new HashMap<String, Float>();
		
		// Add frequencies from matches
		addTermFrequencies(diff_map, matches, 1);
		
		// Subtract frequencies from misses
		addTermFrequencies(diff_map, misses, -1);
		
		return diff_map;
	}
	
	/*
	 * Helper for testing purposes
	 */
	private void printFrequencies(Map<String, Float> map) {
		for (String s : map.keySet())
			System.out.println(s + " : " + map.get(s));
	}
	
	/*
	 * Suggest word to be added to the query.
	 */
	private String suggestedWordToEnhanceQuery(Map<String, Float> diffMap) {
		float highestFrequency = -1;
		String word = "";
		
		for (String s : diffMap.keySet()) {
			if (!origQuery.contains(s) && diffMap.get(s) > highestFrequency) {
				highestFrequency = diffMap.get(s);
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
		//For debugging, look back at printed results marked as relevant, see if it matches.
		//System.out.println("Before count " + before);
		//System.out.println("After count " + after);
		
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
		this.origQuery = query.toLowerCase();
		this.results = results;
		
		// Process the results
		createNormalizedMaps();
		Map<String, Float> diffMap = subtractMaps();
		String word = suggestedWordToEnhanceQuery(diffMap);
		String enhancedQuery = updateQueryWithSuggestion(word);
		
		System.out.println("Recommended word to add to query: " + word);
		System.out.println("Updated query: " + enhancedQuery);
		
		return enhancedQuery;
	}

}
