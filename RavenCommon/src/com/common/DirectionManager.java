package com.common;

import java.util.HashSet;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class DirectionManager implements LocationListener {

	public static final String TAG = "RealityActivity";

	private Location mWaypointLocation = null;

	private HashSet<DirectionObserver> mObservers = new HashSet<DirectionObserver>(
			2); // Generally a low number of observers.

	protected volatile Directions<Leg> mDirections;

	public DirectionManager() {

	}

	/*
	 * Location events.
	 */

	public void onLocationChanged(Location location) {
		if (mWaypointLocation == null) {
			mWaypointLocation = new Location(location);
		}

		/*
		 * This will usually only have one iteration. We loop again just in-case
		 * we happen to skip-over two or more nearby way-points.
		 */
		Leg currentLeg;
		for (;;) {
			currentLeg = mDirections.currentWaypoint();

			if (currentLeg == null) {
				return;
			}

			Coordinate endCoord = (Coordinate) currentLeg.end;
			mWaypointLocation.setLatitude(endCoord.latitude);
			mWaypointLocation.setLongitude(endCoord.longitude);

			int bearing = (int) location.bearingTo(mWaypointLocation);
			int distance = (int) location.distanceTo(mWaypointLocation);

			if (bearing < 0) {
				bearing += 360;
			}

			if (distance < (location.getAccuracy())) {
				if (mDirections.nextWaypoint() == null) {
					// We are at the destination. Broadcast the event.
					DirectionEvent event = new DirectionEvent(
							DirectionEvent.STATUS_ARRIVED, bearing, distance, 0);
					notifyObservers(event);
					return;
				}
			} else {
				// Notify the observers.
				DirectionEvent event = new DirectionEvent(
						DirectionEvent.STATUS_ONCOURSE, bearing, distance, 0);
				notifyObservers(event);

				return;
			}
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

	public void setDirections(Directions<Leg> directions) {
		mDirections = directions;
	}

	public void registerObserver(DirectionObserver observer) {
		mObservers.add(observer);
	}

	protected void notifyObservers(DirectionEvent event) {
		for (DirectionObserver observer : mObservers) {
			observer.onDirectionsChanged(event);
		}
	}

}