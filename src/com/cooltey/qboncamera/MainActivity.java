package com.cooltey.qboncamera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.facebook.Session;
import com.hiiir.facebook.plugin.FacebookPluginUtil;
import com.hiiir.qbonsdk.Qbon;
import com.hiiir.qbonsdk.Qbon.FacebookCallback;
import com.hiiir.qbonsdk.Qbon.QbonCloseListener;
import com.hiiir.qbonsdk.net.data.Offer;
import com.hiiir.qbonsdk.net.data.OfferListResponse;
import com.hiiir.qbonsdk.net.data.Profile;
import com.hiiir.qbonsdk.net.data.SolomoResponse;
import com.hiiir.qbonsdk.util.Solomo;
import com.hiiir.qbonsdk.util.Solomo.OfferListResponseBack;
import com.hiiir.qbonsdk.util.Solomo.SolomoResponseBack;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;

public class MainActivity extends SherlockFragmentActivity  implements SurfaceHolder.Callback {

	private Camera camera;
	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private boolean previewing = false;
	private boolean cameraRelease = false;
	private LayoutInflater controlInflater = null;
	private RelativeLayout topBanner;
	private RelativeLayout bottomBanner;
	private RelativeLayout captureZone;
	private int phoneWidth;
	private int phoneHeight;
	
	// control
	private ImageView takePhoto;
	private ImageView selectPhoto;
	private ImageView findLocation;
	private ImageView sharePhoto;
	private ImageView myQbon;
	private PictureCallback rawCallback;
	private ShutterCallback shutterCallback;
	private PictureCallback jpegCallback;
	private Bitmap bitmap = null;
	private boolean photoHasBeenTaken = false;
	private boolean locationHasBeenTaken = false;
	private Uri fileUri;
	
	// cpature zone
	private ImageView capturePictureBg;
	private ImageView capturePicture;
	private ImageView storeQRCode;
	private TextView  storeName;
	private TextView  storeContent;
	private TextView  storeContentSub;
	private EditText  customText;
	
	// alert dialog
	private AlertDialog alert;
	private AlertDialog qalert;
	private ProgressDialog pdialog;
	
	// alert inflater
	private LayoutInflater inflater;
		
	private int GET_CROP_PHOTO_RESULT  	= 100;
	private int GET_ALBUM_RESULT  		= 200;
	
	private String applicationKey;
	private String privateKey;
	private Solomo solomo;
	// qr code
	private String qrcodeUrl = "https://chart.googleapis.com/chart?chs=150x150&cht=qr&chl=http://bit.ly/QbonCamera?code=";
	
	// offer list
	private ArrayList<Offer> offerList = null;
	
	 // Imageloader
    DisplayImageOptions options;
    
    ImageLoader imageLoader = ImageLoader.getInstance();

	// geo setting
	private Location getLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    
    private Qbon qbon;
    private Profile getProfile;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// initial qbon
		//Qbon qbon = new Qbon(MainActivity.this, true, applicationKey, privateKey);
		applicationKey = getString(R.string.qbon_application_key);
		privateKey     = getString(R.string.qbon_private_key);
		solomo = new Solomo(MainActivity.this, true, applicationKey, privateKey);

		
		qbon = new Qbon(MainActivity.this, true, applicationKey, privateKey);
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
		
		//qbon.open();
		
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		//Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		
		setContentView(R.layout.activity_main);
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		

		AlertDialog.Builder builder  = new AlertDialog.Builder(this);
		alert = builder.create();
		
		
		// progress dialog
    	pdialog = new ProgressDialog(this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
    	
    	inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE); 
    	
    	// ImageLoader 初始設定
        options = new DisplayImageOptions.Builder()
         .showStubImage(R.drawable.qbon_img_list)
         .showImageForEmptyUri(R.drawable.loading_error_img)
         .showImageOnFail(R.drawable.loading_error_img)
         .build();
        
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
         .defaultDisplayImageOptions(options)
         .build();
        imageLoader.init(config);
 
		topBanner		= (RelativeLayout) findViewById(R.id.top_banner);
		bottomBanner	= (RelativeLayout) findViewById(R.id.bottom_banner);
		captureZone		= (RelativeLayout) findViewById(R.id.capture_zone);
				
