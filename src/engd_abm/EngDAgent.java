package engd_abm;

import sim.util.Int2D;
import sim.util.geo.MasonGeometry;

class EngDAgent {

	private int age;
	private EngDNGOTeam team;
	private Int2D location;
	private int shiftStatus = 1; // Formerly healthStatus, default 1 (working), 0 (on break)

	public EngDAgent(int sex, int age, EngDNGOTeam team) {
		this.age = age;
		this.team = team;
	}
	
	public int getShiftStatus() {
		return shiftStatus;
	}

	public void setShiftStatus(int status) {
		this.shiftStatus = status;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	//public Int2D getLocation() {
	public Int2D getLocation() {
		return location;
	}

	//public void setLocation(Int2D location) {
	public void setLocation(Int2D location) {
		this.location = location;
	}

	public EngDNGOTeam getTeam() {
		return team;
	}

	public void setTeam(EngDNGOTeam team) {
		this.team = team;
	}

}
