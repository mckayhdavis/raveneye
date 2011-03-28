package com.reality;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;

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

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			return true;
		}
		return false;
	}

}
