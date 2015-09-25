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
	private int[] feedback;
	private String[] descriptions;
	
	public Interaction(float precision, String query) {
		this.precision = precision;
		this.query = query;
		this.k = 10;
	}
	
	public float currentPrecision() {
		int relevant = 0;
		for (int i=0; i<this.feedback.length; i++) {
			relevant += this.feedback[i];
		}
		return relevant/k;
	}
	
	public String exitMessage() {
		float current = currentPrecision();
		if (current == 0) {
			return "Search engine has failed. Boooo.";
		}
		return "Search engine reached 100% relevancy. Yay.";
	}
	
	public void singleIteration(String query) {
		Scanner in = new Scanner(System.in);
		try {
			JSONArray results = Utils.queryBing(query);
			for (int i=0; i<k; i++) {
				System.out.println("------------------------------------------------------------");
				System.out.println(results.getJSONObject(i).get("Title"));
				System.out.println(results.getJSONObject(i).get("Description"));
				System.out.println(results.getJSONObject(i).get("Url"));
				System.out.println("Enter 1 to mark as relevant, 0 for non-relevant.");
				feedback[i] = in.nextInt();
				descriptions[i] = results.getJSONObject(i).get("Description").toString();
			}
			
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void runBingboost() {
		String currentQuery = query;
		do {
			singleIteration(currentQuery);
			// Run bingboost to modify query, should receive current query, descriptions and feedback arrays.
		} while (currentPrecision() < precision && currentPrecision() > 0);
		System.out.println(exitMessage());
	}
	
	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.println("Enter query");
		String query = in.next();
		System.out.println("Enter precision, between 0 and 1");
		float precision = in.nextFloat();
		Interaction interaction = new Interaction(precision, query);
		interaction.runBingboost();	
	}

}
