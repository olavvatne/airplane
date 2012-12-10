package aircrafts;

import routes.Route;

public class PassengerPlane extends AirPlane{
	
	private final int maxPassengers;
	
	public PassengerPlane(String manufacturer, String modelFamily, String model, int cost, int maxSpeed, int cruiseSpeed, int flightRange, int fuelConsumption, int lifeSpan, Route route, int wingspan, int maxPassengers) {
		super(manufacturer, modelFamily, model, cost,maxSpeed, cruiseSpeed, flightRange, fuelConsumption, lifeSpan, route, wingspan);
		this.maxPassengers = maxPassengers;
		
	}
	
	public int getMaxPassengers() {
		return maxPassengers;
	}

}
