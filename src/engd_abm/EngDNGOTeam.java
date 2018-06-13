package engd_abm;

import java.awt.Color;

import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

class EngDNGOTeam implements Steppable {
	private Int2D location;
	private Bag familyMembers;
	private EngDRoute engdroute;
	private int routePosition;
	private double finStatus;
	private Node home;
	private Edge currentEdge;
	private Node currentLsoa;
	private Node goal;
	static MersenneTwisterFast random = new MersenneTwisterFast();
	private boolean isMoving;
	private HashMap<EngDRoute, Integer> cachedRoutes;
	private HashMap<Node, Integer> cachedGoals;
	private boolean goalChanged;

	public EngDNGOTeam(Int2D location, int size, Node home, double finStatus) {
		this.location = location;
		this.home = home;
		this.goal = home;
		this.finStatus = finStatus;
		familyMembers = new Bag();
		currentLsoa = home;
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
		Bag lsoas = engdModelSim.lsoas;
		Node goalLSOA = calcGoalLSOA(lsoas);

		// if the current location is the goalCity location, then agent has arrived so isn't moving
		// else if agent's finStatus is 0
		// else if agent is not moving
		// else
			// if goalCity name is NOT equal to 0
				// if random is less than GOAL_CHANGE_PROB, change goalCity
				// if goal is equal to home, change goalCity
			// else goal has not changed
			// if current X location is NOT equal to goal X location OR current Y location is NOT equal to goal Y location
				// if name of currentCity is equal to goal Name AND current Location is NOT equal to goal Location, currentCity = (City) currentEdge.to()
				// route = calcRoute
				// if route is NULL print 'no route found'
		

		if (this.location == goalLSOA.location) {									// == 'Equal to'
			goal = goalLSOA;
			isMoving = false;
			// if the current location is the goalCity location, then agent has arrived so isn't moving
		} else if (finStatus <= 0.0) {												// less than or equal to 0
			System.out.println("----NO MONEY LEFT-----");
			return;
		} else if (isMoving == false)												// == 'Equal to'
			return;
		else {
			System.out.println(finStatus);
			if (goalLSOA.getName().compareTo(goal.getName()) != 0) {				// != 'Not equal to'
				double r = random.nextDouble();
				if (r < EngDParameters.GOAL_CHANGE_PROB) {
					this.goal = goalLSOA;
					System.out.println("-----GOAL CHANGE------");
					goalChanged = true;
				}
				if (goal == home) {													// == 'Equal to'
					this.goal = goalLSOA;
					goalChanged = true;
				}
			} else
				goalChanged = false;

			if (this.getLocation().getX() != goal.getLocation().getX() 				// != 'Not equal to'
					|| this.getLocation().getY() != goal.getLocation().getY()) { 	// ||'Conditional-OR'
				System.out.println("Home: " + this.getHome().getName()
						+ " | Goal " + goal.getName());
				System.out.println(this + " Current: " + currentLsoa.getName());
				if (currentLsoa.getName() == goal.getName()							// == 'Equal to'
						&& this.getLocation() != goal.getLocation()) { 				// && 'Conditional-AND'
					System.out.println("-----HERE------");
					currentLsoa = (Node) currentEdge.to();
				}
				// setGoal(currentCity, goal);
				engdroute = calcRoute(currentLsoa, goal);// Astar inside here
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

				Node engdlsoa = (Node) currentEdge.getTo();
				if (this.location.getX() == engdlsoa.getLocation().getX()				// == 'Equal to'
						&& this.location.getY() == engdlsoa.getLocation().getY()) {		// == 'Equal to'
					currentLsoa = engdlsoa;
					EngDRoadInfo einfo = (EngDRoadInfo) this.currentEdge.getInfo();
					this.finStatus -= (einfo.getCost() * this.familyMembers
							.size());
							// finStatus = finStatus - einfo.getCost() * this.familyMembers
							// if at the end of an edge, subtract the money
					// city.addMembers(this.familyMembers);
					for (Object or : this.familyMembers) {
						EngDAgent rr = (EngDAgent) or;
						engdlsoa.addAgent(rr);
					}
				}

				else {
					for (Object lsoa : lsoas) {
						Node lsoaremove = (Node) lsoa;
						for (Object o : this.familyMembers) {
							EngDAgent r = (EngDAgent) o;
							lsoaremove.removeAgent(r);
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

	public Node calcGoalLSOA(Bag lsoalist) { // returns the best city
		Node bestLSOA = null;
		double max = 0.0;
		for (Object lsoaobject : lsoalist) {
			Node lsoa = (Node) lsoaobject;
			double lsoaDesirability = dangerCare() * lsoa.getViolence()			// dangerCare calculated below 
					+ familyAbroadCare() * lsoa.getFamilyPresence()
					+ lsoa.getEconomy() * (EngDParameters.ECON_CARE + random.nextDouble() / 4)
					+ lsoa.getScaledPopulation() * (EngDParameters.POP_CARE + random.nextDouble() / 4);
			if (lsoa.getRefugeePopulation() + familyMembers.size() >= lsoa.getQuota()) // if reached quota, desirability is 0
				lsoaDesirability = 0;
			if (lsoaDesirability > max) {
				max = lsoaDesirability;
				bestLSOA = lsoa;
			}

		}
		return bestLSOA;
	}

	private void setGoal(Node from, Node to) {
		this.goal = to;
		// this.route = from.getRoute(to, this);
		// this.routePosition = 0;
	}

	private EngDRoute calcRoute(Node from, Node to) {
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
		// migrationSim.world.setObjectLocation(this.getFamily(), new
		// Double2D(location.getX() , location.getY() ));
		for (Object o : this.getFamily()) {
			EngDAgent r = (EngDAgent) o;
			double randX = 0;// migrationSim.random.nextDouble() * 0.3;
			double randY = 0;// migrationSim.random.nextDouble() * 0.3;
			// System.out.println("Location: " + location.getX() + " " +
			// location.getY());
			engdModelSim.engdModelSim.setObjectLocation(r,
					new Double2D(location.getX() + randX / 10, location.getY()
							+ randY / 10));
			// migrationSim.worldPopResolution.setObjectLocation(this,
			// (int)location.getX()/10, (int)location.getY()/10);
		}
	}

	public static void determineDeath(EngDRoadInfo edge, EngDNGOTeam ngoagent) {
		double deaths = edge.getDeaths() * EngDParameters.ROAD_DEATH_PROB;
		double rand = random.nextDouble();
		if (rand < deaths) {// first family member dies (for now)
			if (ngoagent.getFamily().size() != 0) {
				EngDAgent r = (EngDAgent) ngoagent.getFamily().get(0);
				r.setHealthStatus(0);
				ngoagent.getFamily().remove(0);
				ngoagent.currentLsoa.getNGOAgents().remove(r);
			}
		}

	}

	// get and set
	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
		for (Object o : this.familyMembers) {
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

	public void setHome(Node home) {
		this.home = home;
	}

	public Node getGoal() {
		return goal;
	}

	public void setGoal(Node goal) {
		this.goal = goal;
	}

	public Node getHome() {
		return home;
	}

	public void setCurrent(Node current) {
		this.currentLsoa = current;
	}

	public Bag getFamily() {
		return familyMembers;
	}

	public void setFamily(Bag family) {
		this.familyMembers = family;
	}

	public double dangerCare() {// 0-1, young, old, or has family weighted more
		double dangerCare = 0.5;
		for (Object o : this.familyMembers) {
			EngDAgent r = (EngDAgent) o;
			if (r.getAge() < 12 || r.getAge() > 60) {	//if refugee is under 12 OR over 60
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
		if (this.familyMembers.size() == 1)	//equal to
			familyCare += EngDParameters.FAMILY_ABROAD_CARE_WEIGHT
					* random.nextDouble();
		return familyCare;
	}
}
