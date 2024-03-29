package com.reality;

import java.util.HashSet;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

/**
 * This class is responsible for interpreting the orientation of the device and
 * ensuring smooth and noise-free sensor values. This is useful for situations
 * in which sensor values play some part in the visual representation and
 * transformation of UI elements on screen.
 * 
 * @author Michael Du Plessis
 */
public class RealityOrientationListener implements SensorEventListener,
		LocationListener {

	public static final String TAG = "RealityOrientationListener";

	public static final float RAD_TO_DEG = (float) (180.0f / Math.PI);
	public static final float DEG_TO_RAD = (float) (Math.PI / 180.0f);

	private static final int DEFAULT_SCALE_RANGE = 180;

	private float[] mR = new float[16];
	private float[] mOutR = new float[16];
	private float[] mI = new float[16];
	private float[] mGravity = new float[] { 0, 0, 0 };
	private float[] mGeomagnetic = new float[] { 0, 0, 0 };
	private double[] mOldOrientation = new double[] { 0, 0, 0 };

	private int mScaleDenominator = 180;

	private float mDeclination = 0.0f;
	private boolean mHasDeclination = false;

	private final HashSet<SensorEventListener> mObservers;

	public RealityOrientationListener() {
		mObservers = new HashSet<SensorEventListener>();
	}

	public void registerForUpdates(SensorEventListener observer) {
		mObservers.add(observer);
	}

	public void deregisterForUpdates(SensorEventListener observer) {
		mObservers.remove(observer);
	}

	/*
	 * SensorEventListener methods.
	 */

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {
		final float[] gravity = mGravity;
		final float[] geomagnetic = mGeomagnetic;

		synchronized (this) {
			final float[] values = event.values;

			// if (event.accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH)
			// {
			// Log.e(TAG, "Accuracy not high");
			// }

			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				System.arraycopy(values, 0, gravity, 0, 3);
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				System.arraycopy(values, 0, geomagnetic, 0, 3);
				break;
			default:
				return;
			}

			if (!SensorManager.getRotationMatrix(mR, mI, gravity, geomagnetic)) {
				return;
			}

			// Re-map the coordinate system for augmented reality.
			SensorManager.remapCoordinateSystem(mR, SensorManager.AXIS_X,
					SensorManager.AXIS_Z, mOutR);

			SensorManager.getOrientation(mOutR, values);

			filterValues(values); // filter sensor noise
			// lightFilterValues(values); // filter sensor noise

			// Log.d(TAG, "(" + values[0] + "," + values[1] + "," + values[2]
			// + ") " + event.sensor.getResolution());
		}

		/*
		 * Broadcast to the observers.
		 */
		for (SensorEventListener listener : mObservers) {
			listener.onSensorChanged(event);
		}
	}

	public void lightFilterValues(final float[] values) {
		values[0] = (values[0] * RAD_TO_DEG) + mDeclination; // azimuth
		values[1] *= RAD_TO_DEG; // pitch
		values[2] *= RAD_TO_DEG; // roll

		// Convert the (-180,180] range to [0,360).
		if (values[0] < 0) {
			values[0] += 360;
		}
	}

	public void filterValues(final float[] values) {
		values[0] = (values[0] * RAD_TO_DEG) + mDeclination; // azimuth
		values[1] *= RAD_TO_DEG; // pitch
		values[2] *= RAD_TO_DEG; // roll

		final double[] oldValues = mOldOrientation;
		double difference;

		for (int i = 0; i < 3; ++i) {
			/*
			 * Account for azimuth rotations around SOUTH. Azimuth values range
			 * is (-180,180] where 0 is NORTH.
			 * 
			 * Only account for wrap-around at SOUTH. Don't use corrections at
			 * the NORTH azimuth value (around 0 degrees).
			 */
			difference = Math.abs(values[i] - oldValues[i]);
			if (difference > 180) {
				difference = 360 - difference;

				if (values[i] < 0) {
					// Only the new azimuth is negative.
					oldValues[i] -= 360;
				} else {
					// Only the old azimuth is negative.
					oldValues[i] += 360;
				}
			}

			difference /= mScaleDenominator;
			oldValues[i] = (oldValues[i] * (1 - difference)) + (values[i] * difference);
		}

		// Save the current values.
		values[0] = (float) oldValues[0];
		values[1] = (float) oldValues[1];
		values[2] = (float) oldValues[2];

		// Convert the (-180,180] range to [0,360).
		if (values[0] < 0) {
			values[0] += 360;
		}
	}

	public boolean hasLocation() {
		return mHasDeclination;
	}

	/*
	 * LocationListener methods.
	 */

	public void onLocationChanged(Location location) {
		/*
		 * We can safely assume that we only need one declination angle for the
		 * duration of the application runtime. Most likely, the user will not
		 * travel such great distances in reality mode.
		 */
		if (!mHasDeclination) {
			float altimeter = 0;

			GeomagneticField field = new GeomagneticField(
					(float) location.getLatitude(),
					(float) location.getLongitude(), altimeter,
					System.currentTimeMillis());

			mDeclination = field.getDeclination();

			mHasDeclination = true;
		}

		float speed = location.getSpeed();
		if (speed > 0) {
			mScaleDenominator = (int) (DEFAULT_SCALE_RANGE + (speed * 10));
		} else {
			mScaleDenominator = DEFAULT_SCALE_RANGE;
		}
	}

	public void onProviderDisabled(String provider) {

	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

}
