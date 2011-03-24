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

	private volatile T mPreviousWaypoint;
	private volatile T mCurrentWaypoint;

	public Directions(T start) {
		mPreviousWaypoint = null;
		mCurrentWaypoint = start;
	}

	public Dictionary<T, T> getWaypoints() {
		return mWaypoints;
	}

	public T currentWaypoint() {
		return mCurrentWaypoint;
	}

	public T nextWaypoint() {
		T prevWaypoint = mPreviousWaypoint;
		T nextWaypoint = null;

		mCurrentWaypoint.visit();

		if (prevWaypoint != null) {
			for (Waypoint waypoint : mCurrentWaypoint.getNeighbours()) {
				if (!waypoint.isVisited() && prevWaypoint != waypoint) {
					nextWaypoint = (T) waypoint;
					break;
				}
			}
			Log.d(TAG, "Got next waypoint");
		} else {
			mCurrentWaypoint = (T) mCurrentWaypoint.next();
		}

		mCurrentWaypoint = nextWaypoint;

		return nextWaypoint;
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
