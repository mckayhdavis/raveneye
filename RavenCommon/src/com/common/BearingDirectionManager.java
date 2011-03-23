package com.common;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class BearingDirectionManager extends DirectionManager<BearingWaypoint>
		implements SensorEventListener {

	public static final int VIEWPORT_BUFFER_ANGLE = 10;

	public BearingDirectionManager() {

	}

	private float mBearing;

	@Override
	public void setDirections(Directions<BearingWaypoint> directions) {
		super.setDirections(directions);

		mBearing = mDirections.currentWaypoint().getData();

		// Notify the observers.
		DirectionEvent dirEvent = new DirectionEvent(
				DirectionEvent.STATUS_OFFCOURSE, (int) mBearing, -1);
		notifyObservers(dirEvent);
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {
		float heading = event.values[0];

		if (Math.abs(heading - mBearing) < VIEWPORT_BUFFER_ANGLE) {
			// Notify the observers.
			DirectionEvent dirEvent = new DirectionEvent(
					DirectionEvent.STATUS_ARRIVED, (int) mBearing, -1);
			notifyObservers(dirEvent);
		}
	}
}
