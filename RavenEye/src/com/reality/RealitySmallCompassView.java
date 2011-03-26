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

		super.onMeasure(parentWidth >> 2, parentWidth >> 2);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			return true;
		}
		return false;
	}

}