		// control
		takePhoto		= (ImageView) findViewById(R.id.take_photo_btn);
		selectPhoto		= (ImageView) findViewById(R.id.select_photo_btn);
		findLocation	= (ImageView) findViewById(R.id.find_location_btn);
		sharePhoto		= (ImageView) findViewById(R.id.share_photo_btn);
		myQbon			= (ImageView) findViewById(R.id.qbon_btn);
		
		takePhoto.setOnTouchListener(onBtnTouchListener);
		selectPhoto.setOnTouchListener(onBtnTouchListener);
		findLocation.setOnTouchListener(onBtnTouchListener);
		sharePhoto.setOnTouchListener(onBtnTouchListener);
		myQbon.setOnTouchListener(onBtnTouchListener);
		
		capturePictureBg 	= (ImageView) findViewById(R.id.capture_picture_bg);
		capturePicture  	= (ImageView) findViewById(R.id.capture_picture);
		storeQRCode  		= (ImageView) findViewById(R.id.store_qrcode);
		storeName  			= (TextView) findViewById(R.id.store_name);
		storeContent  		= (TextView) findViewById(R.id.store_content);
		storeContentSub  	= (TextView) findViewById(R.id.store_content_sub);
		customText	  		= (EditText) findViewById(R.id.custom_text);
		
        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView 	= (SurfaceView) findViewById(R.id.camerapreview);
        surfaceHolder 	= surfaceView.getHolder();
        surfaceHolder.addCallback(this);
		
        // get phone width
        Display display = getWindowManager().getDefaultDisplay();
        Point size 		= new Point();
        display.getSize(size);
        
        phoneWidth 	= size.x;
        phoneHeight = phoneWidth;
        
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(phoneWidth, phoneHeight);
        captureZone.setLayoutParams(params);
        
        
        AlphaAnimation alpha = new AlphaAnimation(0.9F, 0.9F);
        alpha.setDuration(0); 
        alpha.setFillAfter(true);
        topBanner.startAnimation(alpha);
        bottomBanner.startAnimation(alpha);    
        
