package bingboost;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BingBoost {
	// Initial parameters
	String origQuery;
	Result[] results;
	int n_relevant_terms;
	int n_irrelevant_terms;
	Map<String, Boolean> stopwords;
	
	// Normalized frequency for terms
	Map<String, Float> matches;
	Map<String, Float> misses;
	
	public BingBoost() throws FileNotFoundException, IOException {
		stopwords = readStopwords();
	}
	
	/*
	 * Create a map of the stopwords to quickly prune documents
	 */
	private Map<String, Boolean> readStopwords() throws FileNotFoundException, IOException {
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		
		try (BufferedReader br = new BufferedReader(new FileReader("bingboost/stopwords.txt"))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		    	if (line.length() > 0 && line.charAt(0) != '#')
		    		map.put(line, true);
		    }
		    
		}
		
		return map;
	}
	
	/*
	 * Add the terms in the result to the correct map (matches/misses) based on
	 * whether the Result is relevant or not. Keep track of the number of terms going
	 * into each.
	 */
	private void mapTermCounts(Result r) {
		Map<String, Float> map = (r.relevant > 0) ? matches : misses;

		// Build up map with absolute counts
		for (String s : r.description.split("\\W+")) {
			if (stopwords.containsKey(s))
				continue;
			
			// Update total number of relevant or irrelevant words
			if (r.relevant > 0) 
				n_relevant_terms++; 
			else 
				n_irrelevant_terms++;
			
			if (map.get(s) != null)
				map.put(s, map.get(s) + 1);
			else
				map.put(s, 1f);
		}
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
		System.out.println("RELEVANT TERM FREQUENCIES");
		for (String s : matches.keySet())
			System.out.println(s + " : " + matches.get(s));
		
		System.out.println("IRRELEVANT TERM FREQUENCIES");
		for (String s : misses.keySet())
			System.out.println(s + " : " + misses.get(s));
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
		this.origQuery = query;
		this.results = results;
		
		// Process the results
		createNormalizedMaps();
		
		return "";
	}

}
