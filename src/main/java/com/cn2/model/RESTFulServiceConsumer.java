 package com.cn2.model;
 /**
  * @author hyang
  *
  */
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


@Service
public class RESTFulServiceConsumer {
	

	
	public static  String getCn2loginToken(String loginHost, String username, String password) throws JSONException

	{

		RestTemplate restTemplate = new RestTemplate();
		String accessToken = null;
		// create request body
		JSONObject request = new JSONObject();

		request.put("username", username);
		request.put("password", password);

		// set headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

		// send request and parse result
		String url=loginHost+"/api/v2/oauth/token";
		System.out.println("restUrl : " + url);
		ResponseEntity<String> loginResponse = restTemplate.exchange(url, HttpMethod.POST,
				entity, String.class);
		if (loginResponse.getStatusCode() == HttpStatus.OK) {

			JSONObject userJson = new JSONObject(loginResponse.getBody());
			accessToken = userJson.get("accessToken").toString();

		} else if (loginResponse.getStatusCode() == HttpStatus.UNAUTHORIZED) {
			// nono... bad credentials

		}
		return accessToken;

	}
	

	public static void processCn2DataForSubmissionDetail(String loginHost,String voHost, String token, String voToken,String keyId,String porjectId, String className, String dateName,
			Integer postponeMins,Integer minutes,Integer limit) throws JSONException

