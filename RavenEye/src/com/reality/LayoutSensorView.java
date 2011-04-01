package com.reality;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.common.PlaceOverlayWrapper;

public abstract class LayoutSensorView extends RelativeLayout implements SensorEventListener,
		LocationListener {

	protected Location mLocation;

	public LayoutSensorView(Context context) {
		this(context, (Location) null);
	}

	public LayoutSensorView(Context context, Location location) {
		super(context);

		this.setWillNotDraw(true);

		this.mLocation = location;
	}

	public LayoutSensorView(Context context, AttributeSet attr) {
		this(context, attr, null);
	}

	public LayoutSensorView(Context context, AttributeSet attr, Location location) {
		super(context, attr);

		this.setWillNotDraw(true);

		this.mLocation = location;
	}
	
	public void onPlacesChanged(List<PlaceOverlayWrapper> places) {
		
	}

	/*
	 * Retrieve the location.
	 */

	public boolean hasLocation() {
		return mLocation != null;
	}

	public Location getLocation() {
		return mLocation;
	}

	/**
	 * SensorEventListener methods.
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	/**
	 * LocationListener methods.
	 */

	public void onLocationChanged(Location location) {
		mLocation = location;
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

}
