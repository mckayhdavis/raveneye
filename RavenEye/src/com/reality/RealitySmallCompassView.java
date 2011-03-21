package com.reality;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.SensorEvent;
import android.util.AttributeSet;
import android.util.Log;

import com.common.Place;

public class RealitySmallCompassView extends SensorView {

	public static final String TAG = RealityActivity.TAG;// "RealityCompassView";

	private static final int PLACE_RADIUS = 4;
	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private float mOrientationValues[] = new float[3];

	private static final int COMPASS_RADIUS = 50;
	private int mCompassX;
	private int mCompassY;
	private int mRadius;

	final float RAD_TO_DEG = (float) (180.0f / Math.PI);
	final float DEG_TO_RAD = (float) (Math.PI / 180.0f);

	private RectF oval;
	private Canvas mCanvas = null;
	private Bitmap mBitmap = null;

	private boolean mFirstDraw = true;

	private List<Place> mCachedPlaces = null;

	public RealitySmallCompassView(Context context, AttributeSet attr) {
		super(context, attr);

		mPaint.setARGB(100, 61, 89, 171);
		mPaint.setStrokeWidth(20);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int radius = COMPASS_RADIUS;
		int diameter = radius * 2;
		int center = radius;

		mRadius = radius;

		// TODO: memory leak possible here
		mBitmap = Bitmap.createBitmap(diameter, diameter,
				Bitmap.Config.ARGB_4444);
		final Canvas c = new Canvas(mBitmap);
		mCanvas = c;

		final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
		p.setStyle(Style.FILL);

		radius = COMPASS_RADIUS - 1;
		int actualWidth = (int) (radius * 0.25);
		int strokeWidth = actualWidth;
		int adjustWidth = (int) (strokeWidth / 2);

		int w1 = (int) (strokeWidth) + 2;
		int x = center - w1;
		int y = center + w1;
		RectF oval3 = new RectF(x, x, y, y);

		p.setARGB(200, 200, 200, 200);
		c.drawOval(oval3, p);

		p.setStyle(Style.STROKE);

		p.setStrokeWidth(2);
		p.setARGB(255, 50, 50, 50);
		c.drawCircle(center, center, (int) radius, p);
		// OUTER BLACK LINE

		p.setStrokeWidth(strokeWidth + 1);

		radius -= adjustWidth;
		p.setARGB(200, 255, 255, 255);
		c.drawCircle(center, center, (int) radius, p);
		// INNER WHITE

		radius -= strokeWidth;
		p.setARGB(200, 200, 200, 200);
		c.drawCircle(center, center, (int) radius, p);
		// INNER GRAY

		radius -= strokeWidth;
		p.setARGB(200, 255, 255, 255);
		c.drawCircle(center, center, (int) radius, p);
		// INNER WHITE

		p.setStrokeWidth(0);
		p.setStyle(Style.FILL);
		p.setARGB(220, 100, 100, 100);
		c.drawCircle(center, center, 3, p);

		mCompassX = 11;
		mCompassY = 11;
		y = diameter + mCompassX - 1;
		oval = new RectF(mCompassX, mCompassY, y, y);
		RectF oval2 = new RectF(0, 0, diameter, diameter);

		// c.drawArc(oval2, -105, 30, true, p);

		super.onSizeChanged(w, h, oldw, oldh);

		Log.d(TAG, "onSizeChanged() - Bitmap and canvas created.");
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mBitmap != null) {
			if (mCachedPlaces != null) {
				onPlacesChanged(mCachedPlaces);
				mCachedPlaces = null;
			}

			synchronized (this) {
				final float[] values = mOrientationValues;

				// Rotate the canvas according to the device roll.
				canvas.rotate(360 - values[0], mCompassX + mRadius, mCompassY
						+ mRadius);

				canvas.drawBitmap(mBitmap, mCompassX, mCompassY, null);
				canvas.drawArc(oval, values[0] - 105, 30, true, mPaint);
			}
		} else {
			onSizeChanged(0, 0, 0, 0);
		}
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			mOrientationValues = event.values;
			
			// Must copy over the values.
			// System.arraycopy(event.values, 0, mOrientationValues, 0, 3);
		}
	}

	@Override
	public void onPlacesChanged(List<Place> places) {
		if (mCanvas != null) {
			Paint paint = mPaint;

			Log.d(TAG, "onPlacesChanged() - Loading " + places.size()
					+ " places.");

			double bearing, distance, angle, hypotenuse, x, y;
			int radius;
			double maxDistance = 0f;

			if (places.size() > 0) {
				radius = mRadius - PLACE_RADIUS;

				// Find the farthest place in terms of distance.
				for (Place place : places) {
					distance = place.distance;

					if (distance > maxDistance) {
						maxDistance = distance;
					}
				}

				for (Place place : places) {
					/*
					 * Calculate the positioning of the place on the compass
					 * view.
					 */
					bearing = place.bearing;
					distance = place.distance;

					// Calculate the hypotenuse of the point's triangle.
					if (distance >= maxDistance) {
						hypotenuse = radius;
					} else {
						hypotenuse = radius * (distance / maxDistance);
					}

					if (bearing <= 90) {
						angle = (90 - bearing) * DEG_TO_RAD;

						x = Math.cos(angle) * hypotenuse;
						y = Math.sqrt((hypotenuse * hypotenuse) - (x * x));

						x += radius;
						y = radius - y;
					} else if (bearing <= 180) {
						angle = (bearing - 90) * DEG_TO_RAD;

						x = Math.cos(angle) * hypotenuse;
						y = Math.sqrt((hypotenuse * hypotenuse) - (x * x));

						x += radius;
						y += radius;
					} else if (bearing <= 270) {
						angle = (bearing - 180) * DEG_TO_RAD;

						x = Math.sin(angle) * hypotenuse;
						y = Math.sqrt((hypotenuse * hypotenuse) - (x * x));

						x = radius - x;
						y += radius;
					} else {
						angle = (bearing - 270) * DEG_TO_RAD;

						x = Math.cos(angle) * hypotenuse;
						y = Math.sqrt((hypotenuse * hypotenuse) - (x * x));

						x = radius - x;
						y = radius - y;
					}

					synchronized (this) {
						mCanvas.drawCircle((int) x + PLACE_RADIUS, (int) y
								+ PLACE_RADIUS, PLACE_RADIUS, paint);
					}
				}
			}
		} else {
			mCachedPlaces = places;
			Log.d(TAG, "onPlacesChanged() - Not ready, caching results ("
					+ places.size() + ")");
		}
	}

}
