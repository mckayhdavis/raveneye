package com.reality;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;

public class RealitySmallCompassView extends RealityCompassView {
    
    public RealitySmallCompassView(Context context, AttributeSet attr) {
        super(context, attr);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        
        int radius = (int) (parentWidth * 0.3f);
        super.onMeasure(radius, radius);
    }
    
}
