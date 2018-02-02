package microsoft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.parser.JSONParser;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class LUIS {
	// Update the host if your LUIS subscription is not in the West US region
	final static String LUIS_BASE = "https://westus.api.cognitive.microsoft.com";
	final static String LUIS_APP = "/luis/api/v2.0/apps/{app_id}";
	final static String LUIS_VERSION = "/versions/{app_version}";

	// Enter information about your LUIS application and key below
	static final String APP_ID = "3d8619f4-43ba-4032-9be8-582909b2aae6";
	static final String APP_VERSION = "0.1";
	static final String LUIS_PROGRAMMATIC_ID = "0f03533f6b4e4e5b9d6dc33ef75659a4";

	// File names for utterance and result files
	static final String INPUT_UTTERANCE_ADD = "microsoft/utterances/add/input/0.json";
	static final String OUTPUT_UTTERANCES_ADD = "microsoft/utterances/add/output/0.json";
	static final String OUTPUT_UTTERANCES_GET = "microsoft/utterances/get/output.json";

	static final String UTF8 = "UTF-8";

	// endpoint method names
	final String TRAIN = "/train";
	final static String EXAMPLES = "/examples";
	final static String EXAMPLE = "/example";
	final static String APP_INFO = "/";
	static String path;
	static HttpClient httpclient;

	public static void initialize() {
		new File("microsoft/utterances/add/input").mkdirs();
		new File("microsoft/utterances/add/output").mkdirs();
		new File("microsoft/utterances/get").mkdirs();

		path = LUIS_BASE + LUIS_APP.replace("{app_id}", APP_ID) + LUIS_VERSION.replace("{app_version}", APP_VERSION);

		httpclient = HttpClients.createDefault();
	}

	public static void getUtterances() throws URISyntaxException, ClientProtocolException, IOException {
		URIBuilder builder = new URIBuilder(path + EXAMPLES);

		URI uri = builder.build();
		HttpGet request = new HttpGet(uri);
		request.setHeader("Ocp-Apim-Subscription-Key", LUIS_PROGRAMMATIC_ID);
		request.setHeader("Content-Type", MediaType.APPLICATION_JSON);

		HttpResponse response = httpclient.execute(request);
		HttpEntity httpEntityResponse = response.getEntity();

		System.out.println(response.getStatusLine());

		if (httpEntityResponse != null) {
			String entityResponse = EntityUtils.toString(httpEntityResponse);

			File file = new File(OUTPUT_UTTERANCES_GET);
			if (!file.exists())
				file.createNewFile();

			try (FileOutputStream stream = new FileOutputStream(file)) {
				entityResponse = pretitffy(entityResponse);

				stream.write(entityResponse.getBytes(UTF8));
				stream.flush();
			}
		}
	}

	public static void addUtterance() throws URISyntaxException, FileNotFoundException, IOException {
		URIBuilder builder = new URIBuilder(path + EXAMPLE);

		URI uri = builder.build();
		HttpPost request = new HttpPost(uri);
		request.setHeader("Ocp-Apim-Subscription-Key", LUIS_PROGRAMMATIC_ID);
		request.setHeader("Content-Type", MediaType.APPLICATION_JSON);

		try (FileInputStream stream = new FileInputStream(INPUT_UTTERANCE_ADD)) {
			String data = new Scanner(stream, UTF8).useDelimiter("\\A").next();
			
			StringEntity reqEntity = new StringEntity(data);
			request.setEntity(reqEntity);

			HttpResponse response = httpclient.execute(request);
			HttpEntity httpEntityResponse = response.getEntity();

			if (httpEntityResponse != null) {
				
				String entityResponse = EntityUtils.toString(httpEntityResponse);

				File file = new File(OUTPUT_UTTERANCES_ADD);
				if (!file.exists())
					file.createNewFile();

				try (FileOutputStream outputStream = new FileOutputStream(file)) {
					entityResponse = pretitffy(entityResponse);

					outputStream.write(entityResponse.getBytes(UTF8));
					outputStream.flush();
				}
			}
		}
	}

	public static void main(String[] args)
			throws IOException, org.json.simple.parser.ParseException, URISyntaxException {
		initialize();
		getUtterances();
		addUtterance();
	}

	public static String pretitffy(String entityResponse) {
		JsonElement jsonResponse;
		try {
			jsonResponse = new JsonParser().parse(entityResponse);
		} catch (JsonSyntaxException ex) {
			jsonResponse = new JsonParser().parse("{ \"message\": \"Invalid JSON response\" }");
		}
		return new GsonBuilder().setPrettyPrinting().create().toJson(jsonResponse);
	}
}