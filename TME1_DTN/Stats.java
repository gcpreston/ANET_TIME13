import java.awt.*;
import java.util.*;
import java.util.List;

import io.jbotsim.core.event.*;
import io.jbotsim.core.*;

public class Stats implements ClockListener {

	private Topology tp;
    private List<Integer> transmissions;

	public Stats(Topology tp) {
		this.tp = tp;
        this.transmissions = new ArrayList<Integer>();
	}

    @Override
    public void onClock() {
        List<Node> nodes = tp.getNodes();
        int transmissionCount = 0;

        for (Node node : nodes) {
            if (!(boolean)node.getProperty("data")) {
                transmissionCount++;
            }
        }

        this.transmissions.add(transmissionCount);

        // Print if the value changed
        if (this.transmissions.size() > 1) {
            int prevTransmissionCount = this.transmissions.get(this.transmissions.size() - 2);

            if (transmissionCount != prevTransmissionCount) {
                System.out.println("Updated transmission count on tick " + tp.getTime() + ": " + transmissionCount);
            }
        }
    }
}
