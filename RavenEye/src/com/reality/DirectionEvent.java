package com.reality;

public class DirectionEvent {
    
    public final int status;
    public final int bearing;
    
    public static final int STATUS_ARRIVED = 0;
    public static final int STATUS_ONCOURSE = 1;
    public static final int STATUS_OFFCOURSE = 2;
    
    public DirectionEvent(int status, int bearing) {
        this.status = status;
        this.bearing = bearing;
    }
    
}
