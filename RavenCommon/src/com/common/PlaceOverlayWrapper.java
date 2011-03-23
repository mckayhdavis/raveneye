package com.common;

public class PlaceOverlayWrapper {

	public final Place place;

	private int mWidth = 0;
	private int mHeight = 0;

	public PlaceOverlayWrapper(final Place place) {
		this.place = place;
	}

	public void setDimensions(int width, int height) {
		mWidth = width;
		mHeight = height;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

}
