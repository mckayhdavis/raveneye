package com.reality;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class DirectionManager implements SensorEventListener, LocationListener {
    
    private List<DirectionObserver> mObservers = new ArrayList<DirectionObserver>(
            2); // Generally a low number of observers.
    
    public DirectionManager() {
        
    }
    
    public void registerObserver(DirectionObserver observer) {
        mObservers.add(observer);
    }
    
    public void notifyObservers(DirectionEvent event) {
        for(DirectionObserver observer : mObservers) {
            observer.onDirectionsChanged(event);
        }
    }
    
    /*
     * Location events.
     */

    public void onLocationChanged(Location location) {
        
    }

    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
        
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
        
    }
    
    /*
     * Sensor events.
     */

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub
        
    }

    public void onSensorChanged(SensorEvent arg0) {
        // TODO Auto-generated method stub
        
    }
    
}
