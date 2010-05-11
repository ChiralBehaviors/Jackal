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


import java.awt.Color;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.test.msg.GetStatsMsg;
import org.smartfrog.services.anubis.partition.test.msg.GetThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.test.msg.SetIgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.SetTimingMsg;
import org.smartfrog.services.anubis.partition.test.msg.StatsMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;


public class NodeData {
    private Controller     controller     = null;
    private View           view           = null;
    private Color          partitionColor = Color.lightGray;
    private View           partition      = new BitView();
    private int            leader         = -1;
    private Identity       nodeId         = null;
    private View           ignoring       = new BitView();
    private long           heartbeatInterval = 0;
    private long           timeout        = 0;
    private StatsMsg       stats          = null;
    private ThreadsMsg     threadsInfo    = null;
    private long           threadsInfoExpire = 0;

    private long           lastReceive    = 0;
    private long           lastHB         = 0;
    private TestConnection connection     = null;
    private NodeButton     button;
    private NodeFrame      window         = null;
    private MainConsoleFrame consoleFrame;
    private ColorAllocator colorAllocator;
    private Logger          log = Logger.getLogger(this.getClass().toString());


    public NodeData(HeartbeatMsg     hb,
                    MainConsoleFrame consoleFrame,
                    ColorAllocator   colorAllocator,
                    Controller       controller) {
        this.controller           = controller;
        this.colorAllocator       = colorAllocator;
        lastReceive               = System.currentTimeMillis();
        lastHB                    = hb.getTime();
        view                      = hb.getView();
        nodeId                    = hb.getSender();
        ConnectionAddress address = hb.getTestInterface();
        button                    = new NodeButton(nodeId, this);
        button.setBackground(Color.lightGray);
        button.setForeground(Color.black);
        this.consoleFrame = consoleFrame;
        consoleFrame.addNode(this);
        connectIfAvailable(address);
        //System.out.print("Heartbeat from new Node" + nodeId.id + " stamped " + lastHB);
        //long timeNow = System.currentTimeMillis();
        //if( timeNow < (lastHB-100) )
        //    System.out.println(" <=== this is " + (lastHB - timeNow) + "ms old!!!!!");
        //else
        //    System.out.println("");
    }

    public String     getViewString()      { return view.toBitSet().toString(); }
    public View       getView()            { return view; }
    public String     getPartitionString() { return partition.toBitSet().toString(); }
    public String     getIgnoringString()  { return ignoring.toString(); }
    public Identity   getIdentity()        { return nodeId; }
    public NodeButton getButton()          { return button; }


    private void connectIfAvailable(ConnectionAddress address) {

        if( address == null )
            return;

        connection = new TestConnection(address, this, nodeId, controller);
        if( connection.connected() ) {
            connection.sendObject( new SetTimingMsg(consoleFrame.getInterval(),
                                   consoleFrame.getTimeout() ) );
            connection.start();
        } else {
            connection = null;
        }
    }


    public void disconnected() {
        connection = null;
        colorAllocator.deallocate(partition, this);
        partitionColor = Color.lightGray;

        update();
    }


    public void heartbeat(HeartbeatMsg hb) {


        if( lastHB <= hb.getTime() ) {
            lastHB = hb.getTime();
            lastReceive = System.currentTimeMillis();
            if( (view.isStable() != hb.getView().isStable()) ||
                !view.equals(hb.getView()) ) {
                view   = hb.getView();
                update();
            }
            //System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + lastHB);
        } else {
            //System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + hb.getTime() + " <==== This is out of order!!!!");
        }
        if( connection == null )
            connectIfAvailable(hb.getTestInterface());
    }


    public void deliverObject(Object obj) {
        if(obj instanceof PartitionMsg) {
            PartitionMsg msg = (PartitionMsg)obj;
            partitionNotification(msg.partition, msg.leader);
        } else if( obj instanceof TimingMsg) {
            TimingMsg msg = (TimingMsg)obj;
            timing(msg.interval, msg.timeout);
        } else if( obj instanceof StatsMsg) {
            stats((StatsMsg)obj);
        } else if( obj instanceof IgnoringMsg) {
            ignoring((IgnoringMsg)obj);
        } else if( obj instanceof ThreadsMsg ) {
            threads((ThreadsMsg)obj);
        } else {
            if( log.isLoggable(Level.SEVERE) )
                log.severe("Unrecognised object received in test connection at console" + obj);
            connection.shutdown();
        }
    }


