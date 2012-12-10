package player;

import java.util.ArrayList;

import routes.Route;

import aircrafts.Aircraft;

public class Player {
	
	private String airlineName;
	private int value;
	private int cash;
	private ArrayList<Aircraft> aircrafts;
	private ArrayList<Route> routes;
	
	public Player(String airlineName, int value, int cash, ArrayList<Aircraft> aircrafts, ArrayList<Route> routes) {
		this.airlineName = airlineName;
		this.value = value;
		this.cash = cash;
		initAircraftsAndRoutes(aircrafts, routes);
	}
	
	private void initAircraftsAndRoutes(ArrayList<Aircraft> aircrafts, ArrayList<Route> routes) {
		if (aircrafts == null) {
			this.aircrafts = new ArrayList<Aircraft>();
		}
		else {
			this.aircrafts = aircrafts;
		}
		
		if (routes == null) {
			this.routes = new ArrayList<Route>();
		}
		else {
			this.routes = routes;
		}
	}
	
	public void sellAircraft(Aircraft aircraft) {
		if (aircrafts.contains(aircraft)) {
			removeAircraft(aircraft);
			changeCash(aircraft.getCost());
		}
	}
	
	public void buyAircraft(Aircraft aircraft) {
		int price = aircraft.getCost();
		price *= -1;
		changeCash(price);
		aircrafts.add(aircraft);
	}
	
	public void addAircraft(Aircraft aircraft) {
		aircrafts.add(aircraft);
	}
	
	public void removeAircraft(Aircraft aircraft) {
		aircrafts.remove(aircraft);
	}
	
	public ArrayList<Aircraft> getAircrafts() {
		return this.aircrafts;
	}
	
	public void addRoute(Route route) {
		routes.add(route);
	}
	
	public void removeRoute(Route route) {
		routes.remove(route);
	}
	
	public ArrayList<Route> getRoutes() {
		return this.routes;
	}
	
	public void changeValue(int value) {
		this.value += value;
	}
	
	public void changeCash(int cash) {
		this.cash += cash;
	}
	
	public int getValue() {
		return this.value;
	}
	
	public int getCash() {
		return this.cash;
	}
	
	public String toString() {
		String temp = "##########################################" + "\n" + "Airline name: " + this.airlineName + "\n" + "Value: " + this.value + "\n" + "Cash: " + this.cash + "\n" + "\n";
		
		if (aircrafts != null) {	
			for (Aircraft craft : aircrafts) {
				temp += craft + "\n";
			}
		}
		
		else {
			temp += "No aircraft";
		}
		
		temp += "\n";
		
		if (routes != null) {	
			for (Route r : routes) {
				temp += r + "\n";
			}
		}
		
		else {
			temp += "no routes";
		}
		
		return temp;
	}
}
