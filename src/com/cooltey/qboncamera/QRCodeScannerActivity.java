/*
 * Basic no frills app which integrates the ZBar barcode scanner with
 * the camera.
 * 
 * Created by lisah0 on 2012-02-24
 */
package com.cooltey.qboncamera;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.facebook.Session;
import com.hiiir.facebook.plugin.FacebookPluginUtil;
import com.hiiir.qbonsdk.Qbon;
import com.hiiir.qbonsdk.Qbon.FacebookCallback;
import com.hiiir.qbonsdk.Qbon.QbonCloseListener;
import com.hiiir.qbonsdk.net.data.Profile;
import com.hiiir.qbonsdk.net.data.SolomoResponse;
/* Import ZBar Class files */
import com.hiiir.qbonsdk.util.Solomo;
import com.hiiir.qbonsdk.util.Solomo.SolomoResponseBack;

public class QRCodeScannerActivity extends SherlockFragmentActivity{
    private Camera 			mCamera;
    private CameraPreview 	mPreview;
    private Handler 		autoFocusHandler;
    private LinearLayout 	scanControlZone;
    private TextView 		scanText;
    private Button 			scanButton;
    private Button 			saveButton;
    
    private long 			resultCode;

    ImageScanner scanner;

    private boolean barcodeScanned = false;
    private boolean previewing = true;
    
    // get info
 	private String storeToken;
 	private long   storeAccount;
 	private Solomo solomo;
 	private Qbon   qbon;
    private Profile getProfile;
	private String applicationKey;
	private String privateKey;
	
	// geo setting
	private Location getLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
	
	// alert dialog
	private Builder alert;
	private ProgressDialog pdialog;

	private String resultMsg;
	
    static {
        System.loadLibrary("iconv");
    } 

