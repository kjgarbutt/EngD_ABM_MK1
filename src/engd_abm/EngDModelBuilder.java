package engd_abm;

import java.io.File;
import java.net.URL;

import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;

import com.vividsolutions.jts.geom.Envelope;

class EngDModelBuilder {
	public static EngDModel engdModelSim;

	public static void initializeWorld(EngDModel sim) {

		System.out.println("Initializing model world...");
		engdModelSim = sim;

		engdModelSim.world_height = 500;
		engdModelSim.world_width = 500;

		engdModelSim.roads = new GeomVectorField(sim.world_width,
				sim.world_height);
		
		engdModelSim.flood2 = new GeomVectorField(sim.world_width,
				sim.world_height);
		
		engdModelSim.flood3 = new GeomVectorField(sim.world_width,
				sim.world_height);
		
		String[] shapeFiles = { EngDParameters.ROAD_SHP, EngDParameters.FLOOD2_SHP,
				EngDParameters.FLOOD3_SHP };
		GeomVectorField[] vectorFields = { engdModelSim.roads, engdModelSim.flood2, engdModelSim.flood3 };
		System.out.println("Starting to read shapefiles...");
		
		readInShapefile(shapeFiles, vectorFields);

		// expand the extent to include all features
		Envelope MBR = engdModelSim.roads.getMBR();
		MBR.expandToInclude(engdModelSim.flood2.getMBR());
		MBR.expandToInclude(engdModelSim.flood3.getMBR());

		engdModelSim.roads.setMBR(MBR);
		engdModelSim.flood2.setMBR(MBR);
		engdModelSim.flood3.setMBR(MBR);

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
