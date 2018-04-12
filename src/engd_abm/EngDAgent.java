package engd_abm_mk1;

import java.awt.Color;

import ec.util.MersenneTwisterFast;

import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.engine.Stoppable;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;

class EngDAgent {
	
	private Int2D location;
	private int Status = 1; // default 1 (alive), dead 0
	
	
	public int getStatus() {
		return Status;
	}

	public void setStatus(int status) {
		this.Status = status;
	}
	
	public Int2D getLocation() {
		return location;
	}

	public void setLocation(Int2D location) {
		this.location = location;
	}
}
