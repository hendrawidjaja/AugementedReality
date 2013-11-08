package com.widjaja.hendra.geolocation;

import java.util.List;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

@SuppressWarnings("unused")
public class MyMainActivity extends FragmentActivity implements LocationListener, LoaderCallbacks<Cursor> {
	
	private static final long MIN_DISTANCE = 25; 		// in Meters
	private static final long MIN_TIME = 1000; 			// in Milliseconds
	private static final int CAMERA_ZOOM_FACTOR = 19;
	
	private GoogleMap googleMap;
	private Geocoder geocoder;
	private Criteria criteria;
	private Location location;
	private LocationManager locManager;
	private TextView textResLat;	
	private TextView textResLong;
	private TextView textResAccr;
	private SeekBar radSeekBar;
	private Button refreshButton;
	private Button pinPointButton;
	private Button deleteButton;
	private LatLng latlng;
	private Marker marker;
	private Canvas canvas;
	private Paint paint;	
	private TextPaint paintText;
	private BitmapDescriptor bmpDesc;
	private Bitmap bitmap;
	
	private String provider;
	private double latitude, longitude;
	private double accuracy;
	private int radius;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_main);
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
  
        initialize();
        
        // Invoke LoaderCallbacks to retrieve and draw already saved locations in map
        getSupportLoaderManager().initLoader(0, null, this);    	
        
        refreshButton.setOnClickListener(new OnClickListener() {	
			@Override
			public void onClick(View v) {
        		try {
				locManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
				location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				accuracy = location.getAccuracy();
				}
        		catch (Exception e) { }
			}
		});
        
        pinPointButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {

				// Drawing marker on the map
				drawMarker(latlng);
				
				// Creating an instance of ContentValues
				ContentValues contentValues = new ContentValues();
				
				// Setting latitude in ContentValues
	            contentValues.put(LocationsDB.FIELD_LAT, latlng.latitude);
	            
	            // Setting longitude in ContentValues
	            contentValues.put(LocationsDB.FIELD_LNG, latlng.longitude);
	            
	            // Setting zoom in ContentValues
	            contentValues.put(LocationsDB.FIELD_ZOOM, googleMap.getCameraPosition().zoom);
	            
	            // Creating an instance of LocationInsertTask
				LocationInsertTask insertTask = new LocationInsertTask();
				
				// Storing the latitude, longitude and zoom level to SQLite database
				insertTask.execute(contentValues);   
	
//		        Toast.makeText(getBaseContext(), "Marker is added to the Map", Toast.LENGTH_SHORT).show();	
			}
		});
         
        deleteButton.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				// Removing all markers from the Google Map
				googleMap.clear();
				
				// Creating an instance of LocationDeleteTask
				LocationDeleteTask deleteTask = new LocationDeleteTask();
				
				// Deleting all the rows from SQLite database table
				deleteTask.execute();
			}
		});
        
        radSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {			
			public void onStopTrackingTouch(SeekBar seekBar) {
				radSeekBar.setSecondaryProgress(seekBar.getProgress());			
			}
			
			public void onStartTrackingTouch(SeekBar seekBar) { }
			
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				googleMap.clear();
				marker = googleMap.addMarker(new MarkerOptions().position(latlng));
				googleMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
				googleMap.animateCamera(CameraUpdateFactory.zoomTo(CAMERA_ZOOM_FACTOR));
				drawRadiusCircle(progress);
				radius = progress;				
			}
		});
    }

	private void drawMarker(LatLng point){
    	// Creating an instance of MarkerOptions
    	MarkerOptions markerOptions = new MarkerOptions();					
    		
    	// Setting latitude and longitude for the marker
    	markerOptions.position(point);
    		
    	// Adding marker on the Google Map
    	googleMap.addMarker(markerOptions);    		
    }
	
	private class LocationInsertTask extends AsyncTask<ContentValues, Void, Void>{
		@Override
		protected Void doInBackground(ContentValues... contentValues) {
			
			/** Setting up values to insert the clicked location into SQLite database */           
            getContentResolver().insert(LocationsContentProvider.CONTENT_URI, contentValues[0]);			
			return null;
		}		
	}
	
	private class LocationDeleteTask extends AsyncTask<Void, Void, Void>{
		@Override
		protected Void doInBackground(Void... params) {
			
			/** Deleting all the locations stored in SQLite database */
            getContentResolver().delete(LocationsContentProvider.CONTENT_URI, null, null);			
			return null;
		}		
	}	
	
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		
		// Uri to the content provider LocationsContentProvider
		Uri uri = LocationsContentProvider.CONTENT_URI;
		
		// Fetches all the rows from locations table
        return new CursorLoader(this, uri, null, null, null, null);
	}	
	
	
	private void drawRadiusCircle(int radius) {
		paintText = new TextPaint();
        int radiusMDouble = radius * 2;
        int d = 200; // diameter 
        int dHalf = (d / 2);
        
        bitmap = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint  = new Paint();
        paint.setColor(getResources().getColor(R.color.black_overlay));
        canvas.drawCircle(dHalf, dHalf, dHalf, paint);
        
        paint.setColor(Color.RED); 
        paint.setTextSize(20); 
        canvas.drawText("T", dHalf, dHalf, paint);        

        bmpDesc = BitmapDescriptorFactory.fromBitmap(bitmap);
        googleMap.addGroundOverlay(new GroundOverlayOptions().image(bmpDesc).position(latlng, radiusMDouble, radiusMDouble).transparency(0.4f));
        googleMap.getUiSettings().setCompassEnabled(true);
    }
	
    protected void initialize() {
    	textResLat = (TextView) findViewById(R.id.textResLat); 	
    	textResLong = (TextView) findViewById(R.id.textResLong); 
    	textResAccr = (TextView) findViewById(R.id.textResAccr);
    	radSeekBar = (SeekBar) findViewById(R.id.radBar);
    	refreshButton = (Button) findViewById(R.id.refreshButton);
    	pinPointButton = (Button) findViewById(R.id.pinPointButton);
    	deleteButton = (Button) findViewById(R.id.deleteButton);
    	
    	criteria = new Criteria();
    	criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
//        criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
//        criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);  
     
        locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        provider = locManager.getBestProvider(criteria, false);
        
        /* 
         * My Location Manager Guard
         * If getLastKnownLocation returns Null, call Telkom, Ask Them to turn on the GPS service
         * 
         */
        try {
        locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, MyMainActivity.this);
      	location = locManager.getLastKnownLocation(provider);

      	if (location == null) {
            Toast.makeText(getApplicationContext(),"Location Not found",Toast.LENGTH_SHORT).show();
         } else {
           geocoder = new Geocoder(getApplicationContext());
           try {
        	   List<Address> user = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
               latitude = user.get(0).getLatitude();
               longitude = user.get(0).getLongitude();
           }
           catch (Exception e) {
        	  Log.d(getPackageName(), "" + e);
           }
           
            latitude = location.getLatitude();
       		longitude = location.getLongitude();
       		accuracy = location.getAccuracy();
       		textResLat.setText("" + latitude );
       		textResLong.setText("" + longitude );
       		textResAccr.setText("" + accuracy);
       		latlng = new LatLng(latitude, longitude);     
        }
        } 
        catch (Exception e) {
        	Log.d(getPackageName(), "" + e);
        }
      
      	googleMapInitiliaze();
      	radSeekBarInitiliaze();
