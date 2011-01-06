/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.test.mainconsole;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.smartfrog.services.anubis.Anubis;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

import com.hellblazer.anubis.annotations.Deployed;

public class Controller {

    private MulticastAddress address;
    private AsymetryReportFrame asymetryReport = null;
    private long checkPeriod = 1000;
    protected ColorAllocator colorAllocator = new ColorAllocator();
    private MainConsoleFrame consoleFrame;
    private long expirePeriod = 10000;
    private BitView globalView = new BitView();
    private long heartbeatInterval = 1000;
    protected boolean headless = false;
    private long heartbeatTimeout = 2000;
    private Identity identity;
    private Map<Identity, NodeData> nodes = new HashMap<Identity, NodeData>();
    private Snoop snoop;
    private TimerTask task;
    private Timer timer;

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
            if (headless) {
                throw new IllegalArgumentException(ex);
            }
            consoleFrame.inputError("Unknown: " + token);
            return;
        }

        asymPartition(partition);
    }

    public synchronized void asymPartition(BitView partition) {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData node = iter.next();
            if (partition.contains(node.getIdentity())) {
                node.setIgnoringAsymPartition(globalView, partition);
            }
        }
    }

    public synchronized void checkNodes() {
        long timeNow = System.currentTimeMillis();
        long expireTime = timeNow - expirePeriod;
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData nodeData = iter.next();
            if (nodeData.olderThan(expireTime)) {
                iter.remove();
                globalView.remove(nodeData.getIdentity());
                nodeData.removeNode();
            }
        }
    }

    public synchronized void clearNodes() {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData nodeData = iter.next();
            nodeData.removeNode();
            iter.remove();
        }
        globalView = new BitView();
    }

    public synchronized void clearPartitions() {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setIgnoring(new BitView());
        }
    }

    public synchronized void deliverHeartbeat(HeartbeatMsg hb) {
        NodeData nodeData = nodes.get(hb.getSender());
        if (nodeData != null) {
            nodeData.heartbeat(hb);
            return;
        }
        nodeData = createNode(hb);
        if (!headless) {
            consoleFrame.addNode(nodeData);
        }
        nodes.put(hb.getSender(), nodeData);
        globalView.add(hb.getSender());
    }

    public synchronized void deliverObject(Object obj, NodeData node) {
        node.deliverObject(obj);
    }

    @Deployed
    public synchronized void deploy() throws IOException,
                                     ClassNotFoundException,
                                     InstantiationException,
                                     IllegalAccessException,
                                     UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        timer.schedule(getTask(), checkPeriod, checkPeriod);
        snoop = new Snoop(
                          "Anubis: Partition Manager Test Console heartbeat snoop",
                          address, identity, this);
        if (!headless) {
            consoleFrame = new MainConsoleFrame(this);
            consoleFrame.setTitle("Partition Manager Test Controller - "
                                  + Anubis.version);
            consoleFrame.initialiseTiming(heartbeatInterval, heartbeatTimeout);
        }
        snoop.start();
    }

    public synchronized void disconnectNode(NodeData node) {
        node.disconnected();
    }

    public MulticastAddress getAddress() {
        return address;
    }

    public long getCheckPeriod() {
        return checkPeriod;
    }

    public long getExpirePeriod() {
        return expirePeriod;
    }

    public long getHeartbeatInterval() {
        if (!headless) {
            heartbeatInterval = consoleFrame.getInterval();
        }
        return heartbeatInterval;
    }

    public long getHeartbeatTimeout() {
        if (!headless) {
            heartbeatTimeout = consoleFrame.getTimeout();
        }
        return heartbeatTimeout;
    }

    public Identity getIdentity() {
        return identity;
    }

    public NodeData getNode(Identity id) {
        return nodes.get(id);
    }

    public Collection<? extends NodeData> getNodes() {
        return nodes.values();
    }

    public Timer getTimer() {
        return timer;
    }

    public boolean isHeadless() {
        return headless;
    }

    public String nodesToString() {
        String str = "Nodes Table\n===========\n";
        Iterator<Map.Entry<Identity, NodeData>> iter = nodes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Identity, NodeData> entry = iter.next();
            str += entry.getKey() + "--->" + entry.getValue();
        }
        return str;
    }

    public synchronized void removeAsymetryReport() {
        asymetryReport = null;
    }

    public void removeNode(NodeData nodeData) {
        if (!headless) {
            consoleFrame.removeNode(nodeData);
        }
    }

    public void setAddress(MulticastAddress address) {
        this.address = address;
    }

    public void setCheckPeriod(long checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    public synchronized void setExpirePeriod(long expirePeriod) {
        this.expirePeriod = expirePeriod;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public void setHeartbeatTimeout(long heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public synchronized void setTiming(long interval, long timeout) {

        /** set local timers **/
        checkPeriod = interval;
        expirePeriod = interval * timeout;
        if (task != null) {
            task.cancel();
        }
        timer.schedule(getTask(), checkPeriod, checkPeriod);

        /** set node's timers **/
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setTiming(interval, timeout);
        }
    }

    public synchronized void showAsymetryReport() {
        if (asymetryReport == null) {
            asymetryReport = new AsymetryReportFrame(this, nodes, identity);
        } else {
            asymetryReport.recalculate(nodes);
        }
    }

    @PostConstruct
    public synchronized void start() {
        if (!headless) {
            consoleFrame.setVisible(true);
        }
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
            if (headless) {
                throw new IllegalArgumentException(ex);
            }
            consoleFrame.inputError("Unknown: " + token);
            return;
        }

        symPartition(partition);
    }

    public synchronized void symPartition(BitView partition) {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setIgnoringSymPartition(globalView, partition);
        }
    }

    @PreDestroy
    public synchronized void terminate() {
        snoop.shutdown();
        if (task != null) {
            task.cancel();
        }
        clearNodes();
        if (!headless) {
            consoleFrame.setVisible(false);
            consoleFrame.dispose();
            consoleFrame = null;
            System.exit(0);
        }
    }

    private synchronized TimerTask getTask() {
        task = new TimerTask() {

            @Override
            public void run() {
                checkNodes();
            }
        };
        return task;
    }

    protected NodeData createNode(HeartbeatMsg hb) {
        NodeData nodeData;
        nodeData = new NodeData(hb, colorAllocator, this, headless);
        return nodeData;
    }
}
