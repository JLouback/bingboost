package bingboost;

import java.io.*;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Interaction {
	
	private float precision;
	private String query;
	private int k;
	private Result[] results;
	
	public Interaction(float precision, String query) {
		this.precision = precision;
		this.query = query;
		this.k = 10;
		results = new Result[k];
	}
	
	public float currentPrecision() {
		int relevant = 0;
		for (int i=0; i< results.length; i++) {
			relevant += results[i].relevant;
		}
		return (float) relevant / (float) k;
	}
	
	public String exitMessage() {
		float current = currentPrecision();
		if (current == 0) {
			return "Search engine has failed. Boooo.";
		}
		return "Search engine reached precision level. Yay.";
	}
	
	public void queryAndCollectFeedback() {
		Scanner in = new Scanner(System.in);
		try {
			JSONArray jsonResults = Utils.queryBing(query);
			for (int i=0; i<k; i++) {
				System.out.println("------------------------------------------------------------");
				JSONObject json = jsonResults.getJSONObject(i);
				
				System.out.println(json.get("Title"));
				System.out.println(json.get("Description"));
				System.out.println(json.get("Url"));
				System.out.println("Enter 1 to mark as relevant, 0 for non-relevant:");
				
				results[i] = new Result(json.getString("Title"), json.getString("Description"), in.nextInt());
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		
		for (int i = 0; i < k; i++) {
			Result r = results[i];
			System.out.println(r.title + " " + r.description + " " + r.relevant);
		}
	}
	
	public void runBingboost() {
		do {
			// Run bingboost to modify query, should receive current query, descriptions and feedback arrays.
			queryAndCollectFeedback();
			BingBoost boost = new BingBoost(query, results);
			query = boost.updatedQueryForFeedback();
		} while (currentPrecision() < precision && currentPrecision() > 0);
		System.out.println(exitMessage());
	}
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter query:");
		String query = in.nextLine();
		System.out.println("Enter precision, between 0 and 1");
		float precision = in.nextFloat();
		Interaction interaction = new Interaction(precision, query);
		interaction.runBingboost();	
		in.close();
	}
}
