package engd_abm;

import java.util.ArrayList;
import java.util.HashMap;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphEdge;

class EngDModel extends SimState {

	public Continuous2D world;
	
	public Network roadNetwork = new Network();
	public GeomVectorField lsoa;
	public static GeomVectorField roads;
	public GeomVectorField flood2;
	public GeomVectorField flood3;
	public static GeomVectorField agents = new GeomVectorField();
	
	public static GeomPlanarGraph network = new GeomPlanarGraph();
    // Stores road network connections
    public static GeomVectorField junctions = new GeomVectorField();
    // Stores nodes for road intersections
    //static ArrayList<agents.EngDAgent> agentList = new ArrayList<agents.EngDAgent>();
    public static HashMap<Integer, GeomPlanarGraphEdge> idsToEdges =
        new HashMap<Integer, GeomPlanarGraphEdge>();
    public HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>> edgeTraffic =
            new HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>>();
        public GeomVectorField mainagents = new GeomVectorField();
        
    static ArrayList<EngDAgent> agentList = new ArrayList<EngDAgent>();
    
    private static ArrayList<String> csvData = new ArrayList<String>();
	
    public boolean goToLSOA = true;
    
    public boolean getGoToLSOA()	{
        return goToLSOA;
    }
    
    public int activeCount;
	public int pop_width;
	public int pop_height;
	public int world_width;
	public int world_height;
	
	public long total_pop = 0;
	
	//public Bag agents;

	public EngDModel(long seed) {
		super(seed);
		random = new MersenneTwisterFast(12345);
	}

	@Override
	public void start() {
		System.out.println("Model initializing...");
		super.start();
		//agents  = new Bag();
		EngDModelBuilder.initializeWorld(this);
	}

	@Override
	public void finish() {
		System.out.println("Finishing...");
		super.finish();
	}

	public static void main(String[] args) {
		//long seed = System.currentTimeMillis();
		//EngDModel simState = new EngDModel(seed);
		//simState.start();
		doLoop(EngDModel.class, args);
		// long seed = System.currentTimeMillis();
		// EngDModel simState = new EngDModel(seed);
		// simState.start();
		System.exit(0);

	}

	public static ArrayList<String> getCsvData() {
		return csvData;
	}

	public static void setCsvData(ArrayList<String> csvData) {
		EngDModel.csvData = csvData;
	}
}

// THIS WORKS! TOO SCARED TO DELETE AT THE MOMENT!

/*
 * package engd_abm_mk1;
 * 
 * import java.io.FileNotFoundException; import java.net.URL;
 * 
 * import com.vividsolutions.jts.geom.Envelope;
 * 
 * import sim.engine.SimState; import sim.field.continuous.Continuous2D; import
 * sim.field.geo.GeomVectorField; import sim.io.geo.ShapeFileImporter;
 * 
 * class EngDModel extends SimState {
 * 
 * public int width = 800; public int height = 800; public Continuous2D world;
 * public static GeomVectorField flood2 = new GeomVectorField();
 * 
 * public EngDModel(long seed) { super(seed); initialise(); }
 * 
 * public void initialise() { System.out.println("Model initializing...");
 * super.start(); System.out.println("Starting to read shapefiles...");
 * readInShapefile(); }
 * 
 * static void readInShapefile() { System.out.println("Reading shapefiles...");
 * try { ShapeFileImporter.read(
 * EngDModel.class.getResource("/Gloucestershire_FZ_2.shp"), flood2); Envelope
 * MBR = flood2.getMBR(); flood2.setMBR(MBR); } catch (FileNotFoundException ex)
 * { System.out.println("Error opening shapefile!" + ex); System.exit(-1); } }
 * 
 * public void finish() { System.out.println("Finishing..."); super.finish(); }
 * 
 * public static void main(String[] args) {
 * 
 * doLoop(EngDModel.class, args); System.exit(0);
 * 
 * } }
 */