package engd_abm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;

import sim.util.Bag;
import sim.util.Int2D;

class Node {
	Int2D location;
	String name;
	private int quota; // 1
	private int ID;
	private int origin;
	private double scaledPop;
	private int pop;
	private double violence; // 2
	private double economy; // 3
	private double familyPresence; // 2
	private HashSet<EngDAgent> ngoagents;
	private int departures;
	private int arrivals;

	// need name, get name, set name
	// private MigrationBuilder.Node nearestNode;
	protected HashMap<Node, EngDRoute> cachedPaths;

	// links to cityAttributes read in in MigrationBuilder.java
	public Node(Int2D location, int ID, String name, int origin, double scaledPop, int pop, int quota, double violence,
			double economy, double familyPresence) {
		this.name = name;
		this.location = location;
		this.ID = ID;
		this.scaledPop = scaledPop;
		this.pop = pop;
		this.quota = quota;
		this.violence = violence;
		this.economy = economy;
		this.familyPresence = familyPresence;
		this.origin = origin;
		this.ngoagents = new HashSet<EngDAgent>();
		this.departures = 0;
	}

	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getScaledPopulation() {
		return scaledPop;
	}

	public double getPopulation() {
		return pop;
	}

	public int getRefugeePopulation() {
		return ngoagents.size();
	}

	public HashSet<EngDAgent> getNGOAgents() {
		return ngoagents;
	}

	public int getQuota() {
		return quota;
	}

	public void setQuota(int quota) {
		this.quota = quota;
	}

	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public double getViolence() {
		return violence;
	}

	public void setViolence(double violence) {
		this.violence = violence;
	}

	public double getEconomy() {
		return economy;
	}

	public void setEconomy(double economy) {
		this.economy = economy;
	}

	public int getDepartures(){
		return departures;
	}

	public int getArrivals(){
		return arrivals;
	}

	public double getFamilyPresence() {
		return familyPresence;
	}

	public void setFamilyPresence(double familyPresence) {
		this.familyPresence = familyPresence;
	}

	/*public void addMembers(Bag people) {
		refugees.addAll(people);
	}*/

	public void addAgent(EngDAgent r) {
		ngoagents.add(r);
		arrivals++;
	}

	/*public void removeMembers(Bag people){
		refugees.remove(people);
		passerbyCount += people.size();
	}*/

	public void removeAgent(EngDAgent r){
		if (ngoagents.remove(r))
			departures ++;
	}

	/*public void setNearestNode(MigrationBuilder.Node node) {
		nearestNode = node;
	}

	public MigrationBuilder.Node getNearestNode() {
		return nearestNode;
	}

	public void cacheRoute(Route route, City destination) {
		cachedPaths.put(destination, route);
	}*/

	public Map<Node, EngDRoute> getCachedRoutes() {
		return cachedPaths;
	}

	public EngDRoute getRoute(Node destination, EngDNGOTeam ngoAgentTeam) {
		EngDRoute engdroute;

		engdroute = EngDAStar.engdAstarPath(this, destination, ngoAgentTeam);
		//route = EngDAStar.astarPath(Node start, Node goal, EngDNGOTeam ngoagent)
		//System.out.println(route.getNumSteps());

		return engdroute;
	}

	public double getScale(){
		return ngoagents.size() * 1.0 / (EngDParameters.TOTAL_POP);
	}

	

}