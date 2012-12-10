package aircrafts;

import routes.Route;

/* Author: torgeha
 * 
 * */

public class CargoPlane extends AirPlane {

	private final int maxCargoLoad;

	public CargoPlane(String manufacturer, String modelFamily, String model, int cost, int maxSpeed, int cruiseSpeed, int flightRange, int fuelConsumption, int lifeSpan, Route route, int wingspan, int maxCargoLoad) {
		super(manufacturer, modelFamily, model, cost, maxSpeed, cruiseSpeed, flightRange, fuelConsumption, lifeSpan, route, wingspan);
		this.maxCargoLoad = maxCargoLoad;

	}

}
