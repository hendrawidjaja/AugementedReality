package com.widjaja.hendra.geolocation;

import java.text.DecimalFormat;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

@SuppressWarnings("unused")
public class MyMainActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, OnClickListener,
		LocationListener,  OnMarkerClickListener, OnSeekBarChangeListener {

	private static final int CAMERA_ZOOM_FACTOR = 17;
	private static final int CAMERA_BEARING = 45;
	private static final int CAMERA_TILT = 30;
	private static final int STROKE_WIDTH = 4;
	private static final int STROKE_COLOR = Color.RED;
	
	private static final int MILLISECONDS_PER_SECOND = 300; // Milliseconds per second
	public static final int UPDATE_INTERVAL_IN_SECONDS = 300;// Update frequency in seconds
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND	* UPDATE_INTERVAL_IN_SECONDS; 
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 60; 
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

	private GoogleMap googleMap;
	private Location location;
	private LocationRequest locRequest;
	private LocationClient locClient;
	private TextView textResLat;
	private TextView textResLong;
	private TextView textResAccr;
	private SeekBar radSeekBar;
	private Button refreshButton;
	private Button pinPointButton;
	private Button deleteButton;
	private LatLng latlng;
	private LatLng listlatlng;
	private Marker marker;
	private MarkerOptions markerOptions;
	private BitmapDescriptor bmpDesc;
	private Bitmap bitmap;
	private Circle circle;
	
	private double latitude, longitude, accuracy;
	private boolean pauseOnLocationChanged;
	private boolean refreshTriggered;
	private boolean radiusTriggered;
	private int radius;
	private int respond;
	private float[] distance = new float[2];
	private MySQLiteHelper MyDB;
	private List<MyPosition> listOfPosition;
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initialize();     
		// set OnClickListener to all Buttons
		refreshButton.setOnClickListener(this);
		pinPointButton.setOnClickListener(this);
		deleteButton.setOnClickListener(this);
		radSeekBar.setOnSeekBarChangeListener(this);
	}

	protected void initialize() {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_my_main);
		textResLat = (TextView) findViewById(R.id.textResLat);
		textResLong = (TextView) findViewById(R.id.textResLong);
		textResAccr = (TextView) findViewById(R.id.textResAccr);
		radSeekBar = (SeekBar) findViewById(R.id.radBar);
		refreshButton = (Button) findViewById(R.id.refreshButton);
		pinPointButton = (Button) findViewById(R.id.pinPointButton);
		deleteButton = (Button) findViewById(R.id.deleteButton);
		MyDB = new MySQLiteHelper(this);
		listOfPosition = MyDB.getAllPositions();
		locClient = new LocationClient(this, this, this);
		locClient.connect();
		locRequest = LocationRequest.create();
		locRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); 
		locRequest.setInterval(UPDATE_INTERVAL); 
		locRequest.setFastestInterval(FASTEST_INTERVAL); 
		pauseOnLocationChanged = false;
		radiusTriggered = false;

		respond = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
		if (respond == ConnectionResult.SUCCESS) {
			locClient = new LocationClient(this, this, this);
			locClient.connect();
		} else {
			Toast.makeText(this, "Google Play Service Error " + respond, Toast.LENGTH_LONG).show();
			setResult(0);
	 		finish();
		}

		/*
		 * My Location Manager Guard If getLastKnownLocation returns Null, call
		 * Telkom, Ask Them to turn on the GPS service
		 */
		try {
			getMyLocation();						
		} catch (Exception e) {
			Log.d("Initialize", "" + e);
		}

		if (locClient != null) {
			googleMapInitiliaze();
			radSeekBarInitiliaze();
		}
		
		/* Test to load all location to ArrayList
		 * 
		 */
		drawMarkerFromDB();
	}
	
	/*
	 * googleMap Initilization
	 */
	protected void googleMapInitiliaze() {
		try {
			SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.MyMap);
			googleMap = fm.getMap();
			googleMap.setMyLocationEnabled(true);
			googleMap.getUiSettings().setCompassEnabled(true);
			googleMap.setIndoorEnabled(true);
			
		} catch (Exception e) {
			Log.d("googleMapInitiliaze", "" + e);
		}
	}

	/*
	 * radBar Initilization
	 */
	protected void radSeekBarInitiliaze() {
		radius = 0;
		radiusTriggered = false;
		radSeekBar.setProgress(0);
		radSeekBar.setMax(10);
		pauseOnLocationChanged = true;
	}

	@Override
	public void onClick(View v) {
		// refresh Button OnCLickListener
		if (v.getId() == R.id.refreshButton) {
			if (radiusTriggered == true) {
				googleMap.clear();				
				radSeekBarInitiliaze();
			}
			try {
				getMyLocation();
				listOfPosition = MyDB.getAllPositions();
				pauseOnLocationChanged = false;
				refreshTriggered = true;
				googleMap.clear();
				drawMarkerFromDB();
				drawMarker(latlng, false);
				
				// Construct a CameraPosition focusing on Mountain View and animate the camera to that position.
				CameraPosition cameraPosition = new CameraPosition.Builder()
					.target(latlng)             // Sets the center of the map to current latitude longitude
				    .zoom(CAMERA_ZOOM_FACTOR)   // Sets the zoom
				    .bearing(CAMERA_BEARING)    // Sets the orientation of the camera to east
				    .tilt(CAMERA_TILT)          // Sets the tilt of the camera to 30 degrees
				    .build();                   // Creates a CameraPosition from the builder
				googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
			} catch (Exception e) {
				Log.d("Check the refreshOnClick", "" + e);
			}
		}
		
		// pinPoint Button OnCLickListener
		if (v.getId() == R.id.pinPointButton) {
			if (radiusTriggered == true) {
				googleMap.clear();
				radSeekBarInitiliaze();
			} 
			try {
				pauseOnLocationChanged = false;
				MyDB.addPosition(new MyPosition("" + latitude, "" + longitude));
				Toast.makeText(getBaseContext(), "Marker is added to the Map", Toast.LENGTH_SHORT).show();
				
				// Draw a new Marker
				googleMap.clear();
				drawMarkerFromDB();
				drawMarker(latlng, false);
			} catch (Exception e) {
				Log.d("Check the pinPointOnClick", "" + e);
			}
		}
		
		if (v.getId() == R.id.deleteButton) {
			pauseOnLocationChanged = false;
			googleMap.clear();
			radSeekBarInitiliaze();
			
			for (MyPosition myposition : listOfPosition) {
				MyDB.deletePosition(myposition);
			}
			listOfPosition.clear();
		}		
	}

	private void drawMarker(LatLng point, boolean newMarker) {		
		if (newMarker == true) {
			marker = googleMap.addMarker(new MarkerOptions().position(point).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));			
		} else {
		// Creating an instance of MarkerOptions
		MarkerOptions markerOptions = new MarkerOptions().position(point).title("You are here");
		// Adding marker on the Google Map
		googleMap.addMarker(markerOptions).showInfoWindow();
		}
		// Adding ClickListener to the marker
		googleMap.setOnMarkerClickListener(this);
	}
	
	private void drawCircle (LatLng point, int radius, boolean newCircle) {
		if ( newCircle == true ) {
			circle = googleMap.addCircle(new CircleOptions().center(point).radius(radius).strokeWidth(STROKE_WIDTH).strokeColor(STROKE_COLOR));
		} else {
			CircleOptions circleOptions = new CircleOptions().center(point);
			googleMap.addCircle(circleOptions);
		}
	}

	private void drawMarkerFromDB() {
		for (MyPosition myposition : listOfPosition) {
			listlatlng = new LatLng(Double.parseDouble(myposition.getLatitude()), Double.parseDouble(myposition.getLongitude()));
			drawMarker(listlatlng, true);	
		}
	}
	
	private void getMyLocation() {
		// We expect that Location Client is already connected
		if (locClient != null && locClient.isConnected()) {
			location = locClient.getLastLocation();
			latitude = location.getLatitude();
			longitude = location.getLongitude();
			accuracy = location.getAccuracy();
			textResLat.setText("" + latitude);
			textResLong.setText("" + longitude);
			textResAccr.setText("" + accuracy);
			latlng = new LatLng(latitude, longitude);
		}
	}
	
	@Override
	public void onLocationChanged(Location location) {
		// Clear to go to listen to location changed
		if (pauseOnLocationChanged == false) {
		// Location has changed
		// Refresh all values
		getMyLocation();
		// Clear the map
		// Draw old saved postion marker, Draw current position marker
		googleMap.clear();
		drawMarkerFromDB();		
		marker = googleMap.addMarker(new MarkerOptions().position(latlng));
		drawMarker(latlng, false);
		googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, CAMERA_ZOOM_FACTOR));
		}	
	}
		
	@Override
	public boolean onMarkerClick(Marker marker) {
		pauseOnLocationChanged = true;
		if (radiusTriggered = true) {
			if (refreshTriggered = true) {
				googleMap.clear();		
			
			// because the map has been cleared, all marker are gone
			// need to put it back to the map, get all the marker from the database
			for (MyPosition myposition : listOfPosition) {
				listlatlng = new LatLng(Double.parseDouble(myposition.getLatitude()), Double.parseDouble(myposition.getLongitude()));
				drawMarker(latlng, false);		
				drawCircle(latlng, radius, true);			
				
				Location.distanceBetween(latitude, longitude, listlatlng.latitude, listlatlng.longitude, distance);
				if (distance[0] < radius) {
					marker = googleMap.addMarker(new MarkerOptions().position(listlatlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
				} else {
					drawMarker(listlatlng, true);
				}
			}
			}
		} else {
			
		}
		return true;
	}
	
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		pauseOnLocationChanged = true;
		radius = arg1;
		radiusTriggered = true;
		
		// clear the map at beginning
		// pause the OnLocationChanged
		// send the slider value to global variable
		// and initialized the triggered		
		if (refreshTriggered = true) {
			googleMap.clear();		
		
		// because the map has been cleared, all marker are gone
		// need to put it back to the map, get all the marker from the database
		for (MyPosition myposition : listOfPosition) {
			listlatlng = new LatLng(Double.parseDouble(myposition.getLatitude()), Double.parseDouble(myposition.getLongitude()));
			drawMarker(latlng, false);		
			drawCircle(latlng, arg1, true);						
			Location.distanceBetween(latitude, longitude, listlatlng.latitude, listlatlng.longitude, distance);
			if (distance[0] < radius) {
				marker = googleMap.addMarker(new MarkerOptions().position(listlatlng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
			} else {
				drawMarker(listlatlng, true);
			}
		}
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) { }

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		if (refreshTriggered = true) {
			radSeekBar.setSecondaryProgress(seekBar.getProgress());	
		}
	}
	
	@Override
	public void onConnectionFailed(ConnectionResult arg0) { }

	@Override
	public void onConnected(Bundle arg0) { }

	@Override
	public void onDisconnected() { }
	
	@Override
	public void onBackPressed() {
		try {
			locClient.removeLocationUpdates(this);
		}
		catch(Exception e) {
			Log.d(getPackageName(), "" + e);
		}
	 		setResult(0);
	 		finish();
	}	
}