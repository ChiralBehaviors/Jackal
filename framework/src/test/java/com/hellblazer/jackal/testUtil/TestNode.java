/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.testUtil;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

/**
 * 
 * @author hhildebrand
 * 
 */
public class TestNode extends NodeData {
    static final Logger   log = LoggerFactory.getLogger(TestNode.class);

    public int            cardinality;
    public CountDownLatch latch;

    public TestNode(Heartbeat hb, Controller controller) {
        super(hb, controller);
    }

    @Override
    protected void partitionNotification(View partition, int leader) {
        log.trace("Partition notification: " + partition);
        super.partitionNotification(partition, leader);
        if (partition.isStable() && partition.cardinality() == cardinality) {
            latch.countDown();
        }
    }
}