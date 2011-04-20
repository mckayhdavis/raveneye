package com.common;

import java.util.List;

public class Directions<T extends Leg> {

	public static final String TAG = DirectionManager.TAG;

	/*
	 * The way-points to a particular destination. The destination is always the
	 * last way-point.
	 */
	private final List<T> mWaypoints;

	private final int mTotalDistance;
	private int mTotalDuration;

	private volatile int mCurrentWaypointIndex;

	public Directions(List<T> waypoints, int totalDistance, int totalDuration) {
		mWaypoints = waypoints;

		this.mTotalDistance = totalDistance;
		this.mTotalDuration = totalDuration;
	}

	public List<T> getWaypoints() {
		return mWaypoints;
	}

	public T currentWaypoint() {
		if(mCurrentWaypointIndex < mWaypoints.size()) {
			return mWaypoints.get(mCurrentWaypointIndex);
		}
		return null;
	}

	public T nextWaypoint() {
		if ((mCurrentWaypointIndex + 1) < mWaypoints.size()) {
			return mWaypoints.get(++mCurrentWaypointIndex);
		}

		return null;
	}

	public int getTotalDistance() {
		return mTotalDistance;
	}

	public int getTotalDuration() {
		return mTotalDuration;
	}

}
