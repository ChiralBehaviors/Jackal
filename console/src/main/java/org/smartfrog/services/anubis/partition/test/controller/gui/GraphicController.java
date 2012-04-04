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

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.SocketOptions;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class GraphicController extends Controller {
    private ColorAllocator      colorAllocator = new ColorAllocator();

    private MainConsoleFrame    consoleFrame;
    private AsymetryReportFrame asymetryReport = null;

    public GraphicController(Identity partitionIdentity, int heartbeatTimeout,
                             int heartbeatInterval,
                             SocketOptions socketOptions,
                             ExecutorService dispatchExecutor,
                             WireSecurity wireSecurity) throws IOException {
        super(partitionIdentity, heartbeatTimeout, heartbeatInterval,
              socketOptions, dispatchExecutor, wireSecurity);
    }

    public synchronized void asymPartition(String nodeStr) {

        StringTokenizer tokens = new StringTokenizer(nodeStr);
        BitView partition = new BitView();
        String token = "";

        if (tokens.countTokens() == 0) {
            return;
        }

        try {
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                partition.add(new Integer(token).intValue());
            }
        } catch (NumberFormatException ex) {
            consoleFrame.inputError("Unknown: " + token);
        }
        asymPartition(partition);
    }

    @Override
    public synchronized void deploy() throws IOException {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (UnsupportedLookAndFeelException e) {
            throw new IllegalStateException(e);
        }
        super.deploy();
        consoleFrame = new MainConsoleFrame(this);
        consoleFrame.setTitle("Partition Manager Test Controller");
        consoleFrame.initialiseTiming(heartbeatInterval, heartbeatTimeout);
    }

    @Override
    public long getHeartbeatInterval() {
        heartbeatInterval = consoleFrame.getInterval();
        return super.getHeartbeatInterval();
    }

    @Override
    public long getHeartbeatTimeout() {
        heartbeatTimeout = consoleFrame.getTimeout();
        return super.getHeartbeatTimeout();
    }

    public synchronized void removeAsymetryReport() {
        asymetryReport = null;
    }

    @Override
    public void removeNode(NodeData nodeData) {
        consoleFrame.removeNode((GraphicNodeData) nodeData);
    }

    public synchronized void showAsymetryReport() {
        if (asymetryReport == null) {
            asymetryReport = new AsymetryReportFrame(this, nodes, identity);
        } else {
            asymetryReport.recalculate(nodes);
        }
    }

    @Override
    @PostConstruct
    public synchronized void start() throws IOException {
        super.start();
        consoleFrame.setVisible(true);
    }

    public synchronized void symPartition(String nodeStr) {

        StringTokenizer tokens = new StringTokenizer(nodeStr);
        BitView partition = new BitView();
        String token = "";

        if (tokens.countTokens() == 0) {
            return;
        }

        try {
            while (tokens.hasMoreTokens()) {
                token = tokens.nextToken();
                partition.add(new Integer(token).intValue());
            }
        } catch (NumberFormatException ex) {
            consoleFrame.inputError("Unknown: " + token);
            return;
        }

        symPartition(partition);
    }

    @Override
    @PreDestroy
    public synchronized void terminate() {
        super.terminate();
        consoleFrame.setVisible(false);
        consoleFrame.dispose();
        consoleFrame = null;
        System.exit(0);
    }

    @Override
    protected void addNode(Heartbeat hb, NodeData nodeData) {
        consoleFrame.addNode((GraphicNodeData) nodeData);
        super.addNode(hb, nodeData);
    }

    @Override
    protected NodeData createNode(Heartbeat hb) {
        NodeData nodeData;
        nodeData = new GraphicNodeData(hb, colorAllocator, this);
        return nodeData;
    }
}
