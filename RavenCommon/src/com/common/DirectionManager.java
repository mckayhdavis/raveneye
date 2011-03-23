package com.common;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public abstract class DirectionManager<T extends Waypoint> {

	public static final String TAG = "RealityActivity";

	private List<DirectionObserver> mObservers = new ArrayList<DirectionObserver>(
			2); // Generally a low number of observers.

	protected Directions<T> mDirections;

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
