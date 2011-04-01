package com.common;

import java.util.List;

public class Directions<T extends Leg> {

	public static final String TAG = DirectionManager.TAG;

	/*
	 * The way-points to a particular destination. The destination is always the
	 * last way-point.
	 */
	private final List<T> mWaypoints;
	// private final Dictionary<T, T> mWaypoints = new Hashtable<T, T>();

	private volatile int mCurrentWaypointIndex;

	public Directions(List<T> waypoints) {
		mWaypoints = waypoints;
	}

	public List<T> getWaypoints() {
		return mWaypoints;
	}

	public T currentWaypoint() {
		return mWaypoints.get(mCurrentWaypointIndex);
	}

	public T nextWaypoint() {
		if ((mCurrentWaypointIndex + 1) < mWaypoints.size()) {
			return mWaypoints.get(++mCurrentWaypointIndex);
		}

		return null;
	}

}