    private String resultPrefix = "http://bit.ly/QbonCamera?code=";
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_qrcode_scan);
        
        applicationKey = getString(R.string.qbon_application_key);
		privateKey     = getString(R.string.qbon_private_key);
		
		qbon		   = new Qbon(QRCodeScannerActivity.this, true, applicationKey, privateKey);
		qbon.setFacebookPlugin(new FacebookPluginUtil());
		qbon.setFacebookCallBack(new FacebookCallback() {
			
			@Override
			public void onFacebookCallback(Activity callbackActivity,
					int callbackRequestCode, int callbackResultCode, Intent callbackData) {
				Session.getActiveSession().onActivityResult(
						callbackActivity, callbackRequestCode, callbackResultCode, callbackData);
			}
		});
		
		
		qbon.setQbonCloseListener(new QbonCloseListener() {
			
			@Override
			public void onClose() {
				//qbon.close();
			}
		});
		
		solomo 		   = new Solomo(QRCodeScannerActivity.this, true, applicationKey, privateKey);
		
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        
        locationInitial();
        
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);
        
        final Handler handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		
        		long 	qbonAccountId = getProfile.getMember().getId();
				String	qbonToken	  = getProfile.getToken();
				
				if(qbonAccountId == 0){
					qbon.open();
					Toast.makeText(getApplicationContext(), getString(R.string.qbon_list_login), 1500).show();
				}else{
					storeAccount 	= qbonAccountId;
	              	storeToken 		= qbonToken;
				}
              	
        	}
        };
        
        new Thread
		(
	        new Runnable() 
			{
	        	@Override
				public void run() 
				{   
	        		// 確認是否登入
					getProfile = qbon.getProfile();
	                handler.sendEmptyMessage(0);
					
				}
			}	
         ).start(); 
        
      		
		alert  = new AlertDialog.Builder(this);
		// progress dialog
    	pdialog = new ProgressDialog(this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
        
        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        FrameLayout preview = (FrameLayout)findViewById(R.id.cameraPreview);
        preview.addView(mPreview);

        scanText 		= (TextView)		findViewById(R.id.scanText);
        scanControlZone = (LinearLayout) 	findViewById(R.id.scanControlZone);        
        scanButton 		= (Button)			findViewById(R.id.scanButton);
        saveButton		= (Button)			findViewById(R.id.saveButton);

        scanButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        scanControlZone.setAlpha(0.9F);
        
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (barcodeScanned) {
                    barcodeScanned = false;
                    scanButton.setVisibility(View.GONE);
                    saveButton.setVisibility(View.GONE);
                    scanText.setText(getString(R.string.qrcode_scanning));
                    mCamera.setPreviewCallback(previewCb);
                    mCamera.startPreview();
                    previewing = true;
                    mCamera.autoFocus(autoFocusCB);
                }
            }
        });
        
        saveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	alert.setTitle(getString(R.string.qrcode_result_title));
            	alert.setMessage(getString(R.string.qrcode_result_msg));
            	alert.setPositiveButton(getString(R.string.qrcode_result_yes_btn), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
							
						final Handler handler = new Handler() {
				        	@Override
				        	public void handleMessage(Message msg) {
				        	
				        		pdialog.cancel();

				        		if(resultMsg.equals("ok")){
									Toast.makeText(getApplicationContext(), getString(R.string.qrcode_result_success), 1000).show();
								}else{
									Toast.makeText(getApplicationContext(), getString(R.string.qrcode_result_error), 1000).show();
								}
								
								finish();
				        	}
						};
						
						// send 
						pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
						pdialog.setMessage(getString(R.string.dialog_progressing));
						pdialog.setCancelable(false);
						pdialog.show();
						
						new Thread
						(
					        new Runnable() 
							{
					        	@Override
								public void run() 
								{      	

									solomo.addWallet(storeToken, storeAccount, resultCode, false, getLocation, new  SolomoResponseBack(){

										@Override
										public void onConnectFail(String arg0,
												String arg1) {
											// TODO Auto-generated method stub
											Toast.makeText(getApplicationContext(), arg1, 1000).show();
											
										}

										@Override
										public void onFail(String arg0, String arg1) {
											// TODO Auto-generated method stub
											Toast.makeText(getApplicationContext(), arg1, 1000).show();
											
										}

										@Override
										public void onSuccess(SolomoResponse sr) {
											// TODO Auto-generated method stub
											
										   resultMsg = sr.getStatus();																					
											
										   handler.sendEmptyMessage(0);
										}
										
									});
					               
								}
							}	
				         ).start(); 
					}
				});
            	
            	alert.setNegativeButton(getString(R.string.qrcode_result_no_btn), new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
					}
				});
            	alert.show();
            }
        });
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case android.R.id.home:
	        	
	        	finish();
	        	
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	        //getSupportMenuInflater().inflate(R.menu.main, menu);
	        
		return true;
	}   
    
    public void onPause() {
        super.onPause();
        releaseCamera();
        locationManager.removeUpdates(locationListener);
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e){
        }
        return c;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    private Runnable doAutoFocus = new Runnable() {
            public void run() {
                if (previewing)
                    mCamera.autoFocus(autoFocusCB);
            }
        };

    PreviewCallback previewCb = new PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Parameters parameters = camera.getParameters();
                Size size = parameters.getPreviewSize();
                parameters.setFocusMode("continuous-picture");
                mCamera.setParameters(parameters);
                Image barcode = new Image(size.width, size.height, "Y800");
                barcode.setData(data);

                int result = scanner.scanImage(barcode);
                
                if (result != 0) {
                    previewing = false;
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    
                    SymbolSet syms = scanner.getResults();
                    for (Symbol sym : syms) {
                    	
                        // save
                    	String getResult = sym.getData();
                    	Log.d("result", getResult);
                    	if(getResult.indexOf(resultPrefix) != -1){

                    		try{
	                    		String[] getQrcode = getResult.split("=");
	                    		
	                        	resultCode = Long.parseLong(getQrcode[1]);  
	                            scanText.setText(getString(R.string.qrcode_result_code) + resultCode);
	
	                            scanButton.setVisibility(View.VISIBLE);
	                            saveButton.setVisibility(View.VISIBLE);            
                    		}catch(Exception e){
                            	
                                scanText.setText(getString(R.string.qrcode_format_error));

                                scanButton.setVisibility(View.VISIBLE);
                                saveButton.setVisibility(View.GONE);  
                    		}
                    	}else{
                        	
                            scanText.setText(getString(R.string.qrcode_format_error));

                            scanButton.setVisibility(View.VISIBLE);
                            saveButton.setVisibility(View.GONE);                    		
                    	}
                        	
                        barcodeScanned = true;
                    }
                }
            }
        };

    // Mimic continuous auto-focusing
    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                autoFocusHandler.postDelayed(doAutoFocus, 1000);
            }
        };
        
    private void locationInitial(){

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        
        // Define a listener that responds to location updates
        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
              // Called when a new location is found by the network location provider.
              getLocation   = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
          };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        // get locaiton
        getLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);  
        
        if (getLocation == null) {  
            getLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); 
        }  
        
	}    

}
