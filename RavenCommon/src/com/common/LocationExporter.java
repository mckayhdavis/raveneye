package com.common;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

public abstract class LocationExporter {

	public static final String TAG = "LocationGatherer";
	public static final String KEY_PREFIX = "m";

	private LocationWaypoint mGraph = null;
	private LocationWaypoint mCurrentNode = null;

	private int mSize = 0;

	private final Dictionary<String, IntersectionWaypoint> mIntersectionWaypoints = new Hashtable<String, IntersectionWaypoint>();

	public abstract void writeToFile(String fileName) throws IOException;

	public boolean setCurrentWaypoint(String key) {
		IntersectionWaypoint waypoint = mIntersectionWaypoints.get(key);
		if (waypoint != null) {
			mCurrentNode = waypoint;
			return true;
		}
		return false;
	}

	public String add(LocationWaypoint waypoint) {
		if (mGraph == null) {
			mGraph = waypoint;
			mCurrentNode = waypoint;
		} else {
			mCurrentNode.addNext(waypoint);
			mCurrentNode = waypoint;
		}

		String key;
		if (waypoint instanceof IntersectionWaypoint) {
			key = getKey();
			mIntersectionWaypoints.put(key, (IntersectionWaypoint) waypoint);
		} else {
			key = null;
		}

		waypoint.id = mSize++;

		return key;
	}

	public IntersectionWaypoint getIntersectionWaypoint(String key) {
		return mIntersectionWaypoints.get(key);
	}

	private String getKey() {
		return KEY_PREFIX + mIntersectionWaypoints.size() + "";
	}

	public int size() {
		return mSize;
	}

	public LocationWaypoint getGraph() {
		return mGraph;
	}

	public LocationWaypoint getCurrentNode() {
		return mCurrentNode;
	}

}