    private void handlePartitionMsg(PartitionMsg msg) {
        if( !this.partition.toBitSet().equals( partition.toBitSet() ) ) {
            colorAllocator.deallocate(this.partition, this);
            partitionColor = colorAllocator.allocate(partition, this);
        }

    }

    private void partitionNotification(View partition, int leader) {

        /**
         * if partition has changed membership deallocate current color
         * and then reallocate new color
         */
        if( !this.partition.toBitSet().equals( partition.toBitSet() ) ) {
            colorAllocator.deallocate(this.partition, this);
            partitionColor = colorAllocator.allocate(partition, this);
        }
        this.partition = partition;
        this.leader    = leader;
        update();
    }

    private void timing( long interval, long timeout) {
        this.heartbeatInterval = interval;
        this.timeout = timeout;
        update();
    }

    private void stats(StatsMsg stats) {
        this.stats = stats;
        update();
    }

    private void threads(ThreadsMsg threads) {
        threadsInfoExpire = System.currentTimeMillis() + 10000;
        threadsInfo = threads;
        update();
    }

    public void setIgnoring(String str) {
        StringTokenizer nodes = new StringTokenizer(str);
        BitView ignoring = new BitView();
        String token = "";

        if( nodes.countTokens() == 0 ) {
            setIgnoring(new BitView());
            return;
        }

        try {
            while( nodes.hasMoreTokens() ) {
                token = nodes.nextToken();
                // System.out.println("Looking at token: " + token);
                Integer inode = new Integer(token);
                int node = inode.intValue();
                if( node < 0 ) throw new NumberFormatException();
                ignoring.add(node);
            }
        }
        catch (NumberFormatException ex) {
            window.inputError("Not a correct node value: " + token);
            return;
        }

        setIgnoring(ignoring);
    }


    public void setIgnoring(View ignoring) {
        connection.sendObject(new SetIgnoringMsg(ignoring));
    }

    private void ignoring(IgnoringMsg ignoringMsg) {
        this.ignoring = ignoringMsg.ignoring;
        update();
    }

    private View partCompOrIgnoring(View globalView, View partition, View ignoring) {
        View complement = new BitView().copyView(globalView).subtract(partition);
        return new BitView().copyView(complement).merge(ignoring);
    }

    private View partOrIgnoring(View partition, View ignoring) {
        return new BitView().copyView(partition).merge(ignoring);
    }

    public void setIgnoringAsymPartition(View globalView, View partition) {
        if( connection == null )
            return;

        if( partition.contains(getIdentity()) ) {
            connection.sendObject(new SetIgnoringMsg(partCompOrIgnoring(globalView, partition, ignoring)));
        }
    }

    public void setIgnoringSymPartition(View globalView, View partition) {
        if( connection == null )
            return;

        if( partition.contains(getIdentity()) ) {
            connection.sendObject(new SetIgnoringMsg(partCompOrIgnoring(globalView, partition, ignoring)));
        } else {
            connection.sendObject(new SetIgnoringMsg(partOrIgnoring(partition, ignoring)));
        }
    }

    public void openWindow() {
       if( window == null ) {
           window = new NodeFrame(this);
           window.setTitle("Node: " + nodeId.toString());
           update();
           window.setVisible(true);
       }
    }

    public void closeWindow() {
        if( window != null ) {
            window.setVisible(false);
            window.dispose();
            window = null;
        }
    }

    private void update() {
        if( threadsInfoExpire < System.currentTimeMillis() )
            threadsInfo = null;
        if( window != null ) {
            window.update(partition, view, leader, ignoring, heartbeatInterval, timeout, stats, threadsInfo);
        }
        button.setBackground(partitionColor);
        if( partition.isStable() ) button.setForeground(Color.black);
        else button.setForeground(Color.yellow);
    }


    public void removeNode() {
        if( connection != null && connection.connected() )
            connection.shutdown();
        closeWindow();
        consoleFrame.removeNode(this);
    }

    public boolean olderThan(long oldTime) {
        return lastReceive < oldTime;
    }

    public void setTiming(long interval, long timeout) {
        if( connection == null )
            return;
        connection.sendObject(new SetTimingMsg(interval, timeout));
    }

    public void getStats() {
        if( connection == null )
            return;
        connection.sendObject(new GetStatsMsg());
    }

    public void getThreads() {
        if( connection == null )
            return;
        connection.sendObject(new GetThreadsMsg());
    }

}
