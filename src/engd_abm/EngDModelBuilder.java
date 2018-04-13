package engd_abm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import sim.engine.SimState;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import engd_abm.EngDAgent;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;


class EngDModelBuilder {
	public static EngDModel engdModelSim;

	public static void initializeWorld(EngDModel sim) {

		System.out.println("Initializing model world...");
		engdModelSim = sim;

		engdModelSim.world_height = 500;
		engdModelSim.world_width = 500;

		engdModelSim.lsoa = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.roads = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.flood2 = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.flood3 = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.roadNetwork = new Network();

		String[] shapeFiles = { EngDParameters.LSOA_SHP,
				EngDParameters.ROAD_SHP, EngDParameters.FLOOD2_SHP,
				EngDParameters.FLOOD3_SHP };
		GeomVectorField[] vectorFields = { engdModelSim.lsoa,
				engdModelSim.roads, engdModelSim.flood2, engdModelSim.flood3 };
		System.out.println("Starting to read shapefiles...");

		readInShapefile(shapeFiles, vectorFields);

		// expand the extent to include all features
		Envelope MBR = engdModelSim.lsoa.getMBR();
		MBR.expandToInclude(engdModelSim.roads.getMBR());
		MBR.expandToInclude(engdModelSim.flood2.getMBR());
		MBR.expandToInclude(engdModelSim.flood3.getMBR());

		engdModelSim.lsoa.setMBR(MBR);
		engdModelSim.roads.setMBR(MBR);
		engdModelSim.flood2.setMBR(MBR);
		engdModelSim.flood3.setMBR(MBR);

		schedule.scheduleRepeating(
				EngDAgent.agents.scheduleSpatialIndexUpdater(),
				Integer.MAX_VALUE, 1.0);

		createNetwork();
		addAgents("GloucestershireITNAGENT.csv");

	}

