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
package com.hellblazer.anubis.partition.coms.udp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.net.InetSocketAddress;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class HeartbeatTest extends TestCase {

    public void testSmoke() throws Exception {
        HeartbeatReceiver hbReceiver = mock(HeartbeatReceiver.class);
        Identity id = new Identity(6, 6, 6);
        WireSecurity ws = new NoSecurityImpl();
        InetSocketAddress sa = new InetSocketAddress(2033);
        Heartbeat heartbeat = new Heartbeat(hbReceiver, id, ws, sa);
        try {
            heartbeat.addMember(sa);
            InetSocketAddress ca = new InetSocketAddress(sa.getAddress(),
                                                         sa.getPort());
            HeartbeatMsg msg = new HeartbeatMsg(id, ca);
            msg.setCandidate(id);
            msg.setIsPreferred(false);
            msg.setMsgLinks(new NodeIdSet());
            msg.setOrder(1);
            msg.setTime(System.currentTimeMillis());
            msg.setView(new BitView());
            msg.setViewNumber(1);
            ArgumentCaptor<HeartbeatMsg> received = ArgumentCaptor.forClass(HeartbeatMsg.class);
            heartbeat.start();
            heartbeat.sendHeartbeat(msg);
            Thread.sleep(1000);
            verify(hbReceiver).receiveHeartbeat(received.capture());
            verifyNoMoreInteractions(hbReceiver);
            HeartbeatMsg hb = received.getValue();
            assertNotNull(hb);
            assertEquals(id, hb.getSender());
            assertEquals(ca, hb.getSenderAddress());
            assertEquals(id, hb.getCandidate());
            assertFalse(hb.getIsPreferred());
            assertEquals(msg.getTime(), hb.getTime());
            assertEquals(msg.getViewNumber(), hb.getViewNumber());
            assertEquals(msg.getView(), hb.getView());
            assertEquals(msg.getMsgLinks(), hb.getMsgLinks());
        } finally {
            heartbeat.terminate();
        }
    }
}
