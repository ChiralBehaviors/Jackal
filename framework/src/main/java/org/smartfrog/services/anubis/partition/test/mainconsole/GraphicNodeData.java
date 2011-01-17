package org.smartfrog.services.anubis.partition.test.mainconsole;

import java.awt.Color;
import java.util.StringTokenizer;

import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

public class GraphicNodeData extends NodeData {
    private NodeButton button;
    private ColorAllocator colorAllocator;
    private Color partitionColor = Color.lightGray;
    private NodeFrame window;

    public GraphicNodeData(HeartbeatMsg hb, ColorAllocator colorAllocator,
                           Controller controller) {
        super(hb, controller);
        this.colorAllocator = colorAllocator;
        button = new NodeButton(nodeId, this);
        button.setOpaque(true);
        button.setBackground(Color.lightGray);
        button.setForeground(Color.black);
    }

    @Override
    public void disconnected() {
        colorAllocator.deallocate(partition, this);
        partitionColor = Color.lightGray;
        super.disconnected();
    }

    public NodeButton getButton() {
        return button;
    }

    @Override
    protected void partitionNotification(View partition, int leader) {
        /**
         * if partition has changed membership deallocate current color and then
         * reallocate new color
         */
        if (!this.partition.toBitSet().equals(partition.toBitSet())) {
            colorAllocator.deallocate(this.partition, this);
            partitionColor = colorAllocator.allocate(partition, this);
        }
        super.partitionNotification(partition, leader);
    }

    @Override
    protected void update() {
        super.update();
        if (window != null) {
            window.update(partition, view, leader, ignoring, heartbeatInterval,
                          timeout, stats, threadsInfo);
        }
        button.setBackground(partitionColor);
        if (partition.isStable()) {
            button.setForeground(Color.black);
        } else {
            button.setForeground(Color.yellow);
        }
    }

    public void closeWindow() {
        if (window != null) {
            window.setVisible(false);
            window.dispose();
            window = null;
        }
    }

    public void openWindow() {
        if (window == null) {
            window = new NodeFrame(this);
            window.setTitle("Node: " + nodeId.toString());
            update();
            window.setVisible(true);
        }
    }

    @Override
    public void removeNode() {
        super.removeNode();
        closeWindow();
    }

    public void setIgnoring(String str) {
        StringTokenizer nodes = new StringTokenizer(str);
        BitView ignoring = new BitView();
        String token = "";

        if (nodes.countTokens() == 0) {
            setIgnoring(new BitView());
            return;
        }

        try {
            while (nodes.hasMoreTokens()) {
                token = nodes.nextToken();
                // System.out.println("Looking at token: " + token);
                Integer inode = new Integer(token);
                int node = inode.intValue();
                if (node < 0) {
                    throw new NumberFormatException();
                }
                ignoring.add(node);
            }
        } catch (NumberFormatException ex) {
            window.inputError("Not a correct node value: " + token);
            return;
        }

        setIgnoring(ignoring);
    }
}
