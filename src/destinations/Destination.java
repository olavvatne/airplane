package destinations;

/*
 * Author: torgeha
 * 
 * Destination class (city), needed to initialize a Route
 * 
 * */
public class Destination {

	private final String name;
	private int inhabitants;
	private int tourismRating;
	private int businessRating;

	public Destination(String name, int tourismRating, int businessRating, int inhabitants) {
		this.name = name;
		this.tourismRating = tourismRating;
		this.businessRating = businessRating;
		this.inhabitants = inhabitants;
	}

	public String getName() {
		return name;
	}

	public int getTourismRating() {
		return tourismRating;
	}

	public int getBusinessRating() {
		return businessRating;
	}

	public int getInhabitants() {
		return inhabitants;
	}

	public void setInhabitants(int inhabitants) {
		this.inhabitants = inhabitants;
	}

	public String toString() {
		return name;
	}

}
