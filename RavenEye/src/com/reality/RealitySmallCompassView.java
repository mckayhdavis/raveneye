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
import com.common.PlaceOverlayWrapper;

public class RealitySmallCompassView extends SensorView {

	public static final String TAG = RealityActivity.TAG;// "RealityCompassView";

	private static final int COMPASS_RADIUS = 60;
	private static final int PLACE_RADIUS = 1;// (int) ((float) COMPASS_RADIUS /
												// 12);
	private static final int COMPASS_OFFSET = 10;

	private final Paint mHeadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private final Paint mPlacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private float mOrientationValues[] = new float[3];

	private int mCompassCenter;
	private int mRadius;

	public static final float RAD_TO_DEG = (float) (180.0f / Math.PI);
	public static final float DEG_TO_RAD = (float) (Math.PI / 180.0f);

	private RectF oval;
	private Canvas mCanvas = null;
	private Bitmap mBitmap = null;

	private boolean mFirstDraw = true;

	private List<PlaceOverlayWrapper> mCachedPlaces = null;

	public RealitySmallCompassView(Context context, AttributeSet attr) {
		super(context, attr);

		mHeadingPaint.setARGB(100, 255, 255, 255);
		// mHeadingPaint.setARGB(60, 61, 89, 171);
		mHeadingPaint.setStrokeWidth(20);

		mPlacePaint.setARGB(255, 61, 89, 171);
		mPlacePaint.setStrokeWidth(8);
		mPlacePaint.setStyle(Paint.Style.FILL_AND_STROKE);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		int diameter = COMPASS_RADIUS * 2;
		mCompassCenter = (int) (diameter * 0.5f) + COMPASS_OFFSET;

		// TODO: memory leak possible here
		mBitmap = Bitmap.createBitmap(diameter, diameter,
				Bitmap.Config.ARGB_4444);
		final Canvas c = new Canvas(mBitmap);
		mCanvas = c;

		diameter -= 2;

		int radius = (int) (diameter * 0.5f);
		int segment = (int) (radius * 0.25f);

		mRadius = radius;

		final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		pFill.setStyle(Style.FILL);
		pFill.setARGB(100, 100, 100, 100);
		final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
		pStroke.setStyle(Style.STROKE);
		pStroke.setStrokeWidth(3.0f);
		pStroke.setARGB(100, 255, 255, 255);

		RectF oval3 = new RectF(4, 4, diameter, diameter);
		c.drawOval(oval3, pFill);

		oval3 = new RectF(4, 4, diameter, diameter);
		c.drawOval(oval3, pStroke);

		int y = diameter + COMPASS_OFFSET - 1;
		oval = new RectF(13, 13, y + 1, y + 1);
		// RectF oval2 = new RectF(0, 0, diameter, diameter);

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
				canvas.rotate(360 - values[0], mCompassCenter, mCompassCenter);

				canvas.drawArc(oval, values[0] - 112, 44, true, mHeadingPaint);
				canvas.drawBitmap(mBitmap, COMPASS_OFFSET, COMPASS_OFFSET, null);
			}
		} else {
			onSizeChanged(0, 0, 0, 0);
		}
	}

	public void onSensorChanged(SensorEvent event) {
		synchronized (this) {
			// Must copy over the values.
			System.arraycopy(event.values, 0, mOrientationValues, 0, 3);
		}
	}

	@Override
	public void onPlacesChanged(List<PlaceOverlayWrapper> places) {
		if (mCanvas != null) {
			Paint paint = mPlacePaint;

			Log.d(TAG, "onPlacesChanged() - Loading " + places.size()
					+ " places.");

			double bearing, distance, angle, hypotenuse, x, y;
			int radius;
			double maxDistance = 0f;

			if (places.size() > 0) {
				if (!mFirstDraw) {
					mCanvas.restore();
				} else {
					mFirstDraw = false;
				}
				// Save the "place-free" state of the canvas.
				mCanvas.save();

				radius = mRadius - PLACE_RADIUS;

				Place place;
				// Find the farthest place in terms of distance.
				for (PlaceOverlayWrapper p : places) {
					place = p.place;

					distance = place.distance;

					if (distance > maxDistance) {
						maxDistance = distance;
					}
				}

				for (PlaceOverlayWrapper p : places) {
					place = p.place;

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
