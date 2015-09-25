package bingboost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BingBoost {
	// Initial parameters
	String origQuery;
	Result[] results;
	
	// Normalized frequency for terms
	ArrayList<Map<String, Float>> matches;
	ArrayList<Map<String, Float>> misses;
	
	public BingBoost(String query, Result[] results) {
		this.origQuery = query;
		this.results = results;
		matches = new ArrayList<Map<String, Float>>();
		misses = new ArrayList<Map<String, Float>>();
	}
	
	Map<String, Float> normalizedMap(Result r) {
		int n = 0;
		Map<String, Float> map = new HashMap<String, Float>();

		// Build up map with absolute counts
		for (String s : r.description.split("\\W+")) {
			n++;
			if (map.get(s) != null)
				map.put(s, map.get(s) + 1);
			else
				map.put(s, 1f);
		}
		
		/*
		 * I have a feeling we'll also want to include words in the title, and potentially give them 
		 * a bigger weight than words in the description. For now, we can just get the basics working.
		 */
		
		// Normalize map counts
		for (String s : map.keySet()) 
			map.put(s, map.get(s) / n);
		
		// Testing purposes
		for (String s : map.keySet())
			System.out.println(s + " : " + map.get(s));
		
		return map;
	}
	
	private void createNormalizedMaps() {
		for (Result r : results) {
			System.out.println("Calling normalize map for " + r.title);
			Map<String, Float> map = normalizedMap(r);
			if (r.relevant > 0)
				matches.add(map);
			else
				misses.add(map);
		}
	}
	
	public String updatedQueryForFeedback() {
		createNormalizedMaps();
		return "";
	}

}
