package com.reality;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
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

import com.common.BearingDirectionManager;
import com.common.BearingWaypoint;
import com.common.Coordinate;
import com.common.DirectionEvent;
import com.common.DirectionManager;
import com.common.DirectionObserver;
import com.common.Directions;
import com.common.LocationDirectionManager;
import com.common.LocationWaypoint;
import com.common.Place;
import com.common.Waypoint;
import com.common.XmlLocationImporter;

public class RealityActivity extends Activity implements LocationListener,
		DirectionObserver {

	public static final String TAG = "RealityActivity";
	public static final boolean USE_CAMERA = true;

	private static final int DIALOG_LOADING_GPS = 0;
	private static final int DIALOG_LOADING_DIRECTIONS = 1;
	private static final int DIALOG_GPS_UNAVAILABLE = 2;
	private static final int DIALOG_DOWNLOADING_DIRECTIONS = 3;
	private static final int DIALOG_DOWNLOADING_DIRECTIONS_FAILED = 4;

	public static final String DIRECTIONS_FILE_NAME = "raven-graph";

	private Camera camera;
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;

	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private DirectionManager<?> mDirectionManager = null;

	public static final int ACQUIRE_GPS_TIMEOUT = 15000;
	public static final int MIN_TIME_BETWEEN_LOCATION_UPDATES = 1000;
	public static final int MIN_DISTANCE_BETWEEN_LOCATION_UPDATES = 3;

	private RealityOverlayView mSurface;
	private RealitySmallCompassView mCompassView;
	private RealityDirectionView mDirectionView = null;

	private TextView mStatusLabel;
	private TextView mDirectionsLabel;

	private View mPlaceDescriptionView;
	private TextView mTitleView;
	private TextView mDescriptionView;

	private RealityLocationListener mLocationListener;
	private RealityOrientationListener mOrientationListener;

	private Timer mGpsTimer;

	protected static final String PROXIMITY_ALERT = new String(
			"android.intent.action.PROXIMITY_ALERT");
	protected final IntentFilter proximitylocationIntentFilter = new IntentFilter(
			PROXIMITY_ALERT);
	protected final Intent proximitylocationIntent = new Intent(PROXIMITY_ALERT);

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
		mCompassView = (RealitySmallCompassView) findViewById(R.id.compass);
		mStatusLabel = (TextView) findViewById(R.id.status_output);
		mDirectionsLabel = (TextView) findViewById(R.id.directions_output);

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

		mOrientationListener.registerForUpdates(mSurface);
		mOrientationListener.registerForUpdates(mCompassView);

		mLocationListener.registerForUpdates(this);
		mLocationListener.registerForUpdates(mSurface);

		mPlaceDescriptionView = findViewById(R.id.place_description);
		mTitleView = (TextView) findViewById(R.id.title);
		mDescriptionView = (TextView) findViewById(R.id.description);

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
	}

	@Override
	public void onResume() {
		super.onResume();

		mLocationListener.reset();

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
				SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(mOrientationListener, msensor,
				SensorManager.SENSOR_DELAY_GAME);

		mSurface.setWillNotDraw(false); // draw automatically
		mCompassView.setWillNotDraw(false); // draw automatically
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean flag = super.onPrepareOptionsMenu(menu);

		// menu.clear();
		MenuItem item = menu.getItem(0);

		if (mDirectionManager == null) {
			item.setTitle("Start Navigation");
		} else {
			item.setTitle("Stop Navigation");
		}

		return flag;
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
			if (mDirectionView == null
					|| mDirectionView.getVisibility() == View.GONE) {
				new DownloadDirectionsTask().execute((Place) null);
			} else {
				dismissDirectionOverlay();
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
	 * Dialog methods.
	 */

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_LOADING_GPS:
			dialog = ProgressDialog.show(this, null,
					"Acquiring GPS signal. Please wait...");
			break;
		case DIALOG_LOADING_DIRECTIONS:
			dialog = ProgressDialog.show(this, null,
					"Loading directions. Please wait...");
			break;
		case DIALOG_GPS_UNAVAILABLE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Unable to acquire satellite signal")
					.setMessage(
							"The reality compass will still work, but navigation will not.")
					.setCancelable(true)
					.setNegativeButton("Close",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_DOWNLOADING_DIRECTIONS:
			dialog = ProgressDialog.show(this, null,
					"Downloading directions. Please wait...");
			break;
		case DIALOG_DOWNLOADING_DIRECTIONS_FAILED:
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Unable to load directions")
					.setMessage("No directions were found.")
					.setCancelable(true)
					.setNegativeButton("Close",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	/*
	 * SensorEventListener methods.
	 */

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	// public void onSensorChanged(SensorEvent event) {
	// final float[] values = event.values;
	//
	// int val = (int) values[0];
	//
	// String displayDir = headingNames[val];
	//
	// if (!compassHeadingLabel.getText().equals(displayDir)) {
	// compassHeadingLabel.setText(displayDir);
	// }
	// }

	/*
	 * MotionEvent methods.
	 */

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			View view = mPlaceDescriptionView;

			Place place = mSurface.getPlaceAt((int) event.getX(),
					(int) event.getY());

			if (place != null) {
				mTitleView.setText(place.name);
				mDescriptionView.setText(Place
						.getDistanceString(place.distance));
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}
		}

		return true;
	}

	/*
	 * Button listeners.
	 */

	public void onNavigateToPlaceClick(View v) {
		mPlaceDescriptionView.setVisibility(View.GONE);

		Place place = mSurface.getSelectedPlace();
		if (place != null) {
			Coordinate coord = place.coordinate;
			if (coord != null) {

			}
		}
	}

	public void onPlaceInfoClick(View v) {
		Place place = mSurface.getSelectedPlace();
		// TODO: Shouldn't ever be null...
		if (place != null) {
			Intent intent = new Intent(this, PlacesActivity.class);
			intent.putExtra(Place.class.toString(), place);
			this.startActivity(intent);
		}
	}

	private void dismissDirectionOverlay() {
		mOrientationListener.deregisterForUpdates(mDirectionView);

		deregisterDirectionManager();

		mDirectionView.setVisibility(View.GONE);
		mDirectionView.setWillNotDraw(true);

		mDirectionsLabel.setVisibility(View.GONE);
		mDirectionManager = null;
	}

	private void registerDirectionManager(DirectionManager manager) {
		mDirectionManager = manager;

		if (manager instanceof LocationListener) {
			LocationListener listener = (LocationListener) manager;
			mLocationListener.registerForUpdates(listener);

			// Provide a quick location update. An up-to-date GPS location might
			// be seconds away, so provide a recent update so that the user
			// doesn't wait.
			Location location = mLocationListener.getLastKnownLocation();
			if (location != null) {
				listener.onLocationChanged(location);
			}
		} else {
			mOrientationListener
					.registerForUpdates((SensorEventListener) manager);
		}
	}

	private synchronized void deregisterDirectionManager() {
		if (mDirectionManager instanceof LocationListener) {
			mLocationListener
					.deregisterForUpdates((LocationListener) mDirectionManager);
		} else {
			mOrientationListener
					.deregisterForUpdates((SensorEventListener) mDirectionManager);
		}
	}

	/*
	 * Direction listener.
	 */

	public void onDirectionsChanged(DirectionEvent event) {
		if (event.status == DirectionEvent.STATUS_ARRIVED) {
			// dismissDirectionOverlay();

			Log.d(TAG, "Arrived at destination");
			mDirectionsLabel.setText("Arrived!");
		} else {
			mDirectionsLabel.setText(Place.getDistanceString(event.distance));
		}
	}

	/*
	 * Location Listeners.
	 */

	public void onLocationChanged(Location location) {
		if (mLocationListener.hasLocation()) {
			if (mGpsTimer != null) {
				mGpsTimer.cancel();
				mGpsTimer = null;

				dismissDialog(DIALOG_LOADING_GPS);
			}

			if (!mOrientationListener.hasLocation()) {
				mOrientationListener.onLocationChanged(location);
			}
		}

		mStatusLabel.setText("Accuracy: " + location.getAccuracy());
	}

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status != LocationProvider.AVAILABLE) {
			showDialog(DIALOG_LOADING_GPS);
		}
	}

	/*
	 * Async tasks.
	 */

	private class DownloadPlacesTask extends
			AsyncTask<Object[], Void, List<Place>> {
		protected List<Place> doInBackground(Object[]... p) {
			runOnUiThread(new Runnable() {

				public void run() {
					if (!mLocationListener.hasLocation()) {
						showDialog(DIALOG_LOADING_GPS);

						TimerTask task = new TimerTask() {

							@Override
							public void run() {
								runOnUiThread(new Runnable() {

									public void run() {
										dismissDialog(DIALOG_LOADING_GPS);
										showDialog(DIALOG_GPS_UNAVAILABLE);
									}

								});
							}

						};
						mGpsTimer = new Timer();
						mGpsTimer.schedule(task, ACQUIRE_GPS_TIMEOUT);
					}
				}

			});

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

				if (places.length == 1) {
					place = (Place) places[0];
					float bearing = place.bearing;
					if (bearing >= 0) {
						Waypoint waypoint = new BearingWaypoint(bearing);
						waypoint.setPlace(place);
						new GenerateDirectionsTask().execute(waypoint);
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
			updateWithPlaces(places);

			Location location = mLocationListener.getLastKnownLocation();
			if (location != null) {
				mLocationListener.onLocationChanged(location);
			}
		}

	}

	private class DownloadDirectionsTask extends
			AsyncTask<Place, Void, Waypoint> {
		protected Waypoint doInBackground(Place... place) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_DOWNLOADING_DIRECTIONS);
				}
			});

			return null;
		}

		protected void onPostExecute(final Waypoint waypoint) {
			dismissDialog(DIALOG_DOWNLOADING_DIRECTIONS);

			new GenerateDirectionsTask().execute(waypoint);
		}

	}

	private class GenerateDirectionsTask extends
			AsyncTask<Waypoint, Void, DirectionManager> {
		protected DirectionManager doInBackground(Waypoint... placeWaypoint) {
			DirectionManager directionManager = null;
			Waypoint startWaypoint;

			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING_DIRECTIONS);
				}
			});

			if (placeWaypoint[0] == null) {
				// Download the way-points.
				try {
					startWaypoint = new XmlLocationImporter()
							.readFromFile(DIRECTIONS_FILE_NAME);

					Log.d(TAG, "Have waypoint");

					directionManager = getDirections((LocationWaypoint) startWaypoint);

					Log.d(TAG, "Have directions possibly");
				} catch (IOException e) {
					Toast.makeText(RealityActivity.this,
							"Error reading from directions", Toast.LENGTH_LONG);

					Log.e(TAG, "Error reading from serialized object.");
				}
			} else {
				Place place = placeWaypoint[0].getPlace();
				if (place != null) {
					Coordinate coord = place.coordinate;
					if (coord != null) {
						// Use a coordinate.
						startWaypoint = new LocationWaypoint(coord);

						directionManager = getDirections((LocationWaypoint) startWaypoint);
					} else {
						// TODO: Otherwise, use a bearing (if any).
						float bearing = place.bearing;
						if (bearing >= 0) {
							startWaypoint = new BearingWaypoint(bearing);

							directionManager = getDirections((BearingWaypoint) startWaypoint);
						}
					}
				}
			}

			return directionManager;
		}

		private DirectionManager getDirections(LocationWaypoint waypoint) {
			if (waypoint != null) {
				Directions<LocationWaypoint> directions = new Directions<LocationWaypoint>(
						waypoint);

				LocationDirectionManager manager = new LocationDirectionManager();
				manager.setDirections(directions);

				return manager;
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						showDialog(DIALOG_DOWNLOADING_DIRECTIONS_FAILED);
					}
				});
			}
			return null;
		}

		private DirectionManager getDirections(BearingWaypoint waypoint) {
			if (waypoint != null) {
				Directions<BearingWaypoint> directions = new Directions<BearingWaypoint>(
						waypoint);

				BearingDirectionManager manager = new BearingDirectionManager();
				manager.setDirections(directions);

				return manager;
			} else {
				runOnUiThread(new Runnable() {
					public void run() {
						showDialog(DIALOG_DOWNLOADING_DIRECTIONS_FAILED);
					}
				});
			}
			return null;
		}

		protected void onPostExecute(final DirectionManager directionManager) {
			if (directionManager != null) {
				if (mDirectionView == null) {
					mDirectionView = (RealityDirectionView) ((ViewStub) findViewById(R.id.directions_stub))
							.inflate();
				} else {
					mDirectionView.setVisibility(View.VISIBLE);
					mDirectionView.setWillNotDraw(false);
				}

				mDirectionsLabel.setText("...");
				mDirectionsLabel.setVisibility(View.VISIBLE);

				directionManager.registerObserver(mDirectionView);
				directionManager.registerObserver(RealityActivity.this);

				mOrientationListener.registerForUpdates(mDirectionView);

				registerDirectionManager(directionManager);
			} else {
				Log.d(TAG, "Directions not found.");
			}

			dismissDialog(DIALOG_LOADING_DIRECTIONS);
		}
	}

}
