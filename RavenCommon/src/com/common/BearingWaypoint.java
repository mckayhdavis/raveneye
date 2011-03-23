package com.common;

public class BearingWaypoint extends Waypoint {

	private final float bearing;

	public BearingWaypoint(float bearing) {
		this.bearing = bearing;
	}

	public float getData() {
		return bearing;
	}

	@Override
	public String toString() {
		return super.toString() + " " + bearing;
	}

}
