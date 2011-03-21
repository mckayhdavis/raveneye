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
import android.util.Log;

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

	public static final String TAG = "RealityPhysicsSensorListener";

	/*
	 * The higher the alpha, the faster the scaling occurs.
	 */
	public static volatile float ALPHA = (float) 0.1;
	public static volatile float BETA = 1 - ALPHA;

	public static volatile float SLOW_ALPHA = (float) 0.005;
	public static volatile float SLOW_BETA = 1 - SLOW_ALPHA;

	public static final int DEAD_ZONE_MOVEMENT = 4; // in degrees

	float[] mR = new float[16];
	float[] mOutR = new float[16];
	float[] mI = new float[16];
	private float[] mGravity = new float[] { 0, 0, 0 };
	private float[] mGeomagnetic = new float[] { 0, 0, 0 };
	private float[] mOldOrientation = new float[] { 0, 0, 0 };

	private float mDeclination = 0f;
	private boolean mHasDeclination = false;

	final float rad2deg = 180 / (float) Math.PI;

	private final HashSet<SensorEventListener> mObservers;

	public RealityOrientationListener() {
		mObservers = new HashSet<SensorEventListener>();
	}

	public void registerForUpdates(SensorEventListener observer) {
		mObservers.add(observer);
	}

	public void deregister(SensorEventListener observer) {
		mObservers.remove(observer);
	}

	/*
	 * SensorEventListener methods.
	 */

	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			if (event.accuracy != SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
				return;
			}

			final float[] values = event.values;
			final float[] gravity = mGravity;
			final float[] geomagnetic = mGeomagnetic;

			switch (event.sensor.getType()) {
			case Sensor.TYPE_ACCELEROMETER:
				gravity[0] = values[0];
				gravity[1] = values[1];
				gravity[2] = values[2];
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				geomagnetic[0] = values[0];
				geomagnetic[1] = values[1];
				geomagnetic[2] = values[2];
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

			/*
			 * Broadcast to the observers.
			 */
			for (SensorEventListener listener : mObservers) {
				listener.onSensorChanged(event);
			}
		}
	}

	public void lightFilterValues(final float[] values) {
		values[0] *= rad2deg; // azimuth
		values[1] *= rad2deg; // pitch
		values[2] *= rad2deg; // roll

		values[0] -= mDeclination;

		// Convert the (-180,180] range to [0,360).
		if (values[0] < 0) {
			values[0] += 360;
		} else if (values[0] >= 360) {
			values[0] -= 360;
		}
	}

	public void filterValues(final float[] values) {
		values[0] *= rad2deg; // azimuth
		values[1] *= rad2deg; // pitch
		values[2] *= rad2deg; // roll

		final float[] oldValues = mOldOrientation;
		float difference;

		for (int i = 0; i < 3; ++i) {
			/*
			 * Account for azimuth rotations around SOUTH. Azimuth values range
			 * is (-180,180].
			 * 
			 * Only account for wrap-around at SOUTH. Don't use corrections at
			 * the NORTH azimuth value (ie. around 0 degrees).
			 */
			difference = Math.abs(values[i] - oldValues[i]);
			if (difference > 180) {
				if (values[i] < 0) {
					// Only the new azimuth is negative.
					oldValues[i] -= 360;
				} else {
					// Only the old azimuth is negative.
					oldValues[i] += 360;
				}
			}

			// Scale the axis rotations.
			if (difference < DEAD_ZONE_MOVEMENT) {
				values[i] = (float) ((oldValues[i] * SLOW_BETA) + (values[i] * SLOW_ALPHA));
			} else {
				values[i] = (float) ((oldValues[i] * BETA) + (values[i] * ALPHA));
			}
		}

		// Save the current values.
		oldValues[0] = values[0];
		oldValues[1] = values[1];
		oldValues[2] = values[2];

		values[0] -= mDeclination;

		// Convert the (-180,180] range to [0,360).
		if (values[0] < -360) {
			values[0] = 0;

			// TODO: Temporary fix (Sometimes the oldValues[0] is out of
			// bounds).
			oldValues[0] = 0;
		} else if (values[0] < 0) {
			values[0] += 360;
		} else if (values[0] >= 720) {
			values[0] = 0;

			// TODO: Temporary fix (Sometimes the oldValues[0] is out of
			// bounds).
			oldValues[0] = 0;
		} else if (values[0] >= 360) {
			values[0] -= 360;

			// TODO: Temporary fix (Sometimes the oldValues[0] is out of
			// bounds).
			oldValues[0] = 0;
		}

		// Log.d(TAG, "(" + values[0] + "," + values[1] + "," + values[2] +
		// ")");
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
	}

	public void onProviderDisabled(String provider) {

	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {

	}

}
