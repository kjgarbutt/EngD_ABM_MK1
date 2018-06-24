package engd_abm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Int2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import ec.util.MersenneTwisterFast;

@SuppressWarnings("restriction")
public class EngDAStar {

	/**
	 * Assumes that both the start and end location are Cities as opposed to
	 * LOCATIONS
	 * 
	 * @param start
	 * @param goal
	 * @return
	 */
	public static Network roadNetwork = EngDModelBuilder.engdModelSim.roadNetwork;
	public static MersenneTwisterFast random = new MersenneTwisterFast();

	static public EngDRoute astarPath(Centroid start, Centroid goal, EngDNGOTeam team) {
		// initial check - same as Gridlock
		long startTime = System.currentTimeMillis();
		if (start == null || goal == null) {
			System.out.println("Error: invalid City provided to AStar");
		}
		// containers for the metainformation about the Cities relative to the A* search
		
		//Following in EngD:
		//ArrayList<GeomPlanarGraphDirectedEdge> result = new ArrayList<GeomPlanarGraphDirectedEdge>();
		HashMap<Centroid, AStarNodeWrapper> foundCentroids = new HashMap<Centroid, AStarNodeWrapper>();
		//HashMap<Node, AStarNodeWrapper> foundNodes = new HashMap<Node, AStarNodeWrapper>();
		
		AStarNodeWrapper startCentroid = new AStarNodeWrapper(start);
		//AStarNodeWrapper startNode = new AStarNodeWrapper(start);
		AStarNodeWrapper goalCentroid = new AStarNodeWrapper(goal);
		//AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
		foundCentroids.put(start, startCentroid);	//foundNodes.put(start, startNode);
		foundCentroids.put(goal, goalCentroid);	//foundNodes.put(goal, goalNode);

		startCentroid.gx = 0;	//startNode.gx = 0;
		startCentroid.hx = heuristic(start, goal); 
		startCentroid.fx = heuristic(start, goal);

		// A* containers: allRoadCities to be investigated, allRoadCities that have been investigated
		/* Following in EngD:
		 	ArrayList<AStarNodeWrapper> closedSet = new ArrayList<AStarNodeWrapper>(),
	            openSet = new ArrayList<AStarNodeWrapper>();
	        openSet.add(startNode);
	        */
		HashSet<AStarNodeWrapper> closedSet = new HashSet<>(10000), openSet = new HashSet<>(10000);
		PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);
		openSet.add(startCentroid);
		openSetQueue.add(startCentroid);
		while (openSet.size() > 0) {
			// while there are reachable allRoadCities to investigate
			//AStarCityWrapper x = findMin(openSet);
			AStarNodeWrapper x = openSetQueue.peek();
			System.out.println("Peek: " + x.city.getName());
			if (x == null) {
				AStarNodeWrapper n = findMin(openSet);
			}
			// find the shortest path so far
			if (x.city == goal) {
				//we have found the shortest possible path to the goal!
				//Reconstruct the path and send it back.
				if (x.cameFrom == null)
					System.out.println(x.city.getName());
				return reconstructRoute(goalCentroid, startCentroid, goalCentroid, team); //return reconstructPath(goalNode); 
			}
			openSet.remove(x);
			// maintain the lists
			openSetQueue.remove();	//new
			closedSet.add(x);
			Bag edges = roadNetwork.getEdgesOut(x.city);
			for (Object l : edges) {
				Edge e = (Edge) l;
				Centroid n = (Centroid) e.from();
 				if (n == x.city)
					n = (Centroid) e.to();

				// get the A* meta information about this City
				AStarNodeWrapper nextCentroid;	//AStarNodeWrapper nextNode;
				if (foundCentroids.containsKey(n))	//if (foundNodes.containsKey(next))	{
					nextCentroid = foundCentroids.get(n);	//nextNode = foundNodes.get(next);
				else {
					nextCentroid = new AStarNodeWrapper(n);
					foundCentroids.put(n, nextCentroid);
				}
				System.out.println(nextCentroid.city.getName());
				if (closedSet.contains(nextCentroid)) // it has already been considered
					continue;

				// otherwise evaluate the cost of this City/edge combo
				EngDRoadInfo edge = (EngDRoadInfo) e.getInfo();
				// System.out.println(edge.getWeightedDistance());
				double edgeweight = edge.getWeightedDistance() * EngDParameters.DISTANCE_WEIGHT
						+ edge.getSpeed() * EngDParameters.SPEED_WEIGHT - edge.getScaledPopulation() * EngDParameters.POP_WEIGHT
						+ edge.getScaledCost() * EngDParameters.COST_WEIGHT
						+ edge.getTransportLevel() * EngDParameters.TRANSPORT_LEVEL_WEIGHT
						+ edge.getDeaths() * EngDParameters.RISK_WEIGHT * team.dangerCare();
				// edgeweight = getWeightedDistance * 0.1
				//+ 1 * 0.1 -
				
				System.out.println(edge.getScaledPopulation());
				System.out.println("gx: " + x.gx + " edgeweight: " + edgeweight);
				double tentativeCost = x.gx + edgeweight; // changed from integer, still need to change the weighting of the edge weight
				boolean better = false;
				if (!openSet.contains(nextCentroid)) {
					openSet.add(nextCentroid);
					openSetQueue.add(nextCentroid);	//New
					nextCentroid.hx = heuristic(n, goal);
					better = true;
				} else if (tentativeCost < nextCentroid.gx) {
					better = true;
				}

				// store A* information about this promising candidate City
				if (better) {
					nextCentroid.cameFrom = x;
					System.out.println("Edge: " + tentativeCost);
					nextCentroid.gx = tentativeCost;
					System.out.println("hx: " + nextCentroid.hx);
					nextCentroid.fx = nextCentroid.gx + nextCentroid.hx;
					System.out.println("gx: " + nextCentroid.gx + "fx: " + nextCentroid.fx);
				}
			}

		}
		return null;
		//return result;
	}

	/**
	 * Takes the information about the given City n and returns the path that
	 * found it.
	 * @param n the end point of the path
	 * @return an Route from start to goal
	 */
	static EngDRoute reconstructRoute(AStarNodeWrapper n, AStarNodeWrapper start, AStarNodeWrapper end,
			EngDNGOTeam team) {
		//In EngD: new ArrayList<GeomPlanarGraphDirectedEdge>();
		List<Int2D> locations = new ArrayList<Int2D>(100);
		List<Edge> edges = new ArrayList<Edge>(100);
	
		// double mod_speed = speed;
		double totalDistance = 0;
		AStarNodeWrapper x = n; //Same in EngD

		// start by adding the last one
		locations.add(0, x.city.location);
		Edge edge = null;

		if (x.cameFrom != null) {	//while loop in EngD // != 'Not equal to'
			edge = (Edge) roadNetwork.getEdge(x.cameFrom.city, x.city);
			edges.add(0, edge);
			EngDRoadInfo edgeInfo = (EngDRoadInfo) edge.getInfo();
			//RoadInfo edge = (RoadInfo) roadNetwork.getEdge(x.cameFrom.city, x.city).getInfo();
			double mod_speed = edgeInfo.getSpeed() * EngDParameters.TEMPORAL_RESOLUTION;// now km per step
			// convert speed to cell block per step
			mod_speed = EngDParameters.convertFromKilometers(mod_speed);
			// System.out.println("" + mod_speed);
			AStarNodeWrapper to = x;
			x = x.cameFrom;	//Same in EngD

			while (x != null) {		
				double dist = x.city.location.distance(locations.get(0));
				edge =  roadNetwork.getEdge(x.city, to.city);
				 edgeInfo = (EngDRoadInfo) edge.getInfo();
				mod_speed = edgeInfo.getSpeed() * EngDParameters.TEMPORAL_RESOLUTION;// now km per step
				// convert speed to cell block per step
				mod_speed = EngDParameters.convertFromKilometers(mod_speed);

				while (dist > mod_speed) {
					locations.add(0, getPointAlongLine(locations.get(0), x.city.location, mod_speed / dist));
					//System.out.println(x.city.getName());
					edges.add(0, edge);
					dist = x.city.location.distance(locations.get(0));
				}
                locations.add(0, getPointAlongLine(locations.get(0), x.city.location, 1)); //**CRUCIAL***
                edges.add(0,  edge);
				to = x;
				x = x.cameFrom;
				if (x != null && x.cameFrom != null)								// != 'Not equal to'
					totalDistance += x.city.location.distance(x.cameFrom.city.location);
			}
		}
		else{
		edges.add(0, edge);	
		
		}
		//locations.add(0, start.city.location);
		edges.add(0, edge);
		//In EngD: return result;
		return new EngDRoute(locations, edges, totalDistance, start.city, end.city, EngDParameters.WALKING_SPEED);
		//return new Route(locations, totalDistance, start.city, end.city, Parameters.WALKING_SPEED);
	}
	
	/**
	 * NOT in EngD/Gridlock
	 * Gets a point a certain percent a long the line
	 * @param start
	 * @param end
	 * @param percent the percent along the line you want to get. Must be less than 1
	 * @return
	 */
	public static Int2D getPointAlongLine(Int2D start, Int2D end, double percent) {
		return new Int2D((int) Math.round((end.getX() - start.getX()) * percent + start.getX()),
				(int) Math.round((end.getY() - start.getY()) * percent + start.getY()));
	}

	/**
	 * Measure of the estimated distance between two Cities.
	 * Extremely basic, just Euclidean distance as implemented here.
	 * @param x
     * @param y
	 * @return notional "distance" between the given allRoadCities.
	 */
	static double heuristic(Centroid x, Centroid y) {
		return x.location.distance(y.location) * EngDParameters.HEU_WEIGHT;
	}
	
	/**
	 * Considers the list of Cities open for consideration and returns the City
	 * with minimum fx value
	 * 
	 * @param set
	 *            list of open Cities
	 * @return
	 */
	static AStarNodeWrapper findMin(Collection<AStarNodeWrapper> set) {
		double min = Double.MAX_VALUE;	//double min = 100000;
		AStarNodeWrapper minCentroid = null;	//minNode
		for (AStarNodeWrapper n : set) {
			if (n.fx < min) {
				min = n.fx;
				minCentroid = n;
			}
		}
		return minCentroid;
	}

	/**
	 * A wrapper to contain the A* meta information about the Cities
	 *
	 */
	static class AStarNodeWrapper implements Comparable<AStarNodeWrapper> {
		// the underlying City associated with the metainformation
		Centroid city;	//Node node;
		// the City from which this City was most profitably linked
		AStarNodeWrapper cameFrom;	//edgeFrom;
		double gx, hx, fx;

		public AStarNodeWrapper(Centroid n) { //public AStarNodeWrapper(Node n)	{
			city = n;	//node = n;
			gx = 0;
			hx = 0;
			fx = 0;
			cameFrom = null;
			//edgeFrom = null;
		}

		@Override
		public int compareTo(AStarNodeWrapper aStarNodeWrapper) {
			return Double.compare(this.fx, aStarNodeWrapper.fx);
		}
	}
}