package graphics.map;
/**
 * A class used to pack longitude latitude and id of a sheep. Used by MapPanel to draw markers, and paths.
 * @author olav
 *
 */
public class Marker {
	private double longitude;
	private double latitude;
	private int		id = 0;
	
	public Marker(double longitude, double latitude) {
		this.longitude = longitude;
		this.latitude = latitude;
	}
	
	public Marker(double longitude, double latitude, int id) {
		this.longitude = longitude;
		this.latitude = latitude;
		this.id = id;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public int getId() {
		return id;
	}
	public String toString() {
		return latitude+ " " + longitude;
	}
}
