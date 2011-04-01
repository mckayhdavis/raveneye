package com.common;

import java.io.Serializable;

public class Leg implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public final Coordinate start;
	public final Coordinate end;

	private Place mPlace = null;
	private boolean mVisited = false;

	public transient int id;

	public Leg(Coordinate start, Coordinate end) {
		this.start = start;
		this.end = end;
	}

	public boolean isVisited() {
		return mVisited;
	}

	public void visit() {
		mVisited = true;
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

	/*
	 * Serialization methods.
	 */

	// private void writeObject(ObjectOutputStream out) throws IOException {
	// out.write.defaultWriteObject();
	// }
	//
	// private void readObject(ObjectInputStream in) throws IOException,
	// ClassNotFoundException {
	// // our "pseudo-constructor"
	// in.defaultReadObject();
	// // now we are a "live" object again
	// }

}