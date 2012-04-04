/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.awt.Color;
import java.util.StringTokenizer;

import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class GraphicNodeData extends NodeData {
    private final NodeButton     button;
    private final ColorAllocator colorAllocator;
    private Color                partitionColor = Color.lightGray;
    private NodeFrame            window;

    public GraphicNodeData(Heartbeat hb, ColorAllocator colorAllocator,
                           Controller controller) {
        super(hb, controller);
        this.colorAllocator = colorAllocator;
        button = new NodeButton(nodeId, this);
        button.setOpaque(true);
        button.setBackground(Color.lightGray);
        button.setForeground(Color.black);
    }

    public void closeWindow() {
        if (window != null) {
            window.setVisible(false);
            window.dispose();
            window = null;
        }
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
        if (button != null) {
            button.setBackground(partitionColor);
            if (partition.isStable()) {
                button.setForeground(Color.black);
            } else {
                button.setForeground(Color.yellow);
            }
        }
    }
}
