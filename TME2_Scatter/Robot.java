import io.jbotsim.core.Topology;
import io.jbotsim.ui.JViewer;
import io.jbotsim.core.Node;

import io.jbotsim.core.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Robot extends Node {

	static final int mapWidth = 800;
	static final int mapHeight = 400;
	static final int multiplicityThreshold = 10;

	ArrayList<Point> locations = new ArrayList<>();
	int myMultiplicity;
	Point target;

	static int NB = 40; // Number of robots
	// To see a scattering that take the size of the robots into account, set EPS to
	// 10
	static double EPS = 0.000001;

	@Override
	public void onPreClock() {
		for (Node node : getTopology().getNodes()) {
			if (distance(node) < multiplicityThreshold) {
				myMultiplicity++;
			} else {
				locations.add(node.getLocation());
			}
		}
	}

	@Override
	public void onClock() {
		if (target == null || (distance(target) < multiplicityThreshold && myMultiplicity > 1)) {
			target = generateSafePoint();
		}
	}

	private boolean isSafeSpace(Point p) {
		for (Point q : locations) {
			if (p.distance(q) < multiplicityThreshold || distance(q) < multiplicityThreshold) {
				return false;
			}
		}
		return true;
	}

	private Point generateSafePoint() {
		int cols = mapWidth / multiplicityThreshold;
		int rows = mapHeight / multiplicityThreshold;

		// Deterministic grid based on robot's identity
		int robotIndex = System.identityHashCode(this) % (rows * cols);

		int gridX = robotIndex % cols;
		int gridY = (robotIndex / cols) % (mapHeight / multiplicityThreshold);
		Point p = new Point(gridX * multiplicityThreshold, gridY * multiplicityThreshold);

		while (!isSafeSpace(p)) {
			robotIndex = (robotIndex + 1) % (rows * cols);
			p = new Point(gridX * multiplicityThreshold, gridY * multiplicityThreshold);
		}

		return p;
	}

	/**
	 * Generate and return `n` safe destination points.
	 */
	private List<Point> generateDestinations(int n) {
		List<Point> safeSpaces = new ArrayList<>();

		for (int i = 0; i < n; i++) {
			safeSpaces.add(generateSafePoint());
		}

		return safeSpaces;
	}

	@Override
	public void onPostClock() {
		setDirection(target);
		move(Math.min(10, distance(target)));
	}

	// Start the simulation
	public static void main(String[] args) {

		// Create the Topology (a plane of size 800x400)
		Topology tp = new Topology(mapWidth, mapHeight);
		// Create the simulation window
		new JViewer(tp);

		// set the default node to be our Robot class
		// (When the user click in the simulation window,
		// a default node is automatically added to the topology)
		tp.setDefaultNodeModel(Robot.class);

		// Robots cannot communicate
		tp.disableWireless();

		// Here we remove the sensing range since the robots have unlimited visibility
		tp.setSensingRange(0);

		double startX = Math.random() * mapWidth;
		double startY = Math.random() * mapHeight;

		// Add NB Robots to the topology (all with same position)
		for (int i = 0; i < NB; i++)
			tp.addNode(startX, startY);

		// The clock click every 0.5 sec (so that you can see the evolution slowly)
		tp.setTimeUnit(500);

		// We pause the simulation
		// (to start it, you'll have to right click on the window and resume it)
		tp.pause();
	}
}
