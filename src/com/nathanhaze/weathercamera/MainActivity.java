package com.nathanhaze.weathercamera;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.google.android.gms.ads.*;
import com.google.analytics.tracking.android.EasyTracker;
import com.nathanhaze.weathercamera.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements LocationListener, OnMenuItemClickListener  { // weather

	static  ProgressDialog pd;
	volatile  Handler mapHandler = new Handler();
	
	static String temperature = "" ; 
	static StringBuilder climate = new StringBuilder();
	static String[] weather;
	static String temp_unit = "", wind_units = "", precip_units ="", visibilty_units = "";
	
	static int temp_value;
	
	static ProgressDialog dialog;
	
	private static Camera mCamera;
	private static CameraPreview mPreview;
	
	static Location location;
	static LocationListener locationListener;
	static LocationManager locationManager;
	
//	static final int MEDIA_TYPE_IMAGE = 1;
	
	static Context context;
	
	static GlobalVar gv = new GlobalVar();
	
	static ImageView myImage;
	static File imgFile = null;
	
	private InterstitialAd interstitial;


	
	@Override
public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
    	case R.id.settings:
            Intent i = new Intent(this, Preference.class);
            startActivityForResult(i, 1);
        break;
        case R.id.comments:
        	Intent intent = new Intent(Intent.ACTION_SEND);
        	intent.setType("message/rfc822");
        	intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "greenbizkit33@gmail.com" });
        	intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Weather Camera app");
        	startActivity(intent);
        break;
	}
            return(true);  
}

	//private XPath xPath = XPathFactory.newInstance().newXPath();
	
	static SharedPreferences sharedPrefs;
	//public static final String PREFS_NAME = "MyPrefsFile";
	
	static boolean flash = false;

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.camera_preview);

 //   loadAd();
    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
 // Register the listener with the Location Manager to receive location updates
    
    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    
    location = getLastKnownLocation();
    mCamera = getCameraInstance();

    context = getApplicationContext();
    // Create our Preview view and set it as the content of our activity.
    mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);

  //  speedBackground = (LinearLayout)findViewById(R.id.speedBackgound);
    
    
    boolean hasFlash = this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    if(!hasFlash){
    	((ToggleButton)findViewById(R.id.togglebutton)).setVisibility(View.GONE);
    }
    
    myImage = (ImageView) findViewById(R.id.lastPic);
    myImage.setOnTouchListener(new OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_VIEW);
			if(imgFile == null){
				 CharSequence text = "This is only an icon, take a photo"; 
             	 Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
             	 toast.show();
			}
			else{
			intent.setDataAndType(Uri.parse("file://" +  imgFile.getAbsolutePath()), "image/*");
			startActivity(intent);
			}
			return false;
		}
    });
    /*
    
    // Prepare the Interstitial Ad
    interstitial = new InterstitialAd(MainActivity.this);
    // Insert the Ad Unit ID
    interstitial.setAdUnitId("ca-app-pub-2377934805759836/3876184165");

    // Request for Ads
    AdRequest adRequest = new AdRequest.Builder()
    // Add a test device to show Test Ads
     // .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
     // .addTestDevice("5E39C82DA23AB651436D5DA0866A484D")
            .build();

   
    // Load ads into Interstitial Ads
    interstitial.loadAd(adRequest);

    // Prepare an Interstitial Ad Listener
    interstitial.setAdListener(new AdListener() {
        public void onAdLoaded() {
            // Call displayInterstitial() function
            displayInterstitial();
        }
    });
    
    
    */
    
    
    
    try{
    	sharedPrefs = PreferenceManager
            .getDefaultSharedPreferences(this);
    } catch (NullPointerException e) {
    	sharedPrefs = null;
    }
    
    
    if(location == null){
    	//Log.d("LOCATION", Double.toString(location.getLatitude()));
    	((TextView) findViewById(R.id.temp)).setText("Can't find location? Enable GPS and restart app");    }
    else{
    	((TextView) findViewById(R.id.temp)).setText("Looking for weather...");    }

}


public void onToggleClicked(View view) {
    // Is the toggle on?
    boolean on = ((ToggleButton) view).isChecked();
    Parameters param = mCamera.getParameters(); 
    if (on) {
        param.setFlashMode(Parameters.FLASH_MODE_ON);
    } else {
        param.setFlashMode(Parameters.FLASH_MODE_OFF);
    }    
    /*
    mCamera.getParameters().setGpsLatitude(location.getLatitude());
    mCamera.getParameters().setGpsLongitude(location.getLongitude());
    mCamera.getParameters().setGpsAltitude(location.getAltitude());
    mCamera.getParameters().setGpsTimestamp(location.getTime());
    */
    mCamera.setParameters(param);
}


