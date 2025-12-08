import java.awt.*;
import java.util.*;
import java.util.List;

import io.jbotsim.core.event.*;
import io.jbotsim.core.*;

public class Stats implements ClockListener {
	
	private Topology tp;

	public Stats(Topology tp) {
		this.tp = tp;
	}

    @Override
    public void onClock() {
        List<Node> nodes = tp.getNodes();
        for(Node node : nodes)
        {
        	
        }
    }

}
