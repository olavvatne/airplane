package graphics;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import routes.Route;
import aircrafts.Aircraft;
import aircrafts.PassengerPlane;
import destinations.Destination;

import graphics.map.MapPanel.*;


public class MapPanelTester {
	public static void main(String[] args) {
		Aircraft fly = new PassengerPlane("Boieng", "B737", "900", 102034 , 10030304, 3030430, 1030430, 3020, 25, null, 200, 189);
		Destination oslo = new Destination("Oslo", 34, 55, 500000, 10.744629, 59.888937);
		Destination london = new Destination("London", 89, 90, 50078000, -0.109863, 51.495065);
		Destination stavanger = new Destination("Stavanger", 44, 80, 500000, 5.734863,58.972667);
		Route osloStavanger = new Route(oslo, stavanger, 400000, fly);
		Route londonStavanger = new Route(london, stavanger, 400000, fly);
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException
				| IllegalAccessException | UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final JFrame frame = new JFrame();
		frame.setTitle("tester");
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setSize(new Dimension((int)(sz.width*0.65), (int)(sz.height*0.75)));
		frame.setLocation((sz.width /10), sz.height/9);

		Gui gui = new Gui();
		frame.getContentPane().add(gui, BorderLayout.CENTER);

		JMenuBar menuBar = gui.createMenuBar();
		frame.setJMenuBar(menuBar);
		gui.getMapPanel().put(osloStavanger.toString(), osloStavanger);
		gui.getMapPanel().put(londonStavanger.toString(), londonStavanger);
		frame.setVisible(true); 
	}
}
