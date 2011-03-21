package com.common;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class DirectionManager implements LocationListener {

	public static final String TAG = "RealityActivity";

	public static int PROXIMITY_RADIUS = 5;

	private List<DirectionObserver> mObservers = new ArrayList<DirectionObserver>(
			2); // Generally a low number of observers.

	private Directions mDirections;
	private Location mWaypointLocation = null;

	private LocationManager mLocationManager;

	public DirectionManager(LocationManager manager) {
		mLocationManager = manager;
	}

	public void setDirections(Directions directions) {
		mDirections = directions;
		
		Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		onLocationChanged(location);
	}

	public void registerObserver(DirectionObserver observer) {
		mObservers.add(observer);
	}

	private void notifyObservers(DirectionEvent event) {
		for (DirectionObserver observer : mObservers) {
			observer.onDirectionsChanged(event);
		}
	}

	/*
	 * Location events.
	 */

	public void onLocationChanged(Location location) {
		if (mWaypointLocation == null) {
			mWaypointLocation = new Location(location);
		}

		Waypoint curWaypoint = mDirections.currentWaypoint();
		if (curWaypoint == null) {
			return;
		}

		Coordinate coord = curWaypoint.coordinate;
		mWaypointLocation.setLatitude(coord.latitude);
		mWaypointLocation.setLongitude(coord.longitude);

		int bearing = (int) location.bearingTo(mWaypointLocation);
		int distance = (int) location.distanceTo(mWaypointLocation);

		if (bearing < 0) {
			bearing += 360;
		}

		int type;
		if (distance < PROXIMITY_RADIUS) {
			mDirections.nextWaypoint();
		}

		// Notify the observers.
		DirectionEvent event = new DirectionEvent(
				DirectionEvent.STATUS_ONCOURSE, bearing, distance);
		notifyObservers(event);
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

}
