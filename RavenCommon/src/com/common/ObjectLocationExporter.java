package com.common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import android.os.Environment;
import android.util.Log;

public class ObjectLocationExporter implements LocationExporter {

	public static final String TAG = "LocationGatherer";
	public static final String FILE_EXTENSION = ".ser";
	public static final String KEY_PREFIX = "m";

	private LocationWaypoint mGraph = null;
	private LocationWaypoint mCurrentNode = null;

	private int mSize = 0;

	private final Dictionary<String, IntersectionWaypoint> mIntersectionWaypoints = new Hashtable<String, IntersectionWaypoint>();

	public ObjectLocationExporter() {

	}

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

		++mSize;

		return key;
	}

	public IntersectionWaypoint getIntersectionWaypoint(String key) {
		return mIntersectionWaypoints.get(key);
	}

	private String getKey() {
		return KEY_PREFIX + mIntersectionWaypoints.size() + "";
	}

	public void writeToFile(String fileName) throws IOException {
		// create a new file called "new.xml" in the SD card
		File file = new File(Environment.getExternalStorageDirectory() + "/"
				+ fileName + FILE_EXTENSION);

		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.e(TAG, "exception in createNewFile() method");
			return;
		}

		ObjectOutputStream os = null;
		try {
			FileOutputStream fos = new FileOutputStream(file);
			os = new ObjectOutputStream(fos);

			os.writeObject(mGraph);
			os.flush();
		} finally {
			if (os != null) {
				os.close();
			}
		}

		Log.d(TAG, "saved graph to file: " + fileName + FILE_EXTENSION);
	}

	public int size() {
		return mSize;
	}

}
