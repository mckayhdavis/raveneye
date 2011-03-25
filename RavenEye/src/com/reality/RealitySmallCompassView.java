package com.reality;

import android.content.Context;
import android.util.AttributeSet;

public class RealitySmallCompassView extends RealityCompassView {
    
    public RealitySmallCompassView(Context context, AttributeSet attr) {
        super(context, attr);
        
        setRadius(60);
    }
    
}
