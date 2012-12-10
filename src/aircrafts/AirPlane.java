package aircrafts;

import routes.Route;

/* Author: torgeha
 * 
 * Abstract class for planes. Has some additional fields fields exclusively for planes.
 * 
 * */
public abstract class AirPlane extends Aircraft {

	private int wingspan;

	public AirPlane(String manufacturer, String modelFamily, String model, int cost, int maxSpeed, int cruiseSpeed, int flightRange, int fuelConsumption, int lifeSpan, Route route, int wingspan) {
		super(manufacturer, modelFamily, model, cost, maxSpeed, cruiseSpeed, flightRange, fuelConsumption, lifeSpan, route);
		this.wingspan = wingspan;

	}

	public int getWingspan() {
		return this.wingspan;
	}

}
