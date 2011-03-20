package com.reality;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.SensorEvent;
import android.util.AttributeSet;

import com.common.DirectionEvent;
import com.common.DirectionObserver;

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
		final Paint paint = mPaint;

		synchronized (this) {
			// Rotate the canvas according to the device orientation.
			canvas.rotate(-mOrientation[0] + mHeading, mCenterX, mCenterY);

			canvas.drawPath(mPath, paint);
			canvas.drawCircle(mCenterX, mCenterY, mRadius - 37, paint);

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
			// canvas.drawCircle(mCenterX, mCenterY, mRadius - 37, paint);
			//
			// camera.restore();

			// matrix.preTranslate(-mCenterX, -mCenterY);
			// matrix.postTranslate(mCenterX, mCenterY);
		}
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldw, int oldh) {
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

			mInitialized = true;
		}

		super.onSizeChanged(width, height, oldw, oldh);
	}

	public void onSensorChanged(SensorEvent event) {
		/*
		 * Orient the directional arrow.
		 */

		final float[] storedValues = mOrientation;
		float difference;

		synchronized (this) {
			final float[] values = event.values;

			storedValues[0] = values[0];
			storedValues[1] = values[1];
			storedValues[2] = values[2];
		}

		difference = Math.abs(storedValues[0] - mHeading);

		// We cross north (0 degrees).
		if (difference > 180) {
			difference = 360 - difference;
		}

		float alpha = (difference / 180);
		float beta = 1 - alpha;

		mPaint.setARGB(100, (int) (alpha * 255), (int) (beta * 255), 0);
	}

	public void onDirectionsChanged(DirectionEvent event) {
		if (!mHasDirections) {
			mHasDirections = true;

			this.setWillNotDraw(false);
		}

		mHeading = event.bearing;
	}

}
