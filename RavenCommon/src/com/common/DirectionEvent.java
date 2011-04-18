package com.common;

public class DirectionEvent {

	public final int status;
	public final int bearing;
	public final int legDistance;
	public final int distanceRemaining;
	public final int totalDuration;
	public final int totalDistance;

	public static final int STATUS_ARRIVED = 0;
	public static final int STATUS_ONCOURSE = 1;
	public static final int STATUS_OFFCOURSE = 2;

	public static final int STATUS_INITIALIZING = -1;

	public DirectionEvent(int status, int bearing, int legDistance,
			int distanceRemaining, int totalDistance, int totalDuration) {
		this.status = status;
		this.bearing = bearing;
		this.legDistance = legDistance;
		this.distanceRemaining = distanceRemaining;
		this.totalDistance = totalDistance;
		this.totalDuration = totalDuration;
	}

}