//        db = new MySQLiteHelper(this);
     }
 
    protected void googleMapInitiliaze() {
    	/*
    	 * googleMap Initilization
    	 */
    	try {
    	SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MyMap);
    	googleMap = fm.getMap(); 
    	googleMap.setMyLocationEnabled(true);
    	googleMap.moveCamera((CameraUpdateFactory.newLatLng(latlng)));
    	googleMap.animateCamera(CameraUpdateFactory.zoomTo(CAMERA_ZOOM_FACTOR));   	
    	MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude)).title("Hello Maps");   	
    	googleMap.addMarker(marker);
    	}
    	catch (Exception e) {
    		Log.d(getPackageName(), "" + e);
    	}
    }
    
    protected void radSeekBarInitiliaze() {
    	/*
      	 *  radBar Initilization
      	 */
      	radSeekBar.setProgress(0);
      	radSeekBar.setMax(10);
    }   
   
	@Override
	public void onLocationChanged(Location location) {
		// Listener detect a location changes
		// Fetching all information 

		try {
		location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				
		if (location != null) {
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			accuracy = location.getAccuracy();
		}
				
		textResLat.setText("" + latitude);
		textResLong.setText("" + longitude);
		textResAccr.setText("" + accuracy);	
		} 
		catch (Exception e) {
			Log.d(getPackageName(), "" + e);
		}
		googleMap.clear();
		latlng = new LatLng(latitude, longitude);
	
		if (marker != null){
             marker.remove();
         }

		marker = googleMap.addMarker(new MarkerOptions().position(latlng));
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
		googleMap.animateCamera(CameraUpdateFactory.zoomTo(CAMERA_ZOOM_FACTOR));
		
		if (radius != 0) {
			drawRadiusCircle(radius);
		}
	}
	
	@Override
	public void onProviderDisabled(String provider) { }

	@Override
	public void onProviderEnabled(String provider) { }

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) { }	
	
	@Override
	public void onBackPressed()
	{
		 try
		 {           
		     locManager.removeUpdates(this);
		     locManager = null;
		 }
		 catch(Exception e) {
			 Log.d(getPackageName(), "" + e);
		 }
		 setResult(0);
		 finish();
	}






	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		// TODO Auto-generated method stub
		
		int locationCount = 0;
		double lat=0;
		double lng=0;
		float zoom=0;
		
		// Number of locations available in the SQLite database table
		locationCount = arg1.getCount();
		
		// Move the current record pointer to the first row of the table
		arg1.moveToFirst();
		
		for(int i=0;i<locationCount;i++){
			
			// Get the latitude
			lat = arg1.getDouble(arg1.getColumnIndex(LocationsDB.FIELD_LAT));
			
			// Get the longitude
			lng = arg1.getDouble(arg1.getColumnIndex(LocationsDB.FIELD_LNG));
			
			// Get the zoom level
			zoom = arg1.getFloat(arg1.getColumnIndex(LocationsDB.FIELD_ZOOM));
			
			// Creating an instance of LatLng to plot the location in Google Maps
			LatLng location = new LatLng(lat, lng);
			
			// Drawing the marker in the Google Maps
			drawMarker(location);
			
			// Traverse the pointer to the next row
			arg1.moveToNext();
		}
		
		if(locationCount>0){
			// Moving CameraPosition to last clicked position
	        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(lat,lng)));
	        
	        // Setting the zoom level in the map on last position  is clicked
            googleMap.animateCamera(CameraUpdateFactory.zoomTo(zoom));  
		}    
			
	}






	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO Auto-generated method stub
		
	}
}