package com.reality;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.common.Coordinate;
import com.common.Place;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class NavigationMapActivity extends MapActivity {

	public static final int DIALOG_LOADING = 0;

	private MapController mMapController;
	private MapView mMapView;
	private MyLocationOverlay mMyLocationOverlay;
	private List<Overlay> mMapOverlays;
	private LocationManager mLocationManager;
	private String mBestProvider;

	private final List<Place> mPlaces = new ArrayList<Place>();

	private boolean mUseLocationForPosition = true;

	// Carleton University GPS coordinates.
	public static final GeoPoint DEFAULT_LOCATION = new GeoPoint(45386018,
			-75696449);
	public static final int DEFAULT_ZOOM_LEVEL = 19;

	public static final int MIN_TIME_BETWEEN_LOCATION_UPDATES = 1000;
	public static final int MIN_DISTANCE_BETWEEN_LOCATION_UPDATES = 3;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_activity);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);

		mMapController = mMapView.getController();
		mMapOverlays = mMapView.getOverlays();

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		// Place the location overlay.
		mMyLocationOverlay = new MyLocationOverlay(this, mMapView);

		MapGestureDetectorOverlay gestureOverlay = new MapGestureDetectorOverlay(
				mGestureListener);

		mMapOverlays.add(mMyLocationOverlay);
		mMapOverlays.add(gestureOverlay);

		/*
		 * Passed in objects.
		 */
		Object[] places = null;
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			try {
				places = (Object[]) extras.getSerializable(Place.class
						.toString());
				new DownloadPlacesTask().execute(places);
			} catch (ClassCastException e) {
				finish();
				return;
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();

		Criteria criteria = new Criteria();
		mBestProvider = mLocationManager.getBestProvider(criteria, false);

		mMyLocationOverlay.enableMyLocation();
		mMyLocationOverlay.enableCompass();

		mLocationManager.requestLocationUpdates(mBestProvider,
				MIN_TIME_BETWEEN_LOCATION_UPDATES,
				MIN_DISTANCE_BETWEEN_LOCATION_UPDATES, mLocationListener);
	}

	@Override
	public void onPause() {
		super.onPause();

		mMyLocationOverlay.disableMyLocation();
		mMyLocationOverlay.disableCompass();

		mLocationManager.removeUpdates(mLocationListener);
	}

	private class DownloadPlacesTask extends
			AsyncTask<Object[], Void, List<Place>> {
		protected List<Place> doInBackground(Object[]... p) {
			runOnUiThread(new Runnable() {
				public void run() {
					showDialog(DIALOG_LOADING);
				}
			});

			Object[] places = p[0];
			if (places != null) {
				GeoPoint point = null;
				Place place;
				for (Object o : places) {
					place = (Place) o;
					mPlaces.add(place);

					if (place.coordinate != null) {
						Drawable drawable = NavigationMapActivity.this
								.getResources().getDrawable(
										R.drawable.flag_green_icon);
						NavigationItemizedOverlay itemizedOverlay = new NavigationItemizedOverlay(
								drawable, NavigationMapActivity.this);

						Coordinate coordinate = place.coordinate;

						point = new GeoPoint((int) (coordinate.latitude * 1E6),
								(int) (coordinate.longitude * 1E6));
						OverlayItem overlayItem = new OverlayItem(point,
								place.name, place.description);

						itemizedOverlay.addOverlay(overlayItem);
						mMapOverlays.add(itemizedOverlay);
					}
				}

				if (point != null) {
					mMapController.animateTo(point);
					mMapController.zoomToSpan(point.getLatitudeE6(),
							point.getLongitudeE6());
				}
			}

			return mPlaces;
		}

		protected void onPostExecute(final List<Place> places) {
			runOnUiThread(new Runnable() {

				public void run() {
					dismissDialog(DIALOG_LOADING);

					onMyLocationClick(null);
				}

			});
		}

	}

	/*
	 * Location listener handler.
	 */
	private LocationListener mLocationListener = new LocationListener() {

		public void onLocationChanged(Location location) {
			if (mUseLocationForPosition) {
				// Update the map to the current location.
				GeoPoint loc = new GeoPoint(
						(int) (location.getLatitude() * 1000000),
						(int) (location.getLongitude() * 1000000));
				mMapController.animateTo(loc);
			}
		}

		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	};

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/*
	 * MapView gesture listener.
	 */
	private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {

		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}

		public void onLongPress(MotionEvent e) {
			Geocoder geoCoder = new Geocoder(getBaseContext(),
					Locale.getDefault());

			GeoPoint point = mMapView.getProjection().fromPixels(
					(int) e.getX(), (int) e.getY());

			try {
				List<Address> addresses = geoCoder.getFromLocation(
						point.getLatitudeE6() / 1E6,
						point.getLongitudeE6() / 1E6, 1);

				String add = "";
				if (addresses.size() > 0) {
					int len = addresses.get(0).getMaxAddressLineIndex();
					for (int i = 0; i < len; i++) {
						add += addresses.get(0).getAddressLine(i) + "\n";
					}

					Toast.makeText(getBaseContext(), add, Toast.LENGTH_SHORT)
							.show();
				}
			} catch (IOException ex) {
				Toast.makeText(getBaseContext(), "Data connection lost",
						Toast.LENGTH_SHORT);
			}
			// Get the location of the device.
			// GeoPoint point = mMapView.getMapCenter();
			// int latitude = point.getLatitudeE6();
			// int longitude = point.getLongitudeE6();
			// int zoomLevel = mMapView.getMaxZoomLevel();

			// Location location =
			// mLocationManager.getLastKnownLocation(mBestProvider);
			//
			// GeoPoint loc = new GeoPoint((int) location.getLatitude(),
			// (int) location.getLongitude());
			// mMapController.animateTo(loc);
		}

		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			// The user has scrolled, so we don't want location updates to move
			// the map position.
			mUseLocationForPosition = false;

			return false;
		}

		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub

		}

		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return false;
		}

	};

	/*
	 * Dialog methods.
	 */

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_LOADING:
			dialog = ProgressDialog.show(this, null, "Loading. Please wait...");
			// dialog.setCancelable(true);
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.search:
			onSearchClick(mMapView);
			return true;
		case R.id.places:
			onPlacesClick(mMapView);
			return true;
		case R.id.directions:
			onDirectionsClick(mMapView);
			return true;
		case R.id.settings:
			onSettingsClick(mMapView);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onMyLocationClick(View v) {
		Location location = mLocationManager
				.getLastKnownLocation(mBestProvider);

		GeoPoint loc = new GeoPoint((int) (location.getLatitude() * 1000000),
				(int) (location.getLongitude() * 1000000));
		mMapController.animateTo(loc);
		mMapController.setZoom(DEFAULT_ZOOM_LEVEL);

		mUseLocationForPosition = true;
	}

	public void onSearchClick(View v) {

	}

	public void onPlacesClick(View v) {
		this.startActivity(new Intent(this, PlacesListActivity.class));
	}

	public void onDirectionsClick(View v) {
		showDialog(DIALOG_LOADING);

		Intent intent = new Intent(this, RealityActivity.class);
		intent.putExtra(Place.class.toString(), mPlaces.toArray());

		this.startActivity(intent);
	}

	public void onClearMapClick(View v) {

	}

	public void onSettingsClick(View v) {

	}

}
