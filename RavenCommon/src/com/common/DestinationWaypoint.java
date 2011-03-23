package com.common;

public class DestinationWaypoint extends LocationWaypoint {

	public DestinationWaypoint(Place place) {
		super(place.coordinate);

		setPlace(place);
	}

	@Override
	public String toString() {
		return coordinate.toString() + "[" + getPlace() + "]";
	}

}
