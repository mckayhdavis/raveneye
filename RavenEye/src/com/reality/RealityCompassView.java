package com.reality;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.hardware.SensorEvent;
import android.util.AttributeSet;
import android.util.Log;

import com.common.Coordinate;
import com.common.Place;
import com.common.PlaceOverlayWrapper;

public class RealityCompassView extends SensorView {
    
    public static final String TAG = RealityActivity.TAG;// "RealityCompassView";
    
    public static final int COMPASS_VIEW_ANGLE = 50;
    public static final int COMPASS_OFFSET = 10;
    public static final int MAX_PLACE_RADIUS = 7;
    
    private final Paint mHeadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPlacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    
    private float mOrientationValues[] = new float[3];
    
    private float mRadius;
    private float mPlaceRadius;
    
    public static final float RAD_TO_DEG = (float) (180.0f / Math.PI);
    public static final float DEG_TO_RAD = (float) (Math.PI / 180.0f);
    
    private RectF mViewPointOval;
    protected Canvas mCanvas = null;
    private Bitmap mBitmap = null;
    
    private List<PlaceOverlayWrapper> mCachedPlaces = null;
    
    public RealityCompassView(Context context, AttributeSet attr) {
        super(context, attr);
        
        mHeadingPaint.setARGB(50, 255, 255, 255);
        // mHeadingPaint.setARGB(100, 61, 89, 171);
        mHeadingPaint.setStrokeWidth(20);
        
        // mPlacePaint.setARGB(255, 220, 220, 220);
        mPlacePaint.setARGB(255, 200, 200, 200);
        // mPlacePaint.setARGB(255, 176, 226, 255);
        mPlacePaint.setStyle(Paint.Style.FILL);
        
        setWillNotDraw(true);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        
        this.setMeasuredDimension(parentWidth, parentHeight);
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
                canvas.rotate(360 - values[0] - values[2], mRadius, mRadius);
                
                canvas.drawArc(mViewPointOval, values[0] - 115,
                        COMPASS_VIEW_ANGLE, true, mHeadingPaint);
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mPlaceRadius = w * 0.03f;
        if (mPlaceRadius > MAX_PLACE_RADIUS) {
            mPlaceRadius = MAX_PLACE_RADIUS;
        }
        mRadius = w * 0.5f;
        
        // TODO: memory leak possible here
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        mCanvas = new Canvas(mBitmap);
        mViewPointOval = new RectF(2, 2, w - 2, h - 2);
        
        super.onSizeChanged(w, h, oldw, oldh);
        
        Log.d(TAG, "onSizeChanged() - Bitmap and canvas created.");
    }
    
    public synchronized void onSensorChanged(SensorEvent event) {
        // Must copy over the values.
        System.arraycopy(event.values, 0, mOrientationValues, 0, 3);
    }
    
    @Override
    public void onPlacesChanged(List<PlaceOverlayWrapper> places) {
        if (mCanvas != null) {
            Paint paint = mPlacePaint;
            
            Log.d(TAG, "onPlacesChanged() - Loading " + places.size()
                    + " places.");
            
            mCanvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
            
            if (places.size() > 0) {
                double bearing, distance, angle, hypotenuse, x, y;
                double maxDistance = 0;
                float radius;
                
                radius = mRadius - mPlaceRadius;
                
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
                     * Calculate the positioning of the place on the compass view.
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
                        mCanvas.drawCircle((float) x + mPlaceRadius, (float) y
                                + mPlaceRadius, mPlaceRadius, paint);
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
