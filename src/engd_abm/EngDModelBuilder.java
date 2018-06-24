package engd_abm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.BufferedReader;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import net.sf.csv4j.CSVReader;
import ec.util.MersenneTwisterFast;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Int2D;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

class EngDModelBuilder {
	public static EngDModel engdModelSim;
	private static NormalDistribution nd = new NormalDistribution(
			EngDParameters.AVG_TEAM_SIZE, EngDParameters.TEAM_SIZE_SD);
	private static HashMap<Integer, ArrayList<Double>> age_dist;
	private static HashMap<Integer, Double> pop_dist;
	private static HashMap<Integer, NormalDistribution> fin_dist;
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
		String[] lsoaAttributes = { "ID", "LSOA_CODE", "LSOA_NAME", "LA_NAME",
				"MSOA_CODE", "MSOA_NAME", "GOR_NAME", "CFSL", "CFSN" };
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
		System.out.println("	Boundary shapefile: "
				+ EngDParameters.BOUNDARY_SHP);

		engdModelSim.centroids = new GeomVectorField(sim.world_width,
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
		GeomVectorField[] vectorFields = { engdModelSim.centroids,
				engdModelSim.boundary, EngDModel.roads, engdModelSim.flood2,
				engdModelSim.flood3 };

		readInShapefile(shapeFiles, attfiles, vectorFields);

		Envelope MBR = engdModelSim.centroids.getMBR();
		MBR.expandToInclude(engdModelSim.boundary.getMBR());
		MBR.expandToInclude(EngDModel.roads.getMBR());
		MBR.expandToInclude(engdModelSim.flood2.getMBR());
		MBR.expandToInclude(engdModelSim.flood3.getMBR());

		createNetwork();

		engdModelSim.centroids.setMBR(MBR);
		engdModelSim.boundary.setMBR(MBR);
		EngDModel.roads.setMBR(MBR);
		engdModelSim.flood2.setMBR(MBR);
		engdModelSim.flood3.setMBR(MBR);

		//makeCities(engdModelSim.centroids, engdModelSim.cityGrid,
		//engdModelSim.cities, engdModelSim.centroidList);
		//extractFromRoadLinks(engdModelSim.roadLinks, engdModelSim);
		//setUpAgeDist(EngDParameters.AGE_DIST);
		//setUpPopDist(EngDParameters.POP_DIST);
		//setUpFinDist(EngDParameters.FIN_DIST);

		//addNGOAgents();
	}

	private static void printCentroids() {
		for (Object centroid : engdModelSim.lsoacentroids) {
			Centroid c = (Centroid) centroid;
			System.out.format("Name: " + c.getName() + " Ref Pop: "
					+ c.getAgentPopulation());
			System.out.println("\n");
		}
	}

