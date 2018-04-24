package engd_abm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import ec.util.MersenneTwisterFast;

class EngDModelBuilder {
	public static EngDModel engdModelSim;
	public static GeomPlanarGraph network = new GeomPlanarGraph();
	public static GeomVectorField junctions = new GeomVectorField();
	public static HashMap<Integer, GeomPlanarGraphEdge> idsToEdges = new HashMap<Integer, GeomPlanarGraphEdge>();
	private static HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>> edgeTraffic = new HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>>();
	public static GeomVectorField agents = new GeomVectorField();

	static ArrayList<EngDAgent> agentList = new ArrayList<EngDAgent>();

	private static ArrayList<String> csvData = new ArrayList<String>();

	public static int numAgents = 10;
	public MersenneTwisterFast random;

	public static void initializeWorld(EngDModel sim) {

		System.out.println("Initializing model world...");
		engdModelSim = sim;

		engdModelSim.world_height = 500;
		engdModelSim.world_width = 500;

		engdModelSim.boundary = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.lsoa = new GeomVectorField(sim.world_width,
				sim.world_height);

		EngDModel.roads = new GeomVectorField(sim.world_width, sim.world_height);

		engdModelSim.flood2 = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.flood3 = new GeomVectorField(sim.world_width,
				sim.world_height);

		engdModelSim.roadNetwork = new Network();

		String[] shapeFiles = { EngDParameters.LSOA_SHP,
				EngDParameters.BOUNDARY_SHP, EngDParameters.ROAD_SHP,
				EngDParameters.FLOOD2_SHP, EngDParameters.FLOOD3_SHP };
		GeomVectorField[] vectorFields = { engdModelSim.lsoa,
				engdModelSim.boundary, EngDModel.roads, engdModelSim.flood2,
				engdModelSim.flood3 };
		System.out.println("Starting to read shapefiles...");

		readInShapefile(shapeFiles, vectorFields);

		Envelope MBR = engdModelSim.lsoa.getMBR();
		MBR.expandToInclude(engdModelSim.boundary.getMBR());
		MBR.expandToInclude(EngDModel.roads.getMBR());
		MBR.expandToInclude(engdModelSim.flood2.getMBR());
		MBR.expandToInclude(engdModelSim.flood3.getMBR());

		createNetwork();

		engdModelSim.lsoa.setMBR(MBR);
		engdModelSim.boundary.setMBR(MBR);
		EngDModel.roads.setMBR(MBR);
		engdModelSim.flood2.setMBR(MBR);
		engdModelSim.flood3.setMBR(MBR);

		try {
			agentGoals("/GloucestershireAgentGoals.csv");
			addAgents("/GloucestershireITNAGENT.csv");
			agents.setMBR(MBR);

			engdModelSim.schedule.scheduleRepeating(
					agents.scheduleSpatialIndexUpdater(), Integer.MAX_VALUE,
					1.0);

		} catch (FileNotFoundException e) {
			System.out.println("Error: missing required data file");
		} catch (IOException e) {
			e.printStackTrace();
		}

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

	public int getNumAgents() {
		return numAgents;
	}

	public void setNumAgents(int n) {
		if (n > 0)
			numAgents = n;
	}

	public static void agentGoals(String agentfilename) throws IOException {
		String csvGoal = null;
		BufferedReader agentGoalsBuffer = null;

		String agentFilePath = EngDModel.class.getResource(agentfilename)
				.getPath();
		FileInputStream agentfstream = new FileInputStream(agentFilePath);
		System.out.println("Reading Agent's Goals CSV file: " + agentFilePath);

		try {
			agentGoalsBuffer = new BufferedReader(new InputStreamReader(
					agentfstream));
			agentGoalsBuffer.readLine();
			while ((csvGoal = agentGoalsBuffer.readLine()) != null) {
				String[] splitted = csvGoal.split(",");

				ArrayList<String> agentGoalsResult = new ArrayList<String>(
						splitted.length);
				for (String data : splitted)
					agentGoalsResult.add(data);
				csvData.addAll(agentGoalsResult);
			}
			System.out.println();
			System.out.println("Full csvData Array: " + csvData);
		} finally {
			if (agentGoalsBuffer != null)
				agentGoalsBuffer.close();
		}
	}

	public ArrayList<String> getList() {
		return csvData;
	}

	static void addAgents(String filename) {
		try {
			String filePath = EngDModelBuilder.class.getResource(filename)
					.getPath();
			FileInputStream fstream = new FileInputStream(filePath);
			System.out.println();
			System.out.println("Populating model with Agents: " + filePath);

			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));
			String s;

			d.readLine();
			while ((s = d.readLine()) != null) {
				String[] bits = s.split(",");

				int pop = Integer.parseInt(bits[2]);

				String homeTract = bits[3];
				String ROAD_ID = bits[3];
				String random = csvData
						.get(new Random().nextInt(csvData.size()));
				String goalTract = random;
				System.out.println();
				System.out.println("Agent goalTract: " + goalTract);

				GeomPlanarGraphEdge startingEdge = idsToEdges.get((int) Double
						.parseDouble(ROAD_ID));
				GeomPlanarGraphEdge goalEdge = idsToEdges.get((int) Double
						.parseDouble(goalTract));

				for (int i = 0; i < numAgents; i++) {
					EngDAgent newEngDAgent = new EngDAgent(engdModelSim,
							homeTract, goalTract, startingEdge, goalEdge);
					boolean successfulStart = newEngDAgent.start(null);

					if (!successfulStart) {
						System.out
								.println("ERROR: Agents *NOT* added properly!");
						continue;
					} else {
						System.out.println("Agent added successfully!");
					}

					MasonGeometry newGeometry = newEngDAgent.getGeometry();
					newGeometry.isMovable = true;
					agents.addGeometry(newGeometry);
					agentList.add(newEngDAgent);
					engdModelSim.schedule.scheduleRepeating(newEngDAgent);

				}
			}

			d.close();
			System.out.println();
			System.out.println("All agents added successfully!");
		} catch (Exception e) {
			System.out.println("ERROR: issue with population file: ");
			e.printStackTrace();
		}
	}

	private static void createNetwork() {
		System.out.println("Creating road network...");
		System.out.println();
		network.createFromGeomField(EngDModel.roads);

		for (Object o : network.getEdges()) {
			GeomPlanarGraphEdge e = (GeomPlanarGraphEdge) o;

			idsToEdges.put(e.getIntegerAttribute("ROAD_ID_1").intValue(), e);

			e.setData(new ArrayList<EngDAgent>());
		}

		addIntersectionNodes(network.nodeIterator(), junctions);
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

			junctions.addGeometry(new MasonGeometry(point));
			counter++;
		}
	}

	public static HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>> getEdgeTraffic() {
		return edgeTraffic;
	}

	public static void setEdgeTraffic(
			HashMap<GeomPlanarGraphEdge, ArrayList<EngDAgent>> edgeTraffic) {
		EngDModelBuilder.edgeTraffic = edgeTraffic;
	}

	public static ArrayList<EngDAgent> getAgents() {
		return agentList;
	}

	public static void setAgents(ArrayList<EngDAgent> agents) {
		EngDModelBuilder.agentList = agents;
	}

}
