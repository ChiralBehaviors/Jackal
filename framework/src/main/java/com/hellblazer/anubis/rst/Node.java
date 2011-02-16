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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final List<Channel> children = new CopyOnWriteArrayList<Channel>();
    private final Map<Integer, Channel> members;
    private final ThisChannel myChannel;
    private Channel parent;
    private int root;
    private final Executor protocolEvaluator = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running = new AtomicBoolean();
    private final Semaphore stateModified = new Semaphore(0);

    public Node(ThisChannel channel, Map<Integer, Channel> M) {
        myChannel = channel;
        members = new HashMap<Integer, Channel>(M.size());
        members.putAll(M);
        parent = myChannel;
        root = myChannel.getId();
    }

    /**
     * Add the channel as a child of the receiver in the tree
     * 
     * @param child
     *            - the channel to add as a child
     */
    public void addChild(Channel child) {
        children.add(child);
    }

    /**
     * Evaluate all the actions of the protocol, ensuring that the protocol will
     * be evaluated at least once. The evaluation of the protocol is
     * asynchronously processed in a seperate thread.
     */
    public void evaluateProtocol() {
        stateModified.release();
    }

    /**
     * Remove the channel from the list of receiver's children
     * 
     * @param child
     *            - the channel to remove
     */
    public void removeChild(Channel child) {
        children.remove(child);
    }

    /**
     * Start the protocol evaluation thread
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            protocolEvaluator.execute(evaluationAction());
        }
    }

    /**
     * Stop the protocol evaluation thread
     */
    public void stop() {
        if (running.compareAndSet(false, true)) {

        }
    }

    /**
     * The first action of the protocol. This action colors the node red if the
     * parent of the node is red, or the parent is not a member of the adjacent
     * set.
     * 
     * @return true if there was a change in state
     */
    boolean colorRed() {
        if (myChannel.isRed()) {
            return false;
        }
        if (parent.isRed() || !members.containsKey(parent.getId())) {
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
        root = myChannel.getId();
        myChannel.setRoot(root);
        return true;
    }

    /**
     * Evaluate the protocol's actions. All actions will be evaluated at least
     * once. The protocol will continue to evaluate until there are no state
     * changes resulting from the evaluation.
     */
    void evaluate() {
        boolean modified = true;
        while (modified) {
            modified |= colorRed() | disownParent() | merge();
        }
    }

    /**
     * The runnable which implements the protocol evaluation loop
     * 
     * @return the Runnable protocol evaluation action
     */
    Runnable evaluationAction() {
        return new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        stateModified.acquire();
                        evaluate();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
        };
    }

    /**
     * The third action of the protocol. This action merges the nodes in to the
     * tree. If the node detects a member that has a higher root id, the node
     * marks that value as the root and the member as the parent.
     * 
     * @return true if there was a change in state.
     */
    boolean merge() {
        if (myChannel.isRed()) {
            return false;
        }
        Channel newParent = parent;
        int newRoot = root;
        for (Channel channel : members.values()) {
            if (channel.isGreen()) {
                if (newRoot < channel.getRoot()) {
                    newRoot = channel.getRoot();
                    newParent = channel;
                }
            }
        }
        if (parent != newParent) {
            parent.removeChild(myChannel);
            parent = newParent;
            root = newRoot;
            myChannel.setRoot(root);
            parent.addChild(myChannel);
            return true;
        }
        return false;
    }
}