	/*
	 * static void makeCities(GeomVectorField cities_vector, SparseGrid2D grid,
	 * Bag addTo, Map<Integer, Centroid> cityList) { Bag cities =
	 * cities_vector.getGeometries(); Envelope e = cities_vector.getMBR();
	 * double xmin = e.getMinX(), ymin = e.getMinY(), xmax = e.getMaxX(), ymax =
	 * e .getMaxY(); int xcols = engdModelSim.world_width - 1, ycols =
	 * engdModelSim.world_height - 1; System.out.println("Reading in Cities");
	 * for (int i = 0; i < cities.size(); i++) { MasonGeometry cityinfo =
	 * (MasonGeometry) cities.objs[i]; Point point =
	 * cities_vector.getGeometryLocation(cityinfo); double x = point.getX(), y =
	 * point.getY(); int xint = (int) Math.floor(xcols * (x - xmin) / (xmax -
	 * xmin)), yint = (int) (ycols - Math .floor(ycols * (y - ymin) / (ymax -
	 * ymin))); String name = cityinfo.getStringAttribute("NAME_1"); int ID =
	 * cityinfo.getIntegerAttribute("ID"); int origin =
	 * cityinfo.getIntegerAttribute("ORIG"); double scaledPop =
	 * cityinfo.getDoubleAttribute("SPOP_1"); int pop =
	 * cityinfo.getIntegerAttribute("POP"); int quota =
	 * cityinfo.getIntegerAttribute("QUOTA_1"); double violence =
	 * cityinfo.getDoubleAttribute("VIOL_1"); double economy =
	 * cityinfo.getDoubleAttribute("ECON_1"); double familyPresence =
	 * cityinfo.getDoubleAttribute("FAMILY_1"); Int2D location = new Int2D(xint,
	 * yint);
	 * 
	 * Centroid city = new Centroid(location, ID, name, origin, scaledPop, pop,
	 * quota, violence, economy, familyPresence); addTo.add(city);
	 * cityList.put(ID, city); grid.setObjectLocation(city, location); } }
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

	private static void addNGOAgents() {
		System.out.println("Adding NGO Agents ");
		engdModelSim.world = new Continuous2D(
				EngDParameters.WORLD_DISCRETIZTION, engdModelSim.world_width,
				engdModelSim.world_height);
		for (Object c : engdModelSim.lsoacentroids) {

			Centroid centroid = (Centroid) c;
			if (centroid.getOrigin() == 1) {
				int currentPop = 0;// 1,4,5,10,3,14,24
				int lsoapop = (int) Math.round(pop_dist.get(centroid.getID())
						* EngDParameters.TOTAL_POP);
				System.out.println(centroid.getName() + ": " + lsoapop);
				while (currentPop <= lsoapop) {
					EngDNGOTeam r = createNGOTeam(centroid);
					System.out.println(r.getTeam().size());
					engdModelSim.agentTeams.add(r);
					for (Object o : r.getTeam()) {
						EngDAgent engdagent = (EngDAgent) o;
						currentPop++;
						centroid.addMember(engdagent);
						engdModelSim.agents.add(engdagent);
						Int2D loc = centroid.getLocation();
						double y_coord = (loc.y * EngDParameters.WORLD_TO_POP_SCALE)
								+ (int) (engdModelSim.random.nextDouble() * EngDParameters.WORLD_TO_POP_SCALE);
						double x_coord = (loc.x * EngDParameters.WORLD_TO_POP_SCALE)
								+ (int) (engdModelSim.random.nextDouble() * EngDParameters.WORLD_TO_POP_SCALE);
						engdModelSim.world.setObjectLocation(engdagent,
								new Double2D(x_coord, y_coord));
						int y_coordint = loc.y
								+ (int) ((engdModelSim.random.nextDouble() - 0.5) * 3);
						int x_coordint = loc.x
								+ (int) ((engdModelSim.random.nextDouble() - 0.5) * 3);
						engdModelSim.total_pop++;
					}
					engdModelSim.schedule.scheduleRepeating(r);

				}

			}

		}
	}

	private static EngDNGOTeam createNGOTeam(Centroid centroid) {
		System.out.println("Creating NGO Teams ");
		int teamSize = pickTeamSize();
		double finStatus = pick_fin_status(fin_dist, centroid.getID())
				* teamSize;
		// System.out.println(finStatus);
		EngDNGOTeam team = new EngDNGOTeam(centroid.getLocation(), teamSize,
				centroid, finStatus);
		for (int i = 0; i < teamSize; i++) {

			// first pick sex
			int sex;
			if (engdModelSim.random.nextBoolean())
				sex = EngDConstants.MALE;
			else
				sex = EngDConstants.FEMALE;

			// now get age
			int age = pick_age(age_dist, centroid.getID());
			System.out.println(age);

			EngDAgent engdagent = new EngDAgent(sex, age, team);
			team.getTeam().add(engdagent);
		}
		return team;

	}

	private static int pick_age(HashMap<Integer, ArrayList<Double>> age_dist,
			int centroidid) {
		int category = 0;
		double rand = engdModelSim.random.nextDouble();
		ArrayList<Double> dist = age_dist.get(centroidid);
		for (int i = 1; i < 4; i++) {
			if (rand >= dist.get(i - 1) && rand <= dist.get(i)) {
				category = i;
				System.out.println("" + category);
				break; // TODO DOES THIS ACTUALLY BREAK
			}
		}

		switch (category) {
		case 0:
			return engdModelSim.random.nextInt(5); // 0-4
		case 1:
			return engdModelSim.random.nextInt(13) + 5; // 5-17
		case 2:
			return engdModelSim.random.nextInt(42) + 18; // 18-59
		case 3:
			return engdModelSim.random.nextInt(41) + 60; // 60+
		default:
			return 0;
		}
		// return 5;

	}

	private static void setUpPopDist(String pop_dist_file) {
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new FileReader(new File(
					pop_dist_file)));
			// csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int centroid_id = NumberFormat
						.getNumberInstance(java.util.Locale.UK)
						.parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				double percentage = Double.parseDouble(line.get(1));
				pop_dist.put(centroid_id, percentage);
				line = csvReader.readLine();
			}
			System.out.println(pop_dist);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static void setUpFinDist(String fin_dist_file) {
		try {
			// buffer reader for fin distribution data
			CSVReader csvReader = new CSVReader(new FileReader(new File(
					fin_dist_file)));
			// csvReader.readLine();// skip the headers
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int centroid_id = NumberFormat
						.getNumberInstance(java.util.Locale.UK)
						.parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				double avgfin = Double.parseDouble(line.get(2));
				double sd = Double.parseDouble(line.get(3));
				fin_dist.put(centroid_id, new NormalDistribution(avgfin, sd));
				line = csvReader.readLine();
			}
			System.out.println("fin");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static void setUpAgeDist(String age_dist_file) {
		try {
			// buffer reader for age distribution data
			CSVReader csvReader = new CSVReader(new FileReader(new File(
					age_dist_file)));
			csvReader.readLine();
			List<String> line = csvReader.readLine();
			while (!line.isEmpty()) {
				// read in the county ids
				int centroid_id = NumberFormat
						.getNumberInstance(java.util.Locale.UK)
						.parse(line.get(0)).intValue();
				// relevant info is from 5 - 21
				ArrayList<Double> list = new ArrayList<Double>();
				double sum = 0;
				for (int i = 1; i <= 4; i++) {
					double percentage = Double.parseDouble(line.get(i));
					sum += percentage;
					list.add(sum);
				}
				// System.out.println("sum = " + sum);
				// System.out.println();

				// now add it to the hashmap
				age_dist.put(centroid_id, list);

				line = csvReader.readLine();
			}
			System.out.println(age_dist);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		}
	}

	private static double pick_fin_status(
			HashMap<Integer, NormalDistribution> fin_dist, int centroidid) {
		// TODO Auto-generated method stub
		NormalDistribution nd = fin_dist.get(centroidid);
		return nd.sample();

	}

	private static int pickTeamSize() {
		int familySize = (int) Math.round(nd.sample());
		return familySize;
	}

	/*
	 * static void extractFromRoadLinks(GeomVectorField roadLinks, EngDModel
	 * engdModelSim) { Bag geoms = roadLinks.getGeometries(); Envelope e =
	 * roadLinks.getMBR(); double xmin = e.getMinX(), ymin = e.getMinY(), xmax =
	 * e.getMaxX(), ymax = e .getMaxY(); int xcols = engdModelSim.world_width -
	 * 1, ycols = engdModelSim.world_height - 1; int count = 0;
	 * 
	 * for (Object o : geoms) { MasonGeometry gm = (MasonGeometry) o; int from =
	 * gm.getIntegerAttribute("FR"); int to = gm.getIntegerAttribute("TO");
	 * double speed = gm.getDoubleAttribute("SPEED_1"); double distance =
	 * gm.getDoubleAttribute("LENGTH_1"); double spop =
	 * gm.getDoubleAttribute("SPOP"); double cost =
	 * gm.getDoubleAttribute("COST"); double transportlevel =
	 * gm.getDoubleAttribute("TLEVEL_1"); double deaths =
	 * gm.getDoubleAttribute("DEATH_1"); System.out.println("pop weight: " +
	 * spop); EngDRoadInfo edgeinfo = new EngDRoadInfo(gm.geometry, from, to,
	 * speed, spop, distance, cost, transportlevel, deaths);
	 * 
	 * // build road network
	 * engdModelSim.roadNetwork.addEdge(engdModelSim.centroidList.get(from),
	 * engdModelSim.centroidList.get(to), edgeinfo);
	 * engdModelSim.roadNetwork.addEdge(engdModelSim.centroidList.get(to),
	 * engdModelSim.centroidList.get(from), edgeinfo); }
	 * 
	 * // addRedirects(); }
	 */

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

}
