import io.jbotsim.core.Topology;
import io.jbotsim.ui.JViewer;
import io.jbotsim.core.Node;

import io.jbotsim.core.Point;
import java.util.ArrayList;
import java.util.Random;


public class Robot extends Node{

	// ArrayList<Point> locations;
	int myMultiplicity;
	Point target;

	static int NB = 40; // Number of robots
	// To see a scattering that take the size of the robots into account, set EPS to 10
	static double EPS = 0.000001;

	@Override
	public void onPreClock() {
		// locations = new ArrayList<Point>();
		// for (Node node : getTopology().getNodes() )
		// {
		// 	locations.add(node.getLocation());
		// }

		for (Node node : getTopology().getNodes()) {
			if (distance(node) < 5) {
				myMultiplicity++;
			}
		}
	}

	@Override
	public void onClock() {
		// target = locations.get(0);
		// for(Point r : locations)
		// {
		// 	if(r.getX() > target.getX() || (r.getX() == target.getX() && r.getY() > target.getY()))
		// 	{
		// 		target = r;
		// 	}
		// }
	}

	@Override
	public void onPostClock() {
		setDirection(target);
		move(Math.min(10, distance(target)));
	}


	// Start the simulation
	public static void main(String[] args){
		int width = 800;
		int height = 400;

		// Create the Topology (a plane of size 800x400)
		Topology tp = new Topology(width, height);
		// Create the simulation window
		new JViewer(tp);

		// set the default node to be our Robot class
		// (When the user click in the simulation window,
		//  a default node is automatically added to the topology)
		tp.setDefaultNodeModel(Robot.class);

		// Robots cannot communicate
		tp.disableWireless();

		// Here we remove the sensing range since the robots have unlimited visibility
		tp.setSensingRange(0);

		double startX = Math.random() * width;
		double startY = Math.random() * height;

		// Add NB Robots to the topology (all with same position)
		for (int i = 0; i < NB; i++)
			tp.addNode(startX, startY);

		//The clock click every 0.5 sec (so that you can see the evolution slowly)
		tp.setTimeUnit(500);

		// We pause the simulation
		// (to start it, you'll have to right click on the window and resume it)
		tp.pause();
	}
}
