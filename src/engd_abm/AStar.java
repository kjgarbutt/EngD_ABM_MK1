package engd_abm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Int2D;
import sim.util.geo.GeomPlanarGraphDirectedEdge;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import ec.util.MersenneTwisterFast;

/**
 * AStar.java
 *
 * Copyright 2011 by Sarah Wise, Mark Coletti, Andrew Crooks, and
 * George Mason University.
 *
 * Licensed under the Academic Free License version 3.0
 *
 * See the file "LICENSE" for more information
 *
 * $Id: AStar.java 842 2012-12-18 01:09:18Z mcoletti $
 */
public class AStar {
	
	public static Network roadNetwork = EngDModelBuilder.engdModelSim.roadNetwork;
	public static MersenneTwisterFast random = new MersenneTwisterFast();

	//public ArrayList<GeomPlanarGraphDirectedEdge> astarPath(Node start, Node goal, EngDNGOTeam refugeeFamily)	{
	static public <Route> Route astarPath(Node engDLSOA, Node goal, EngDNGOTeam refugee)	{
        // initial check
    	long startTime = System.currentTimeMillis();
        if (engDLSOA == null || goal == null)	{
            System.out.println("Error: invalid node provided to AStar");
        }

        // set up the containers for the result
        //ArrayList<GeomPlanarGraphDirectedEdge> result =
            //new ArrayList<GeomPlanarGraphDirectedEdge>();

        // containers for the metainformation about the Nodes relative to the
        // A* search
        HashMap<Node, AStarNodeWrapper> foundNodes =
            new HashMap<Node, AStarNodeWrapper>();

        AStarNodeWrapper startNode = new AStarNodeWrapper(engDLSOA);
        AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(engDLSOA, startNode);
        foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = heuristic(engDLSOA, goal);
        startNode.fx = heuristic(engDLSOA, goal);

        // A* containers: nodes to be investigated, nodes that have been investigated
        HashSet<AStarNodeWrapper> closedSet = new HashSet<>(10000), openSet = new HashSet<>(10000);
		PriorityQueue<AStarNodeWrapper> openSetQueue = new PriorityQueue<>(10000);
		openSet.add(startNode);
		openSetQueue.add(startNode);

        while (openSet.size() > 0)	{
        	// while there are reachable nodes to investigate
            AStarNodeWrapper x = findMin(openSet);
            // find the shortest path so far
            if (x.node == goal)	{
            	// we have found the shortest possible path to the goal!
                // Reconstruct the path and send it back.
                return reconstructRoute(goalNode, startNode, goalNode, refugee);
            }
            openSet.remove(x);
            // maintain the lists
            closedSet.add(x);

            // check all the edges out from this Node
            DirectedEdgeStar des = x.node.getOutEdges();
            for (Object o : des.getEdges().toArray())	{
                GeomPlanarGraphDirectedEdge l = (GeomPlanarGraphDirectedEdge) o;
                Node next = null;
                next = l.getToNode();

                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if (foundNodes.containsKey(next))	{
                    nextNode = foundNodes.get(next);
                } else	{
                    nextNode = new AStarNodeWrapper(next);
                    foundNodes.put(next, nextNode);
                }

                if (closedSet.contains(nextNode))	{
                	// it has already been considered	
                    continue;
                }

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + length(l);
                boolean better = false;

                if (!openSet.contains(nextNode))	{
                    openSet.add(nextNode);
                    nextNode.hx = heuristic(next, goal);
                    better = true;
                } else if (tentativeCost < nextNode.gx)	{
                    better = true;
                }

                // store A* information about this promising candidate node
                if (better)	{
                    nextNode.cameFrom = x;
                    nextNode.edgeFrom = l;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }
        }
        return null;
    }
    
    
    /**
	 * Takes the information about the given City n and returns the path that
	 * found it.
	 * @param n the end point of the path
	 * @return an Route from start to goal
	 */
	//In EngD:
	//ArrayList<GeomPlanarGraphDirectedEdge> reconstructPath(AStarNodeWrapper n)	
	//ArrayList<GeomPlanarGraphDirectedEdge> result =
    	//new ArrayList<GeomPlanarGraphDirectedEdge>();
	static Route reconstructRoute(AStarNodeWrapper n, AStarNodeWrapper start, AStarNodeWrapper end,
			EngDNGOTeam agent) {
		//In EngD: new ArrayList<GeomPlanarGraphDirectedEdge>();
		List<Int2D> locations = new ArrayList<Int2D>(100);
		List<Edge> edges = new ArrayList<Edge>(100);
	
		// double mod_speed = speed;
		double totalDistance = 0;
		AStarNodeWrapper x = n; //Same in EngD

		// start by adding the last one
		locations.add(0, x.node.location);
		Edge edge = null;

		if (x.cameFrom != null) {	//while loop in EngD // != 'Not equal to'
			edge = (Edge) roadNetwork.getEdge(x.cameFrom.node, x.node);
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
				double dist = x.node.location.distance(locations.get(0));
				edge =  roadNetwork.getEdge(x.node, to.node);
				 edgeInfo = (EngDRoadInfo) edge.getInfo();
				mod_speed = edgeInfo.getSpeed() * EngDParameters.TEMPORAL_RESOLUTION;// now km per step
				// convert speed to cell block per step
				mod_speed = EngDParameters.convertFromKilometers(mod_speed);

				while (dist > mod_speed) {
					locations.add(0, getPointAlongLine(locations.get(0), x.node.location, mod_speed / dist));
					//System.out.println(x.city.getName());
					edges.add(0, edge);
					dist = x.node.location.distance(locations.get(0));
				}
                locations.add(0, getPointAlongLine(locations.get(0), x.node.location, 1)); //**CRUCIAL***
                edges.add(0,  edge);
                
				/*if (x.cameFrom != null) {
					edge = roadNetwork.getEdge(x.cameFrom.city, x.city);
					 edgeInfo = (RoadInfo) edge.getInfo();
					mod_speed = edgeInfo.getSpeed() * Parameters.TEMPORAL_RESOLUTION;// now km per step
					// convert speed to cell block per step
					mod_speed = Parameters.convertFromKilometers(mod_speed);
				}

				if (x.cameFrom == null) {
					refugee.setCurrent(x.city);
				}*/
				to = x;
				x = x.cameFrom;
				if (x != null && x.cameFrom != null)								// != 'Not equal to'
					totalDistance += x.node.location.distance(x.cameFrom.node.location);
			}
		}
		else{
		edges.add(0, edge);	
		
		}
		//locations.add(0, start.city.location);
		edges.add(0, edge);
		//In EngD: return result;
		return new Route(locations, edges, totalDistance, start.node, end.node, EngDParameters.WALKING_SPEED);
		//return new Route(locations, totalDistance, start.city, end.city, Parameters.WALKING_SPEED);
	}

