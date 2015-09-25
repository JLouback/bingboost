package bingboost;

/*
 * A Result is an individual item returned by a query. It will contain information
 * deemed important enough to include in our algorithms to find more relevant keywords.
 */

public class Result {
	
	String title;
	String description;
	int relevant;
	
	public Result(String title, String description, int relevant) {
		this.title = title;
		this.description = description;
		this.relevant = relevant;
	}
}
