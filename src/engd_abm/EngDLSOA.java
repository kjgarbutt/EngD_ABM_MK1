package engd_abm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import sim.util.Bag;
import sim.util.Int2D;

class EngDLSOA {
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
	private HashSet<EngDAgent> agents;
	private int departures;
	private int arrivals;

	// need name, get name, set name
	//private MigrationBuilder.Node nearestNode;
	protected HashMap<EngDLSOA, EngDRoute> cachedPaths;

	public EngDLSOA(Int2D location, int ID, String name, int origin, double scaledPop, int pop, int quota, double violence,
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
		this.agents = new HashSet<EngDAgent>();
		this.departures = 0;
	}

	public EngDLSOA(Int2D location2, int iD2, String name2, String code,
			String laName, int cFSL, int cFSN) {
		// TODO Auto-generated constructor stub
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
		return agents.size();
	}

	public HashSet<EngDAgent> getRefugees() {
		return agents;
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

	public void addMember(EngDAgent a) {
		agents.add(a);
		arrivals++;
	}
	
	/*public void removeMembers(Bag people){
		refugees.remove(people);
		passerbyCount += people.size();
	}*/
	
	public void removeMember(EngDAgent a){
		if (agents.remove(a))				
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

	public Map<EngDLSOA, EngDRoute> getCachedRoutes() {
		return cachedPaths;
	}

	public EngDRoute getRoute(EngDLSOA destination, EngDNGOAgent refugeeFamily) {
		EngDRoute route;

		route = EngDAStar.astarPath(this, destination, refugeeFamily);
		//System.out.println(route.getNumSteps());

		return route;
	}
	
	public double getScale(){
		return agents.size() * 1.0 / (EngDParameters.TOTAL_POP);
	}
	

}
