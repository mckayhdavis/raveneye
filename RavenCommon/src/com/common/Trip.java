package com.common;


public class Trip<T extends Waypoint> {

	public final T to;
	public final T from;

	public Trip(T to, T from) {
		this.to = to;
		this.from = from;
	}

}
