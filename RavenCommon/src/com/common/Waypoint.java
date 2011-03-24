package com.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Waypoint implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Usually a way-point has at most 4 neighbors.
	private List<Waypoint> mNeighbours = new ArrayList<Waypoint>(4);
	private Place mPlace = null;

	public transient int id;

	private boolean mVisited = false;

	public int numberOfChildren() {
		return mNeighbours.size();
	}

	public List<Waypoint> getNeighbours() {
		return mNeighbours;
	}

	public Waypoint next(int index) {
		return mNeighbours.get(index);
	}

	public Waypoint next() {
		if (mNeighbours.size() > 0) {
			return mNeighbours.get(0);
		}
		return null;
	}

	public void addNext(Waypoint waypoint) {
		mNeighbours.add(waypoint);

		waypoint.mNeighbours.add(this);
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