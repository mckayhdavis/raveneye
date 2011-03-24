package com.common;

public class LocationWaypoint extends Waypoint {

	public final Coordinate coordinate;

	public LocationWaypoint(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public Coordinate getData() {
		return coordinate;
	}

	@Override
	public String toString() {
		return "LocationWaypoint: " + coordinate.toString();
	}

}
