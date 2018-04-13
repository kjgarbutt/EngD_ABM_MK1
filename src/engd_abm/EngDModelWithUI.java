package engd_abm;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.continuous.ContinuousPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.OvalPortrayal2D;

public class EngDModelWithUI extends GUIState {

	// public EngDModel model;
	public Display2D display;
	public JFrame displayFrame;
	
	GeomVectorFieldPortrayal lsoaPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal flood3Portrayal = new GeomVectorFieldPortrayal();
    GeomVectorFieldPortrayal flood2Portrayal = new GeomVectorFieldPortrayal();
    ContinuousPortrayal2D agentsPortrayal = new ContinuousPortrayal2D();

	public EngDModelWithUI(EngDModel sim) {
		super(sim);
		//super(new EngDModel(System.currentTimeMillis()));
	}

	public void init(Controller c) {
		super.init(c);

		//((Console) c).setSize(350, 80);
		//((Console) c).setLocation(0, 520);
		display = new Display2D(750, 520, this);

		display.attach(lsoaPortrayal, "LSOA");
		display.attach(roadsPortrayal, "Roads");
		display.attach(flood2Portrayal, "Flood Zone #2");
        display.attach(flood3Portrayal, "Flood Zone #3");
        display.attach(agentsPortrayal, "Agents");

		displayFrame = display.createFrame();
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		displayFrame.setSize(800, 600);
		display.setBackdrop(Color.WHITE);
		displayFrame.setTitle("EngD ABM Model");
	}

	@Override
	public void start() {
		super.start();
		System.out.println("start()");
		System.out.println("Setting up Portrayals...");
		setupPortrayals();
		//setupMovingPortrayals();

	}

	public void setupPortrayals() {

		EngDModel engDModelWorld = (EngDModel)state;
		
		roadsPortrayal.setField(engDModelWorld.roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal
        		(Color.DARK_GRAY, 0.0005, false));
        System.out.println("Setting up roadsPortrayal...");
        
		flood2Portrayal.setField(engDModelWorld.flood2);
		flood2Portrayal.setPortrayalForAll(new GeomPortrayal(Color.BLUE, true));
		System.out.println("Setting up flood2Portrayal...");

		flood3Portrayal.setField(engDModelWorld.flood3);
		flood3Portrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN, true));
		System.out.println("Setting up flood3Portrayal...");
		
		lsoaPortrayal.setField(engDModelWorld.lsoa);
		lsoaPortrayal.setPortrayalForAll(new GeomPortrayal
        		(Color.LIGHT_GRAY, 0.0005, false));
		System.out.println("Setting up lsoaPortrayal...");
	}
	
	/*
	public void setupMovingPortrayals() {
		agentsPortrayal.setField(((EngDModel) this.state).world);
		agentsPortrayal.setPortrayalForAll(new OvalPortrayal2D() {
			@Override
			public void draw(Object object, Graphics2D graphics, DrawInfo2D info) {

				EngDAgent agents = (EngDAgent) object;
				if (agents.getStatus() == EngDConstants.DEAD)
					paint = Color.RED;
				else
					paint = Color.GREEN;
				// super.draw(object, graphics, info);
				super.filled = true;
				super.scale = 5;
				super.draw(object, graphics, info);
			}
		});

		display.reset();
		display.setBackdrop(Color.WHITE);
		display.repaint();
	}
	*/

	@Override
	public void quit() {
		System.out.println("EngDModelWithUI quitting...");
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null;
		display = null;

	}

	public static void main(String[] args) {
		EngDModelWithUI ebUI = new EngDModelWithUI(new EngDModel(
				System.currentTimeMillis()));
		Console c = new Console(ebUI);
		c.setVisible(true);
	}
}

// THIS WORKS! TOO SCARED TO DELETE AT THE MOMENT!

/*
 * package engd_abm_mk1;
 * 
 * 
 * import java.awt.Color;
 * 
 * import javax.swing.JFrame;
 * 
 * import sim.app.geo.sickStudents.SickStudentsModel; import
 * sim.display.Console; import sim.display.Controller; import
 * sim.display.Display2D; import sim.display.GUIState; import
 * sim.engine.SimState; import sim.portrayal.continuous.ContinuousPortrayal2D;
 * import sim.portrayal.geo.GeomPortrayal; import
 * sim.portrayal.geo.GeomVectorFieldPortrayal;
 * 
 * public class EngDModelWithUI extends GUIState {
 * 
 * public EngDModel model; public Display2D display; public JFrame displayFrame;
 * GeomVectorFieldPortrayal flood2Portrayal = new GeomVectorFieldPortrayal();
 * 
 * public EngDModelWithUI() { super(new EngDModel(System.currentTimeMillis()));
 * model = (EngDModel) state; } public EngDModelWithUI(EngDModel state) {
 * super(state); model = (EngDModel) state; }
 * 
 * public void init(Controller c) { super.init(c);
 * 
 * display = new Display2D(800, 600, this); displayFrame =
 * display.createFrame(); displayFrame.setTitle("EngD ABM Model");
 * displayFrame.setVisible(true); display.attach(flood2Portrayal,
 * "Flood Zone #2"); display.setBackdrop(Color.WHITE);
 * c.registerFrame(displayFrame);
 * 
 * ((Console) controller).setSize(380, 540); }
 * 
 * public void start() { super.start(); System.out.println("start()");
 * System.out.println("Setting up Portrayals..."); setupPortrayals();
 * 
 * }
 * 
 * public void load(SimState state) { super.load(state);
 * System.out.println("load()"); setupPortrayals();
 * 
 * }
 * 
 * public void setupPortrayals() {
 * 
 * flood2Portrayal.setField(EngDModel.flood2);
 * flood2Portrayal.setPortrayalForAll(new GeomPortrayal (Color.BLUE, true));
 * System.out.println("Setting up flood2Portrayal...");
 * 
 * display.reset(); display.setBackdrop(Color.WHITE); display.repaint(); }
 * 
 * @Override public void quit() {
 * System.out.println("EngDModelWithUI quitting..."); super.quit();
 * 
 * if (displayFrame != null) displayFrame.dispose(); displayFrame = null;
 * display = null;
 * 
 * }
 * 
 * public static void main(String[] args) { new
 * EngDModelWithUI().createController(); } }
 */
