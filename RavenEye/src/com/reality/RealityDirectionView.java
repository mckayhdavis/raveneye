package com.reality;

import android.content.Context;
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
    
    private static final int ALPHA = 255;
    
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    private float mBearing = 0; // The actual orientation.
    private int mHeading = 0; // The destination direction.
    
    private boolean mInitialized = false;
    private boolean mHasDirections = false;
    
    private int mRadius;
    
    private int mCenterX;
    private int mCenterY;
    
    private Path mPath = null;
    
    public RealityDirectionView(Context context, AttributeSet attr) {
        super(context, attr);
        
        mPaint.setARGB(100, 255, 0, 0);
        mPaint.setStrokeWidth(15);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setShadowLayer(10, 5, 5, Color.BLACK);
        
        this.setWillNotDraw(true);
    }
    
    @Override
    public void onDraw(Canvas canvas) {
        final Paint paint = mPaint;
        
        synchronized (this) {
            // Rotate the canvas according to the device orientation.
            canvas.rotate(mBearing, mCenterX, mCenterY);
            
            canvas.drawPath(mPath, paint);
            canvas.drawCircle(mCenterX, mCenterY, mRadius, paint);
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
            
            mRadius -= 37;
            
            mInitialized = true;
        }
        
        super.onSizeChanged(width, height, oldw, oldh);
    }
    
    public void onSensorChanged(SensorEvent event) {
        /*
         * Orient the directional arrow.
         */
        float difference;
        
        synchronized (this) {
            final float[] values = event.values;
            
            mBearing = -values[0] + mHeading - values[2];
            
            difference = Math.abs(values[0] - mHeading);
            
            // We cross north (0 degrees).
            if (difference > 180) {
                difference = 360 - difference;
            }
            
            float alpha = (difference / 180);
            float beta = 1 - alpha;
            
            mPaint.setARGB(ALPHA, (int) (alpha * 255), (int) (beta * 255), 0);
        }
    }
    
    public void onDirectionsChanged(DirectionEvent event) {
        synchronized (this) {
            if (!mHasDirections) {
                mHasDirections = true;
                
                this.setWillNotDraw(false);
            }
            
            mHeading = event.bearing;
        }
    }
    
}
