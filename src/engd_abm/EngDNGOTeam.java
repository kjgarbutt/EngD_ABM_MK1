package engd_abm;

import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import ec.util.MersenneTwisterFast;

class EngDNGOTeam implements Steppable {
	private Int2D location;
	private Bag teamMembers;
	private EngDRoute engdroute;
	//private int routePosition;
	private double finStatus;
	private Centroid home;
	private Edge currentEdge;
	private Centroid currentCentroid;
	private Centroid goal;
	static MersenneTwisterFast random = new MersenneTwisterFast();
	private boolean isMoving;
	private HashMap<EngDRoute, Integer> cachedRoutes;
	private HashMap<Centroid, Integer> cachedGoals;
	private boolean goalChanged;

	public EngDNGOTeam(Int2D location, int size, Centroid home, double finStatus) {
		this.location = location;
		this.home = home;
		this.goal = home;
		this.finStatus = finStatus;
		teamMembers = new Bag();
		currentCentroid = home;
		isMoving = true;
		// routePosition = 0;
		cachedRoutes = new HashMap<EngDRoute, Integer>();
		goalChanged = false;
	}

	@Override
	public void step(SimState state) {
		// random = new MersenneTwisterFast();
		// System.out.println("here");
		System.out.println();
		EngDModel engdModelSim = (EngDModel) state;
		Bag centroids = engdModelSim.lsoacentroids;
		Centroid goalCentroid = calcGoalLSOA(centroids);

		if (this.location == goalCentroid.location) {									// == 'Equal to'
			goal = goalCentroid;
			isMoving = false;
			// if the current location is the goalCity location, then agent has arrived so isn't moving
		} else if (finStatus <= 0.0) {												// less than or equal to 0
			System.out.println("----NO MONEY LEFT-----");
			return;
		} else if (isMoving == false)												// == 'Equal to'
			return;
		else {
			System.out.println(finStatus);
			if (goalCentroid.getName().compareTo(goal.getName()) != 0) {				// != 'Not equal to'
				double r = random.nextDouble();
				if (r < EngDParameters.GOAL_CHANGE_PROB) {
					this.goal = goalCentroid;
					System.out.println("-----GOAL CHANGE------");
					goalChanged = true;
				}
				if (goal == home) {													// == 'Equal to'
					this.goal = goalCentroid;
					goalChanged = true;
				}
			} else
				goalChanged = false;

			if (this.getLocation().getX() != goal.getLocation().getX() 				// != 'Not equal to'
					|| this.getLocation().getY() != goal.getLocation().getY()) { 	// ||'Conditional-OR'
				System.out.println("Home: " + this.getHome().getName()
						+ " | Goal " + goal.getName());
				System.out.println(this + " Current: " + currentCentroid.getName());
				if (currentCentroid.getName() == goal.getName()							// == 'Equal to'
						&& this.getLocation() != goal.getLocation()) { 				// && 'Conditional-AND'
					System.out.println("-----HERE------");
					currentCentroid = (Centroid) currentEdge.to();
				}
				// setGoal(currentCity, goal);
				engdroute = calcRoute(currentCentroid, goal);// Astar inside here
				// System.out.println(route);
				if (engdroute == null) { 												// == 'Equal to'
					System.out.println("No route found:");
					return;
				}
				// System.out.println(route);
				int index = engdroute.getLocIndex(this.location);
				int newIndex = 0;
				if (index != -1) {	// if already on the route (in between cities)	// != 'Not equal to'
					newIndex = index + 1;
					System.out.println("ALREADY ON: " + newIndex);
				} else {// new route
					newIndex = 1;
					System.out.println("NEW");
				}
				Edge edge = engdroute.getEdge(newIndex);
				EngDRoadInfo edgeinfo = (EngDRoadInfo) edge.getInfo();
				if (this.finStatus - edgeinfo.getCost() < 0) {
					isMoving = false;
				} else {
					Int2D nextStep = engdroute.getLocation(newIndex);
					this.setLocation(nextStep);
					updatePositionOnMap(engdModelSim);
					// System.out.println(route.getNumSteps() + ", " +
					// route.getNumEdges());
					this.currentEdge = edge;
					determineDeath(edgeinfo, this);
					engdroute.printRoute();
				}

				Centroid centroid = (Centroid) currentEdge.getTo();
				if (this.location.getX() == centroid.getLocation().getX()				// == 'Equal to'
						&& this.location.getY() == centroid.getLocation().getY()) {		// == 'Equal to'
					currentCentroid = centroid;
					EngDRoadInfo einfo = (EngDRoadInfo) this.currentEdge.getInfo();
					this.finStatus -= (einfo.getCost() * this.teamMembers
							.size());
							// finStatus = finStatus - einfo.getCost() * this.familyMembers
							// if at the end of an edge, subtract the money
					// city.addMembers(this.familyMembers);
					for (Object or : this.teamMembers) {
						EngDAgent rr = (EngDAgent) or;
						centroid.addMember(rr);
					}
				}

				else {
					for (Object c : centroids) {
						Centroid cremove = (Centroid) c;
						for (Object o : this.teamMembers) {
							EngDAgent r = (EngDAgent) o;
							cremove.removeMember(r);
						}
					}
				}
			}
		}
		// }
		System.out.println(this.location.x + ", " + this.location.y);
		/*
		 * for (Object c: cities){ City city = (City)c;
		 * 
		 * if (this.location == city.getLocation()){
		 * System.out.println(this.location.x + ", " + this.location.y + "|| " +
		 * city.getName()); } }
		 */
	}

