package com.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import android.os.Environment;
import android.util.Log;

public class ObjectLocationImporter implements LocationImporter {

	public static final String TAG = "LocationGatherer";
	public static final String FILE_EXTENSION = ".ser";
	public static final String KEY_PREFIX = "m";

	public ObjectLocationImporter() {

	}

	public LocationWaypoint readFromFile(String fileName) throws IOException {
		LocationWaypoint waypoint = null;

		// create a new file called "new.xml" in the SD card
		File file = new File(Environment.getExternalStorageDirectory() + "/"
				+ fileName + FILE_EXTENSION);

		file.createNewFile();

		ObjectInputStream is = null;
		try {
			FileInputStream fos = new FileInputStream(file);
			is = new ObjectInputStream(fos);

			Object obj;
			try {
				obj = is.readObject();

				if (obj instanceof LocationWaypoint) {
					waypoint = (LocationWaypoint) obj;
				}
			} catch (ClassNotFoundException e) {
				throw new IOException("Object not of type LocationWaypoint.");
			}
		} finally {
			if (is != null) {
				is.close();
			}
		}

		Log.d(TAG, "read graph to file: " + fileName + FILE_EXTENSION);

		return null;
	}
}