	/**
	 * Gets a point a certain percent a long the line
	 * 
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
     * /////////////////////////// Euclidean Distance ////////////////////////////
     * Measure of the estimated distance between two Nodes. Extremely basic, just
     * Euclidean distance as implemented here.
     * @param x
     * @param y
     * @return notional "distance" between the given nodes.
     */
    static double heuristic(Node x, Node y)	{
        Coordinate xnode = x.getCoordinate();
        Coordinate ynode = y.getCoordinate();
        return Math.sqrt(Math.pow(xnode.x - ynode.x, 2)
            + Math.pow(xnode.y - ynode.y, 2));
     //static double heuristic(City x, City y) {
    		//return x.location.distance(y.location) * Parameters.HEU_WEIGHT;
    }

    /**
     * //////////////////////////// Road Length //////////////////////////////////
     * @param e
     * @return The length of an edge
     */
    static double length(GeomPlanarGraphDirectedEdge e)	{
        Coordinate xnode = e.getFromNode().getCoordinate();
        Coordinate ynode = e.getToNode().getCoordinate();
        return Math.sqrt(Math.pow(xnode.x - ynode.x, 2)
            + Math.pow(xnode.y - ynode.y, 2));
    }
    

    /**
     *  //////////////////////// Nodes to Consider ///////////////////////////////
     *  Considers the list of Nodes open for consideration and returns the node
     *  with minimum fx value
     * @param openSet list of open Nodes
     * @return
     */
    static AStarNodeWrapper findMin(HashSet<AStarNodeWrapper> openSet)	{
        double min = 100000;
        AStarNodeWrapper minNode = null;
        for (AStarNodeWrapper n : openSet)	{
            if (n.fx < min)	{
                min = n.fx;
                minNode = n;
            }
        }
        return minNode;
    }

    /**
     * 
     * /////////////////////////// A* Node Meta Info /////////////////////////////
     * A wrapper to contain the A* meta information about the Nodes
     *
     */
    static class AStarNodeWrapper implements Comparable<AStarNodeWrapper> {
        // the underlying Node associated with the meta information
        Node node;
        // the Node from which this Node was most profitably linked
        AStarNodeWrapper cameFrom;
        // the edge by which this Node was discovered
        GeomPlanarGraphDirectedEdge edgeFrom;
        double gx, hx, fx;

        public AStarNodeWrapper(Node engDLSOA)	{
            node = engDLSOA;
            gx = 0;
            hx = 0;
            fx = 0;
            cameFrom = null;
            edgeFrom = null;
        }

		@Override
		public int compareTo(AStarNodeWrapper aStarNodeWrapper) {
			return Double.compare(this.fx, aStarNodeWrapper.fx);
		}
    }
}