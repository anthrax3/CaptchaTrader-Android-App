package com.captchatrader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.*;
import android.widget.*;

public class CaptchaTraderActivity extends Activity {
	/**
	 * @author Philipp Böhm
	 * 
	 * main activity for the CaptchaTrader-App
	 */

    private EditText captcha_text;
	private ImageView captcha_image;
	
	private String ct_username;
	private String ct_password;
	private boolean should_vibrate;
	private boolean auto_get_captchas;
	
	private Button btn_get_captcha;
	private Button btn_submit;

	private CaptchaTrader trader;
	private ProgressDialog dialog;
	private Vibrator vibrator; 
	

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        captcha_text = (EditText) findViewById(R.id.txt_captchacode);
        captcha_image = (ImageView) findViewById(R.id.img_captcha);
        btn_get_captcha = (Button) findViewById(R.id.btn_get_captcha);
        btn_submit = (Button) findViewById(R.id.btn_submit);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        //dialog for showing the ETA time
        dialog = new ProgressDialog(CaptchaTraderActivity.this);
        dialog.setIndeterminate(true);
        
    }
	
	@Override
	protected void onResume() {		
		super.onResume();
		
        /**
         * network check
         */
        if (! isOnline()) {
        	Toast t = Toast.makeText(
        				getApplicationContext(), 
        				getResources().getString(R.string.no_internet_connection), 
        				Toast.LENGTH_LONG);
        	t.setGravity(Gravity.TOP|Gravity.CENTER, 0, 60);
        	t.show();
        	btn_get_captcha.setEnabled(false);
        	return;
        }
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		// vibrate if there is a new Captcha to solve
		should_vibrate = prefs.getBoolean("should_vibrate", false);
		auto_get_captchas = prefs.getBoolean("auto_get_captchas", false);
		
		// get account data		
		String username = prefs.getString("ct_username", "");
		if (! username.equals("")) ct_username = username;
			
		String password = prefs.getString("ct_password", "");
		if (! password.equals("")) ct_password = password;
		
		//get an instance if there are suitable credentials
		if ( ct_username != null && 
				ct_password != null && 
				  ! ct_username.equals("") && 
				    ! ct_password.equals("")) {
			
			btn_get_captcha.setEnabled(true);
			trader = new CaptchaTrader(ct_username, ct_password);
			
			Toast t = Toast.makeText(
						getApplicationContext(), 
						String.format(
								getResources().getString(R.string.credit_number), 
								trader.getCredits() ), 
						Toast.LENGTH_LONG);
			t.setGravity(Gravity.TOP|Gravity.CENTER, 0, 60);
			t.show();
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		if (trader != null) trader.logoutFromCT();
	}

	public void buttonClick(View v) {
		
    	switch (v.getId()) {
    	case R.id.btn_get_captcha:
    		/**
    		 * getCaptcha-Button
    		 */
    		
    		String message = String.format(
    					getResources().getString(R.string.wait_for_captcha), 
    					trader.getWaitTime());
			dialog.setMessage(message);
    		dialog.show();
    		
			captcha_text.setEnabled(true);
    		btn_submit.setEnabled(true);
    		btn_get_captcha.setEnabled(false);
			
			/**
			 * The task of getting the next Captcha is performed in a separate Thread
			 * so that the UI-Thread is not blocked.
			 * A Handler is used to get access to the UI elements from the UI thread, 
			 * because modifying Activities and Views is only allowed from inside the 
			 * UI-Thread. With a Handler instance you the Thread will get access to the 
			 * Message Queue of the UI thread and is able to change views .... 
			 */
			final Handler h = new Handler();
			Thread t = new Thread(new Runnable() {
				
				public void run() {
					
					final Bitmap captcha = trader.getCaptcha();
					
					if (captcha != null) {
						/**
						 *  add the Bitmap to the ImageView and interrupt the dialog
						 */
						h.post(new Runnable() {
							public void run() {
								captcha_image.setImageBitmap(captcha);
								if (should_vibrate) vibrator.vibrate(1000);
								dialog.dismiss();
							}
						});
	        		}
				}
			});
			t.start();   				
    		break;
    		
		case R.id.btn_submit:
			/**
			 * Submit-Button for the answer
			 */
			if (! captcha_text.getText().equals("")) {
				
				/**
				 * send answer
				 */
				final String answer = captcha_text.getText().toString();
				Thread thread = new Thread(new Runnable() {
					
					public void run() {
						trader.answerCaptcha(answer);
					}
				});
				thread.start();
				
				/**
				 * prepare next process of getting captchas
				 */
				captcha_text.setText("");
				captcha_text.setEnabled(false);
				btn_submit.setEnabled(false);
				btn_get_captcha.setEnabled(true);
				
				// getting Captchas in a loop if the user wants it
				if (auto_get_captchas) btn_get_captcha.performClick();
			}
			break;
			
		default:
			break;
		}
	}
	
	public boolean isOnline() {
		/**
		 * check for an internet connection
		 */
	    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnected()) {
	        return true;
	    }
	    return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.optionsmenu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.option_preferences:
			startActivity(new Intent(this, PreferenceActivity.class));
			return true;
		case R.id.option_quit:
			trader.logoutFromCT();
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}	

}