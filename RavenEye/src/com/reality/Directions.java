package com.reality;

import java.util.Dictionary;
import java.util.Hashtable;

import android.util.Log;

public class Directions {

	public static final String TAG = RealityActivity.TAG;

	/*
	 * The way-points to a particular destination. The destination is always the
	 * last way-point.
	 */
	private final Dictionary<Waypoint, Waypoint> mWaypoints = new Hashtable<Waypoint, Waypoint>();

	private Waypoint mNextWaypoint;

	public Directions(Waypoint start) {
		mNextWaypoint = start;

		populateWaypointList(start);
	}

	/*
	 * Add all the way-points to the dictionary. We keep a dictionary of
	 * way-points as well as the linked list so that we can reach any way-point
	 * in O(1) time.
	 */
	private void populateWaypointList(Waypoint waypoint) {
		while (waypoint != null) {
			Log.d(TAG, waypoint.toString());

			mWaypoints.put(waypoint, waypoint);

			waypoint = waypoint.next();
		}
	}

	public Dictionary<Waypoint, Waypoint> getWaypoints() {
		return mWaypoints;
	}

	public Waypoint currentWaypoint() {
		return mNextWaypoint;
	}
	
	public Waypoint nextWaypoint() {
		mNextWaypoint = mNextWaypoint.next();

		return mNextWaypoint;
	}

	public void setNextWaypoint(Waypoint waypoint) {
		mNextWaypoint = mWaypoints.get(waypoint);
	}

}
