package engd_abm;

import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Int2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;

class EngDAgent implements Steppable {

	private int age;
	private int sex; // 0 male, 1 female
	private EngDNGOTeam family;
	private Int2D location;
	private int healthStatus = 1; // default 1 (alive), dead 0

	EngDModel world;
	String homeTract = "";
	String workTract = "";
	Node headquartersNode = null;
	Node lsoaNode = null;
	private MasonGeometry locationGeom;
	static int agentSpeed = 10;
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
	boolean reachedGoal = false;
	PointMoveTo pointMoveTo = new PointMoveTo();

	public EngDAgent(EngDModel g, String home, String work,
			GeomPlanarGraphEdge startingEdge, GeomPlanarGraphEdge goalEdge) {
		world = g;

		headquartersNode = startingEdge.getDirEdge(0).getFromNode();
		lsoaNode = goalEdge.getDirEdge(0).getToNode();
		homeTract = home;
		workTract = work;

		GeometryFactory geometryFactory = new GeometryFactory();
		locationGeom = new MasonGeometry(
				geometryFactory.createPoint(new Coordinate(10, 10)));
		locationGeom.isMovable = true;

		//setAgentSpeed((int) (Math.random() * 70) + 1);
		//System.out.println("Agent's MoveRate = " + getAgentSpeed());
		//location.addDoubleAttribute("MOVE RATE", getAgentSpeed());

		Coordinate startCoord = null;
		startCoord = headquartersNode.getCoordinate();
		//updatePosition(startCoord);
	}
	
	public int getHealthStatus() {
		return healthStatus;
	}

	public void setHealthStatus(int status) {
		this.healthStatus = status;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
	
	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}

	public int getSex() {
		return sex;
	}

	public void setSex(int sex) {
		this.sex = sex;
	}

	public boolean start(EngDModelBuilder engdModelSim) {
		findNewAStarPath(engdModelSim);
		if (pathFromHQToLSOA.isEmpty()) {
			System.out
					.println("Initialization of a Agent failed: it is located "
							+ "in a part of the network that cannot "
							+ "access the given goal.");
			return false;
		} else {
			return true;
		}
	}

	private void findNewAStarPath(EngDModelBuilder geoTest) {
		Node currentJunction = geoTest.network.findNode(locationGeom.geometry
				.getCoordinate());
		Node destinationJunction = lsoaNode;
		if (currentJunction == null) {
			return;
		}

		EngDAStar pathfinder = new EngDAStar();
		ArrayList<GeomPlanarGraphDirectedEdge> path = pathfinder.engdAstarPath(
				currentJunction, destinationJunction);

		if (path != null && path.size() > 0) {
			pathFromHQToLSOA = path;
			GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) path.get(0)
					.getEdge();
			setupEdge(edge);
			updatePosition(segment.extractPoint(currentIndex));
		}
	}

	double progress(double val) {
		double edgeLength = currentEdge.getLine().getLength();
		double traffic = EngDModelBuilder.getEdgeTraffic().get(currentEdge)
				.size();
		double factor = 1000 * edgeLength / (traffic * 5);
		factor = Math.min(1, factor);
		return val * linkDirection * factor;
	}

	public void step(SimState state) {
		if (segment == null) {
			System.out.println(this + "'s 'segment'"
					+ "is NULL. For some reason!");
			return;
		} else if (reachedGoal)	{
	    	   boolean toWork = ((EngDModel) state).goToLSOA;
	    	   if ((toWork && pathDirection < 0) || (!toWork && pathDirection > 0))	{
	           flipPath();
	           speed = progress(agentSpeed);
	           currentIndex += speed;
		       if (linkDirection == 1 && currentIndex > endIndex)	{
		           Coordinate currentPos = segment.extractPoint(endIndex);
		           updatePosition(currentPos);
		           transitionToNextEdge(currentIndex - endIndex);
		       // ELSE IF 1 is SAME as -1 AND currentIndex LESS THAN startIndex
		      		// THEN move to the nextEdge (startIndex - currentIndex)   
		       } else if (linkDirection == -1 && currentIndex < startIndex)	{
		           Coordinate currentPos = segment.extractPoint(startIndex);
		           updatePosition(currentPos);
		           transitionToNextEdge(startIndex - currentIndex);
		       } else
		       {
		    	   // just update the position!
		           Coordinate currentPos = segment.extractPoint(currentIndex);
		           updatePosition(currentPos);
		           }
	    	   }
		}
	}

	public void flipPath() {
		reachedGoal = false;
		pathDirection = -pathDirection;
		linkDirection = -linkDirection;
	}

	void transitionToNextEdge(double residualMove) {

		indexOnPath += pathDirection;
		if ((pathDirection > 0 && indexOnPath >= pathFromHQToLSOA.size())
				|| (pathDirection < 0 && indexOnPath < 0)) {
			System.out.println(this + " has reached its destination");
			reachedGoal = true;
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
		if (currentEdge != null) {
			ArrayList<EngDAgent> traffic = EngDModelBuilder.getEdgeTraffic()
					.get(currentEdge);
			traffic.remove(this);
		}
		currentEdge = edge;
		if (EngDModelBuilder.getEdgeTraffic().get(currentEdge) == null) {
			EngDModelBuilder.getEdgeTraffic().put(currentEdge,
					new ArrayList<EngDAgent>());
		}
		EngDModelBuilder.getEdgeTraffic().get(currentEdge).add(this);

		LineString line = edge.getLine();
		segment = new LengthIndexedLine(line);
		startIndex = segment.getStartIndex();
		endIndex = segment.getEndIndex();
		linkDirection = 1;

		double distanceToStart = line.getStartPoint().distance(
				locationGeom.geometry), distanceToEnd = line.getEndPoint()
				.distance(locationGeom.geometry);
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
		EngDModelBuilder.agents.setGeometryLocation(location, pointMoveTo);
	}

	public MasonGeometry getGeometry() {
		return locationGeom;
	}

	public static int getAgentSpeed() {
		return agentSpeed;
	}

	public void setAgentSpeed(int agentSpeed) {
		this.agentSpeed = agentSpeed;
	}

}
