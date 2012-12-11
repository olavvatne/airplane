package graphics;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;

import aircrafts.Aircraft;
import aircrafts.PassengerPlane;

import destinations.Destination;

import routes.Route;



public class ShowData extends JFrame {
	JButton buy;
	private JButton sell;
	private JTextArea description;
	private JComboBox routesBox;
	private HashMap<String, Route> routes;
	
	
	public ShowData() {
		this.setSize(new Dimension(700, 400));
		this.setTitle("Tester");
		this.setVisible(true);
		
		Aircraft fly = new PassengerPlane("Boieng", "B737", "900", 102034 , 10030304, 3030430, 1030430, 3020, 25, null, 200, 189);
		Destination oslo = new Destination("Oslo", 34, 55, 500000, 10.345, 56.4504);
		Destination stavanger = new Destination("Stavanger", 44, 80, 500000, 5.45434, 56.545);
		Route osloStavanger = new Route(oslo, stavanger, 400000, fly);
		routes = new HashMap<String, Route>();
		routes.put("oslo-stavanger", osloStavanger);
		init();
		setLayout();
	}
	
	
	public void routeActionPerformed(ActionEvent e) {
		Route route = routes.get((String)routesBox.getSelectedItem());
		if(route !=null){
			description.setText(route.getAircraft().toString()
					+route.getEndDestination().toString()
					+route.getStartDestination().toString()
					);
		}
	}
	public void init() {
		description = new JTextArea();
		routesBox = new JComboBox();
		buy = new JButton();
		sell = new JButton();
		
		routesBox.setMaximumSize(new Dimension(200, 30));
		routesBox.setModel(new DefaultComboBoxModel(new String[] {"-----","oslo-stavanger","lol",
				"lol2", "lol3"}));
		routesBox.addActionListener(new ActionListener() {
		
			@Override
			public void actionPerformed(ActionEvent e) {
				routeActionPerformed(e);
				
			}
		});
		
		buy.setText("buy");
		buy.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		sell.setText("sell");
		sell.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	private void setLayout() {
		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		layout.setAutoCreateGaps(true);
		
		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(routesBox)
						.addGap(40,100,200))
				.addGroup(layout.createParallelGroup()
						.addComponent(description)
						.addGroup(layout.createSequentialGroup()
								.addComponent(buy)
								.addComponent(sell)))
				
		);
		
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup()
						.addComponent(routesBox)
						.addComponent(description))
				.addGroup(layout.createParallelGroup()
						.addComponent(buy)
						.addComponent(sell))
		);
	}
	public static void main(String[] args) {
		ShowData data = new ShowData();
	}
}
