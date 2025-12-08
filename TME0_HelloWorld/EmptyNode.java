import io.jbotsim.core.Topology;
import io.jbotsim.ui.JViewer;

import io.jbotsim.core.Node;
import io.jbotsim.core.Color;
import io.jbotsim.core.Message;

public class EmptyNode extends Node{
    @Override
    public void onStart() {
        // JBotSim executes this method on each node upon initialization
    }

    @Override
    public void onSelection() {
        // JBotSim executes this method on a selected node
    }

    @Override
    public void onClock() {
        // JBotSim executes this method on each node in each round
    }

    @Override
    public void onMessage(Message message) {
        // JBotSim executes this method on a node every time it receives a message
    }
}
