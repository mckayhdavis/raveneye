package com.activities;

import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.common.Coordinate;

public class LocationGathererService extends Service implements
		LocationListener {

	public static final String TAG = "LocationGatherer";

	private NotificationManager mNM;
	private LocationManager mLocationManager;

	private int mLocationUpdateCount = 0;

	public static final String BROADCAST_LOCATION = "Location";

	private final Intent mLocationBroadcast = new Intent(BROADCAST_LOCATION);

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.local_service_started;

	public static final int MIN_TIME_BETWEEN_LOCATION_UPDATES = 500;
	public static final int MIN_DISTANCE_BETWEEN_LOCATION_UPDATES = 0;

	private Coordinate mCurrentCoordinate;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		LocationGathererService getService() {
			return LocationGathererService.this;
		}
	}

	@Override
	public void onCreate() {
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		/*
		 * Register for location events.
		 */
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String bestProvider = mLocationManager.getBestProvider(criteria, true);

		List<String> providers = mLocationManager.getProviders(true);
		Location location = null;

		for (int i = providers.size() - 1; i >= 0; i--) {
			bestProvider = providers.get(i);
			location = mLocationManager.getLastKnownLocation(bestProvider);
			if (location != null) {
				break;
			}
		}

		if (bestProvider == null) {
			int x = 0;
			Log.d(TAG, "no provider found");
			return;
		}

		mLocationManager.requestLocationUpdates(bestProvider,
				MIN_TIME_BETWEEN_LOCATION_UPDATES,
				MIN_DISTANCE_BETWEEN_LOCATION_UPDATES, this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("LocalService", "Received start id " + startId + ": " + intent);
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		// Cancel the persistent notification.
		mNM.cancel(NOTIFICATION);

		// Tell the user we stopped.
		Toast.makeText(this, R.string.local_service_stopped, Toast.LENGTH_SHORT)
				.show();

		/*
		 * Remove the update listeners of the compass and GPS.
		 */
		mLocationManager.removeUpdates(this);

		mLocationManager = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients. See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();

	public Coordinate getLatestCoordinate() {
		return mCurrentCoordinate;
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.local_service_started);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.icon, text,
				System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, MainActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this,
				getText(R.string.local_service_label), text, contentIntent);

		// Send the notification.
		mNM.notify(NOTIFICATION, notification);
	}

	/*
	 * Location listeners.
	 */

	@Override
	public void onLocationChanged(Location location) {
		Log.d(TAG, "onLocationChanged() " + location);

		Coordinate coord = new Coordinate(location.getLatitude(),
				location.getLongitude());

		synchronized (this) {
			mCurrentCoordinate = coord;
		}

		mLocationBroadcast.putExtra(Coordinate.class.toString(), coord);
		mLocationBroadcast.putExtra("count", ++mLocationUpdateCount);

		this.sendBroadcast(mLocationBroadcast);
	}

	@Override
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "onProviderDisabled() " + provider);
	}

	@Override
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "onProviderEnabled() " + provider);
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, "onStatusChanged() " + provider + ", " + status);
	}

	public int getCount() {
		return mLocationUpdateCount;
	}

}