        // initial the camera setting;
        takePictureInitial();
        // initial the location manager
        locationInitial();
        
        
        // control zone
        takePhoto.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub	
				takePhoto();
			}
        	
        });
        
        sharePhoto.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				sharePhoto();
			}
        });
        

		// set photo data
        selectPhoto.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
			    Intent intent = new Intent();
			    
			    intent.setType("image/*");
			    
			    intent.setAction(Intent.ACTION_GET_CONTENT);
			    
			    intent.addCategory(Intent.CATEGORY_OPENABLE);
			    
			    startActivityForResult(intent, GET_CROP_PHOTO_RESULT);
			}
			
		});
        
        // find location
        findLocation.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// gen list
				findLocation();
			}
			
		});
        
        // my qbon
        myQbon.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				myQbon();
			}
			
		});
        
	}
	
	private void myQbon(){
		
		AlertDialog.Builder qbuilder  = new AlertDialog.Builder(this);
		qalert = qbuilder.create();
		
		
		
		final Handler handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		long 	qbonAccountId = getProfile.getMember().getId();
				String	qbonToken	  = getProfile.getToken();
				
				pdialog.dismiss();
				
				if(qbonAccountId == 0){
					
					Builder alert  = new AlertDialog.Builder(MainActivity.this);
	            	alert.setTitle(getString(R.string.qbon_login_alert_title));
	            	alert.setMessage(getString(R.string.qbon_login_alert_msg));
	            	alert.setPositiveButton(getString(R.string.qbon_login_alert_yes_btn), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							qbon.open();
						}
					});
	            	
	            	alert.setNegativeButton(getString(R.string.qbon_login_alert_no_btn), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
						}
					});
	            	alert.show();
	            	
				}else{
					
					View dialogView = inflater.inflate(R.layout.dialog_qbon_list, null);
					
					TextView myQbonBtn 		= (TextView) dialogView.findViewById(R.id.dialog_my_qbon_btn);
					TextView scanQRCodeBtn 	= (TextView) dialogView.findViewById(R.id.dialog_qrcode_scan_btn);
					ImageView  qbonIconBtn  = (ImageView) dialogView.findViewById(R.id.qboniconbtn);
					ImageView  qrcodeBtn	= (ImageView) dialogView.findViewById(R.id.qrcodebtn);
					
					qrcodeBtn.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub

							qalert.dismiss();

							if(previewing){
								// stop camrea
								camera.stopPreview();
						        camera.release();
						        camera = null;
						        previewing = false;
						        cameraRelease = true;
							}
							
							Intent intent = new Intent().setClass(MainActivity.this ,QRCodeScannerActivity.class);
				        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							
						}
						
					});
					
					scanQRCodeBtn.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							// TODO Auto-generated method stub

							qalert.dismiss();

							if(previewing){
								// stop camrea
								camera.stopPreview();
						        camera.release();
						        camera = null;
						        previewing = false;
						        cameraRelease = true;
							}
							
							Intent intent = new Intent().setClass(MainActivity.this ,QRCodeScannerActivity.class);
				        	intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							startActivity(intent);
							
						}
						
					});
					
					myQbonBtn.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							qalert.dismiss();
							qbon.open();
							Toast.makeText(getApplicationContext(), getString(R.string.qbon_list_wallet), 1500).show();
						}
						
					});
					
					qbonIconBtn.setOnClickListener(new OnClickListener(){

						@Override
						public void onClick(View v) {
							qalert.dismiss();
							qbon.open();
							Toast.makeText(getApplicationContext(), getString(R.string.qbon_list_wallet), 1500).show();
						}
						
					});
					
					qbonIconBtn.setOnTouchListener(onBtnTouchListener);
					qrcodeBtn.setOnTouchListener(onBtnTouchListener);
					
					qalert.setView(dialogView);
					qalert.show();
				}
				
        	}
		};
		
		// send 
		pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pdialog.setMessage(getString(R.string.dialog_progressing));
		pdialog.setCancelable(false);
		pdialog.show();
		
		

		handler.postDelayed(new Runnable(){

			@Override
			public void run() {
				// TODO Auto-generated method stub

				// 取得數值
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
			}
			
		}, 1000);
	}
	
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
	
	private void takePhoto(){
		 if(photoHasBeenTaken == false){
			 camera.takePicture(shutterCallback, rawCallback, jpegCallback);		
		 }else{
			 // restart camera
			bitmap.recycle();
			bitmap = null;
			capturePicture.setImageBitmap(null);
            capturePictureBg.setImageBitmap(null);
          	surfaceView.setVisibility(View.VISIBLE);
            surfaceHolder 	= surfaceView.getHolder();
            surfaceHolder.addCallback(MainActivity.this);
			photoHasBeenTaken = false;
		 }
	}
	
	private void sharePhoto(){
		// save the image for layout
		
		// take photo first
		if(photoHasBeenTaken == true && locationHasBeenTaken == true){			 

			captureZone.setDrawingCacheEnabled(true);
			captureZone.buildDrawingCache(true);
			captureZone.setDrawingCacheQuality(100);
			Bitmap bitmap = captureZone.getDrawingCache();
			
			// bitmap to byte[]
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			
			File tempFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera");
			
	        tempFolder.mkdir();
	        
	        final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera",
	                String.valueOf("share_" + System.currentTimeMillis()) + ".jpg");

	        galleryAddPic(file);
	        
	        FileOutputStream outStream = null;
	        
	        try {
	        	// saving
	            outStream = new FileOutputStream(file);
	            outStream.write(byteArray);
	            outStream.close();
	            
	            Toast.makeText(getApplicationContext(), getString(R.string.sharing_preparing), 3000).show();

				// send 
				pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				pdialog.setMessage(getString(R.string.dialog_loading));
				pdialog.setCancelable(false);
				pdialog.show();
				

	            Handler h = new Handler();
	            h.postDelayed(new Runnable(){

					@Override
					public void run() {
						// TODO Auto-generated method stub
						Intent shareIntent = new Intent();
			            shareIntent.setAction(Intent.ACTION_SEND);
			            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
			            shareIntent.setType("image/jpeg");
			            startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.sharing_preparing_title)));
			            pdialog.dismiss();
			            
			            captureZone.setDrawingCacheEnabled(false);
					}
	            	
	            }, 1500);
	            
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        } finally {
	        }
		}else if(locationHasBeenTaken == false){
			// need to find location
			findLocation();
			
		}else{
			
			// need to take photo
			camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		}
	}
	
	private void findLocation(){

		alert.setView(null);
		View dialogView = inflater.inflate(R.layout.dialog_location_list, null);
		alert.setView(dialogView);
		alert.show();
		
		final LinearLayout processView 		= (LinearLayout) 	dialogView.findViewById(R.id.process_status);
		final TextView     scrollViewTitle	= (TextView) 		dialogView.findViewById(R.id.dialog_action_title);
		final ScrollView   scrollView		= (ScrollView) 		dialogView.findViewById(R.id.dialog_action);
		final LinearLayout listZone	 		= (LinearLayout) 	dialogView.findViewById(R.id.item_list);
		final TextView	   emptyView		= (TextView) 		dialogView.findViewById(R.id.empty_view);
						
		final Handler handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		
        		processView.setVisibility(View.GONE);
        		scrollViewTitle.setVisibility(View.VISIBLE);
				scrollView.setVisibility(View.VISIBLE);
				
        		if(offerList != null && offerList.size() > 0){
    				
					for(int i = 0; i < offerList.size(); i++){
						
						String getImageUrl 		= offerList.get(i).getImage();
						final String getName	= offerList.get(i).getOfferName();
						final String getDesc	= offerList.get(i).getDetail();
						final String getAddress = offerList.get(i).getLocationDisplay();
						final String getPhone   = offerList.get(i).getLocationPhone();
						Double getLat 			= offerList.get(i).getLatitude();
						Double getLot			= offerList.get(i).getLongitude();
						final Long   getId		= offerList.get(i).getOfferLocationId();
						
						Location storeLocation	= new Location("dummyprovider");
						storeLocation.setLatitude(getLat);
						storeLocation.setLongitude(getLot);
						
						float getDistince		= getLocation.distanceTo(storeLocation)/1000;
						String getDIstinceString = getDistince + "";
						if(getDIstinceString.length() > 4){
							getDIstinceString = getDIstinceString.substring(0, 4);
						}
						
						View list_item = inflater.inflate(R.layout.item_location_list, null);
						
						ImageView img 		= (ImageView) list_item.findViewById(R.id.img);
						TextView  name 		= (TextView)  list_item.findViewById(R.id.name);
						TextView  desc 		= (TextView)  list_item.findViewById(R.id.desc);
						TextView  distince 	= (TextView)  list_item.findViewById(R.id.distince);
						
						imageLoader.displayImage(getImageUrl, img);
						
						name.setText(getName);
						desc.setText(getDesc);
						distince.setText(getString(R.string.distince_prefix) + getDIstinceString + getString(R.string.distince_lastfix));
						
						// add tag
						list_item.setTag(i);
						
						// set action
						list_item.setOnClickListener(new OnClickListener(){

							@Override
							public void onClick(View v) {
								// TODO Auto-generated method stub
								
								// send 
								pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
								pdialog.setMessage(getString(R.string.dialog_qrcode_gening));
								pdialog.setCancelable(false);
								pdialog.show();
								
								// display on surfaceview
								storeName.setText(getName); 
								storeContent.setText(getAddress);
								storeContentSub.setText(getPhone);
								String combineUrl = qrcodeUrl + "" + getId;
								imageLoader.displayImage(combineUrl, storeQRCode, options, new ImageLoadingListener(){

									@Override
									public void onLoadingCancelled(String arg0,
											View arg1) {
										// TODO Auto-generated method stub
										
									}

									@Override
									public void onLoadingComplete(String arg0,
											View arg1, Bitmap arg2) {
										// TODO Auto-generated method stub
										pdialog.dismiss();
										locationHasBeenTaken = true;
									}

									@Override
									public void onLoadingFailed(String arg0,
											View arg1, FailReason arg2) {
										// TODO Auto-generated method stub
										
									}

									@Override
									public void onLoadingStarted(String arg0,
											View arg1) {
										// TODO Auto-generated method stub
										
									}
									
								});
								
								alert.dismiss();
							}
							
						});
						
						// set on touch
						list_item.setOnTouchListener(onTouchListener);
						
						listZone.addView(list_item);
					}
        		}else{
					processView.setVisibility(View.GONE);
					scrollView.setVisibility(View.GONE);
					scrollViewTitle.setVisibility(View.GONE);
					emptyView.setVisibility(View.VISIBLE);
					
					// send 
					pdialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					pdialog.setMessage(getString(R.string.dialog_qrcode_gening));
					pdialog.setCancelable(false);
					pdialog.show();
					
					// display on surfaceview
					storeName.setText(getString(R.string.offerlist_default_title)); 
					storeContent.setText(getString(R.string.offerlist_content));
					storeContentSub.setText(getString(R.string.offerlist_sub_content));
					String combineUrl = qrcodeUrl + "" ;
					imageLoader.displayImage(combineUrl, storeQRCode, options, new ImageLoadingListener(){

						@Override
						public void onLoadingCancelled(String arg0,
								View arg1) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void onLoadingComplete(String arg0,
								View arg1, Bitmap arg2) {
							// TODO Auto-generated method stub
							pdialog.dismiss();
							locationHasBeenTaken = true;
						}

						@Override
						public void onLoadingFailed(String arg0,
								View arg1, FailReason arg2) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void onLoadingStarted(String arg0,
								View arg1) {
							// TODO Auto-generated method stub
							
						}
						
					});
				}
        	}
		};
		
		
		processView.setVisibility(View.VISIBLE);
		scrollViewTitle.setVisibility(View.GONE);
		scrollView.setVisibility(View.GONE);
		emptyView.setVisibility(View.GONE);
		
		new Thread
		(
	        new Runnable() 
			{
	        	@Override
				public void run() 
				{      	
	        		
	        		//getLocation.setLatitude(22.459927322483253);
	        		//getLocation.setLongitude(120.52666248125001);
	        		solomo.getOfferList(0L, 
							new ArrayList<Long>(), //類別Fliter
							false, 				//是否要包含任務
							"", 				//關鍵字
							20, 				//limit
							0, 					//位置ID
							20, 				//搜尋範圍(英里)
							0, 					//起始資料
							"ALL",				//搜尋參數
							getLocation, 		//想要搜尋的中心點
							"DISTANCE", 		//搜尋類型
							new OfferListResponseBack() {
								
								@Override
								public void onFail(String arg0, String arg1) {

									Toast.makeText(getApplicationContext(), getString(R.string.find_location_error), 1000).show();
								
									locationInitial();
					                handler.sendEmptyMessage(0);
									alert.dismiss();

									AlertDialog.Builder builder  = new AlertDialog.Builder(MainActivity.this);
									alert = builder.create();
								}
								
								@Override
								public void onConnectFail(String arg0, String arg1) {

									Toast.makeText(getApplicationContext(), getString(R.string.find_location_error), 1000).show();

					                handler.sendEmptyMessage(0);
									alert.dismiss();
									
									AlertDialog.Builder builder  = new AlertDialog.Builder(MainActivity.this);
									alert = builder.create();
								}
								
								@Override
								public void onSuccess(OfferListResponse listData) {
									
									offerList = listData.getData();
					                handler.sendEmptyMessage(0);
								}
							});
				}
			}	
         ).start(); 
	}
	
	// take picture call back
	private void takePictureInitial(){
		rawCallback = new PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
            }
        };

        /** Handles data for jpeg picture */
        shutterCallback = new ShutterCallback() {
            public void onShutter() {
            }
        };
        
        jpegCallback = new PictureCallback() {
            public void onPictureTaken(final byte[] data, Camera camera) {
				
				final Handler handler = new Handler() {
		        	@Override
		        	public void handleMessage(Message msg) {
		        	
		        		pdialog.cancel();

	                    // display on screen
	                    capturePicture.setImageBitmap(bitmap);
	                    capturePictureBg.setImageBitmap(bitmap);
	               	 	surfaceView.setVisibility(View.GONE);
	               	 	
	               	 	photoHasBeenTaken = true;
	               	 	
	               	 	// need to select location
	               	 	if(locationHasBeenTaken == false){
	               	 		findLocation();
	               	 	}
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
			        		// making dir
			            	File tempFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera");
							
					        tempFolder.mkdir();
					        
					        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera",
					                String.valueOf(System.currentTimeMillis()) + ".jpg");
					        
					        galleryAddPic(file);
					        
			                FileOutputStream outStream = null;
			                
			                try {
			                	// saving
			                    outStream = new FileOutputStream(file);
			                    outStream.write(data);
			                    outStream.close();
			                    
			                    // get new bitmap
			                    bitmap = getNewBitmap(bitmap, Uri.fromFile(file), true, true);
			                    
			                } catch (FileNotFoundException e) {
			                    e.printStackTrace();
			                } catch (IOException e) {
			                    e.printStackTrace();
			                } finally {
			                }
			                handler.sendEmptyMessage(0);
						}
					}	
		         ).start(); 
                
            }
        };
	}
	
	// get photo 
	private Bitmap getNewBitmap(Bitmap bp, Uri uriFile, boolean rotate, boolean camera){
		
		Bitmap bitmap = bp;
		
		try{
			// get photo
	        InputStream stream 			= null;
	        ByteArrayOutputStream baos 	= new ByteArrayOutputStream();
	        
	        stream = getContentResolver().openInputStream(uriFile);
	        bitmap = BitmapFactory.decodeStream(stream);
	        
	        // compress bitmap
	        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
	                    			                    
	        float getWidth = bitmap.getWidth();
	        float getHeight = bitmap.getHeight();

	        float raitoAspect 	= phoneWidth / getHeight;
	        if(!camera){
	        	raitoAspect 	= phoneWidth / getWidth;
	        }
	        
	        // resize & rotate
	        Matrix matrix = new Matrix();
	        matrix.postScale(raitoAspect, raitoAspect);
	        if(rotate){
	        	matrix.postRotate(90);
	        }
	        
	        // rotate first
	        bitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) getWidth, (int) getHeight, matrix, true);
	        // cut bitmap
	        
		}catch(Exception e){
			bitmap = null;
		}
        return bitmap;
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
	        getSupportMenuInflater().inflate(R.menu.main, menu);
	        
		return true;
	}

	 
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    	
    	cameraRelease = false;
    	
        if(previewing){
            camera.stopPreview();
            previewing = false;
        }
 
        if (camera != null){
            try {            	
            	Camera.Parameters params = camera.getParameters();
            	
            	// set focus
            	params.setFocusMode("continuous-picture");
            	//params.setRotation(90);
            	
            	// set best size
            	Camera.Size myBestSize = getBestPreviewSize(width, height, params);
            	params.setPreviewSize(myBestSize.width, myBestSize.height);
            	
            	// set pic size
            	//Camera.Size pictureSize = getSmallestPictureSize(params);
            	//params.setPictureSize(pictureSize.width, pictureSize.height);
            	
                camera.setParameters(params);
                // set orientation
                camera.setDisplayOrientation(90);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                previewing = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();
        
        cameraRelease = false;
        
    }
 
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    	
    	if(cameraRelease == false){
	        camera.stopPreview();
	        camera.release();
	        camera = null;
	        previewing = false;
	        cameraRelease = true;
    	}
    }
    
    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters){
        Camera.Size bestSize = null;
        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();
        
        bestSize = sizeList.get(0);
        
        for(int i = 1; i < sizeList.size(); i++){
         if((sizeList.get(i).width * sizeList.get(i).height) >
           (bestSize.width * bestSize.height)){
          bestSize = sizeList.get(i);
         }
        }

        return bestSize;
    }
    

	// add pic to gallery
	private void galleryAddPic(File f) {
	    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
	    Uri contentUri = Uri.fromFile(f);
	    mediaScanIntent.setData(contentUri);
	    this.sendBroadcast(mediaScanIntent);
	}
	
	
	 private Camera.Size getSmallestPictureSize(Camera.Parameters parameters) {
	    Camera.Size result=null;

	    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
	      if (result == null) {
	        result=size;
	        
	      }else {
	        int resultArea=result.width * result.height;
	        int newArea=size.width * size.height;

	                
	        if (newArea < resultArea) {
	          result=size;
	        }
	      }
	    }

	    
	    // get the
	    List<Size> getList   = parameters.getSupportedPictureSizes();
	    int totalSize = getList.size();
	    
	    if(totalSize > 1){
	    	result = getList.get(totalSize - 1);
	    }
	    
	    return(result);
	  }
    
    
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
    	  InputStream stream 			= null;
          ByteArrayOutputStream baos 	= new ByteArrayOutputStream();
        
    	 if (requestCode == GET_CROP_PHOTO_RESULT && resultCode == Activity.RESULT_OK){
    		 
    		 try {
	    		 File tempFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera");
				 tempFolder.mkdir();
				 
			     File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/qbon_camera",
			     String.valueOf("crop_" + System.currentTimeMillis()) + ".jpg");
			     
			     fileUri = Uri.fromFile(file);
			     
			     galleryAddPic(file);
			     
	    		 Intent intent = new Intent("com.android.camera.action.CROP"); 
			     intent.putExtra( MediaStore.EXTRA_OUTPUT,  fileUri);   
	    		 intent.setType("image/*");
	    		 intent.setData(data.getData());  
	    		 intent.putExtra("crop", "true");  
	    		 intent.putExtra("aspectX", 1); // This sets the max width.
	    		 intent.putExtra("aspectY", 1); // This sets the max height.
	    		 intent.putExtra("outputX", 350); 
	    		 intent.putExtra("outputY", 350);
	    		 intent.putExtra("noFaceDetection", true);  
	    		 intent.putExtra("return-data", true);                               
	    		 startActivityForResult(intent, GET_ALBUM_RESULT);
    		 }catch (ActivityNotFoundException anfe) {
    			    Toast toast = Toast.makeText(this, getString(R.string.sharing_prepare_error), Toast.LENGTH_SHORT);
    			    toast.show();
    		}
    	 }
    	
        if (requestCode == GET_ALBUM_RESULT && resultCode == Activity.RESULT_OK){       
    		try{    			
    			bitmap = getNewBitmap(bitmap, fileUri, false, false);
    		}catch(Exception e){
    			
    		}
    		
    		// display on screen
            capturePicture.setImageBitmap(bitmap);
            capturePictureBg.setImageBitmap(null);
       	 	surfaceView.setVisibility(View.GONE);
       	 	
       	 	photoHasBeenTaken = true; 
        }
	}
    

    
    // dialog touch event
    private OnTouchListener onTouchListener = new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent me) {
			// TODO Auto-generated method stub

			LinearLayout tv = (LinearLayout) v;
			
			if(me.getAction() == MotionEvent.ACTION_DOWN){
				tv.setBackgroundColor(Color.parseColor("#ECECEC"));
			}else{
				tv.setBackgroundColor(Color.parseColor("#FFFFFF"));				
			}
			
			return false;
		}
		
	};
	
	 // dialog touch event
    private OnTouchListener onBtnTouchListener = new OnTouchListener(){

		@Override
		public boolean onTouch(View v, MotionEvent me) {
			// TODO Auto-generated method stub

			ImageView tv = (ImageView) v;
			
			if(me.getAction() == MotionEvent.ACTION_DOWN){
				tv.setAlpha(0.7F);
			}else{
				tv.setAlpha(1.0F);		
			}
			
			return false;
		}
		
	};
	
	
	protected void onPause() {
        super.onPause();

        locationManager.removeUpdates(locationListener);
    }
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)){
			exitOptionsDialog(getString(R.string.dialog_exit_sure_title), getString(R.string.dialog_exit_sure_content));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private void exitOptionsDialog(String title, String Content) { 
        new AlertDialog.Builder(MainActivity.this) 
        .setTitle(title)
        .setMessage(Content) 
        .setPositiveButton(getString(R.string.dialog_btn_yes), 
        		new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						MainActivity.this.finish();
					} 
        		}) 
        .setNegativeButton(getString(R.string.dialog_btn_no), 
        		new DialogInterface.OnClickListener(){ 
					@Override
					public void onClick( 
							DialogInterface dialoginterface, int i){ 
					} 
        }) 
        .show();
    }
	

}

