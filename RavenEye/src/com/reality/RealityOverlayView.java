package com.reality;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff.Mode;
import android.hardware.SensorEvent;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;

import com.common.Coordinate;
import com.common.Place;
import com.common.PlaceOverlayWrapper;

/**
 * This class handles the drawing of different overlay views on the canvas. It is responsible for
 * interpreting the orientation and current location of the device and displaying different overlay
 * points accordingly.
 * 
 * @author Michael Du Plessis
 */
public class RealityOverlayView extends SensorView {
    
    public static final String NORTH = "N";
    public static final String NORTH_EAST = "NE";
    public static final String EAST = "E";
    public static final String SOUTH_EAST = "SE";
    public static final String SOUTH = "S";
    public static final String SOUTH_WEST = "SW";
    public static final String WEST = "W";
    public static final String NORTH_WEST = "NW";
    
    public static final String TAG = RealityActivity.TAG;// "RealityOverlayView";
    public static final int COMPASS_TEXT_SIZE = 46;
    
    private ArrayList<PlaceOverlayWrapper> mPlaceOverlays;
    private ArrayList<SensorView> mSensorViews;
    
    private Dictionary<PlaceOverlayWrapper, TouchPoint> mPlacePositions;
    
    private Place mSelectedPlace = null;
    
    private int mPlaceDiameter;
    private int mWidth;
    private int mHeight;
    
    private int mX = 0;
    private int mY = 0;
    private float mRoll = 0;
    private float mFactorX = 0;
    private float mFactorY = 0;
    private int mHorizontalTranslationCorrection = 0;
    private int mVerticalTranslationCorrection = 0;
    
    private boolean mInitialized = false;
    private boolean mDisplayedBefore = false;
    
    private static final int HORIZONTAL_QUADRANTS = 8;
    private static final int HORIZONTAL_QUADRANT_ANGLE = 360 / HORIZONTAL_QUADRANTS;
    private static final int VERTICAL_QUADRANTS = 1;
    private static final int VERTICAL_QUADRANT_ANGLE = 90 / VERTICAL_QUADRANTS;
    
    private int mCanvasWidth;
    private int mCanvasHeight;
    
    private Canvas mCanvas = null;
    private Bitmap mBitmap;
    
    private final Paint mGridPaint;
    private final Paint mCompassPaint;
    private final Paint mPlaceTextPaint;
    private final Paint mPlacePaint;
    
    public RealityOverlayView(Context context, AttributeSet attr) {
        super(context, attr);
        
        mPlaceOverlays = new ArrayList<PlaceOverlayWrapper>();
        mSensorViews = new ArrayList<SensorView>(1);
        
        mPlacePositions = new Hashtable<PlaceOverlayWrapper, TouchPoint>();
        
        mGridPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mGridPaint.setARGB(50, 0, 0, 0);
        
        mCompassPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCompassPaint.setColor(Color.WHITE);
        mCompassPaint.setTextAlign(Align.CENTER);
        mCompassPaint.setTextSize(COMPASS_TEXT_SIZE);
        mCompassPaint.setFakeBoldText(true);
        
        mPlacePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        
        mPlaceTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPlaceTextPaint.setColor(Color.WHITE);
        mPlaceTextPaint.setTextSize(20);
        mPlaceTextPaint.setTextAlign(Align.CENTER);
        mPlaceTextPaint.setFakeBoldText(true);
    }
    
    public synchronized void unbindBitmaps() {
        /*
         * The bitmap will need to be recreated next time.
         */
        mInitialized = false;
        
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        mCanvas = null;
    }
    
    public int getPlacesCount() {
        return mPlaceOverlays.size();
    }
    
    public Place getSelectedPlace() {
        return mSelectedPlace;
    }
    
    public void addOverlay(Place place) {
        mPlaceOverlays.add(new PlaceOverlayWrapper(place));
        
        if (mInitialized) {
            int len = mSensorViews.size();
            for (int i = 0; i < len; ++i) {
                mSensorViews.get(i).onPlacesChanged(mPlaceOverlays);
            }
        }
    }
    
    public void addAllOverlays(List<Place> places) {
        int len = places.size();
        for (int i = 0; i < len; ++i) {
            mPlaceOverlays.add(new PlaceOverlayWrapper(places.get(i)));
        }
    }
    
    public void registerForUpdates(SensorView view) {
        mSensorViews.add(view);
    }
    
    public boolean isInitialized() {
        return mInitialized;
    }
    
