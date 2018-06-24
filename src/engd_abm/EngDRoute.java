package engd_abm;

import java.util.ArrayList;
import java.util.List;

import sim.field.network.Edge;
import sim.util.Int2D;

/**
 * This class is a wrapper class for an ArrayList that manages a locations and
 * other information
 */
public class EngDRoute {
	private List<Int2D> locations;// list of places this person needs to go
	private List<Edge> edges;
	private double distance;
	private Centroid start;
	private Centroid end;
	private double speed;

	public EngDRoute(List<Int2D> locations, List<Edge> edges, double distance,
			Centroid start, Centroid end, double speed) {
		this.locations = locations;
		this.edges = edges;
		this.distance = distance;
		this.start = start;
		this.end = end;
		this.speed = speed;
	}

	public EngDRoute(List<Int2D> locations, double distance, Centroid start,
			Centroid end, double speed) {
		this.locations = locations;
		// this.edges = edges;
		this.distance = distance;
		this.start = start;
		this.end = end;
		this.speed = speed;
	}

	/**
	 * @return next location to move, null if no more moves
	 */

	public List<Int2D> getLocations() {
		return locations;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public Int2D getLocation(int index) {
		Int2D location = locations.get(index);
		return location;
	}

	public Edge getEdge(int index) {
		Edge edge = edges.get(index);
		return edge;
	}

	public int getLocIndex(Int2D loc) {
		return locations.lastIndexOf(loc);
	}

	public int getEdgeIndex(EngDRoadInfo edge) {
		return edges.lastIndexOf(edge);
	}

	public double getTotalDistance() {
		return distance;
	}

	public int getNumSteps() {
		return locations.size();
	}

	public int getNumEdges() {
		return edges.size();
	}

	public Centroid getStart() {
		return start;
	}

	public Centroid getEnd() {
		return end;
	}

	public boolean equals(EngDRoute r) {
		if (locations.containsAll(r.getLocations())
				&& edges.containsAll(r.getEdges())) {
			return true;
		} else
			return false;
	}

	public void printRoute() {
		for (Edge e : edges) {
			Centroid c = (Centroid) e.getTo();
			System.out.print(c.getName() + " ");
		}
	}

	public EngDRoute reverse() {
		List<Int2D> reversedlocations = new ArrayList<Int2D>(locations.size());
		List<Edge> reversedEdges = new ArrayList<Edge>(edges.size());
		for (int i = locations.size() - 1; i >= 0; i--) {
			reversedlocations.add(locations.get(i));
			reversedEdges.add(edges.get(i));
		}
		return new EngDRoute(reversedlocations, reversedEdges, this.distance,
				this.end, this.start, speed);
		// return new Route(reversedlocations, this.distance, this.end,
		// this.start, speed);
	}

	public double getSpeed() {
		return speed;
	}
}