	public Centroid calcGoalLSOA(Bag centroidlist) { // returns the best LSOA
		Centroid bestCentroid = null;
		double max = 0.0;
		for (Object newCentroid : centroidlist) {
			Centroid c = (Centroid) newCentroid;
			double cityDesirability = dangerCare() * c.getViolence()			// dangerCare calculated below 
					+ familyAbroadCare() * c.getFamilyPresence()
					+ c.getEconomy() * (EngDParameters.ECON_CARE + random.nextDouble() / 4)
					+ c.getScaledPopulation() * (EngDParameters.POP_CARE + random.nextDouble() / 4);
			if (c.getAgentPopulation() + teamMembers.size() >= c.getQuota()) // if reached quota, desirability is 0
				cityDesirability = 0;
			if (cityDesirability > max) {
				max = cityDesirability;
				bestCentroid = c;
			}

		}
		return bestCentroid;
	}

	private void setGoal(Centroid from, Centroid to) {
		this.goal = to;
		// this.route = from.getRoute(to, this);
		// this.routePosition = 0;
	}

	private EngDRoute calcRoute(Centroid from, Centroid to) {
		EngDRoute newRoute = from.getRoute(to, this);
		// if there's a route that contains this route
		// access it and see if decided not to use it before
		// use new route if old one changed mind, keep label as good
		// if not continue with it and keep label as good

		// TODO **** can't return back to an old route unless goal has changed
		/*
		 * for (Route r: cachedRoutes.keySet()){ if (r.equals(newRoute)){
		 * System.out.println("---------FOUND SAME---------"); if
		 * (cachedRoutes.get(r) == 1 || goalChanged) return newRoute; else
		 * return this.route; } else cachedRoutes.put(r, 0); }
		 * cachedRoutes.put(newRoute, 1); return newRoute;
		 */

		if (goalChanged)
			return newRoute;
		else
			return this.engdroute;
	}

	public void updatePositionOnMap(EngDModel engdModelSim) {
			for (Object o : this.getTeam()) {
			EngDAgent r = (EngDAgent) o;
			double randX = 0;
			double randY = 0;
			engdModelSim.world.setObjectLocation(r,
					new Double2D(location.getX() + randX / 10, location.getY()
							+ randY / 10));
		}
	}

	public static void determineDeath(EngDRoadInfo edge, EngDNGOTeam agent) {
		double deaths = edge.getDeaths() * EngDParameters.ROAD_DEATH_PROB;
		double rand = random.nextDouble();
		if (rand < deaths) {// first family member dies (for now)
			if (agent.getTeam().size() != 0) {
				EngDAgent r = (EngDAgent) agent.getTeam().get(0);
				r.setShiftStatus(0);
				agent.getTeam().remove(0);
				agent.currentCentroid.getAgents().remove(r);
			}
		}

	}

	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
		for (Object o : this.teamMembers) {
			EngDAgent r = (EngDAgent) o;
			r.setLocation(location);
		}
	}

	public double getFinStatus() {
		return finStatus;
	}

	public void setFinStatus(int finStatus) {
		this.finStatus = finStatus;
	}

	public void setHome(Centroid home) {
		this.home = home;
	}

	public Centroid getGoal() {
		return goal;
	}

	public void setGoal(Centroid goal) {
		this.goal = goal;
	}

	public Centroid getHome() {
		return home;
	}

	public void setCurrent(Centroid current) {
		this.currentCentroid = current;
	}

	public Bag getTeam() {
		return teamMembers;
	}

	public void setFamily(Bag family) {
		this.teamMembers = family;
	}

	public double dangerCare() {// 0-1, young, old, or has family weighted more
		double dangerCare = 0.5;
		for (Object o : this.teamMembers) {
			EngDAgent r = (EngDAgent) o;
			if (r.getAge() < 12 || r.getAge() > 60) { // if refugee is under 12 OR over 60
				dangerCare += EngDParameters.DANGER_CARE_WEIGHT
						* random.nextDouble();
				// adds Parameters.DANGER_CARE_WEIGHT * random.nextDouble() to
				// dangerCare
			}
		}
		return dangerCare;
	}

	public double familyAbroadCare() { // 0-1, if travelling without family, cares more
		double familyCare = 1.0;
		if (this.teamMembers.size() == 1)	//equal to
			familyCare += EngDParameters.FAMILY_ABROAD_CARE_WEIGHT
					* random.nextDouble();
		return familyCare;
	}
}
