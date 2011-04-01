package com.reality;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
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
import android.os.Debug;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;
import android.widget.Toast;

import com.common.Coordinate;
import com.common.DirectionEvent;
import com.common.DirectionManager;
import com.common.DirectionObserver;
import com.common.Directions;
import com.common.HttpBuilder;
import com.common.Leg;
import com.common.Place;

public class RealityActivity extends Activity implements LocationListener,
		DirectionObserver {

	public static final String TAG = "RealityActivity";
	public static final boolean USE_CAMERA = true;

	private static final int DIALOG_DEFAULT_STATUS = -1;
	private static final int DIALOG_LOADING_GPS = 0;
	private static final int DIALOG_LOADING_DIRECTIONS = 1;
	private static final int DIALOG_GPS_UNAVAILABLE = 2;
	private static final int DIALOG_DOWNLOADING_DIRECTIONS = 3;
	private static final int DIALOG_LOADING_MAPVIEW = 5;

	private static final int TOAST_NETWORK_UNAVAILABLE = 20;
	private static final int TOAST_DOWNLOADING_DIRECTIONS_FAILED = 4;

	public static final String DIRECTIONS_FILE_NAME = "raven-graph";

	private Camera camera;
	private SurfaceView preview = null;
	private SurfaceHolder previewHolder = null;

	private LocationManager mLocationManager;
	private SensorManager mSensorManager;
	private DirectionManager mDirectionManager = null;

	public static final int ACQUIRE_GPS_TIMEOUT = 15000;
	public static final int MIN_TIME_BETWEEN_LOCATION_UPDATES = 1500;
	public static final int MIN_DISTANCE_BETWEEN_LOCATION_UPDATES = 1;

	private RealityOverlayView mSurface;
	private RealitySmallCompassView mCompassView;
	private RealityDirectionView mDirectionView = null;
	private View mDistanceStatusView = null;
	private TextView mTotalDistanceRemainingView = null;
	private TextView mLegDistanceRemainingView = null;

	private TextView mRealityStatusLabel;
	private TextView mStatusLabel;

	private View mPlaceDescriptionView;
	private TextView mTitleView;
	private TextView mDescriptionView;

	private RealityLocationListener mLocationListener;
	private RealityOrientationListener mOrientationListener;

	private Menu mMenu;

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
		mRealityStatusLabel = (TextView) findViewById(R.id.reality_status_output);
		mStatusLabel = (TextView) findViewById(R.id.status_output);

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
				SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(mOrientationListener, msensor,
				SensorManager.SENSOR_DELAY_UI);

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

		mMenu = menu;

		if (mDirectionView != null
				&& mDirectionView.getVisibility() == View.VISIBLE) {
			menu.getItem(0).setVisible(true);
		}

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.navigate:
			dismissDirectionOverlay();
			item.setVisible(false);
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
		this.startActivity(new Intent(this, PlaceListActivity.class));
	}

	public void onViewMapClick(View v) {
		showDialog(DIALOG_LOADING_MAPVIEW);

		Runnable runnable = new Runnable() {

			public void run() {
				startActivity(new Intent(RealityActivity.this,
						NavigationMapActivity.class));

				dismissDialog(DIALOG_LOADING_MAPVIEW);
			}

		};
		new Thread(runnable).start();
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

	protected void showStatusLabel(int id) {
		switch (id) {
		case DIALOG_LOADING_GPS:
			mRealityStatusLabel.setText("Acquiring satellite signal");
			break;
		case DIALOG_GPS_UNAVAILABLE:
			mRealityStatusLabel.setText("Lost satellite signal");
			break;
		default:
			mRealityStatusLabel.setText(mSurface.getPlacesCount()
					+ " results found");
		}
	}

	/*
	 * Dialog methods.
	 */

	private void showToast(int id) {
		onCreateToast(id).show();
	}

	protected Toast onCreateToast(int id) {
		LayoutInflater inflator = this.getLayoutInflater();
		View layout = inflator.inflate(R.layout.toast_alert,
				(ViewGroup) findViewById(R.id.toast_layout_root));
		TextView text = (TextView) layout.findViewById(R.id.text);

		Toast toast;
		switch (id) {
		case TOAST_DOWNLOADING_DIRECTIONS_FAILED:
			text.setText("Unable to download directions.");

			toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			break;
		case TOAST_NETWORK_UNAVAILABLE:
			text.setText("Network connection lost.");

			toast = new Toast(getApplicationContext());
			toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			toast.setDuration(Toast.LENGTH_LONG);
			toast.setView(layout);
			break;
		default:
			toast = null;
		}

		return toast;
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_LOADING_GPS:
			dialog = ProgressDialog.show(this, null,
					"Acquiring GPS signal. Please wait...");
			dialog.setCancelable(true);
			break;
		case DIALOG_LOADING_DIRECTIONS:
			dialog = ProgressDialog.show(this, null,
					"Loading directions. Please wait...");
			dialog.setCancelable(true);
			break;
		case DIALOG_GPS_UNAVAILABLE:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("No satellite signal")
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
			dialog.setCancelable(true);
			break;
		case DIALOG_LOADING_MAPVIEW:
			builder = new AlertDialog.Builder(this);
			builder.setTitle("Loading map. Please wait...")
					.setCancelable(false);
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
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

			if (place != null) {
				mTitleView.setText(place.name);
				mDescriptionView.setText(Place
						.getDistanceString(place.distance));
				view.setVisibility(View.VISIBLE);
			} else {
				view.setVisibility(View.GONE);
			}

			return true;
		}

		return false;
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
				new DownloadDirectionsTask().execute(place);

				if (mMenu != null) {
					mMenu.getItem(0).setVisible(true);
				}
			}
		}
	}

	public void onPlaceInfoClick(View v) {
		Place place = mSurface.getSelectedPlace();
		if (place != null) {
			Intent intent = new Intent(this, PlaceActivity.class);
			intent.putExtra(Place.class.toString(), place);
			this.startActivity(intent);
		}
	}

	private void dismissDirectionOverlay() {
		mOrientationListener.deregisterForUpdates(mDirectionView);

		deregisterDirectionManager();

		mDirectionView.setVisibility(View.GONE);
		mDirectionView.setWillNotDraw(true);

		mDistanceStatusView.setVisibility(View.GONE);
		mDirectionManager = null;

		// TODO: stop tracing
		Debug.stopMethodTracing();
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
			// mTotalDistanceRemainingView.setText("Arrived!");
			mLegDistanceRemainingView.setBackgroundColor(Color.GREEN);
			mLegDistanceRemainingView.setText("Arrived!");
		} else {
			// mTotalDistanceRemainingView.setText(Place
			// .getDistanceString(event.distance));
			mLegDistanceRemainingView.setText(Place
					.getDistanceString(event.distance));
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

				// dismissDialog(DIALOG_LOADING_GPS);
				showStatusLabel(DIALOG_DEFAULT_STATUS);
			}

			if (!mOrientationListener.hasLocation()) {
				mOrientationListener.onLocationChanged(location);
			}
		}

		mStatusLabel.setText("Accuracy: (" + ++mLocationCount + ") "
				+ location.getAccuracy());
	}

	private int mLocationCount = 0;

	public void onProviderDisabled(String provider) {
	}

	public void onProviderEnabled(String provider) {
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status != LocationProvider.AVAILABLE) {
			// showDialog(DIALOG_LOADING_GPS);
			showStatusLabel(DIALOG_LOADING_GPS);
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
						showStatusLabel(DIALOG_LOADING_GPS);

						TimerTask task = new TimerTask() {

							@Override
							public void run() {
								runOnUiThread(new Runnable() {

									public void run() {
										showStatusLabel(DIALOG_GPS_UNAVAILABLE);
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

				// if (places.length == 1) {
				// place = (Place) places[0];
				// float bearing = place.bearing;
				// if (bearing >= 0) {
				// Waypoint waypoint = new BearingWaypoint(bearing);
				// waypoint.setPlace(place);
				// new GenerateDirectionsTask().execute(waypoint);
				// }
				// }
			}

			return placeList;
		}

		protected void onPostExecute(final List<Place> places) {
			if (places != null) {
				updateWithPlaces(places);
			}

			Location location = mLocationListener.getLastKnownLocation();
			if (location != null) {
				mLocationListener.onLocationChanged(location);
			} else {
				// TODO: temporary for development purposes.
				location = getInitialLocation();
				mLocationListener.onLocationChanged(location);
			}
		}

	}

	/*
	 * Download the directions from the current location to the given place's
	 * location.
	 */
	private class DownloadDirectionsTask extends
			AsyncTask<Place, Void, List<Leg>> {
		protected List<Leg> doInBackground(Place... place) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_DOWNLOADING_DIRECTIONS);
				}
			});

			Place aPlace = place[0];
			if (aPlace != null) {
				try {
					Location loc = mLocationListener.getLastKnownLocation();
					String origin = loc.getLatitude() + ","
							+ loc.getLongitude();

					Coordinate coordinate = aPlace.coordinate;
					String destination = coordinate.latitude + ","
							+ coordinate.longitude;

					URL aUrl = new URL(
							"http://maps.googleapis.com/maps/api/directions/json?origin="
									+ origin + "&destination=" + destination
									+ "&sensor=true");

					return getDirections(aUrl);
				} catch (Exception e) {
					Log.e(TAG, e.toString());

					runOnUiThread(new Runnable() {
						public void run() {
							showToast(TOAST_NETWORK_UNAVAILABLE);
						}
					});
				}
			}

			return null;
		}

		@SuppressWarnings("unchecked")
		protected void onPostExecute(final List<Leg> waypoints) {
			dismissDialog(DIALOG_DOWNLOADING_DIRECTIONS);

			new GenerateDirectionsTask().execute(waypoints);
		}

	}

	private class GenerateDirectionsTask extends
			AsyncTask<List<Leg>, Void, DirectionManager> {
		protected DirectionManager doInBackground(List<Leg>... placeWaypoint) {
			List<Leg> waypoints = placeWaypoint[0];

			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING_DIRECTIONS);
				}
			});

			if (waypoints != null) {
				return getDirections(waypoints);
			}

			return null;
		}

		private DirectionManager getDirections(List<Leg> waypoints) {
			Directions<Leg> directions = new Directions<Leg>(waypoints);

			DirectionManager manager = new DirectionManager();
			manager.setDirections(directions);

			return manager;
		}

		protected void onPostExecute(final DirectionManager directionManager) {
			dismissDialog(DIALOG_LOADING_DIRECTIONS);

			if (directionManager != null) {
				if (mDirectionView == null) {
					mDirectionView = (RealityDirectionView) ((ViewStub) findViewById(R.id.directions_stub))
							.inflate();
					mDistanceStatusView = ((ViewStub) findViewById(R.id.directions_status_stub))
							.inflate();
					mTotalDistanceRemainingView = (TextView) (mDistanceStatusView
							.findViewById(R.id.directions_totaldistance));
					mLegDistanceRemainingView = (TextView) (mDistanceStatusView
							.findViewById(R.id.directions_legdistance));
				} else {
					mDirectionView.setVisibility(View.VISIBLE);
					mDistanceStatusView.setVisibility(View.VISIBLE);
				}
				mDirectionView.setWillNotDraw(false);

				directionManager.registerObserver(mDirectionView);
				directionManager.registerObserver(RealityActivity.this);

				mOrientationListener.registerForUpdates(mDirectionView);

				registerDirectionManager(directionManager);
			} else {
				showToast(TOAST_DOWNLOADING_DIRECTIONS_FAILED);
			}
		}
	}

	private List<Leg> getDirections(URL url) throws IllegalStateException,
			IOException, JSONException {
		final HttpClient httpclient = new DefaultHttpClient();
		final HttpGet httpget = new HttpGet(url.toString());
		HttpResponse response;

		response = httpclient.execute(httpget);

		// Examine the response status.
		Log.i(TAG, response.getStatusLine().toString());

		// Get hold of the response entity.
		HttpEntity entity = response.getEntity();
		// If the response does not enclose an entity, there is no need
		// to worry about connection release.

		if (entity != null) {
			List<Leg> waypoints = new ArrayList<Leg>();

			// A Simple JSON Response Read
			InputStream instream = entity.getContent();
			String result = HttpBuilder.convertStreamToString(instream);

			// A Simple JSONObject Creation
			JSONObject json = new JSONObject(result);

			JSONArray routes = json.getJSONArray("routes");
			JSONArray legs, steps;
			JSONObject startLocation, endLocation;

			int len = routes.length();
			for (int i = 0; i < len; i++) {
				legs = routes.getJSONObject(i).getJSONArray("legs");

				int len2 = legs.length();
				for (int j = 0; j < len2; j++) {
					steps = legs.getJSONObject(j).getJSONArray("steps");

					int len3 = steps.length();
					for (int k = 0; k < len3; k++) {
						startLocation = steps.getJSONObject(k).getJSONObject(
								"start_location");
						endLocation = steps.getJSONObject(k).getJSONObject(
								"start_location");

						Coordinate start = new Coordinate(
								Double.parseDouble(startLocation
										.getString("lat")),
								Double.parseDouble(startLocation
										.getString("lng")));

						Coordinate end = new Coordinate(
								Double.parseDouble(endLocation.getString("lat")),
								Double.parseDouble(endLocation.getString("lng")));

						Leg waypoint = new Leg(start, end);
						waypoints.add(waypoint);
					}
				}
			}
			// Closing the input stream will trigger connection release
			instream.close();

			return waypoints;
		}

		return null;
	}
}
