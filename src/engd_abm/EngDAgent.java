package engd_abm;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;



import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Int2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;

import ec.util.MersenneTwisterFast;

public class EngDAgent implements Steppable {

	EngDModel world;
	protected MasonGeometry location;
	public boolean hasResources = false;
    public static boolean active = false;
    public boolean activated = false;String homeTract = "";
	String goalTract = "";
	Node headquartersNode = null;
	Node lsoaNode = null;

	// //////////////////////STATUS //////////////////////////////////
	public static EngDStatus status = null;
	public static int numActive = 0;

	// How is STATUS utilised. Must RETHINK
	public boolean getReachedGoal() {
		return hasResources;
	}

	public void setReachedGoal(boolean val) {
		hasResources = val;
	}

	public boolean homebound = false;
	// Definitely needs a RETHINK on how it is called
	public boolean reachedGoal = false;
	// Should be announced, then STATUS updated

	// ////////////////////// ATTRIBUTES //////////////////////////////
	String type = "";
	private final int range = 0;
	// Color NOT currently used.
	// Can't change color for individual Agents at the moment...
	private Color inboundColor = Color.black;
	private Color outboundColor = Color.red;

	// ////////////////////// TIME ///////////////////////////////////
	public int timeSinceDeparted = 0;
	// Taken from PACMAN!
	// Is this utilised?
	public static final int WAITING_PERIOD = 360;
	public static final int INITIAL_WAITING_PERIOD = WAITING_PERIOD / 4;
	public int waiting = INITIAL_WAITING_PERIOD;

	// ////////////////////// RESOURCES //////////////////////////////
	public int resources_Available = 0;
	public int resources_Distributed = 0;
	public static int resources_Capacity = 100;
	double resources = 100;

	private double moveRate = 10.0;
	private LengthIndexedLine segment = null;
	double startIndex = 0.0; // start position of current line
	double endIndex = 0.0; // end position of current line
	double currentIndex = 0.0; // current location along line
	GeomPlanarGraphEdge currentEdge = null;
	int linkDirection = 1;
	public double speed = 0; // useful for graph
	ArrayList<GeomPlanarGraphDirectedEdge> pathFromHQToLSOA = new ArrayList<GeomPlanarGraphDirectedEdge>();
	int indexOnPath = 0;
	int pathDirection = 1;
	PointMoveTo pointMoveTo = new PointMoveTo();

	static private GeometryFactory geometryFactory = new GeometryFactory();

	public EngDAgent(EngDModelBuilder engDModelBuilder, String homeTract,
			GeomPlanarGraphEdge startingEdge, GeomPlanarGraphEdge goalEdge) {
		EngDModel world;

		// set up information about where the node is and where it's going
		lsoaNode = startingEdge.getDirEdge(0).getFromNode();
		headquartersNode = goalEdge.getDirEdge(0).getToNode();
		this.homeTract = homeTract;
		this.goalTract = goalTract;

		// set the location to be displayed
		// GeometryFactory fact = new GeometryFactory();

		location = new MasonGeometry(
				geometryFactory.createPoint(new Coordinate(10, 10)));
		location.isMovable = true;

		// Now set up attributes for this agent
		if (engDModelBuilder.random.nextBoolean()) {
			location.addStringAttribute("TYPE", "4x4");
			int age = (int) (20.0 + 2.0 * engDModelBuilder.random.nextGaussian());
			location.addIntegerAttribute("AGE", age);
		} else {
			location.addStringAttribute("TYPE", "Car");
			int age = (int) (40.0 + 9.0 * engDModelBuilder.random.nextGaussian());
			location.addIntegerAttribute("AGE", age);
		}

		// Not everyone moves at the same speed
		// moveRate *= Math.abs(g.random.nextGaussian());
		// Assigns random speed between 0-70
		moveRate = (int) (Math.random() * 70) + 1;
		System.out.println("Agent's MoveRate = " + moveRate);
		location.addDoubleAttribute("MOVE RATE", moveRate);

		Coordinate startCoord = null;
		startCoord = lsoaNode.getCoordinate();
		updatePosition(startCoord);
	}

	public boolean start(EngDModel state) {
		findNewAStarPath(state);
		if (pathFromHQToLSOA.isEmpty()) {
			System.out
					.println("Initialization of a Agent ("
							+ homeTract
							+ ") failed: it is located in a part of the network that cannot "
							+ "access the given goal.");
			return false;
		} else {
			return true;
		}
	}

