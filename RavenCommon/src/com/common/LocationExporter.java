package com.common;

import java.io.IOException;

public interface LocationExporter {

	public static final String TAG = "LocationGatherer";

	public boolean setCurrentWaypoint(String key);

	public String add(LocationWaypoint coord);

	public IntersectionWaypoint getIntersectionWaypoint(String intersectionName);

	public void writeToFile(String fileName) throws IOException;

	public int size();

}
