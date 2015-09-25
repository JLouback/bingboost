package bingboost;

public class BingBoost {
	
	String origQuery;
	Result[] results;
	
	public BingBoost(String query, Result[] results) {
		this.origQuery = query;
		this.results = results;
	}
	
	public String updatedQueryForFeedback() {
		return "";
	}

}
