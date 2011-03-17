package com.reality;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

public class RealityActivity extends Activity implements SensorEventListener,
		LocationListener {

	public static final String TAG = "RealityActivity";
	public static final boolean USE_CAMERA = false;

	private Camera camera;
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;

	private LocationManager mLocationManager;
	private SensorManager mSensorManager;

	public static final int MIN_TIME_BETWEEN_LOCATION_UPDATES = 1000;
	public static final int MIN_DISTANCE_BETWEEN_LOCATION_UPDATES = 3;

	private RealityOverlayView mSurface;
	private RealitySmallCompassView mCompassView;
	private RealityDirectionView mDirectionView = null;
	private TextView compassHeadingLabel;

	private View mPlaceDescriptionView;
	private TextView mTitleView;
	private TextView mDescriptionView;

	private String[] headingNames = HeadingString.getHeadingNames();

	private RealityLocationListener mLocationListener;
	private RealityOrientationListener mOrientationListener;

	private ProgressDialog mLoadingDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.reality_activity);

		if (USE_CAMERA) {
			preview = (SurfaceView) findViewById(R.id.preview);
			previewHolder = preview.getHolder();
			previewHolder.addCallback(mSurfaceCallback);
			previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		/*
		 * Views
		 */
		mSurface = (RealityOverlayView) findViewById(R.id.surface);
		compassHeadingLabel = (TextView) findViewById(R.id.compass_heading);
		mCompassView = (RealitySmallCompassView) findViewById(R.id.compass);

		mSurface.registerForUpdates(mCompassView);

		/*
		 * Sensors
		 */
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

		/*
		 * Listeners
		 */
		mLocationListener = new RealityLocationListener();
		mOrientationListener = new RealityOrientationListener();

		mOrientationListener.registerForUpdates(this);
		mOrientationListener.registerForUpdates(mSurface);
		mOrientationListener.registerForUpdates(mCompassView);

		mLocationListener.registerForStatusUpdates(this);
		mLocationListener.registerForUpdates(mSurface);
		// mLocationListener.registerForUpdates(mOrientationListener);

		mPlaceDescriptionView = findViewById(R.id.place_description);
		mTitleView = (TextView) findViewById(R.id.title);
		mDescriptionView = (TextView) findViewById(R.id.description);

		// mStatusLabel.setText("Loading content...");
		// mStatusLabel.setVisibility(View.VISIBLE);

		/*
		 * Passed in objects.
		 */
		Object[] places = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			try {
				places = (Object[]) extras.getSerializable(Place.class
						.toString());
			} catch (ClassCastException e) {
				finish();
				return;
			}
		}
		new DownloadPlacesTask().execute(places);

		Log.d(TAG, "onCreate()");

		mLoadingDialog = ProgressDialog.show(this, "",
				"Loading. Please wait...", true);
	}

	@Override
	public void onResume() {
		super.onResume();

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String bestProvider = mLocationManager.getBestProvider(criteria, false);

		mLocationManager.requestLocationUpdates(bestProvider,
				MIN_TIME_BETWEEN_LOCATION_UPDATES,
				MIN_DISTANCE_BETWEEN_LOCATION_UPDATES, mLocationListener);

		Sensor gsensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor msensor = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		mSensorManager.registerListener(mOrientationListener, gsensor,
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(mOrientationListener, msensor,
				SensorManager.SENSOR_DELAY_FASTEST);

		mSurface.setWillNotDraw(false); // draw automatically
		mCompassView.setWillNotDraw(false); // draw automatically

		if (mSurface.hasDisplayedBefore()) {
			Location location = getInitialLocation();
			if (location != null) {
				Log.d(TAG,
						"onLocationChanged() - Initial (possibly innaccurate) location found. "
								+ mSurface.isInitialized());

				mLocationListener.onLocationChanged(location);
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		/*
		 * Remove the update listeners of the compass and GPS.
		 */
		mLocationManager.removeUpdates(mLocationListener);
		mSensorManager.unregisterListener(mOrientationListener);

		mCompassView.setWillNotDraw(true); // don't draw automatically
		mSurface.setWillNotDraw(true); // don't draw automatically
	}

	@Override
	public void onStop() {
		super.onStop();

		/*
		 * Need to tell the OS to do a garbage collection to ensure we have
		 * enough memory if the application is re-launched.
		 */
		mSurface.unbindBitmaps();
		System.gc();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * Get last known location.
	 */
	private Location getInitialLocation() {
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		List<String> providers = lm.getProviders(true);
		Location location = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			location = lm.getLastKnownLocation(providers.get(i));
			if (location != null) {
				break;
			}
		}

		return location;
	}

	private class DownloadPlacesTask extends
			AsyncTask<Object[], Void, List<Place>> {
		protected List<Place> doInBackground(Object[]... p) {
			List<Place> placeList = null;

			Object[] places = p[0];
			if (places != null) {
				placeList = new ArrayList<Place>();
				Place place;
				for (Object o : places) {
					if (o != null) {
						place = (Place) o;
						if (place.coordinate != null) {
							placeList.add(place);
						}
					}
				}
			} else {
				/*
				 * This is for debugging purposes only.
				 */
				placeList = getPlacesTest();
			}

			return placeList;
		}

		protected void onPostExecute(final List<Place> places) {
			runOnUiThread(new Runnable() {

				public void run() {
					updateWithPlaces(places);

					Location location = getInitialLocation();
					if (location != null) {
						Log.d(TAG,
								"onLocationChanged() - Initial (possibly innaccurate) location found.");

						mLocationListener.onLocationChanged(location);
					}

					mLoadingDialog.dismiss();
					mLoadingDialog = null;
				}

			});
		}

	}

	/**
	 * Update the surface UI with the list of places.
	 * 
	 * @param places
	 */
	private void updateWithPlaces(final List<Place> places) {
		mSurface.addAllOverlays(places);
	}

	private final List<Place> getPlacesTest() {
		List<Place> places = new ArrayList<Place>();

		Coordinate loc1 = new Coordinate();
		loc1.setLatitude(45.309485);
		loc1.setLongitude(-75.90909);
		Coordinate loc2 = new Coordinate();
		loc2.setLatitude(45.296928);
		loc2.setLongitude(-75.9272);
		Coordinate loc3 = new Coordinate();
		loc3.setLatitude(45.294332);
		loc3.setLongitude(-75.901537);
		Coordinate loc4 = new Coordinate();
		loc4.setLatitude(45.425203);
		loc4.setLongitude(-75.700092);
		Coordinate loc5 = new Coordinate();
		loc5.setLatitude(45.382575);
		loc5.setLongitude(-75.699352);
		Coordinate loc6 = new Coordinate();
		loc6.setLatitude(45.346354);
		loc6.setLongitude(-75.893555);
		Coordinate loc7 = new Coordinate();
		loc7.setLatitude(45.304595);
		loc7.setLongitude(-75.811844);
		Coordinate loc8 = new Coordinate();
		loc8.setLatitude(45.252655);
		loc8.setLongitude(-75.890808);
		Coordinate loc9 = new Coordinate();
		loc9.setLatitude(45.304112);
		loc9.setLongitude(-75.968742);
		Coordinate loc10 = new Coordinate();
		loc10.setLatitude(45.304248);
		loc10.setLongitude(-75.892664);
		Coordinate loc11 = new Coordinate();
		loc11.setLatitude(48.951366);
		loc11.setLongitude(-75.849609);
		Coordinate loc12 = new Coordinate();
		loc12.setLatitude(45.163642);
		loc12.setLongitude(-75.417023);

		Place place1 = new Place("AMC", "description", "NA", loc1);
		Place place2 = new Place("ScotiaBank Place", "description", "NA", loc2);
		Place place3 = new Place("Walter Baker Park", "description", "NA", loc3);
		Place place4 = new Place("Parliament", "description", "NA", loc4);
		Place place5 = new Place("Dunton Tower", "description", "NA", loc5);
		Place place6 = new Place("North", "description", "NA", loc6);
		Place place7 = new Place("East", "description", "NA", loc7);
		Place place8 = new Place("South", "description", "NA", loc8);
		Place place9 = new Place("West", "description", "NA", loc9);
		Place place10 = new Place("Doug's", "description", "NA", loc10);
		Place place11 = new Place("North 2", "description", "NA", loc11);
		Place place12 = new Place("Random place", "description", "NA", loc12);

		places.add(place1);
		places.add(place2);
		places.add(place3);
		places.add(place4);
		places.add(place5);
		places.add(place6);
		places.add(place7);
		places.add(place8);
		places.add(place9);
		places.add(place10);
		places.add(place11);
		places.add(place12);

		return places;
	}

	public static void setCameraDisplayOrientation(Activity activity,
			int cameraId, android.hardware.Camera camera) {
		// take a look at camera.setDisplayOrientation()
		// android.hardware.Camera.CameraInfo info = new
		// android.hardware.Camera.CameraInfo();
		// android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			return;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		// if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
		// result = (info.orientation + degrees) % 360;
		// result = (360 - result) % 360; // compensate the mirror
		// } else { // back-facing
		// result = (info.orientation - degrees + 360) % 360;
		// }

		result = degrees;
		camera.setDisplayOrientation(result);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.reality, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.navigate:
			if (mDirectionView == null) {
				mDirectionView = (RealityDirectionView) ((ViewStub) findViewById(R.id.directions_stub))
						.inflate();

				mOrientationListener.registerForUpdates(mDirectionView);
				mDirectionView.setWillNotDraw(false);
				item.setTitle("Stop Navigation");
			} else {
				if (mDirectionView.getVisibility() == View.VISIBLE) {
					mDirectionView.setVisibility(View.GONE);

					mOrientationListener.deregister(mDirectionView);
					mDirectionView.setWillNotDraw(true);
					item.setTitle("Start Navigation");
				} else {
					mDirectionView.setVisibility(View.VISIBLE);

					mOrientationListener.registerForUpdates(mDirectionView);
					mDirectionView.setWillNotDraw(false);
					item.setTitle("Stop Navigation");
				}
			}

			return true;
		case R.id.directory:
			onDirectoryClick(null);
			return true;
		case R.id.map:
			onViewMapClick(null);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onDirectoryClick(View v) {
		this.startActivity(new Intent(this, PlacesListActivity.class));
	}

	public void onViewMapClick(View v) {
		this.startActivity(new Intent(this, NavigationMapActivity.class));
	}

	/*
	 * Camera surface callbacks.
	 */
	private final SurfaceHolder.Callback mSurfaceCallback = new SurfaceHolder.Callback() {
		public void surfaceCreated(SurfaceHolder holder) {
			camera = Camera.open();
			camera.setDisplayOrientation(90);

			try {
				camera.setPreviewDisplay(previewHolder);
				camera.startPreview();
			} catch (Throwable t) {
				Log.e("PreviewDemo-mSurfaceCallback",
						"Exception in setPreviewDisplay()", t);
				Toast.makeText(RealityActivity.this, t.getMessage(),
						Toast.LENGTH_LONG).show();
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int width,
				int height) {
			if (camera != null) {
				Camera.Parameters parameters = camera.getParameters();

				// parameters.setPreviewSize(width, height);
				parameters.setPictureFormat(PixelFormat.JPEG);
				parameters.set("orientation", "portrait");
				camera.setParameters(parameters);
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	};

	/*
	 * SensorEventListener methods.
	 */

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		Log.d(TAG, "Sensor Accuracy changed: " + sensor + ", " + accuracy);
	}

	public void onSensorChanged(SensorEvent event) {
		final float[] values = event.values;

		int val = (int) values[0];

		String displayDir = headingNames[val];

		if (!compassHeadingLabel.getText().equals(displayDir)) {
			compassHeadingLabel.setText(displayDir);
		}
	}

	/*
	 * LocationListener methods.
	 */

	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		String statusString;
		switch (status) {
		case LocationProvider.OUT_OF_SERVICE:
			statusString = "Out of service";
			break;
		case LocationProvider.TEMPORARILY_UNAVAILABLE:
			statusString = "Temporarily unavailable";
			break;
		case LocationProvider.AVAILABLE:
			statusString = "Available";
			break;
		default:
			statusString = "";
		}

		Toast.makeText(this,
				statusString + " - Using " + extras.getInt("satellites")
						+ " satellites", Toast.LENGTH_SHORT);
	}

	/*
	 * MotionEvent methods.
	 */

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View view = mPlaceDescriptionView;

			Place place = mSurface.getPlaceAt((int) event.getX(),
					(int) event.getY());

			boolean isPlace = place != null;

			int visibility = isPlace ? View.VISIBLE : View.GONE;

			if (isPlace) {
				mTitleView.setText("Place");
				mDescriptionView.setText("Description");
			}

			view.setVisibility(visibility);
		}

		return true;
	}

}
