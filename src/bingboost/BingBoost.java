package bingboost;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BingBoost {
	// Initial parameters
	String origQuery;
	Result[] results;
	Map<String, Boolean> stopwords;
	
	// Normalized frequency for terms
	ArrayList<Map<String, Float>> matches;
	ArrayList<Map<String, Float>> misses;
	
	public BingBoost() throws FileNotFoundException, IOException {
		stopwords = readStopwords();
		matches = new ArrayList<Map<String, Float>>();
		misses = new ArrayList<Map<String, Float>>();
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
	
	private Map<String, Float> normalizedMap(Result r) {
		int n = 0;
		Map<String, Float> map = new HashMap<String, Float>();

		// Build up map with absolute counts
		for (String s : r.description.split("\\W+")) {
			if (stopwords.containsKey(s))
				continue;
			
			n++;
			if (map.get(s) != null)
				map.put(s, map.get(s) + 1);
			else
				map.put(s, 1f);
		}
		
		// Normalize map counts
		for (String s : map.keySet()) 
			map.put(s, map.get(s) / n);
		
		return map;
	}
	
	private void createNormalizedMaps() {
		for (Result r : results) {
			Map<String, Float> map = normalizedMap(r);
			if (r.relevant > 0)
				matches.add(map);
			else
				misses.add(map);
		}
	}
	
	public String updatedQueryForFeedback(String query, Result[] results) {
		this.origQuery = query;
		this.results = results;
		createNormalizedMaps();
		return "";
	}

}
