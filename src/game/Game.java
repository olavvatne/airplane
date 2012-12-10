package game;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import destinations.Destination;

import player.Player;
import routes.Route;

import aircrafts.Aircraft;
import aircrafts.PassengerPlane;

public class Game {
	
	public static void main(String[] args) {
		new Game();
	}
	
	public Game() {
		initGame();
	}
	
	public void initGame() {
		testInit();
	}
	
	public void testInit() {
		String airlineName = JOptionPane.showInputDialog("Airline name: ");
		int startValue = 300000;
		int startCash = 300000;
		
		PassengerPlane planeLong = new PassengerPlane("Boeing", "B737", "900", 87000, 990, 900, 3700, 10, 25, null, 30, 330);
		PassengerPlane planeShort = new PassengerPlane("Boeing", "B737", "600", 58000, 800, 900, 2500, 20, 25, null, 23, 237);
		
		ArrayList<Aircraft> aircrafts = new ArrayList<Aircraft>();
		aircrafts.add(planeLong);
		aircrafts.add(planeShort);
		
		Player p1 = new Player(airlineName, startValue, startCash, aircrafts, null);
		System.out.println(p1);
		
		//add route
		Destination d1 = new Destination("Stavanger", 100, 100, 110000);
		Destination d2 = new Destination("Trondheim", 10, 10, 130000);
		Route r1 = new Route(d1, d2, 1000, planeLong);
		p1.addRoute(r1);
		
		System.out.println(p1);
		
		p1.sellAircraft(planeShort);
		System.out.println(p1);
		
		p1.buyAircraft(new PassengerPlane("Boeing", "B737", "600", 58000, 800, 900, 2500, 20, 25, null, 23, 237));
		System.out.println(p1);
		
		
		
	}

}
