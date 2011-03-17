package com.reality;

import java.util.HashSet;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

public class RealityLocationListener implements LocationListener {

	public static final String TAG = RealityActivity.TAG;// "RealityLocationListener";

	private Location mDeviceLocation = null;
	private HashSet<LocationListener> mObservers;
	private HashSet<LocationListener> mStatusObservers;

	public RealityLocationListener() {
		mObservers = new HashSet<LocationListener>();
		mStatusObservers = new HashSet<LocationListener>();
	}

	public void registerForUpdates(LocationListener observer) {
		mObservers.add(observer);

		/*
		 * if (mDeviceLocation != null) {
		 * observer.onLocationChanged(mDeviceLocation); }
		 */
	}

	public void registerForStatusUpdates(LocationListener observer) {
		mStatusObservers.add(observer);
	}

	public void deregister(LocationListener observer) {
		mObservers.remove(observer);
	}

	/**
	 * Returns the last known current location.
	 * 
	 * @return location
	 */
	public Location getCurrentLocation() {
		return mDeviceLocation;
	}

	public void onLocationChanged(Location location) {
		/*
		 * Broadcast to the observers.
		 */
		int size = mObservers.size();
		if (size > 0) {
			Log.d(TAG, "onLocationChanged() - Broadcasting location to " + size
					+ " listeners");

			for (LocationListener listener : mObservers) {
				listener.onLocationChanged(location);
			}
		}
	}

	public void onProviderDisabled(String provider) {

	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, "onStatusChanged() - Using " + extras.getInt("satellites")
				+ " satellites");
		/*
		 * Broadcast to the observers.
		 */
		if (mStatusObservers.size() > 0) {
			for (LocationListener listener : mStatusObservers) {
				listener.onStatusChanged(provider, status, extras);
			}
		}
	}

}
