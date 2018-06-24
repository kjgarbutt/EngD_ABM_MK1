package engd_abm;

import java.awt.Color;

import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;

public class EngDModelWithUI extends GUIState {

	public Display2D display;
	public JFrame displayFrame;

	GeomVectorFieldPortrayal osviPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal boundaryPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal flood3Portrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal flood2Portrayal = new GeomVectorFieldPortrayal();
	GeomVectorFieldPortrayal agentsPortrayal = new GeomVectorFieldPortrayal();

	public EngDModelWithUI(EngDModel sim) {
		super(sim);
	}

	@Override
	public void init(Controller c) {
		super.init(c);
		display = new Display2D(750, 520, this);

		display.attach(osviPortrayal, "OSVI");
		display.attach(boundaryPortrayal, "Boundary");
		display.attach(roadsPortrayal, "Roads");
		display.attach(flood2Portrayal, "Flood Zone #2");
		display.attach(flood3Portrayal, "Flood Zone #3");
		display.attach(agentsPortrayal, "Agents", true);

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
		setupPortrayals();

		EngDModel engdModelSim = (EngDModel) state;

	}

	public void setupPortrayals() {
		System.out.println("Setting up Portrayals...");

		roadsPortrayal.setField(((EngDModel) this.state).roads);
		roadsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.DARK_GRAY,
				0.0005, false));

		flood2Portrayal.setField(((EngDModel) this.state).flood2);
		flood2Portrayal.setPortrayalForAll(new GeomPortrayal(Color.BLUE, true));

		flood3Portrayal.setField(((EngDModel) this.state).flood3);
		flood3Portrayal.setPortrayalForAll(new GeomPortrayal(Color.CYAN, true));

		boundaryPortrayal.setField(((EngDModel) this.state).boundary);
		boundaryPortrayal.setPortrayalForAll(new GeomPortrayal(Color.YELLOW,
				true));

		agentsPortrayal.setField(EngDModelBuilder.agents);
		agentsPortrayal.setPortrayalForAll(new GeomPortrayal(Color.MAGENTA,
				150, true));

		display.reset();
		display.setBackdrop(Color.WHITE);
		display.repaint();
	}

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