package com.common;

import java.util.Dictionary;
import java.util.Hashtable;

import android.util.Log;

public class Directions<T extends Waypoint> {

	public static final String TAG = DirectionManager.TAG;

	/*
	 * The way-points to a particular destination. The destination is always the
	 * last way-point.
	 */
	private final Dictionary<T, T> mWaypoints = new Hashtable<T, T>();

	private T mCurrentWaypoint;

	public Directions(T start) {
		mCurrentWaypoint = start;

		populateWaypointList(start);
	}

	/*
	 * Add all the way-points to the dictionary. We keep a dictionary of
	 * way-points as well as the linked list so that we can reach any way-point
	 * in O(1) time.
	 */
	private void populateWaypointList(T waypoint) {
		while (waypoint != null) {
			Log.d(TAG, waypoint.toString());

			mWaypoints.put(waypoint, waypoint);

			waypoint = (T) waypoint.next();
		}
	}

	public Dictionary<T, T> getWaypoints() {
		return mWaypoints;
	}

	public T currentWaypoint() {
		return mCurrentWaypoint;
	}

	public T nextWaypoint() {
		mCurrentWaypoint = (T) mCurrentWaypoint.next();

		return mCurrentWaypoint;
	}

	public boolean selectWaypoint(T waypoint) {
		waypoint = mWaypoints.get(waypoint);
		if (waypoint != null) {
			mCurrentWaypoint = waypoint;
			return true;
		}
		return false;
	}

}
