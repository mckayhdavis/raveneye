package com.common;

public class Waypoint {

	public final Coordinate coordinate;

	private Waypoint mNext;
	private Place mPlace;

	public Waypoint(Coordinate coordinate) {
		this.coordinate = coordinate;
	}

	public Waypoint next() {
		return mNext;
	}

	public void setNext(Waypoint waypoint) {
		mNext = waypoint;
	}

	/**
	 * Assign a landmark to this way-point.
	 * 
	 * @param place
	 */
	public void setPlace(Place place) {
		mPlace = place;
	}

	/**
	 * Returns the place associated with this way-point.
	 * 
	 * @return
	 */
	public Place getPlace() {
		return mPlace;
	}

	public String toString() {
		return "[" + mPlace + "] - " + coordinate.toString();
	}

}
