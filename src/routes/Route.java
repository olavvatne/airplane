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
		assignAircraft(aircraft);
	}

	private void assignAircraft(Aircraft aircraft) {
		this.aircraft = aircraft;
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

	public void setAircraft(Aircraft aircraft) {
		this.aircraft = aircraft;
	}

	public String toString() {
		return start + " - " + end;
	}
}