	{

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/data/" + porjectId)
				.path("/classes/" + className).queryParam("dateName", dateName).queryParam("postponeMins", postponeMins).queryParam("minutes", minutes).queryParam("limit", limit);
		
		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject();
		// request.put("x-alchemy-session-token", token);

		// set headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-alchemy-session-token", token);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET,
				entity, String.class);	
		JSONObject dataJson = new JSONObject(loginResponse.getBody());		
		JSONArray array = new JSONArray();
		if (dataJson.has("elements")) {
			array = dataJson.getJSONArray("elements");
			System.out.println("elements length is "+array.length());
			if (array != null && array.length() > 0) {

				for (int i = 0; i < array.length(); i++) {

					String objId = array.getJSONObject(i).getString("objectId");
					String data = array.getJSONObject(i).getString("data");
					JSONObject dataDetailJson = new JSONObject(data);
					String postData = getSubmissionDetailJson(dataDetailJson,keyId);
					if (postData == null)
						continue;
					String voResponse = postVODataForSubmissionDetail(voHost, voToken, postData);
					String voResponseJson=removeQuotesFromStartAndEndOfString(voResponse);
					String vo_id = getVOresponse(voResponseJson);
					if (vo_id != null) {
						String submissionDetailIdData = getVODetailId("submissionDetail", vo_id);
						postCn2Data(loginHost, token, porjectId, className, objId, submissionDetailIdData);
					}
				}
			}
		}
	}
	

	
	private static String removeQuotesFromStartAndEndOfString(String inputStr) {

	    String result = inputStr;

	    int firstQuote = inputStr.indexOf('\"');

	    int lastQuote = result.lastIndexOf('\"');

	    int strLength = inputStr.length();

	    if (firstQuote == 0 && lastQuote == strLength - 1) {

	        result = result.substring(1, strLength - 1);

	    }
    String finalString=    result.replace("\\","");
	    return finalString;

	}
	
	
	private static String getVODetailId(String voi_id_name, String vo_id) throws JSONException {
		JSONObject request = new JSONObject();
		request.put(voi_id_name, vo_id);
		return request.toString();
	}
	
	
	
	private static String getSubmissionDetailJson(JSONObject dataDetailJson,String keyId) throws JSONException

	{
		if(hasSubmissionDetailId(dataDetailJson))
			return null;
		
		CN2Data cn2Data = getCn2Data(dataDetailJson);
		JSONObject submissionDetailJson = new JSONObject();
		submissionDetailJson.put("firstName", cn2Data.getFirstName());
		submissionDetailJson.put("lastName", cn2Data.getLastName());
		submissionDetailJson.put("email", cn2Data.getEmail());
		if(StringUtils.isEmpty(cn2Data.getCountry()))
			submissionDetailJson.put("country", "USA");
		else
		submissionDetailJson.put("country", cn2Data.getCountry());
		submissionDetailJson.put("zip", cn2Data.getZip_code());
		submissionDetailJson.put("formType", "App");		
		submissionDetailJson.put("requestDate", getDateFormat1());
		submissionDetailJson.put("keyId", keyId);
		submissionDetailJson.put("newsletter", "true");
		submissionDetailJson.put("appUUID", cn2Data.getUser_id());
		submissionDetailJson.put("languagePreference", "ENG");
		submissionDetailJson.put("appProfile", cn2Data.getProfile_type());
		submissionDetailJson.put("appContestEntry", cn2Data.getSweepstakes_entered());
		submissionDetailJson.put("appDeviceType", cn2Data.getDevice_type());

		return submissionDetailJson.toString();
	}
	
	private static String getDateFormat1()
	{
		Date today = new Date();
		SimpleDateFormat ft = new SimpleDateFormat("YYYY-MM-dd");
		String dateString=ft.format(today);
		return dateString;
	}
	
	
	private static String getDateFormat(String timeStamp)
	{
	    String dateString=	StringUtils.substring(timeStamp, 0, 10);
		return dateString;
	}
	
	
	private static boolean hasSubmissionDetailId(JSONObject dataDetailJson)
	{
		
		if(dataDetailJson.has("submissionDetail"))
			  return true;
		else return false;
	}
	
	private static CN2Data getCn2Data(JSONObject dataDetailJson) throws JSONException
	
	{
		
		CN2Data cn2Data= new CN2Data();
		if(dataDetailJson.has("device_type"))
			cn2Data.setDevice_type(dataDetailJson.getString("device_type"));	
		if(dataDetailJson.has("zip_code"))
			cn2Data.setZip_code(dataDetailJson.getString("zip_code"));	
		if(dataDetailJson.has("profile_type"))
		{
			String profile_type=dataDetailJson.getString("profile_type");
			String profile_string="None";
			if ( profile_type == "adults_without_kids" ) {
	              profile_string = "Adults Without Kids";
	            } else if ( profile_type == "family_with_kids" ) {
	              profile_string = "Family With Kids";
	            } else if ( profile_type == "business" ) {
	              profile_string = "Business";
	            } 			
			cn2Data.setProfile_type(profile_string);	
		}
		if(dataDetailJson.has("location_services"))
			cn2Data.setLocation_services(dataDetailJson.getString("location_services"));
		if(dataDetailJson.has("email"))
			cn2Data.setEmail(dataDetailJson.getString("email"));	
		if(dataDetailJson.has("account_type"))
			cn2Data.setAccount_type(dataDetailJson.getString("account_type"));	
		if(dataDetailJson.has("push_notifications"))
			cn2Data.setPush_notifications(dataDetailJson.getString("push_notifications"));	
		if(dataDetailJson.has("firstName"))
			cn2Data.setFirstName(dataDetailJson.getString("firstName"));
		if(dataDetailJson.has("camera_access"))
			cn2Data.setCamera_access(dataDetailJson.getString("camera_access"));	
		if(dataDetailJson.has("country"))
			cn2Data.setCountry(dataDetailJson.getString("country"));	
		if(dataDetailJson.has("device_id"))
			cn2Data.setDevice_id(dataDetailJson.getString("device_id"));
		if(dataDetailJson.has("user_id"))
			cn2Data.setUser_id(dataDetailJson.getString("user_id"));
		if(dataDetailJson.has("client"))
			cn2Data.setClient(dataDetailJson.getString("client"));	
		if(dataDetailJson.has("lastName"))
			cn2Data.setLastName(dataDetailJson.getString("lastName"));	
		if(dataDetailJson.has("sweepstakes_entered"))
			cn2Data.setSweepstakes_entered(dataDetailJson.getString("sweepstakes_entered"));	
		if(dataDetailJson.has("leadRecord"))
			cn2Data.setLeadRecord(dataDetailJson.getString("leadRecord"));	
			
		return cn2Data;
	}
	
	
	
	public static void processCn2DataForLead(String loginHost, String voHost, String token, String voToken,String porjectId, String className, String dateName,
			Integer postponeMins,Integer minutes,Integer limit) throws JSONException

	{

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/data/" + porjectId)
				.path("/classes/" + className).queryParam("dateName", dateName).queryParam("postponeMins", postponeMins).queryParam("minutes", minutes).queryParam("limit", limit);
		
		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject();
		// request.put("x-alchemy-session-token", token);

		// set headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-alchemy-session-token", token);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET,
				entity, String.class);

		JSONObject dataJson = new JSONObject(loginResponse.getBody());
		JSONArray array = new JSONArray();
		if (dataJson.has("elements")) {
			array = dataJson.getJSONArray("elements");
			 System.out.println("elements length is "+array.length());
			if (array != null && array.length() > 0) {
				for (int i = 0; i < array.length(); i++) {

					String objId = array.getJSONObject(i).getString("objectId");
					String data = array.getJSONObject(i).getString("data");
					JSONObject dataDetailJson = new JSONObject(data);
					String postData = getLeadJson(dataDetailJson);
					if (postData == null)
						continue;
					String voResponse = postVODataForLeadDetail(voHost, voToken, postData);					
					String voResponseJson=removeQuotesFromStartAndEndOfString(voResponse);
					String vo_id = getVOresponse(voResponseJson);					
					if (vo_id != null) {
						String leadIdData = getVODetailId("leadRecord", vo_id);
						postCn2Data(loginHost, token, porjectId, className, objId, leadIdData);
					}
				}
			}
		}
	}
	
	
	private static boolean hasLeadId(JSONObject dataDetailJson) {

		if (dataDetailJson.has("submissionDetail")) {
			if (dataDetailJson.has("leadRecord"))
				return true;
			else
				return false;

		} else
			return true;
	}
	
	private static String getLeadJson(JSONObject dataDetailJson) throws JSONException

	{
		if(hasLeadId(dataDetailJson))
			return null;		
		CN2Data cn2Data = getCn2Data(dataDetailJson);		
		JSONObject leadJson = new JSONObject();		
		leadJson.put("lastName", cn2Data.getLastName());		
		leadJson.put("appUUID", cn2Data.getUser_id());
		leadJson.put("leadType", "Consumer");
		leadJson.put("cameraEnabled", cn2Data.getCamera_access());
		leadJson.put("locationEnabled", cn2Data.getLocation_services());
		leadJson.put("mobilePushEnabled", cn2Data.getPush_notifications());
		return leadJson.toString();
	}
	
	public static   void processCn2DataForConsumerRequest(String loginHost, String voHost, String token, String voToken,String porjectId, String className, String dateName,
			Integer postponeMins,Integer minutes,Integer limit) throws JSONException

	{

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/data/" + porjectId)
				.path("/classes/" + className).queryParam("dateName", dateName).queryParam("postponeMins", postponeMins).queryParam("minutes", minutes).queryParam("limit", limit);
		
		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject();
		// request.put("x-alchemy-session-token", token);

		// set headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-alchemy-session-token", token);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET,
				entity, String.class);
		JSONObject dataJson = new JSONObject(loginResponse.getBody());

		JSONArray array = new JSONArray();
		if (dataJson.has("elements")) {
			array = dataJson.getJSONArray("elements");
			  System.out.println("elements length is "+array.length());
			if (array != null && array.length() > 0) {
				
				for (int i = 0; i < array.length(); i++) {

					String data = array.getJSONObject(i).getString("data");
					JSONObject dataDetailJson = new JSONObject(data);
					String objId = array.getJSONObject(i).getString("objectId");
										
					if (dataDetailJson.has("leadRecord")) {
						
					CN2Data cn2Data=getCn2Data( dataDetailJson);				
						if (dataDetailJson.has("completions")) {
							JSONArray completionsArray = dataDetailJson.getJSONArray("completions");
							if (completionsArray != null && completionsArray.length() > 0) {
								Boolean success=false;
								for (int j = 0; j < completionsArray.length(); j++) {
									System.out.println(completionsArray.getJSONObject(j).toString());
									JSONObject completionJson =completionsArray.getJSONObject(j);
									if (completionJson.has("consumerRequest")) 
								    	continue;
									String postData = getConsumerRequestJson(completionJson,cn2Data);
									String voResponse = postVODataForConsumerRequest(voHost, voToken, postData);
									String voResponseJson=removeQuotesFromStartAndEndOfString(voResponse);
									String vo_id = getVOresponse(voResponseJson);																		
									if (vo_id != null) 		
									{
									completionJson.put("consumerRequest", vo_id);
									success=true;
									}
								}		
								if(success){
						      		JSONObject compJson = new JSONObject();
							    	compJson.put("completions", completionsArray);
							    	postCn2Data(loginHost, token, porjectId, className, objId, compJson.toString());
								}
							}
						}
						if (dataDetailJson.has("devices")) {
							JSONArray devicesArray = dataDetailJson.getJSONArray("devices");
							if (devicesArray != null && devicesArray.length() > 0) {
								Boolean success=false;
								for (int j = 0; j < devicesArray.length(); j++) {
								//	System.out.println(completionsArray.getJSONObject(j).toString());
									JSONObject deviceJson =devicesArray.getJSONObject(j);
									if (deviceJson.has("consumerRequest")) 							
									{
										if(deviceJson.has("updated"))
										{
									      String updateFlag=deviceJson.getString("updated");
									      if(updateFlag.equalsIgnoreCase("false"))
								         	continue;
										}
									}
									String postData = getConsumerRequestDeviceJson(deviceJson,cn2Data);
									String voResponse = postVODataForConsumerRequest(voHost, voToken, postData);
									String voResponseJson=removeQuotesFromStartAndEndOfString(voResponse);
									String vo_id = getVOresponse(voResponseJson);																		
									if (vo_id != null) 		
									{
									deviceJson.put("consumerRequest", vo_id);
									deviceJson.put("updated", "false");
									success=true;
									}
								}		
								if(success){
						      		JSONObject compJson = new JSONObject();
							    	compJson.put("devices", devicesArray);
							    	postCn2Data(loginHost, token, porjectId, className, objId, compJson.toString());
								}
							}
						}
					}
				}
			}
		}
	}
	

	private static String getConsumerRequestJson(JSONObject completionJson,CN2Data cn2Data) throws JSONException

	{
			
		Completion completion = getCompletionData(completionJson);		
		JSONObject postJson = new JSONObject();	
		String name = cn2Data.getLastName()+","+cn2Data.getFirstName()+"-"+completion.getType();
		postJson.put("name", name);	
		postJson.put("consumer", cn2Data.getLeadRecord());	
		postJson.put("completionDate", completion.getDate());
		postJson.put("languagePreference", "ENG");
		postJson.put("lat", completion.getLatitude());
		postJson.put("llong", completion.getLongitude());
		postJson.put("appShareActType", "cn2");
		if(completion.getType().equalsIgnoreCase("orb"))
			postJson.put("appShareType", "orb game");
		else
		postJson.put("appShareType", completion.getType());		
		return postJson.toString();
	}
	
	private static String getConsumerRequestDeviceJson(JSONObject deviceJson,CN2Data cn2Data) throws JSONException

	{
			
		Device device = getDeviceData(deviceJson);		
		JSONObject postJson = new JSONObject();	
		String name = cn2Data.getLastName()+","+cn2Data.getFirstName()+"-"+"App";
		postJson.put("name", name);	
		postJson.put("consumer", cn2Data.getLeadRecord());
		if(StringUtils.isNoneBlank(device.getDate()))
		postJson.put("completionDate", getDateFormat(device.getDate()));
		else
		postJson.put("completionDate", getDateFormat1());	
		postJson.put("appDeviceType", device.getType());	
		postJson.put("appDeviceId", device.getId());
		postJson.put("cameraEnabled", device.getCameraAccess());
		postJson.put("locationEnabled", device.getLocationServices());	
		postJson.put("mobilePushEnabled", device.getPushNotifications());
		return postJson.toString();
	}
	
	
	private static Completion getCompletionData(JSONObject completionJson) throws JSONException
	
	{
		
		Completion completion= new Completion();
		
		if(completionJson.has("code"))
			completion.setCode(completionJson.getString("code"));	
		if(completionJson.has("date"))
			completion.setDate(completionJson.getString("date"));	
		if(completionJson.has("latitude"))
			completion.setLatitude(completionJson.getString("latitude"));	
		if(completionJson.has("longitude"))
			completion.setLongitude(completionJson.getString("longitude"));			
		if(completionJson.has("name"))
			completion.setName(completionJson.getString("name"));	
		if(completionJson.has("type"))
			completion.setType(completionJson.getString("type"));	
		if(completionJson.has("share_type"))
			completion.setShareType(completionJson.getString("share_type"));	
	
		return completion;
	}

	private static Device getDeviceData(JSONObject deviceJson) throws JSONException
	
	{
		
		Device device= new Device();
		
		if(deviceJson.has("id"))
			device.setId(deviceJson.getString("id"));	
		if(deviceJson.has("location_services"))
			device.setLocationServices(deviceJson.getString("location_services"));	
		if(deviceJson.has("camera_access"))
			device.setCameraAccess(deviceJson.getString("camera_access"));	
		if(deviceJson.has("push_notifications"))
			device.setPushNotifications(deviceJson.getString("push_notifications"));	
		if(deviceJson.has("type"))
			device.setType(deviceJson.getString("type"));	
		if(deviceJson.has("updated"))
			device.setUpdated(deviceJson.getString("updated"));	
		if(deviceJson.has("date"))
			device.setUpdated(deviceJson.getString("date"));	
		return device;
	}
	  
	public static  void postCn2Data(String loginHost, String token, String porjectId, String className, String objId,String postData) throws JSONException

	{

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/data/" + porjectId)
				.path("/classes/" + className).path("/" + objId);

		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject(postData);	
		// set headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("x-alchemy-session-token", token);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST,
				entity, String.class);		
	}

	public static   String postVODataForSubmissionDetail(String loginHost, String token,String postData) throws JSONException

	{
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/services/apexrest/SubmissionDetailRestSvc/");

		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject(postData);
		// set headers
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("authorization","Bearer " +token);
		
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST,
				entity, String.class);	
	     return   loginResponse.getBody();
		
	}
	
	private static String getVOresponse(String voResponse){
		System.out.println(voResponse);
		JSONObject voRes=null;
		try {
			voRes = new JSONObject(voResponse);			
			if (voRes.has("RecordId"))
				return voRes.getString("RecordId");
			else
				return null;
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		
	}
	
	public static   String postVODataForLeadDetail(String loginHost, String token,String postData) throws JSONException

	{
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/services/apexrest/LeadRestSvc/");

		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject(postData);
		// set headers
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("authorization","Bearer " +token);
		
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST,
				entity, String.class);	
	     return   loginResponse.getBody();
		
	}
	
	
	public static   String postVODataForConsumerRequest(String loginHost, String token,String postData) throws JSONException

	{
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(loginHost).path("/services/apexrest/ConsumerRequestRestSvc/");

		RestTemplate restTemplate = new RestTemplate();
		// create request body
		JSONObject request = new JSONObject(postData);
		// set headers
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("authorization","Bearer " +token);
		
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);
		
		System.out.println(builder.build().encode().toUri());
		// send request and parse result
		HttpEntity<String> loginResponse = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.POST,
				entity, String.class);	
	     return   loginResponse.getBody();
		
	}
	
	public static  String getAuthSessionProviderToken(String loginHost, String username, String password, String clientId,
			String secret) throws HttpException, IOException, ParseException, JSONException {
		// Set up an HTTP client that makes a connection to REST API.
		DefaultHttpClient client = new DefaultHttpClient();
		HttpParams params = client.getParams();
		HttpClientParams.setCookiePolicy(params, CookiePolicy.RFC_2109);
		params.setParameter(HttpConnectionParams.CONNECTION_TIMEOUT, 30000);

		// Set the SID.
		System.out.println("Logging in as " + username + " in environment " + loginHost);
		String baseUrl = loginHost + "/services/oauth2/token";
		// Send a post request to the OAuth URL.
		HttpPost oauthPost = new HttpPost(baseUrl);
		// The request body must contain these 5 values.
		List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();
		parametersBody.add(new BasicNameValuePair("grant_type", "password"));
		parametersBody.add(new BasicNameValuePair("username", username));
		parametersBody.add(new BasicNameValuePair("password", password));
		parametersBody.add(new BasicNameValuePair("client_id", clientId));
		parametersBody.add(new BasicNameValuePair("client_secret", secret));
		oauthPost.setEntity(new UrlEncodedFormEntity(parametersBody, HTTP.UTF_8));

		// Execute the request.

		HttpResponse response = client.execute(oauthPost);
		int code = response.getStatusLine().getStatusCode();
		JSONObject oauthLoginResponse = new JSONObject(EntityUtils.toString(response.getEntity()));

		String accessToken = oauthLoginResponse.get("access_token").toString();
		System.out.println("accessToken" + accessToken);
		return accessToken;
	}

	
    public static void main(String args[]) {
    		

	
   
    }
 
}