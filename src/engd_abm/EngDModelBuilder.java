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
import java.util.Map;
import java.util.Random;

import sim.field.geo.GeomVectorField;
import sim.field.grid.SparseGrid2D;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Int2D;
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
		System.out.println("Model initializing...");
		engdModelSim = sim;

		String[] boundaryAttributes = { "NAME", "AREA_CODE", "DESCRIPTIO",
				"FILE_NAME", "NUMBER", "NUMBER0", "POLYGON_ID", "UNIT_ID",
				"CODE", "HECTARES", "AREA", "TYPE_CODE", "DESCRIPT0",
				"TYPE_COD0", "DESCRIPT1" };
		String[] lsoaAttributes = { "ID", "LSOA_CODE", "LSOA_NAME", 
				"LA_NAME", "MSOA_CODE", "MSOA_NAME", "GOR_NAME", 
				"CFSL", "CFSN" };
		String[] roadAttributes = { "JOIN_FID", "fictitious", "identifier",
				"roadNumber", "name1", "formOfWay", "length", "primary",
				"trunkRoad", "loop", "startNode", "endNode", "nameTOID",
				"numberTOID", "function", "objectid", "st_areasha",
				"st_lengths", "Shape_Leng", "GOR_Name", "GOR_Code",
				"MSOA_Name", "MSOA_Code", "LA_Name", "LA_Code", "LSOA_Name",
				"LSOA_Code", "ROAD_ID_1" };
		String[] flood2Attributes = { "TYPE", "LAYER" };
		String[] flood3Attributes = { "TYPE", "LAYER" };

		engdModelSim.world_height = 500;
		engdModelSim.world_width = 500;

		engdModelSim.boundary = new GeomVectorField(sim.world_width,
				sim.world_height);
		Bag boundaryAtt = new Bag(boundaryAttributes);
		System.out.println("	Boundary shapefile: " + EngDParameters.BOUNDARY_SHP);

		engdModelSim.cityPoints = new GeomVectorField(sim.world_width,
				sim.world_height);
		Bag lsoaAtt = new Bag(lsoaAttributes);
		System.out.println("	LSOA shapefile: " + EngDParameters.LSOA_SHP);

		EngDModel.roads = new GeomVectorField(sim.world_width, sim.world_height);
		Bag roadAtt = new Bag(roadAttributes);
		System.out.println("	Roads shapefile: " + EngDParameters.ROAD_SHP);

		engdModelSim.flood2 = new GeomVectorField(sim.world_width,
				sim.world_height);
		Bag flood2Att = new Bag(flood2Attributes);
		System.out.println("	Floods 2 shapefile: " + EngDParameters.FLOOD2_SHP);

		engdModelSim.flood3 = new GeomVectorField(sim.world_width,
				sim.world_height);
		Bag flood3Att = new Bag(flood3Attributes);
		System.out.println("	Floods 3 shapefile: " + EngDParameters.FLOOD3_SHP);
		
		engdModelSim.roadNetwork = new Network();

		String[] shapeFiles = { EngDParameters.LSOA_SHP,
				EngDParameters.BOUNDARY_SHP, EngDParameters.ROAD_SHP,
				EngDParameters.FLOOD2_SHP, EngDParameters.FLOOD3_SHP };
		Bag[] attfiles = { boundaryAtt, lsoaAtt, roadAtt, flood2Att, flood3Att };
		GeomVectorField[] vectorFields = { engdModelSim.cityPoints,
				engdModelSim.boundary, EngDModel.roads, engdModelSim.flood2,
				engdModelSim.flood3 };

		readInShapefile(shapeFiles, attfiles, vectorFields);

		Envelope MBR = engdModelSim.cityPoints.getMBR();
		MBR.expandToInclude(engdModelSim.boundary.getMBR());
		MBR.expandToInclude(EngDModel.roads.getMBR());
		MBR.expandToInclude(engdModelSim.flood2.getMBR());
		MBR.expandToInclude(engdModelSim.flood3.getMBR());

		createNetwork();

		engdModelSim.cityPoints.setMBR(MBR);
		engdModelSim.boundary.setMBR(MBR);
		EngDModel.roads.setMBR(MBR);
		engdModelSim.flood2.setMBR(MBR);
		engdModelSim.flood3.setMBR(MBR);

		//makeLSOAs(engdModelSim.cityPoints, engdModelSim.lsoas, engdModelSim.lsoaList);
		
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

	private static void printLSOA() {
		for (Object lsoa : engdModelSim.lsoas) {
			EngDLSOA l = (EngDLSOA) lsoa;
		}
	}
