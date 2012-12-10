package aircrafts;

import player.Player;
import routes.Route;

/*Author: torgeha
 * 
 * Abstract class, all aircraft inherits from this.
 * Has the basic functionality of a general aircraft
 * 
 * */
public abstract class Aircraft {

	private final String manufacturer;
	private final String modelFamily;
	private final String model;

	private int cost;
	private final int maxSpeed;
	private final int cruiseSpeed;
	private final int flightRange;
	private final int fuelConsumption;
	private final int lifeSpan;

	private Route route; // allowed to be null

	public Aircraft(String manufacturer, String modelFamily, String model, int cost, int maxSpeed, int cruiseSpeed, int flightRange, int fuelConsumption, int lifeSpan, Route route) {
		this.manufacturer = manufacturer;
		this.modelFamily = modelFamily;
		this.model = model;
		this.cost = cost;
		this.maxSpeed = maxSpeed;
		this.cruiseSpeed = cruiseSpeed;
		this.flightRange = flightRange;
		this.fuelConsumption = fuelConsumption;
		this.lifeSpan = lifeSpan;
		this.route = route;
	}

	public int getCost() {
		return cost;
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getModelFamily() {
		return modelFamily;
	}

	public String getModel() {
		return model;
	}

	public int getMaxSpeed() {
		return maxSpeed;
	}

	public int getCruiseSpeed() {
		return cruiseSpeed;
	}

	public int getFlightRange() {
		return flightRange;
	}

	public int getFuelConsumption() {
		return fuelConsumption;
	}

	public int getLifeSpan() {
		return lifeSpan;
	}

	public Route getRoute() {
		return route;
	}

	public void assignToRoute(Route route) {
		this.route = route;
		this.route.setAircraft(this);
	}

	public String toString() {
		String temp = manufacturer + " - " + modelFamily + " - " + model;

		if (this.route != null) {
			temp += "; Route: " + route;
		} else {
			temp += "; Not assigned to any route";
		}

		return temp;
	}

}
