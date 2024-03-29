package com.common;

import java.io.Serializable;

public class Coordinate implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6812733027545069991L;

	public double latitude;
	public double longitude;

	public Coordinate() {
		this(0, 0);
	}

	public Coordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Coordinate) {
			Coordinate coord = (Coordinate) o;
			return coord.latitude == latitude && coord.longitude == longitude;
		}
		return false;
	}

	public String toString() {
		return "(" + latitude + "," + longitude + ")";
	}

}