    public boolean hasDisplayedBefore() {
        return mDisplayedBefore;
    }
    
    /**
     * Draw only the views that are in the device viewpoint. Scale the movement to ensure smooth
     * transitions of the overlays.
     */
    @Override
    public void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            synchronized (this) {
                // Rotate the canvas according to the device roll.
                canvas.rotate(mRoll, mHorizontalTranslationCorrection,
                        mVerticalTranslationCorrection);
                canvas.translate(mX, mY);
                
                /*
                 * Draw the generated bitmap. Account for wrap-around at North.
                 */
                if (mX >= -mHorizontalTranslationCorrection) {
                    canvas.drawBitmap(mBitmap, -mCanvasWidth, 0, null);
                } else if (mX <= ((mWidth + mHorizontalTranslationCorrection) - mCanvasWidth)) {
                    canvas.drawBitmap(mBitmap, mCanvasWidth, 0, null);
                }
                
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
            
            mDisplayedBefore = true;
        } else {
            onSizeChanged(mWidth, mHeight, mWidth, mHeight);
        }
    }
    
    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        if (!mInitialized) {
            if (width == 0 || height == 0) {
                return;
            }
            
            mWidth = width;
            mHeight = height;
            
            mHorizontalTranslationCorrection = width >> 1;
            // mVerticalTranslationCorrection = height >> 1;
            
            mCanvasWidth = width * HORIZONTAL_QUADRANTS;
            mCanvasHeight = height * VERTICAL_QUADRANTS;
            
            mPlaceDiameter = (int) (mCanvasWidth * 0.09f);
            
            mFactorX = (float) width / HORIZONTAL_QUADRANT_ANGLE;
            mFactorY = (float) height / VERTICAL_QUADRANT_ANGLE;
            
            unbindBitmaps();
            
            mBitmap = Bitmap.createBitmap(mCanvasWidth, mCanvasHeight,
                    Bitmap.Config.ARGB_4444);
            
            mCanvas = new Canvas(mBitmap);
            
            mInitialized = true;
            
            // Display the overlay.
            onLocationChanged(mLocation);
        }
        
        super.onSizeChanged(width, height, oldw, oldh);
    }
    
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            final float[] values = event.values;
            
            mX = (-(int) (values[0] * mFactorX))
                    + mHorizontalTranslationCorrection;
            // TODO: optimize statement:
            mY = (int) ((mWidth * 0.5f)
                    - (Math.abs((mWidth - mHeight) * (values[2] / 90))) - (values[1] * mFactorY));
            mRoll = -values[2];
        }
        
        invalidate();
    }
    
    @Override
    public void onLocationChanged(Location location) {
        // Store the location.
        super.onLocationChanged(location);
        
        /*
         * Recreate the augmented reality bitmap.
         */
        if (mInitialized) {
            remapOverlays(location);
            
            notifyPlacesChanged();
        } else {
            onSizeChanged(mWidth, mHeight, mWidth, mHeight);
        }
    }
    
    public void notifyPlacesChanged() {
        int size = mSensorViews.size();
        if (size > 0) {
            Log.d(TAG, "onPlacesChanged() - Broadcasting places changed to "
                    + size + " listeners.");
            
            for (int i = 0; i < size; ++i) {
                mSensorViews.get(i).onPlacesChanged(mPlaceOverlays);
            }
        }
    }
    
    private void remapOverlays(Location location) {
        final Canvas canvas = mCanvas;
        final Paint placePaint = mPlacePaint;
        final Paint placeTextPaint = mPlaceTextPaint;
        final int canvasHeight = canvas.getHeight();
        final int canvasWidth = canvas.getWidth();
        final int horizon = 100;
        
        synchronized (this) {
            final Paint gridPaint = mGridPaint;
            final Paint compassPaint = mCompassPaint;
            
            final int increment = mWidth;
            final int textHeight = horizon - 20;
            int i = 0;
            
            canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);
            
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            // Draw N on both sides of north.
            canvas.drawText(NORTH, i, textHeight, compassPaint);
            canvas.drawText(NORTH, canvasWidth + i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(NORTH_EAST, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(EAST, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(SOUTH_EAST, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(SOUTH, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(SOUTH_WEST, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(WEST, i, textHeight, compassPaint);
            i += increment;
            canvas.drawLine(i, horizon, i, canvasHeight, gridPaint);
            canvas.drawText(NORTH_WEST, i, textHeight, compassPaint);
            
            int horizontal = horizon + 2;
            for (i = canvasHeight; i >= horizontal;) {
                i -= ((i - horizon) * 0.5) + 1;
                
                canvas.drawLine(0, i, canvasWidth, i, gridPaint);
            }
            
            if (location != null) {
                Log.d(TAG, "remapOverlays() - Mapped " + mPlaceOverlays.size()
                        + " overlays.");
                int len = mPlaceOverlays.size();
                for (i = 0; i < len; ++i) {
                    drawPlace(canvas, placePaint, placeTextPaint, location,
                            mPlaceOverlays.get(i), canvasWidth, canvasHeight,
                            horizon);
                }
            }
        }
    }
    
    private void drawPlace(Canvas canvas, Paint paint, Paint textPaint,
            Location currentLocation, PlaceOverlayWrapper placeWrapper,
            int canvasWidth, int canvasHeight, int horizon) {
        Place place = placeWrapper.place;
        
        final Coordinate placeCoordinate = place.coordinate;
        final Location placeLocation = new Location(currentLocation);
        placeLocation.setLatitude(placeCoordinate.latitude);
        placeLocation.setLongitude(placeCoordinate.longitude);
        
        if (placeLocation != null) {
            float bearing = currentLocation.bearingTo(placeLocation);
            if (bearing < 0) {
                bearing += 360;
            }
            
            // Set the place's current bearing.
            place.bearing = bearing;
            // Cache the distance in place.
            place.updateWithLocation(currentLocation);
            float distance = place.distance;
            String distanceString = Place.getDistanceString(distance);
            
            /*
             * Do some scaling (size).
             */

            byte alpha;
            double factor;
            if (distance >= 15000) {
                factor = 0.1;
                alpha = 0x55;
            } else {
                factor = 1 - (distance / 20000);
                alpha = (byte) ((0xAA * (1 - (distance / 15000))) + 0x55);
            }
            
            int width = (int) (mPlaceDiameter * factor);
            // height = (int) (mPlaceDiameter * factor);
            
            placeWrapper.setDimensions(width, width); // cache the dimensions
            
            int halfWidth = (width / 2);
            // int halfHeight = (height / 2);
            
            int x = (int) (bearing * mFactorX);
            int depth = (canvasHeight / 5);
            int y = (int) (horizon + (depth * factor));
            
            // Add the place to the hash table.
            mPlacePositions.put(placeWrapper, new TouchPoint(x, y));
            
            /*
             * Draw on the bitmap.
             */

            int r, g, b;
            if (mSelectedPlace == place) {
                r = 255;
                g = 0;
                b = 0;
            } else {
                r = 61;
                g = 89;
                b = 171;
            }
            paint.setARGB(alpha, r, g, b);
            
            int left = x - halfWidth;
            // int top = y - halfHeight;
            int right = x + halfWidth;
            // int bottom = y + halfHeight;
            
            // TODO: The text is cutoff at North if the actual shape is not
            // crossing North. (long place names are therefore cut off at North.
            if (left < 0) {
                // Draw on the left side of the canvas with some overlap across
                // north. Draw the right side of the canvas as well to ensure
                // wrap-around.
                // canvas.drawRect(canvasWidth + left, top, canvasWidth, bottom,
                // paint);
                // canvas.drawRect(0, top, right, bottom, paint);
                canvas.drawCircle(x, y, halfWidth, paint);
                canvas.drawCircle(canvasWidth + x, y, halfWidth, paint);
                
                canvas.drawText(place.name, x, y, textPaint);
                canvas.drawText(distanceString, x, y + 20, textPaint);
                
                canvas.drawText(place.name, canvasWidth + x, y, textPaint);
                canvas.drawText(distanceString, canvasWidth + x, y + 20,
                        textPaint);
            } else if (right > canvasWidth) {
                // Draw on the right side of the canvas with some overlap across
                // north. Draw the left side of the canvas as well to ensure
                // wrap-around.
                // canvas.drawRect(0, top, right - canvasWidth, bottom, paint);
                // canvas.drawRect(left, top, canvasWidth, bottom, paint);
                canvas.drawCircle(x, y, halfWidth, paint);
                canvas.drawCircle(x - canvasWidth, y, halfWidth, paint);
                
                canvas.drawText(place.name, x, y, textPaint);
                canvas.drawText(distanceString, x, y + 20, textPaint);
                
                canvas.drawText(place.name, x - canvasWidth, y, textPaint);
                canvas.drawText(distanceString, x - canvasWidth, y + 20,
                        textPaint);
            } else {
                // Draw the canvas normally.
                // canvas.drawRect(left, top, right, bottom, paint);
                canvas.drawCircle(x, y, halfWidth, paint);
                
                canvas.drawText(place.name, x, y, textPaint);
                canvas.drawText(distanceString, x, y + 20, textPaint);
            }
            
            // Log.d(TAG, "Draw place: " + place + " at (" + left + "px," +
            // right
            // + "px)");
        }
    }
    
    /**
     * Gets the place at the specified Cartesian coordinate. If no place is found at that
     * coordinate, then null is returned.
     * 
     * @param x
     *            - x coordinate
     * @param y
     *            - y coordinate
     * @return
     */
    // public Place getPlaceAt(int x, int y) {
    // Place place = null;
    //
    // synchronized (this) {
    // // Project the (x,y) coordinate onto the bitmap.
    //
    // double hypotenuse = Math.sqrt(Math.pow(x - (mWidth / 2), 2)
    // + Math.pow(y - (mHeight / 2), 2));
    //
    // int correctedX = (int) (Math.cos(mRoll
    // * RealityOrientationListener.DEG_TO_RAD) * hypotenuse);
    // int correctedY = mY - y;
    //
    // int projectedX = mX - x;
    // int projectedY = mY - y;
    //
    // }
    //
    // if (mSelectedPlace != place) {
    // mSelectedPlace = place;
    // remapOverlays(mLocation);
    // }
    //
    // return place;
    // }
    
    public Place getPlaceAt(int x, int y) {
        Place place = null;
        
        synchronized (this) {
            // Project the (x,y) coordinate onto the bitmap.
            
            int actualX = mX - x;
            int actualY = mY - y;
            
            if (actualX < -mCanvasWidth) {
                actualX += mCanvasWidth;
            } else if (actualX > 0) {
                actualX -= mCanvasWidth;
            }
            
            // Log.d(TAG, "bitmap(" + mX + "," + mY + ") canvas(" + x + "," + y
            // + ") actual(" + actualX + "," + actualY + ")");
            
            TouchPoint touch = new TouchPoint(-actualX, -actualY);
            // TouchPoint foundPlacePosition = null;
            TouchPoint placePosition;
            PlaceOverlayWrapper placeOverlay = null;
            // double max = 1000000;
            // double distance;
            
            Dictionary<PlaceOverlayWrapper, TouchPoint> placePositions = mPlacePositions;
            Enumeration<PlaceOverlayWrapper> enumeration = placePositions
                    .keys();
            while (enumeration.hasMoreElements()) {
                placeOverlay = enumeration.nextElement();
                placePosition = placePositions.get(placeOverlay);
                
                // Log.d(TAG, "place(" + placePosition.x + "," + placePosition.y
                // + ")");
                
                if (touch.inProximity(
                        placePosition,
                        Math.max(placeOverlay.getWidth(),
                                placeOverlay.getHeight()))) {
                    place = placeOverlay.place;
                    break;
                }
                
                // TODO: get more accurate result.
                // distance = touch.distanceTo(placePosition);
                // if (distance < max) {
                // max = distance;
                //
                // place = placeOverlay.place;
                // foundPlacePosition = placePosition;
                // }
            }
            
            // if (foundPlacePosition != null
            // && !touch.inProximity(
            // foundPlacePosition,
            // Math.max(placeOverlay.getWidth(),
            // placeOverlay.getHeight()))) {
            // place = null;
            // }
        }
        
        if (mSelectedPlace != place) {
            mSelectedPlace = place;
            remapOverlays(mLocation);
        }
        
        return place;
    }
    
    private class TouchPoint {
        
        public static final int DEFAULT_PROXIMITY_RADIUS = 100;
        
        public final int x;
        public final int y;
        
        public TouchPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof TouchPoint) {
                TouchPoint t = (TouchPoint) o;
                return inProximity(t, DEFAULT_PROXIMITY_RADIUS);
            }
            return false;
        }
        
        public boolean inProximity(TouchPoint a, int radius) {
            // Use the Cartesian distance formula.
            return distanceTo(a) <= radius;
        }
        
        public double distanceTo(TouchPoint a) {
            // Use the Cartesian distance formula.
            return Math.sqrt(Math.pow((x - a.x), 2) + Math.pow((y - a.y), 2));
        }
        
    }
    
}