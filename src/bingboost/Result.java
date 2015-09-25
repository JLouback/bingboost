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
	
	/*
	 * Return the description without common words (the, a, that, etc.)
	 * Use a string buffer when creating this new description
	 */
	public String prunedDescription() {
		// While testing, just return the actual description
		return description;
	}
	
	/*
	 * I have a feeling it's going to be useful to weight the words in the title heavier 
	 * than words in the description (say 3 to 1). This can act as a placehold for now.
	 */
	public String prunedTitle() {
		return title;
	}
}