@Override
public boolean onCreateOptionsMenu(Menu menu) {
	// Inflate the menu; this adds items to the action bar if it is present.
	getMenuInflater().inflate(R.menu.main, menu);
	return true;
}

public void menu(View v){
	PopupMenu popup = new PopupMenu(this, v);
    MenuInflater inflater = popup.getMenuInflater();
    inflater.inflate(R.menu.main, popup.getMenu());
    popup.setOnMenuItemClickListener(this);
    popup.show();
}



@Override
public boolean onMenuItemClick(MenuItem item) {
	switch (item.getItemId()) {
		case R.id.settings:
		    Intent i = new Intent(this, Preference.class);
		    startActivityForResult(i, 1);
		break;
		case R.id.comments:
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType("message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[] { "greenbizkit33@gmail.com" });
			intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Weather Camera app");
			startActivity(intent);
		break;
	}
    return(true);  
}



protected class retrieve_weatherTask extends AsyncTask<Void, String, String> {
	
	protected void onPreExecute(){

	}
	
	@Override
	protected String doInBackground(Void ...arg0) {
	// TODO Auto-generated method stub
	String qResult = "";
	HttpClient httpClient = new DefaultHttpClient();
	HttpContext localContext = new BasicHttpContext();
	
	if(location == null){
		  location = getLastKnownLocation();  
		  if(location == null)return "Can not find location";
	}
	HttpGet httpGet = new HttpGet("http://api.wunderground.com/api/51cda8abeca78e10/conditions/q/" + Double.toString(location.getLatitude()) + "," + Double.toString(location.getLongitude()) + ".xml");
	
	try {
	HttpResponse response = httpClient.execute(httpGet,
	localContext);
	HttpEntity entity = response.getEntity();
	
	if (entity != null) {
	InputStream inputStream = entity.getContent();
	Reader in = new InputStreamReader(inputStream);
	BufferedReader bufferedreader = new BufferedReader(in);
	StringBuilder stringBuilder = new StringBuilder();
	String stringReadLine = null;
	while ((stringReadLine = bufferedreader.readLine()) != null) {
	stringBuilder.append(stringReadLine + "\n");
	}
	qResult = stringBuilder.toString();
	}
	
	} catch (ClientProtocolException e) {
		temperature = "Internet Issue";
	e.printStackTrace();
	} catch (IOException e) {
		temperature = "Internet Issue";
	e.printStackTrace();
	}
	
	Document dest = null;
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory
	.newInstance();
	DocumentBuilder parser;
	try {
	parser = dbFactory.newDocumentBuilder();
	dest = parser
	.parse(new ByteArrayInputStream(qResult.getBytes()));
	} catch (ParserConfigurationException e1) {
		temperature = "Error: document";
	e1.printStackTrace();
	} catch (SAXException e) {
		temperature = "Error: document";
	e.printStackTrace();
	} catch (IOException e) {
		temperature = "Error: document";
	e.printStackTrace();
	}
	
	climate = new StringBuilder();
	climate.setLength(0);
	if(dest != null){
		if(weather[0] != null){ //location
			climate.append(sv (weather[0], dest) + " ");
		}
		if(weather[1] != null){  //temperature
			climate.append("Temperature: " + sv(weather[1], dest) + " "+  temp_unit + " ");
		}
		if(weather[2] != null){ //humidity
			climate.append("Humidity: " + sv(weather[2], dest) + " ");
		}
		if(weather[3] != null){ //wind
			climate.append("Wind: " + sv(weather[3], dest) + " "+  wind_units +  " ");
		}
		if(weather[4] != null){ //feels
			climate.append("Feels like: " + sv(weather[4], dest) + " "+ temp_unit + " ");
		}
		if(weather[5] != null){//visibility
			climate.append("Visibility: " + sv(weather[5], dest) +" "+  visibilty_units  + " ");
		}
		if(weather[6] != null){ // preciptation
			climate.append("Preciptation: " + sv(weather[6], dest) +" "+  precip_units);
		}
		 if(sharedPrefs.getString("units", "null").equals("metric")){
		    temperature = sv ("//current_observation/temp_c", dest);
	    }
	    else{
			temperature = sv ("//current_observation/temp_f", dest);
	    }
	}	
	
	return temperature;
	}
	
	public String sv(String query, Node node) {

		String rs = "";

		try {
			Node n = (Node) XPathFactory.newInstance().newXPath().evaluate(query, node, XPathConstants.NODE);
			if (n != null) {
				rs = n.getTextContent();
			}
		} catch (Exception e) {
			rs = "";
		}
		return rs;
	}

	        
	
	protected void onPostExecute(String result) {
		System.out.println("POST EXECUTE");
		if(temperature == ""){
			temperature = "error";
		}
    	((TextView) findViewById(R.id.temp)).setText(climate);
    	//((TextView) findViewById(R.id.wind)).setText("Wind: " + wind_mph + "mph (" + wind_kph + "kph)");
    }

  }

 /**** Camera App ***/



/** A safe way to get an instance of the Camera object. */
public static Camera getCameraInstance(){
    Camera c = null;
    try {
        c = Camera.open(); // attempt to get a Camera instance

    }
    catch (Exception e){
        // Camera is not available (in use or does not exist)
    }
    return c; // returns null if camera is unavailable
}

public void takePhoto(View v){
   	pd = ProgressDialog.show(this, "Saving" , "");
    Thread t = new Thread() {
        public void run() {
            mCamera.takePicture(null, null, mPicture);
        }
      };
      t.start();
    //mCamera.startPreview();
}

private PictureCallback mPicture = new PictureCallback() {

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File pictureFile = getOutputMediaFile(1);

        if (pictureFile == null){
            //Log.d("ERROR", "Error creating media file, check storage permissions:" );
        }

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close(); // was commented out??
            
        } catch (FileNotFoundException e) {
            //Log.d("ERROR", "File not found: " + e.getMessage());
        } catch (IOException e) {
            //Log.d("ERROR", "Error accessing file: " + e.getMessage());
        }
        
        
        Bitmap myBitmap = null;
        if (android.os.Build.VERSION.SDK_INT>=android.os.Build.VERSION_CODES.HONEYCOMB) {
        	 BitmapFactory.Options opt = new BitmapFactory.Options();
        	 opt.inMutable = true;
        	 
        	 opt.inJustDecodeBounds = false;
        	 opt.inPreferredConfig = Config.RGB_565;
        	 opt.inDither = true;
        	 
        	 myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), opt);
        	 myBitmap = timestampItAndSave(myBitmap);
        }
        else{
        	myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
        	myBitmap = convertToMutable(context, myBitmap);
        }
        
        
        if(myBitmap ==null){

        	 myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
        	 myBitmap = timestampItAndSaveOLD(myBitmap);
        }

		//Bitmap myBitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath());
		//Bitmap second = timestampItAndSave(myBitmap);
        
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		myBitmap.compress(CompressFormat.JPEG, 98, bos);
		
		byte[] bitmapdata = bos.toByteArray();
		
		//write the bytes in file
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(pictureFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			fos.write(bitmapdata);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fos.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        setImage();
        pd.dismiss();
	    mapHandler.post(Success);
        mCamera.startPreview();
    }
};

