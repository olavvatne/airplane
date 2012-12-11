package routes;

import aircrafts.Aircraft;
import destinations.Destination;

/*
 * Author: torgeha
 * 
 * A route between two destinations
 * 
 * */

public class Route {

	private final Destination start;
	private final Destination end;
	private Aircraft aircraft;
	private final int range;

	public Route(Destination start, Destination end, int range, Aircraft aircraft) {
		this.start = start;
		this.end = end;
		this.range = range;
		setAircraft(aircraft);
	}

	public void setAircraft(Aircraft aircraft) {
		this.aircraft = aircraft;
		if (this.aircraft.getRoute() == null)
			this.aircraft.assignToRoute(this);
	}

	public Destination getStartDestination() {
		return start;
	}

	public Destination getEndDestination() {
		return end;
	}

	public Aircraft getAircraft() {
		return this.aircraft;
	}

	public int getRange() {
		return range;
	}

	public String toString() {
		return start + " - " + end;
	}
}
