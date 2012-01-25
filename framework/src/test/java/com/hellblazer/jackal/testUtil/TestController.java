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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.SocketOptions;

/**
 * 
 * @author hhildebrand
 * 
 */
public class TestController extends Controller {
    public int            cardinality;
    public CountDownLatch latch;

    public TestController(Identity partitionIdentity, int heartbeatTimeout,
                          int heartbeatInterval, SocketOptions socketOptions,
                          ExecutorService dispatchExecutor,
                          WireSecurity wireSecurity) throws IOException {
        super(partitionIdentity, heartbeatTimeout, heartbeatInterval,
              socketOptions, dispatchExecutor, wireSecurity);
    }

    @Override
    protected NodeData createNode(Heartbeat hb) {
        TestNode node = new TestNode(hb, this);
        node.cardinality = cardinality;
        node.latch = latch;
        return node;
    }

}