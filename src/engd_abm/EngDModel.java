package engd_abm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import sim.engine.Schedule;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;

class EngDModel extends SimState {

	public Continuous2D world;

	public Network roadNetwork = new Network();
	public GeomVectorField centroids;
	public GeomVectorField boundary;
	public GeomVectorField osvi;
	public static GeomVectorField roads;
	public GeomVectorField flood2;
	public GeomVectorField flood3;

	private static ArrayList<String> csvData = new ArrayList<String>();

	public boolean goToLSOA = true;

	public boolean getGoToLSOA() {
		return goToLSOA;
	}

	public int activeCount;
	public int pop_width;
	public int pop_height;
	public int world_width;
	public int world_height;

	public long total_pop = 0;

	public Bag agents;
	public Bag agentTeams;
	public Bag lsoacentroids = new Bag();
	public Map<Integer, Centroid> centroidList = new HashMap<>();

	public EngDModel(long seed) {
		super(seed);
	}

	@Override
	public void start() {
		super.start();
		agents = new Bag();
		agentTeams = new Bag();
		EngDModelBuilder.initializeWorld(this);
	}

	@Override
	public void finish() {
		System.out.println("Simulation ended by user.");
		super.finish();
	}

	public static void main(String[] args) {
		long seed = System.currentTimeMillis();
		EngDModel simState = new EngDModel(seed);
		simState.start();
		Schedule schedule = simState.schedule;
		while (true) {
			if (!schedule.step(simState)) {
				break;
			}
		}
		System.exit(0);

	}

	public static ArrayList<String> getCsvData() {
		return csvData;
	}

	public static void setCsvData(ArrayList<String> csvData) {
		EngDModel.csvData = csvData;
	}
}