package com.common;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class LocationDirectionManager extends
		DirectionManager<LocationWaypoint> implements LocationListener {

	/*
	 * The accuracy of the Location object is used to determine whether we are
	 * in proximity to a way-point. TODO: until further testing, we use a buffer
	 * as well to ensure navigation between way-points is as seamless as
	 * possible.
	 */
	public static int PROXIMITY_BUFFER = 5;

	private Location mWaypointLocation = null;

	public LocationDirectionManager() {

	}

	@Override
	public void setDirections(Directions<LocationWaypoint> directions) {
		super.setDirections(directions);
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
		for (;;) {
			if (mDirections.currentWaypoint() == null) {
				return;
			}

			Coordinate coord = (Coordinate) mDirections.currentWaypoint()
					.getData();

			mWaypointLocation.setLatitude(coord.latitude);
			mWaypointLocation.setLongitude(coord.longitude);
			coord = null;

			int bearing = (int) location.bearingTo(mWaypointLocation);
			int distance = (int) location.distanceTo(mWaypointLocation);

			if (bearing < 0) {
				bearing += 360;
			}

			if (distance < (location.getAccuracy())) { // + PROXIMITY_BUFFER
				if (mDirections.nextWaypoint() == null) {
					// We are at the destination. Broadcast the event.
					DirectionEvent event = new DirectionEvent(
							DirectionEvent.STATUS_ARRIVED, bearing, distance);
					notifyObservers(event);
					return;
				}
			} else {
				// Notify the observers.
				DirectionEvent event = new DirectionEvent(
						DirectionEvent.STATUS_ONCOURSE, bearing, distance);
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

}
