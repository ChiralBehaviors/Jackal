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
package com.hellblazer.anubis.rst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The logic for maintaining a node in a rooted spanning tree. The protocol is
 * derived from "Fault-Tolerant Reconfiguration of Trees and Rings in
 * Distributed Systems", Anish Arora and Ashish Singai
 * <p>
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Node {
    private final List<Channel> children = new ArrayList<Channel>();
    private final Map<Integer, Channel> members;
    private final Channel myChannel;
    private Channel parent;
    private Channel root;
    private ReentrantLock stateLock = new ReentrantLock();

    public Node(Channel channel, Map<Integer, Channel> M) {
        myChannel = channel;
        members = new HashMap<Integer, Channel>(M.size());
        members.putAll(M);
        parent = myChannel;
        root = parent;
    }

    /**
     * Evaluate the protocol.
     * 
     * @throws InterruptedException
     */
    public void evaluate() throws InterruptedException {
        final ReentrantLock myLock = stateLock;
        myLock.lockInterruptibly();
        try {
            boolean modified = true;
            while (modified) {
                modified |= colorRed() | disownParent() | merge();
            }
        } finally {
            myLock.unlock();
        }
    }

    void addChild(Channel child) {
        children.add(child);
    }

    void removeChild(Channel child) {
        children.remove(child);
    }

    /**
     * The first action of the protocol. This action colors the node red if the
     * parent of the node is red.
     * 
     * @return true if there was a change in state
     */
    boolean colorRed() {
        if (myChannel.isRed()) {
            return false;
        }
        if (parent.isRed() || members.get(parent.getId()) == null) {
            myChannel.markRed();
            return true;
        }
        return false;
    }

    /**
     * The second action of the protocol. This action disowns the parent of the
     * node if the node has no children. The node then elects itself the root of
     * the tree, coloring the node to green.
     * 
     * @return true if there was a change in state
     */
    boolean disownParent() {
        if (myChannel.isGreen()) {
            return false;
        }
        if (!children.isEmpty()) {
            return false;
        }
        myChannel.markGreen();
        parent = myChannel;
        root = myChannel;
        return true;
    }

    /**
     * This action merges the nodes in to the tree. If the node detects a member
     * that has a higher root id, the node marks that value as the root and the
     * member as the parent.
     * 
     * @return true if there was a change in state.
     */
    boolean merge() {
        if (myChannel.isRed()) {
            return false;
        }
        Channel currentParent = parent;
        Channel currentRoot = root;
        for (Channel channel : members.values()) {
            if (channel.isGreen()) {
                if (currentRoot.getId() < channel.getRoot()) {
                    currentRoot = members.get(channel.getRoot());
                    currentParent = channel;
                }
            }
        }
        if (parent != currentParent) {
            parent.removeChild(myChannel);
            parent = currentParent;
            root = currentRoot;
            parent.addChild(myChannel);
            return true;
        }
        return false;
    }
}
