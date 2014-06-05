package com.cooltey.qboncamera;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class StartActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set the fullscreen mode
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		
		setContentView(R.layout.activity_start);
				
		Handler handler = new Handler();
		
		handler.postDelayed(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub
				
				// going into the app
				Intent intent = new Intent().setClass(
						StartActivity.this,
						MainActivity.class);
	        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				
				// end this activity
				finish();
			}}, 1500);

		
		//getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		//getSupportActionBar().setHomeButtonEnabled(false);
	}
}
