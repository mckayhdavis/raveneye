package com.reality;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.hardware.SensorEvent;
import android.location.Location;
import android.util.AttributeSet;

public class RealityDirectionView extends SensorView {

	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private final float[] mOrientation = new float[3];

	private boolean mInitialized = false;

	private int mDiameter;

	private int mCenterX;
	private int mCenterY;
	private int mBitmapX;
	private int mBitmapY;

	private Canvas mCanvas = null;
	private Bitmap mBitmap = null;

	public RealityDirectionView(Context context, AttributeSet attr) {
		super(context, attr);

		mPaint.setARGB(100, 255, 0, 0);
		mPaint.setStrokeWidth(15);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setShadowLayer(10, 5, 5, Color.BLACK);
	}

	@Override
	public void onDraw(Canvas canvas) {
		if (mBitmap != null) {
			synchronized (this) {
                //canvas.getMatrix().postSkew(0.1f, 0.6f, 0.4f, 0.3f);

				// Rotate the canvas according to the device orientation.
				canvas.rotate(-mOrientation[0], mCenterX, mCenterY);

				canvas.drawBitmap(mBitmap, mBitmapX, mBitmapY, null);
			}
		}
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
		synchronized (this) {
			if (!mInitialized) {
				mDiameter = width < height ? width - 100 : height - 100;
				mCenterX = width / 2;
				mCenterY = height / 2;

				int radius = (mDiameter / 2);

				mBitmapX = mCenterX - radius;
				mBitmapY = mCenterY - radius;

				unbindBitmaps();

				mBitmap = Bitmap.createBitmap(mDiameter, mDiameter,
						Bitmap.Config.ARGB_4444);

				mCanvas = new Canvas(mBitmap);

				Path path = new Path();
				path.moveTo(radius, 10);
				path.lineTo(radius - 15, 22);
				path.lineTo(radius + 15, 22);
				// path.addArc(new RectF(140, 180, 180, 220), 180, 180);
				path.lineTo(radius, 10);
				path.close();

				// mCanvas.drawCircle(radius, 35, 10, mPaint);
				mCanvas.drawPath(path, mPaint);
				mCanvas.drawCircle(radius, radius, radius - 37, mPaint);

				mInitialized = true;
			}
		}

		super.onSizeChanged(width, height, oldw, oldh);
	}

	@Override
	public void onLocationChanged(Location location) {
		/*
		 * Check way-points.
		 */
	}

	public void onSensorChanged(SensorEvent event) {
		/*
		 * Orient the directional arrow.
		 */

		final float[] values = event.values;

		mOrientation[0] = values[0];
		mOrientation[1] = values[1];
		mOrientation[2] = values[2];
	}

	public void unbindBitmaps() {
		synchronized (this) {
			/*
			 * The bitmap will need to be recreated next time.
			 */
			mInitialized = false;

			if (mBitmap != null) {
				mBitmap.recycle();
			}

			mBitmap = null;
			mCanvas = null;
		}
	}

}
