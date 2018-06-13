package engd_abm;

import sim.util.geo.MasonGeometry;
import java.util.ArrayList;
public class Polygon extends MasonGeometry	{
    String soc;

    ArrayList<Polygon> neighbors;

    public Polygon()	{
        super();
        neighbors = new ArrayList<Polygon>();
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