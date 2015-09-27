package bingboost;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utils {

	public static JSONArray queryBing(String query) throws IOException {
		query = query.replaceAll(" ", "%20");
        String bingUrlPattern = "https://api.datamarket.azure.com/Bing/Search/Web?Query=%%27%s%%27&$format=JSON";
        String bingUrl = String.format(bingUrlPattern, query);

      	String accountKey = "";
      	byte[] accountKeyBytes = Base64.encodeBase64((accountKey + ":" + accountKey).getBytes());
      	String accountKeyEnc = new String(accountKeyBytes);

      	URL url = new URL(bingUrl);
		URLConnection urlConnection = url.openConnection();
		urlConnection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);

		JSONArray results = null;
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            final JSONObject json = new JSONObject(response.toString());
            results = json.getJSONObject("d").getJSONArray("results");
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}

	public static String getPageContent(String resultUrl) {
        URLConnection connection;
        StringBuilder content = new StringBuilder();
		try {
			URL url = new URL(resultUrl);
			connection = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
	        String line;
	        while ((line = in.readLine()) != null) 
	            content.append(line);

	        in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return content.toString();
	}

	/*
	 * Helper for testing purposes
	 */
	private static void printMap(Map<String, Float> map) {
		for (String s : map.keySet())
			System.out.println(s + " : " + map.get(s));
	}

}