	static void readInShapefile(String[] files, GeomVectorField[] vectorFields) {
		System.out.println("Reading in shapefiles...");
		try {
			for (int i = 0; i < files.length; i++) {
				String filePath = files[i];
				File file = new File(filePath);
				URL shapeURI = file.toURI().toURL();
				ShapeFileImporter.read(shapeURI, vectorFields[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Object random;

	private void addAgents(String filename) {
		try {
			String filePath = EngDModel.class.getResource(filename).getPath();
			FileInputStream fstream = new FileInputStream(filePath);
			System.out.println("Adding Agents: " + filePath);
			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));
			String s;

			// get rid of the header
			d.readLine();
			// read in all data
			while ((s = d.readLine()) != null) {
				String[] bits = s.split(",");

				int pop = Integer.parseInt(bits[2]);

				String homeTract = bits[3];
				String ROAD_ID = bits[3];

				Random randomiser = new Random();
				String random = EngDModel.getCsvData().get(
						new Random().nextInt(EngDModel.getCsvData().size()));
				String goalTract = random;
				System.out.println();
				System.out.println("Agent goalTract: " + goalTract);

				GeomPlanarGraphEdge startingEdge = EngDModel.idsToEdges
						.get((int) Double.parseDouble(ROAD_ID));
				GeomPlanarGraphEdge goalEdge = EngDModel.idsToEdges
						.get((int) Double.parseDouble(goalTract));

				EngDAgent a = new EngDAgent(this, homeTract, startingEdge,
						goalEdge);

				boolean successfulStart = a.start(this);

				System.out.println("Agent Status = " + EngDAgent.getStatus());

				if (!successfulStart) {
					System.out
							.println("ERROR: Main agents *NOT* added properly!");
					continue; // DON'T ADD IT if it's bad
				} else {
					// System.out.println("Agent added successfully!");
				}

				// MasonGeometry newGeometry = new
				// MasonGeometry(a.getGeometry());
				MasonGeometry newGeometry = a.getGeometry();
				newGeometry.isMovable = true;
				EngDModel.agents.addGeometry(newGeometry);
				EngDModel.agentList.add(a);
				EngDModel.schedule.scheduleRepeating(a);
			}

			d.close();
			System.out.println();
			System.out.println("Agents added successfully!");
		} catch (Exception e) {
			System.out.println("ERROR: issue with population file: ");
			e.printStackTrace();
		}
	}

	private static void createNetwork() {
		System.out.println("Creating road network...");
		System.out.println();
		EngDModel.network.createFromGeomField(EngDModel.roads);

		for (Object o : EngDModel.network.getEdges()) {
			GeomPlanarGraphEdge e = (GeomPlanarGraphEdge) o;

			EngDModel.idsToEdges.put(e.getIntegerAttribute("ROAD_ID_1")
					.intValue(), e);

			e.setData(new ArrayList<EngDAgent>());
		}

		addIntersectionNodes(EngDModel.network.nodeIterator(),
				EngDModel.junctions);
	}

	private static void addIntersectionNodes(Iterator<?> nodeIterator,
			GeomVectorField intersections) {
		GeometryFactory fact = new GeometryFactory();
		Coordinate coord = null;
		Point point = null;
		int counter = 0;

		while (nodeIterator.hasNext()) {
			Node node = (Node) nodeIterator.next();
			coord = node.getCoordinate();
			point = fact.createPoint(coord);

			EngDModel.junctions.addGeometry(new MasonGeometry(point));
			counter++;
		}
	}

}

// THIS WORKS! TOO SCARED TO DELETE AT THE MOMENT!

/*
 * package engd_abm_mk1;
 * 
 * 
 * import java.io.File; import java.io.FileNotFoundException; import
 * java.net.URL;
 * 
 * import sim.app.geo.sickStudents.SickStudentsModel; import
 * sim.field.geo.GeomVectorField; import sim.io.geo.ShapeFileImporter; import
 * sim.util.Bag;
 * 
 * import com.vividsolutions.jts.geom.Envelope;
 * 
 * class EngDModelBuilder { public static EngDModel engdModelSim;
 * 
 * public static void initializeWorld(EngDModel sim) {
 * 
 * System.out.println("Initializing model world..."); engdModelSim = sim;
 * 
 * //String[] lsoaAttributes = { "REGION" }; //String[] roadAttributes = {
 * "COUNTRY" }; //String[] flood2Attributes = { "NAME1" }; //String[]
 * flood3Attributes = { "ID" };
 * 
 * //engdModelSim.world_height = 500; //engdModelSim.world_width = 500;
 * 
 * //String[] shapeFiles = { EngDParameters.LSOA_SHP, EngDParameters.ROADS_SHP,
 * //EngDParameters.FLOOD2_SHP, EngDParameters.FLOOD3_SHP }; //String[]
 * shapeFiles = { EngDParameters.FLOOD2_SHP }; //Bag[] attFiles = { lsoaAtt,
 * roadAtt, roadAtt, flood2Att, //flood3Att }; //GeomVectorField[] vectorFields
 * = { engdModelSim.flood2, };
 * System.out.println("Starting to read shapefiles...");
 * //readInShapefile(shapeFiles, attFiles, vectorFields); readInShapefile();
 * 
 * // expand the extent to include all features Envelope MBR =
 * engdModelSim.flood2.getMBR();
 * //MBR.expandToInclude(engdModelSim.roads.getMBR());
 * //MBR.expandToInclude(engdModelSim.flood2.getMBR());
 * //MBR.expandToInclude(engdModelSim.flood3.getMBR());
 * 
 * //engdModelSim.lsoa.setMBR(MBR); //engdModelSim.roads.setMBR(MBR);
 * //engdModelSim.flood2.setMBR(MBR); //engdModelSim.flood3.setMBR(MBR);
 * 
 * }
 * 
 * static void readInShapefile() { System.out.println("Reading shapefiles...");
 * try { // read the data
 * //ShapeFileImporter.read(EngDModel.class.getResource(EngDParameters
 * .FLOOD2_SHP), engdModelSim.flood2);
 * ShapeFileImporter.read(EngDModel.class.getResource
 * ("/Gloucestershire_FZ_2.shp"), engdModelSim.flood2); Envelope MBR =
 * engdModelSim.flood2.getMBR(); engdModelSim.flood2.setMBR(MBR); } catch
 * (FileNotFoundException ex) { System.out.println("Error opening shapefile!" +
 * ex); System.exit(-1); } }
 * 
 * }
 */
