package com.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.graphics.drawable.Drawable;
import android.location.Location;

public class Place implements Comparable<Place>, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String NO_DISTANCE_STRING = "Unknown";

	public final int id;
	public final String name;
	public final Coordinate coordinate;

	public float distance = -1;
	public String description = null;
	public String buildingCode = null;
	private ArrayList<Review> mReviews = null;

	public transient float bearing = -1;
	private transient Drawable mImageResource = null;
	private transient int mImageResourceId = -1;

	public Place(int id, String name, Coordinate coordinate) {
		this.id = id;
		this.name = name;
		this.coordinate = coordinate;
	}

	public void addReviews(List<Review> reviews) {
		if (mReviews == null) {
			mReviews = new ArrayList<Review>();
		}
		mReviews.addAll(reviews);
	}

	public void addReview(Review review) {
		if (mReviews == null) {
			mReviews = new ArrayList<Review>();
		}
		mReviews.add(review);
	}

	/**
	 * Return the first @number of reviews.
	 * 
	 * @param number
	 * @return
	 */
	public List<Review> getReviews(int number) {
		if (mReviews != null && number > 0) {
			int size = mReviews.size();
			if (number > size) {
				number = size;
			}
			try {
				// Return a copy of the reviews.
				return new ArrayList<Review>(mReviews.subList(0, number));
			} catch (IndexOutOfBoundsException e) {

			}
		}
		return null;
	}

	public Place setImageResource(Drawable resource) {
		mImageResource = resource;
		return this;
	}

	public Drawable getImageResource() {
		return mImageResource;
	}

	public Place setImageResourceId(int resourceId) {
		mImageResourceId = resourceId;
		return this;
	}

	public int getImageResourceId() {
		return mImageResourceId;
	}

	public static String getDistanceString(float distance) {
		String units;
		if (distance < 0) {
			return NO_DISTANCE_STRING;
		} else if (distance < 1000) {
			units = (int) distance + "m";
		} else {
			distance = (int) (distance / 100);
			distance = distance / 10;
			units = distance + "km";
		}
		return units;
	}

	public void updateWithLocation(Location location) {
		if (coordinate == null) {
			return;
		}

		float[] actualDistance = new float[1];
		Location.distanceBetween(location.getLatitude(),
				location.getLongitude(), coordinate.latitude,
				coordinate.longitude, actualDistance);

		distance = (float) ((int) (actualDistance[0] * 10)) / 10;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Place) {
			if (name.equals(((Place) object).name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	public int compareTo(Place place) {
		boolean hasDistance = distance >= 0;
		boolean otherHasDistance = place.distance >= 0;
		if (!hasDistance) {
			if (otherHasDistance) {
				return 1;
			}
		} else if (!otherHasDistance || distance < place.distance) {
			return -1;
		} else if (distance > place.distance) {
			return 1;
		}

		return name.compareTo(place.name);
	}

	@Override
	public String toString() {
		return name;
	}

}
