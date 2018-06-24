package engd_abm;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import sim.util.Int2D;

class Centroid {
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
	private HashSet<EngDAgent> engdagents;
	private int departures;
	private int arrivals;

	// need name, get name, set name
	// private MigrationBuilder.Node nearestNode;
	protected HashMap<Centroid, EngDRoute> cachedPaths;

	// links to cityAttributes read in in MigrationBuilder.java
	public Centroid(Int2D location, int ID, String name, int origin, double scaledPop, int pop, int quota, double violence,
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
		this.engdagents = new HashSet<EngDAgent>();
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

	public int getAgentPopulation() {
		return engdagents.size();
	}

	public HashSet<EngDAgent> getAgents() {
		return engdagents;
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

	public void addMember(EngDAgent r) {
		engdagents.add(r);
		arrivals++;
	}

	public void removeMember(EngDAgent r){
		if (engdagents.remove(r))
			departures ++;
	}

	public Map<Centroid, EngDRoute> getCachedRoutes() {
		return cachedPaths;
	}

	public EngDRoute getRoute(Centroid destination, EngDNGOTeam team) {
		EngDRoute route;

		route = EngDAStar.astarPath(this, destination, team);
		//System.out.println(route.getNumSteps());

		return route;
	}

	public double getScale(){
		return engdagents.size() * 1.0 / (EngDParameters.TOTAL_POP);
	}

}