/** Create a File for saving an image or video */
private static File getOutputMediaFile(int type){

    Boolean isSDPresent = android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
    if(!isSDPresent)
    {	
    	int duration = Toast.LENGTH_LONG;

    	Toast toast = Toast.makeText(context, "card not mounted", duration);
    	toast.show();
    }
    File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getPath() + "/WeatherCamera/");
    if (! mediaStorageDir.exists()){
        if (! mediaStorageDir.mkdirs()){
            return null;
        }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    File mediaFile;
    if (type == 1){
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
        "IMG_"+ timeStamp + ".jpg");
        imgFile = mediaFile;
    } else {
        return null;
    }

    return mediaFile;
}


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public static Bitmap convertToMutable(final Context context, final Bitmap imgIn) {
    final int width = imgIn.getWidth(), height = imgIn.getHeight();
    final Config type = imgIn.getConfig();
    File outputFile = null;
    final File outputDir = context.getCacheDir();
    try {
        outputFile = File.createTempFile(Long.toString(System.currentTimeMillis()), null, outputDir);
        outputFile.deleteOnExit();
        final RandomAccessFile randomAccessFile = new RandomAccessFile(outputFile, "rw");
        final FileChannel channel = randomAccessFile.getChannel();
        final MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes() * height);
        imgIn.copyPixelsToBuffer(map);
        imgIn.recycle();
        final Bitmap result = Bitmap.createBitmap(width, height, type);
        map.position(0);
        result.copyPixelsFromBuffer(map);
        channel.close();
        randomAccessFile.close();
        outputFile.delete();
        return result;
    } catch (final Exception e) {
    } finally {
        if (outputFile != null)
            outputFile.delete();
    }
    return null;
}