	private void findNewAStarPath(EngDModel geoTest) {
		// get the home and work Nodes with which this Agent is associated
		Node currentJunction = geoTest.network.findNode(location.geometry
				.getCoordinate());
		Node destinationJunction = lsoaNode;
		if (currentJunction == null) {
			return; // just a check
		}

		// find the appropriate A* path between them
		EngDAStar pathfinder = new EngDAStar();
		ArrayList<GeomPlanarGraphDirectedEdge> path = pathfinder.astarPath(
				currentJunction, destinationJunction);

		// if the path works, lay it in
		if (path != null && path.size() > 0) {

			// save it
			pathFromHQToLSOA = path;

			// set up how to traverse this first link
			GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) path.get(0)
					.getEdge();
			setupEdge(edge);

			// update the current position for this link
			updatePosition(segment.extractPoint(currentIndex));
		}
	}

	double progress(double val) {
		double edgeLength = currentEdge.getLine().getLength();
		double traffic = world.edgeTraffic.get(currentEdge).size();
		double factor = 1000 * edgeLength / (traffic * 5);
		factor = Math.min(1, factor);
		return val * linkDirection * factor;
	}

	public void step(SimState state) {
		EngDModel gstate = (EngDModel) state;

		// check that we've been placed on an Edge
		if (segment == null) {
			System.out.println(this + "'s 'segment'"
					+ "is NULL. For some reason!");
			return;
		} else if (reachedGoal) {
			status = EngDStatus.DISTRIBUTING;
			waiting = WAITING_PERIOD;
			setActive(gstate);
			resources--;
			System.out.println(this + " has " + resources + " resources.");
			System.out.println(this + " is " + status);
			// make sure that we're heading in the right direction
			boolean toWork = ((EngDModel) state).goToLSOA;
			if ((toWork && pathDirection < 0) || (!toWork && pathDirection > 0)) {
				flipPath();

				speed = progress(moveRate);
				currentIndex += speed;

				// check to see if the progress has taken the current index
				// beyond its goal
				// given the direction of movement. If so, proceed to the next
				// edge
				// IF 1 is SAME as 1 AND currentIndex GREATER THAN endIndex
				// THEN move to the nextEdge (currentIndex - endIndex)
				if (linkDirection == 1 && currentIndex > endIndex) {
					Coordinate currentPos = segment.extractPoint(endIndex);
					updatePosition(currentPos);
					transitionToNextEdge(currentIndex - endIndex);
					// ELSE IF 1 is SAME as -1 AND currentIndex LESS THAN
					// startIndex
					// THEN move to the nextEdge (startIndex - currentIndex)
				} else if (linkDirection == -1 && currentIndex < startIndex) {
					Coordinate currentPos = segment.extractPoint(startIndex);
					updatePosition(currentPos);
					transitionToNextEdge(startIndex - currentIndex);
				} else {
					// just update the position!
					Coordinate currentPos = segment.extractPoint(currentIndex);
					updatePosition(currentPos);
				}
			}
		}
	}
	private void setActive(EngDModel gState)	{
		boolean alreadyActive = false;
		
		if(EngDAgent.active)
			alreadyActive = true;
		
		int numActive = 0;
		
		//Loop through all the agents within vision and count the number of active civilians
		//MasonGeometry buffer = new MasonGeometry(this.location.buffer(osviState.personVision), this);	
		//Bag persons = osviState.persons.getCoveredObjects(buffer);
		//Bag cops = osviState.cops.getCoveredObjects(buffer);
		//for(Object person : MainAgent){
			//MainAgent p = (MainAgent)((MasonGeometry) person).getUserData();
			
			if(EngDAgent.active){
				numActive++;
			}
		//}
		
		/*Calculations for going active*/
		//arrestProbability = 1 - Math.exp(-2.3*((cops.size()/(numActive+1))));
		//grievance = perceivedHardship * (1 - govtLegitimacy);
		//this.active = (grievance - (riskAversion * arrestProbability)) > osviState.threshold;
		
		if(!alreadyActive && this.active)	{
			gState.activeCount++;	
			activated = true;
		}
		else if(alreadyActive && !this.active)	{
			gState.activeCount--;
		}
	}

	public void flipPath() {
		reachedGoal = false;
		homebound = false;
		status = EngDStatus.OUTBOUND;
		System.out.println(this + " is " + status);
		this.timeSinceDeparted = 0;
		pathDirection = -pathDirection;
		linkDirection = -linkDirection;
	}

	public static void agentGoals(String agentfilename) throws IOException {
		String csvGoal = null;
		BufferedReader agentGoalsBuffer = null;

		String agentFilePath = EngDModel.class.getResource(agentfilename).getPath();
		FileInputStream agentfstream = new FileInputStream(agentFilePath);
		System.out.println("Reading Agent's Goals CSV file: " + agentFilePath);

		try {
			agentGoalsBuffer = new BufferedReader(new InputStreamReader(
					agentfstream));
			agentGoalsBuffer.readLine();
			while ((csvGoal = agentGoalsBuffer.readLine()) != null) {
				String[] splitted = csvGoal.split(",");

				ArrayList<String> agentGoalsResult = new ArrayList<String>(
						splitted.length);
				for (String data : splitted)
					agentGoalsResult.add(data);
				EngDModel.getCsvData().addAll(agentGoalsResult);
			}
			System.out.println();
			System.out.println("Full CSV Array: " + EngDModel.getCsvData());
		} finally {
			if (agentGoalsBuffer != null)
				agentGoalsBuffer.close();
		}
		Random randomiser = new Random();
		String random = csvData.get(new Random().nextInt(EngDModel.getCsvData().size()));
		// String random1 = csvData.get(new Random().nextInt(csvData.size()));
		String goalTract = random;
		// String goalTract1 = random1;
		System.out.println();
		System.out.println("Agent goalTract: " + goalTract);

	}

	public ArrayList<String> getList() {
		return EngDModel.getCsvData();
	}

	void transitionToNextEdge(double residualMove) {

		indexOnPath += pathDirection;
		if ((pathDirection > 0 && indexOnPath >= pathFromHQToLSOA.size())
				|| (pathDirection < 0 && indexOnPath < 0)) {
			status = EngDStatus.INBOUND;
			reachedGoal = true;
			homebound = true;
			System.out.println(this + " is " + status
					+ " reachedGoal = true; homebound = true");

			indexOnPath -= pathDirection;
			return;
		}

		GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) pathFromHQToLSOA.get(
				indexOnPath).getEdge();
		setupEdge(edge);
		speed = progress(residualMove);
		currentIndex += speed;

		if (linkDirection == 1 && currentIndex > endIndex) {
			transitionToNextEdge(currentIndex - endIndex);
		} else if (linkDirection == -1 && currentIndex < startIndex) {
			transitionToNextEdge(startIndex - currentIndex);
		}
	}

	void setupEdge(GeomPlanarGraphEdge edge) {

		// clean up on old edge
		if (currentEdge != null) {
			ArrayList<EngDAgent> traffic = world.edgeTraffic.get(currentEdge);
			traffic.remove(this);
		}
		currentEdge = edge;

		// update new edge traffic
		if (world.edgeTraffic.get(currentEdge) == null) {
			world.edgeTraffic.put(currentEdge, new ArrayList<EngDAgent>());
		}
		world.edgeTraffic.get(currentEdge).add(this);

		// set up the new segment and index info
		LineString line = edge.getLine();
		segment = new LengthIndexedLine(line);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();
		linkDirection = 1;

		// check to ensure that Agent is moving in the right direction
		double distanceToStart = line.getStartPoint().distance(
				location.geometry), distanceToEnd = line.getEndPoint()
				.distance(location.geometry);
		if (distanceToStart <= distanceToEnd) { // closer to start
			currentIndex = startIndex;
			linkDirection = 1;
		} else if (distanceToEnd < distanceToStart) { // closer to end
			currentIndex = endIndex;
			linkDirection = -1;
		}
	}

	public void updatePosition(Coordinate c) {
		pointMoveTo.setCoordinate(c);
		// location.geometry.apply(pointMoveTo);

		world.EngDModel.setGeometryLocation(location, pointMoveTo);
	}

	public MasonGeometry getGeometry() {
		return location;
	}

	public EngDAgent(EngDStatus status) {
		EngDAgent.status = status;
	}

	public static EngDStatus getStatus() {
		return status;
	}

	public void setStatus(EngDStatus status) {
		EngDAgent.status = status;
	}

	public MasonGeometry getLocation() {
		return location;
	}

	public void setLocation(MasonGeometry location) {
		this.location = location;
	}
}
