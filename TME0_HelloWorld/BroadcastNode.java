import io.jbotsim.core.Topology;
import io.jbotsim.ui.JViewer;

import io.jbotsim.core.Node;
import io.jbotsim.core.Color;
import io.jbotsim.core.Message;

public class BroadcastNode extends Node {
    private boolean informed;

    @Override
    public void onStart() {
        informed = false;
        setColor(null);
    }

    @Override
    public void onSelection() {
        informed = true;
        setColor(Color.RED);
        sendAll(new Message(this.getID()));
    }

    @Override
    public void onMessage(Message message) {
        System.out.println("Node " + this.getID() + " got message: " + message.getContent());

        if (!informed) {
            informed = true;
            setColor(Color.RED);
            sendAll(new Message(this.getID()));
        }
    }
}
