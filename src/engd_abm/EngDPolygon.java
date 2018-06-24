package engd_abm;

import sim.util.geo.MasonGeometry;
import java.util.ArrayList;
public class EngDPolygon extends MasonGeometry	{
    String soc;

    ArrayList<EngDPolygon> neighbors;

    public EngDPolygon()	{
        super();
        neighbors = new ArrayList<EngDPolygon>();
    }

    public void init()	{
        soc = getStringAttribute("RankColN");
    }

    String getSoc()	{
        if (soc == null)	{
            init();
        }
        return soc;
    }
}