package engd_abm;

import java.util.HashSet;

import sim.util.Int2D;


class EngDLSOA {
	Int2D location;
	String name;
	private int ID;
	private int pop;
	
	public EngDLSOA(Int2D location) {
		this.name = name;
		this.location = location;
	}
	
	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public EngDRoute getRoute() {
		EngDRoute route;

		route = EngDAStar.astarPath(this);
		//System.out.println(route.getNumSteps());

		return route;
	}

}
