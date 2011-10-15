package com.captchatrader;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ParseException;
import android.util.Base64;
import android.util.Log;

public class CaptchaTrader {
	
	private String TAG = CaptchaTrader.class.getSimpleName();
	
	private String ct_username;
	private String ct_password;
	
	private int current_ticket = -1;

	public CaptchaTrader(String username, String password) {
		/**
		 * class encapsulates the communication to captchatrader.com
		 * 
		 * @author Philipp Böhm
		 * @param username CT accountname
		 * @param password CT accoundpassword
		 */
		ct_username = username;
		ct_password = password;
	}
	
	public Bitmap getCaptcha () {
		/**
		 * adds the user to the queue and waits for a new captcha
		 * 
		 * @author Philipp Böhm
		 * @return A Captcha as a Bitmap
		 */
		Bitmap bmp = null;
		try {
			URL url = new URL("http://api.captchatrader.com/enqueue/username:" + 
					 ct_username + "/password:" + ct_password + "/");
			
			JSONArray arr = talkToCT(url);
			
			// get ticket number or throw an exception
    		int ticketid = arr.getInt(0);
			if (ticketid == -1) throw new Exception(arr.getString(1)); //Error aufgetreten
			current_ticket = ticketid;
    		
			// extract the base64 coded image and make a Bitmap
			String base64 = arr.getString(1);
    		String[] parts = base64.split(",");
    		String codedimage = parts[1];
    		
    		byte[] decodedImage = Base64.decode(codedimage, Base64.DEFAULT);
    		bmp = BitmapFactory.decodeByteArray(decodedImage, 0, decodedImage.length);
			
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		return bmp;
	}
	
	public int getCredits () {
		/**
		 * get the number of earned credits
		 * 
		 * @author Philipp Böhm
		 * @return Credits
		 */
		int Credits = 0;
		try {
			URL url = new URL("http://api.captchatrader.com/get_credits/username:" + 
					 ct_username + "/password:" + ct_password + "/");
			
			JSONArray arr = talkToCT(url);
			
			if (arr.getInt(0) == 0) {
				Credits = arr.getInt(1);
			}
			
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		return Credits;
	}
	
	public boolean answerCaptcha(String answer) {
		/**
		 * sends the answer of a captcha to CT
		 * 
		 * @author philipp
		 * @param answer Captchacode
		 * @return true if request is successful
		 */
		boolean success = false;
		if (current_ticket > 0) {
			
			//instantiate HTTP-Client 
			HttpClient httpclient = new DefaultHttpClient();
		    HttpPost httppost = new HttpPost("http://api.captchatrader.com/answer/");

		    try {
		        // build parameters
		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("ticket", Integer.toString(current_ticket)));
		        nameValuePairs.add(new BasicNameValuePair("username", ct_username));
		        nameValuePairs.add(new BasicNameValuePair("password", ct_password));
		        nameValuePairs.add(new BasicNameValuePair("value", answer));
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		        // send a POST and check the result
		        HttpResponse response = httpclient.execute(httppost);
		        
		        String responsebody = getResponseBody(response);
		        JSONArray arr = new JSONArray(responsebody);
		        
		        if (arr.getInt(0) == 0) success = true;
		        
		    } catch (Exception e) {
		    	Log.e(TAG, e.getMessage());
		    }
		}
		return success;
	}
	
	public void logoutFromCT () {
		/**
		 * say CT to remove the user from the queue
		 * 
		 * @author Philipp Böhm
		 */
		
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://api.captchatrader.com/dequeue/");

	    try {
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("username", ct_username));
	        nameValuePairs.add(new BasicNameValuePair("password", ct_password));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        httpclient.execute(httppost);
	        
	    } catch (Exception e) {
	    	Log.e(TAG, e.getMessage());
	    }
	}
	
	public int getWaitTime () {
		/**
		 * get the time the user has to wait for an new captcha
		 * 
		 * @author Philipp Böhm
		 * @return int seconds
		 */
		int time = 0;
		try {
			URL url = new URL("http://api.captchatrader.com/get_wait_time/username:" + 
					 ct_username + "/password:" + ct_password + "/");
	
			JSONArray arr = talkToCT(url);
			time = arr.getInt(2);
			
		} catch (Exception e) {
			Log.e(TAG, e.getMessage());
		}
		
		return time;
	}
	
	private JSONArray talkToCT ( URL url ) throws Exception {
		/**
		 * utility method for opening a specified URL and returning the response 
		 * as an JSON Array
		 * 
		 * @author Philipp Böhm
		 * @param url specific URL at CaptchaTrader
		 * @return JSONArray
		 */
		
		// sending the request and read the respnse
		URLConnection urlConnection = url.openConnection();
		InputStreamReader inreader = new InputStreamReader(urlConnection.getInputStream());
		BufferedReader reader = new BufferedReader(inreader);

		// parse JSON
		String response = reader.readLine();
		JSONArray arr = new JSONArray(response);
		return arr;
	}
	
	/**
	 * the following methods are used to extract the body from an HTTPResponse object,
	 * because it is not possible with the standard class library
	 * 
	 * @HACK
	 * @from http://thinkandroid.wordpress.com/2009/12/30/getting-response-body-of-httpresponse/
	 */
	public static String getResponseBody(HttpResponse response) {

		String response_text = null;
		HttpEntity entity = null;

		try {
			entity = response.getEntity();
			response_text = _getResponseBody(entity);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e1) {
				}
			}
		}
		return response_text;
	}
	
	public static String _getResponseBody(final HttpEntity entity) throws IOException, ParseException {

		if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
		
		InputStream instream = entity.getContent();

		if (instream == null) { return ""; }
		if (entity.getContentLength() > Integer.MAX_VALUE) { throw new IllegalArgumentException(
				"HTTP entity too large to be buffered in memory"); }

		String charset = getContentCharSet(entity);
		if (charset == null) {
			charset = HTTP.DEFAULT_CONTENT_CHARSET;
		}

		Reader reader = new InputStreamReader(instream, charset);
		StringBuilder buffer = new StringBuilder();

		try {
			char[] tmp = new char[1024];
			int l;

			while ((l = reader.read(tmp)) != -1) {
				buffer.append(tmp, 0, l);
			}

		} finally {
			reader.close();
		}
		return buffer.toString();
	}

	public static String getContentCharSet(final HttpEntity entity) throws ParseException {

		if (entity == null) { throw new IllegalArgumentException("HTTP entity may not be null"); }
		String charset = null;

		if (entity.getContentType() != null) {

			HeaderElement values[] = entity.getContentType().getElements();
			if (values.length > 0) {
				NameValuePair param = values[0].getParameterByName("charset");
	
				if (param != null) {
					charset = param.getValue();
				}
			}
		}
		return charset;
	}
}
