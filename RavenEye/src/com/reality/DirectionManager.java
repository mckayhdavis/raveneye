package com.reality;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class DirectionManager implements LocationListener {

	private List<DirectionObserver> mObservers = new ArrayList<DirectionObserver>(
			2); // Generally a low number of observers.

	private Directions mDirections;
	private Location mWaypointLocation = null;

	public DirectionManager() {

	}

	public void setDirections(Directions directions) {
		mDirections = directions;
	}

	public void registerObserver(DirectionObserver observer) {
		mObservers.add(observer);
	}

	public void notifyObservers(DirectionEvent event) {
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

		Coordinate coord = mDirections.currentWaypoint().coordinate;
		mWaypointLocation.setLatitude((double) coord.latitude / 1000000);
		mWaypointLocation.setLongitude((double) coord.longitude / 1000000);

		int bearing = (int) location.bearingTo(mWaypointLocation);
		int distance = (int) location.distanceTo(mWaypointLocation);

		if (bearing < 0) {
			bearing += 180;
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