/*
	static void makeLSOAs(GeomVectorField lsoas_vector,Bag addTo, Map<Integer, EngDLSOA> lsoaList) {
		Bag lsoas = lsoas_vector.getGeometries();
		Envelope e = lsoas_vector.getMBR();
		double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax = e
				.getMaxY();
		int xcols = engdModelSim.world_width - 1, ycols = engdModelSim.world_height - 1;
		System.out.println("Reading in LSOA attributes...");
		for (int i = 0; i < lsoas.size(); i++) {
			MasonGeometry lsoainfo = (MasonGeometry) lsoas.objs[i];
			Point point = lsoas_vector.getGeometryLocation(lsoainfo);
			double x = point.getX(), y = point.getY();
			int xint = (int) Math.floor(xcols * (x - xmin) / (xmax - xmin)), yint = (int) (ycols - Math
					.floor(ycols * (y - ymin) / (ymax - ymin)));
			String lsoaname = lsoainfo.getStringAttribute("LSOA_NAME");
			int ID = lsoainfo.getIntegerAttribute("ID");
			String lsoacode = lsoainfo.getStringAttribute("LSOA_CODE");
			String laName = lsoainfo.getStringAttribute("LA_NAME");
			// int pop = lsoainfo.getIntegerAttribute("POP");
			// int quota = lsoainfo.getIntegerAttribute("QUOTA_1");
			int CFSL = lsoainfo.getIntegerAttribute("CFSL");
			int CFSN = lsoainfo.getIntegerAttribute("CFSN");
			// double economy = lsoainfo.getDoubleAttribute("ECON_1");
			// double familyPresence = lsoainfo.getDoubleAttribute("FAMILY_1");
			Int2D location = new Int2D(xint, yint);

			EngDLSOA lsoa = new EngDLSOA(location, ID, lsoaname, lsoacode, laName,
					CFSL, CFSN);
			addTo.add(lsoa);
			lsoaList.put(ID, lsoa);
			//grid.setObjectLocation(lsoa, location);
		}
	}
	*/

	static void readInShapefile(String[] files, Bag[] attfiles,
			GeomVectorField[] vectorFields) {
		System.out.println("Reading in shapefiles...");
		try {
			for (int i = 0; i < files.length; i++) {
				Bag attributes = attfiles[i];
				String filePath = files[i];
				File file = new File(filePath);
				URL shapeURI = file.toURI().toURL();
				ShapeFileImporter.read(shapeURI, vectorFields[i], attributes);
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
		System.out.println("Reading Goals CSV file: " + agentFilePath);

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
			System.out.println("Agent Goals: " + csvData);
		} finally {
			if (agentGoalsBuffer != null)
				agentGoalsBuffer.close();
		}
		Random randomiser = new Random();
		String random = csvData.get(new Random().nextInt(csvData.size()));
		// String random1 = csvData.get(new Random().nextInt(csvData.size()));
		String goalTract = random;
		// String goalTract1 = random1;
		System.out.println();
		System.out.println("RANDOMLY SELECTED GOALTRACT: " + goalTract);

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
				System.out
						.println("Agent " + ROAD_ID + "'s goal: " + goalTract);

				GeomPlanarGraphEdge startingEdge = idsToEdges.get((int) Double
						.parseDouble(ROAD_ID));
				GeomPlanarGraphEdge goalEdge = idsToEdges.get((int) Double
						.parseDouble(goalTract));

				for (int i = 0; i < pop; i++) {
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
