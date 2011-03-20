package com.reality;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.SensorEvent;
import android.location.Location;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;

public class RealityDirectionView extends SensorView implements
		DirectionObserver {

	public static final String TAG = RealityActivity.TAG;

	private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	private final float[] mOrientation = new float[3];
	private int mHeading = 0; // The destination direction.

	private boolean mInitialized = false;
	private boolean mHasDirections = false;

	private int mRadius;

	private int mCenterX;
	private int mCenterY;

	private Path mPath = null;

	private int mBitmapX;
	private int mBitmapY;

	private Canvas mCanvas = null;
	private Bitmap mBitmap = null;
	private Camera mCamera;

	public RealityDirectionView(Context context, AttributeSet attr) {
		super(context, attr);

		mPaint.setARGB(100, 255, 0, 0);
		mPaint.setStrokeWidth(15);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setShadowLayer(10, 5, 5, Color.BLACK);

		mCamera = new Camera();

		this.setWillNotDraw(true);
	}

	@Override
	public void onDraw(Canvas canvas) {
		synchronized (this) {
			// Rotate the canvas according to the device orientation.

			// canvas.drawBitmap(mBitmap, mBitmapX, mBitmapY, null);

			canvas.rotate(-mOrientation[0] + mHeading, mCenterX, mCenterY);

			canvas.drawPath(mPath, mPaint);
			canvas.drawCircle(mCenterX, mCenterY, mRadius - 37, mPaint);

			// //////

			// final Camera camera = mCamera;
			//
			// Transformation t = new Transformation();
			// final Matrix matrix = t.getMatrix();
			//
			// float mDepthZ = 180.0f;
			// float interpolatedTime = 0.0f;
			//
			// // canvas.scale(-1f, 1f, super.getWidth() * 0.5f,
			// // super.getHeight() * 0.5f);
			// canvas.rotate(-mOrientation[0] + mHeading, mCenterX,
			// mCenterY);
			//
			// camera.save();
			// if (false) {
			// camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
			// } else {
			// camera.translate(0.0f, 0.0f, mDepthZ
			// * (1.0f - interpolatedTime));
			// }
			//
			// // camera.rotateZ(-mOrientation[0]);
			//
			// float pitch = mOrientation[1];
			// camera.rotateX(80);
			// // camera.rotateZ(-20);
			// camera.getMatrix(matrix);
			//
			// camera.applyToCanvas(canvas);
			//
			// canvas.drawPath(mPath, mPaint);
			// canvas.drawCircle(mCenterX, mCenterY, mRadius - 37, mPaint);
			//
			// camera.restore();

			// matrix.preTranslate(-mCenterX, -mCenterY);
			// matrix.postTranslate(mCenterX, mCenterY);
		}
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
		synchronized (this) {
			if (!mInitialized) {
				int diameter = width < height ? width - 100 : height - 100;
				mCenterX = width / 2;
				mCenterY = height / 2;

				mRadius = (diameter / 2);

				mPath = new Path();
				mPath.moveTo(mCenterX, mCenterY - mRadius - 10);
				mPath.lineTo(mCenterX - 15, mCenterY - mRadius);
				mPath.lineTo(mCenterX + 15, mCenterY - mRadius);
				// path.addArc(new RectF(140, 180, 180, 220), 180, 180);
				mPath.lineTo(mCenterX, mCenterY - mRadius - 10);
				mPath.close();

				// mBitmapX = mCenterX - mRadius;
				// mBitmapY = mCenterY - mRadius;
				//
				// unbindBitmaps();
				//
				// mBitmap = Bitmap.createBitmap(diameter, diameter,
				// Bitmap.Config.ARGB_4444);
				//
				// mCanvas = new Canvas(mBitmap);
				//
				// Path path = new Path();
				// path.moveTo(mRadius, 10);
				// path.lineTo(mRadius - 15, 22);
				// path.lineTo(mRadius + 15, 22);
				// // path.addArc(new RectF(140, 180, 180, 220), 180, 180);
				// path.lineTo(mRadius, 10);
				// path.close();
				//
				// mCanvas.drawPath(path, mPaint);
				// mCanvas.drawCircle(mRadius, mRadius, mRadius - 37, mPaint);

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

		float diff = Math.abs(values[0] - mHeading);
		if (diff < 10 || diff > 350) {
			mPaint.setARGB(100, 0, 255, 0);
		} else if (diff < 30 || diff > 330) {
			mPaint.setARGB(100, 255, 255, 0);
		} else if (diff < 50 || diff > 310) {
			mPaint.setARGB(100, 255, 165, 0);
		} else {
			mPaint.setARGB(100, 255, 0, 0);
		}
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

	public void onDirectionsChanged(DirectionEvent event) {
		if (!mHasDirections) {
			mHasDirections = true;

			this.setWillNotDraw(false);
		}

		mHeading = event.bearing;

		Log.d(TAG, "[HEAD] " + mHeading);
	}

}
