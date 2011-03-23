package com.common;

import java.io.IOException;

public interface LocationImporter {

	public static final String TAG = "LocationGatherer";

	public LocationWaypoint readFromFile(String fileName) throws IOException;

}
