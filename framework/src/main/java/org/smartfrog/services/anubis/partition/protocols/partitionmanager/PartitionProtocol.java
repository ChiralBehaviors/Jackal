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
package org.smartfrog.services.anubis.partition.protocols.partitionmanager;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;

public class PartitionProtocol {

    private AtomicBoolean          changed       = new AtomicBoolean(false);
    private ConnectionSet          connectionSet = null;
    private final Identity         identity;
    private volatile Identity      leader        = null;
    private final PartitionManager partitionMgr;
    private volatile boolean       terminated    = false;
    private final BitView          view          = new BitView();

    public PartitionProtocol(Identity identity, PartitionManager partitionMgr) {
        this.identity = identity;
        this.partitionMgr = partitionMgr;
        this.partitionMgr.setPartitionProtocol(this);
        leader = identity;
    }

    /**
     * Changed view is called during regular connection set checks if the
     * connection set has changed.
     * 
     * removeCompletement() will remove nodes from the partition if they are not
     * in the connection set. If this happens then we note that there have been
     * changes and elect a new leader. The result is that the partition can only
     * contract when it is unstable (i.e. when there is a view change).
     */
    public void changedView() {
        if (view.removeComplement(connectionSet.getView()) || view.isStable()) {
            changed.set(true);
        }
        view.setTimeStamp(View.undefinedTimeStamp);
        view.destablize();
        leader = connectionSet.electLeader(view);
    }

    /**
     * Establish a connection to the remote node
     * 
     * @param id
     *            - id of remote node
     * @return - the message connection
     */
    public MessageConnection connect(int id) {
        return connectionSet.connect(id);
    }

    /**
     * Force the destabilization of the partition.
     */
    public void destabilize() {
        connectionSet.destabilize();
    }

    /**
     * Answer the identity of the leader
     * 
     * @return
     */
    public Identity getLeader() {
        return leader;
    }

    public InetAddress getNodeAddress(int id) {
        return connectionSet.getNodeAddress(id);
    }

    /**
     * Issue notifications from the partition manager.
     */
    public void notifyChanges() {
        if (changed.compareAndSet(true, false)) {
            partitionMgr.notify(view, leader.id);
        }
    }

    public void receiveObject(Object obj, Identity id, long time) {
        if (terminated) {
            return;
        }
        partitionMgr.receiveObject(obj, id.id, time);
    }

    /**
     * remove a node from the partition - if it is there. If it was there then
     * destablise and elect a new leader.
     * 
     * @param id
     */
    public void remove(Identity id) {
        if (view.remove(id.id)) {
            changed.set(true);
            view.setTimeStamp(View.undefinedTimeStamp);
            view.destablize();
            leader = connectionSet.electLeader(view);
        }
    }

    /**
     * copy the stable view from the connection set - includes the time stamp in
     * the copy. Note that a partition can expand when it is stable (the
     * connectionSet may be bigger than the partition).
     * 
     * Note that the node that wins the leader election at stability believed it
     * was leader prior to stability.
     */
    public void stableView() {
        changed.set(true);
        view.copyView(connectionSet.getView());
        leader = connectionSet.electLeader(view);
    }

    public void start() {
        view.add(identity);
        view.stablize();
        view.setTimeStamp(identity.epoch);
        partitionMgr.notify(view, leader.id);
    }

    @PreDestroy
    public void terminate() {
        terminated = true;
    }

    void setConnectionSet(ConnectionSet connectionSet) {
        this.connectionSet = connectionSet;
    }
}
