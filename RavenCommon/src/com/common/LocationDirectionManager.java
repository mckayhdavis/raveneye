package com.common;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationDirectionManager extends
		DirectionManager<LocationWaypoint> implements LocationListener {

	public static int PROXIMITY_RADIUS = 10;

	private Location mWaypointLocation = null;

	private LocationManager mLocationManager;

	public LocationDirectionManager(LocationManager manager) {
		mLocationManager = manager;
	}

	@Override
	public void setDirections(Directions<LocationWaypoint> directions) {
		super.setDirections(directions);

		Location location = mLocationManager
				.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		onLocationChanged(location);
	}

	/*
	 * Location events.
	 */

	public void onLocationChanged(Location location) {
		if (mWaypointLocation == null) {
			mWaypointLocation = new Location(location);
		}

		LocationWaypoint curWaypoint = mDirections.currentWaypoint();
		if (curWaypoint == null) {
			return;
		}

		Coordinate coord = (Coordinate) curWaypoint.getData();
		mWaypointLocation.setLatitude(coord.latitude);
		mWaypointLocation.setLongitude(coord.longitude);

		int bearing = (int) location.bearingTo(mWaypointLocation);
		int distance = (int) location.distanceTo(mWaypointLocation);

		if (bearing < 0) {
			bearing += 360;
		}

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
