package engd_abm;

import sim.engine.SimState;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;

class EngDModel extends SimState {

	public Continuous2D world;
	public GeomVectorField roads;
	public GeomVectorField flood2;
	public GeomVectorField flood3;
	public int world_width;
	public int world_height;

	public EngDModel(long seed) {
		super(seed);
	}

	@Override
	public void start() {
		System.out.println("Model initializing...");
		super.start();
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