public static void setImage(){
    if(imgFile !=null){
        if(imgFile.exists()){
			Bitmap myBitmap = decodeSampleImage(imgFile, 100, 100);
            myImage.setImageBitmap(myBitmap);

        }
    }
}

private Bitmap timestampItAndSaveOLD(Bitmap toEdit){	
	Bitmap dest = toEdit.copy(Bitmap.Config.ARGB_8888, true);		
    Canvas canvas = new Canvas(dest);
    Paint paint = new Paint();
    paint.setAntiAlias(true);

    paint.setColor(Color.WHITE);

  
    Typeface tf =Typeface.createFromAsset(getAssets(),
            "Archistico_Bold.ttf");
    paint.setTypeface(tf);
    paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
    paint.setStyle(Style.FILL);

    
   int pictureHeight = toEdit.getHeight();
  // int pictureWidth = toEdit.getWidth();

   int length = climate.length();

   float minFontSize = 0.014f;

   paint.setTextSize(pictureHeight * (minFontSize + (1f/length)));
  // paint.setAlpha(150);


   canvas.drawText(
		climate.toString(),
		10 , pictureHeight -200, paint);

    return dest;
}


private Bitmap timestampItAndSave(Bitmap toEdit){	
       Canvas canvas = new Canvas(toEdit);

	    Paint paint = new Paint();
	    paint.setAntiAlias(true);

	    paint.setColor(Color.WHITE);

	  
	    Typeface tf =Typeface.createFromAsset(getAssets(),
                "Archistico_Bold.ttf");
	    paint.setTypeface(tf);
	    paint.setShadowLayer(2.0f, 1.0f, 1.0f, Color.BLACK);
	    paint.setStyle(Style.FILL);

	    
	   int pictureHeight = toEdit.getHeight();
	//   int pictureWidth = toEdit.getWidth();
	
	   int length = climate.length();
	
	   float minFontSize = 0.014f;

	   paint.setTextSize(pictureHeight * (minFontSize + (1.3f/length)));
      // paint.setAlpha(150);

    canvas.drawText(
    		climate.toString(),
   		10 , pictureHeight -200, paint);
    return toEdit;
}


 public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
      
        // Setting Dialog Title
        alertDialog.setTitle("GPS is settings");
  
        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
  
        // Setting Icon to Dialog
        //alertDialog.setIcon(R.drawable.delete);
  
        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
  
        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
            }
        });
  
        // Showing Alert Message
        alertDialog.show();
  }

@Override
protected void onPause() {
    super.onPause();
    releaseCamera();              // release the camera immediately on pause event
}

@Override
protected void onStart() {
    super.onStart();  
  //  mCamera = getCameraInstance(); //was commented out
  EasyTracker.getInstance(this).activityStart(this);  // Add this method.
  
  weather = new String[7];
  if(sharedPrefs.getBoolean("location", false)){
	  weather[0] = ("//display_location/full");
  }
  if(sharedPrefs.getBoolean("temperature", true)){
	  if(sharedPrefs.getString("units", "null").equals("metric")){
		  weather[1] = ("//current_observation/temp_c");
		  temp_unit = "c";
	  }
	  else{
		  weather[1] = ("//current_observation/temp_f");
		  temp_unit = "f";
	  }
  }
  if(sharedPrefs.getBoolean("humidity", false)){
		  weather[2] = ("//current_observation/relative_humidity");
  }
  if(sharedPrefs.getBoolean("wind", false)){
	  if(sharedPrefs.getString("units", "null").equals("metric")){
		  weather[3] = ("//current_observation/wind_kph");
		  wind_units = "kph";
	  }
	  else{
		  weather[3] = ("//current_observation/wind_mph");
		  wind_units = "mph";
	  }
  }
  if(sharedPrefs.getBoolean("feels", false)){
	  if(sharedPrefs.getString("units", "null").equals("metric")){
		  weather[4] = ("//current_observation/feelslike_c");
		  temp_unit ="c";
	  }
	  else{
		  weather[4] = ("//current_observation/feelslike_f");
		  temp_unit = "f";
	  }
  }
  if(sharedPrefs.getBoolean("visibility", false)){
	  if(sharedPrefs.getString("units", "null").equals("metric")){
		  weather[5] = ("//current_observation/visibility_km");
		  visibilty_units = "km";
	  }
	  else{
		  weather[5] = ("//current_observation/visibility_mi");
		  visibilty_units  = "mi";
	  }
  }
  if(sharedPrefs.getBoolean("preciptation", false)){
	  if(sharedPrefs.getString("units", "null").equals("metric")){
		  weather[6] = ("//current_observation/precip_today_metric");
		  precip_units = "mm";
	  }
	  else{
		  weather[6] = ("//current_observation/precip_today_in");
		  precip_units ="in";
	  }
  }

  new retrieve_weatherTask().execute();
}

