package com.reality;

import com.common.Place;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class PlaceOverlayItem extends OverlayItem {

	public final Place place;

	public PlaceOverlayItem(GeoPoint point, Place place) {
		super(point, place.name, place.description);

		this.place = place;
	}

}
