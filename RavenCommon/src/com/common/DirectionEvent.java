package com.common;

public class DirectionEvent {

	public final int status;
	public final int bearing;
	public final int distance;
	public final int totalDistance;

	public static final int STATUS_ARRIVED = 0;
	public static final int STATUS_ONCOURSE = 1;
	public static final int STATUS_OFFCOURSE = 2;

	public static final int STATUS_INITIALIZING = -1;

	public DirectionEvent(int status, int bearing, int distance, int totalDistance) {
		this.status = status;
		this.bearing = bearing;
		this.distance = distance;
		this.totalDistance = totalDistance;
	}

}
