package com.common;

import java.util.HashSet;

public abstract class DirectionManager<T extends Waypoint> {

	public static final String TAG = "RealityActivity";

	private HashSet<DirectionObserver> mObservers = new HashSet<DirectionObserver>(
			2); // Generally a low number of observers.

	protected volatile Directions<T> mDirections;

	public void setDirections(Directions<T> directions) {
		mDirections = directions;
	}

	public void registerObserver(DirectionObserver observer) {
		mObservers.add(observer);
	}

	protected void notifyObservers(DirectionEvent event) {
		for (DirectionObserver observer : mObservers) {
			observer.onDirectionsChanged(event);
		}
	}

}