@Override
protected void onResume() {
    super.onResume();  
    if(mCamera == null){
    mCamera = getCameraInstance();
    
    mCamera.getParameters().setJpegQuality(75);
    
    context = getApplicationContext();
    mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);
    }
}

protected void onRestart(){
	super.onRestart();
// 	mCamera = getCameraInstance();
}

@Override
protected void onStop() {
    super.onStop();
//      releaseCamera();              // release the camera immediately on pause event
    EasyTracker.getInstance(this).activityStop(this);  // Add this method.
}

@Override
protected void onDestroy(){
    super.onDestroy();
 //   releaseCamera();              // release the camera immediately on pause event
}



private void releaseCamera(){
    if (mCamera != null){
      //  mCamera.release();        // release the camera for other applications
      //  mCamera = null;
        
      //  mCamera.setPreviewCallback(null);
        mPreview.getHolder().removeCallback(mPreview);
        mCamera.release();
        mCamera = null;
        
    }
}

@Override
public void onLocationChanged(Location location) {
	if(climate.length() <1){
		  new retrieve_weatherTask().execute();
	}
}

@Override
public void onProviderDisabled(String provider) {

}

@Override
public void onProviderEnabled(String provider) {

}

@Override
public void onStatusChanged(String provider, int status, Bundle extras) {
	// TODO Auto-generated method stub
	
}

private Location getLastKnownLocation() {
    List<String> providers = locationManager.getProviders(true);
    Location bestLocation = null;
    for (String provider : providers) {
        Location l = locationManager.getLastKnownLocation(provider);
        if (l == null) {
            continue;
        }
        if (bestLocation == null
                || l.getAccuracy() < bestLocation.getAccuracy()) {
            bestLocation = l;
        }
    }
    if (bestLocation == null) {
        return null;
    }
    return bestLocation;
}

public static Bitmap decodeSampleImage(File f, int width, int height) {
    try {
        System.gc(); // First of all free some memory

        // Decode image size

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(new FileInputStream(f), null, o);

        // The new size we want to scale to

        final int requiredWidth = width;
        final int requiredHeight = height;

        // Find the scale value (as a power of 2)

        int sampleScaleSize = 1;

        while (o.outWidth / sampleScaleSize / 2 >= requiredWidth && o.outHeight / sampleScaleSize / 2 >= requiredHeight)
            sampleScaleSize *= 2;

        // Decode with inSampleSize

        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = sampleScaleSize;

        return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
    } catch (Exception e) {
      //  Log.d(TAG, e.getMessage()); // We don't want the application to just throw an exception
    }

    return null;
}


final Runnable Success = new Runnable() {
	   public void run() {
		    	
	   }
  };
  
  public void loadAd(){
	    // Create the interstitial.
	    interstitial = new InterstitialAd(this);
	    interstitial.setAdUnitId("ca-app-pub-2377934805759836/3876184165");

	    // Create ad request.
	    AdRequest adRequest = new AdRequest.Builder().build();

	    // Begin loading your interstitial.
	    interstitial.loadAd(adRequest);
  }
  
  // Invoke displayInterstitial() when you are ready to display an interstitial.
  public void displayInterstitial() {
    if (interstitial.isLoaded()) {
      interstitial.show();
    }
  }
  
  
  public ExifInterface saveEXIF(String oldFilePath){

      ExifInterface oldexif = null;
	try {
		oldexif = new ExifInterface(oldFilePath);
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      return oldexif;
  }

}

