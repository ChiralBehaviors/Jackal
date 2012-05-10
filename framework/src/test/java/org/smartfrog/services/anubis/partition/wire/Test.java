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
package org.smartfrog.services.anubis.partition.wire;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.MessageMsg;
import org.smartfrog.services.anubis.partition.wire.msg.PingHeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.SerializedMsg;

import com.hellblazer.jackal.util.ByteBufferPool;

public class Test extends TestCase {
    public static void main(String[] args) {
        Test untitled1 = new Test();
        untitled1.testIt3();
    }

    private final ByteBufferPool bufferPool = new ByteBufferPool("test", 1);

    public void testIt() {
        try {

            int loops = 100000;

            Identity outId = new Identity(123456, 1, 654321);
            InetSocketAddress outAddress = new InetSocketAddress("127.0.0.1",
                                                                 1066);

            InetSocketAddress outTestIF = new InetSocketAddress("127.0.0.1",
                                                                2020);

            NodeIdSet outView = new NodeIdSet();
            outView.add(700);

            NodeIdSet outMsgLinks = new NodeIdSet();
            outMsgLinks.add(700);

            HeartbeatMsg outMsg = new HeartbeatMsg(outId, outAddress);
            outMsg.setCandidate(outId);
            outMsg.setMsgLinks(outMsgLinks);
            outMsg.setController(outTestIF);
            outMsg.setView(new BitView(true, outView, 162534));
            outMsg.setViewNumber(23);
            outMsg.setTime(9876);
            System.out.println("Heartbeat Test message for output is: "
                               + outMsg);

            System.out.println("STARTING HeabeatTestMsg marshalling test run");
            long start = System.currentTimeMillis();
            ByteBuffer wireForm = null;
            for (int i = 0; i < loops; i++) {
                wireForm = outMsg.toWire(bufferPool);
            }
            long end = System.currentTimeMillis();
            System.out.println("END HeartbeatTestMsg marshalling test run took "
                               + (end - start) + "ms");

            System.out.println("STARTING HeabeatTestMsg UNmarshalling test run");
            HeartbeatMsg inMsg;
            start = System.currentTimeMillis();
            for (int i = 0; i < loops; i++) {
                inMsg = new HeartbeatMsg(wireForm);
                inMsg.getCandidate();
                inMsg.getMsgLinks();
                inMsg.getView();
                inMsg.getControllerInterface();
                inMsg.getSenderAddress();
            }
            end = System.currentTimeMillis();
            System.out.println("END HeartbeatTestMsg UNmarshalling test run took "
                               + (end - start) + "ms");

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void testIt2() {
        try {
            String outStr = "[This is my message]";
            Identity outId = new Identity(123456, 1, 654321);
            MessageMsg outMsg = new MessageMsg(outId, outStr);
            outMsg.setTime(98765);
            System.out.println("Message message for output is: " + outMsg);

            ByteBuffer wire = outMsg.toWire(bufferPool);

            System.out.println("Wire length " + wire.capacity());

            MessageMsg inMsg = (MessageMsg) Wire.fromWire(wire);
            System.out.println("Message message input is: " + inMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void testIt3() {
        try {
            Identity outId = new Identity(123456, 1, 654321);
            InetSocketAddress outAddress = new InetSocketAddress("127.0.0.1",
                                                                 1066);

            InetSocketAddress outTestIF = new InetSocketAddress("127.0.0.1",
                                                                2020);

            NodeIdSet outView = new NodeIdSet();
            outView.add(700);

            NodeIdSet outMsgLinks = new NodeIdSet();
            outMsgLinks.add(700);

            PingHeartbeatMsg outMsg = new PingHeartbeatMsg(outId, outAddress);
            outMsg.setCandidate(outId);
            outMsg.setMsgLinks(outMsgLinks);
            outMsg.setController(outTestIF);
            outMsg.setView(new BitView(true, outView, 162534));
            outMsg.setViewNumber(23);
            outMsg.setTime(9876);
            outMsg.setPingBit(outId);
            System.out.println("Heartbeat Test message for output is: "
                               + outMsg);

            ByteBuffer wireForm = outMsg.toWire(bufferPool);

            PingHeartbeatMsg inMsg = (PingHeartbeatMsg) Wire.fromWire(wireForm);
            System.out.println("Heartbeat Test message for input is: " + inMsg);
            inMsg.getCandidate();
            System.out.println("Heartbeat Test message with candidate is: "
                               + inMsg);
            inMsg.getMsgLinks();
            System.out.println("Heartbeat Test message with msgLinks is: "
                               + inMsg);
            inMsg.getView();
            System.out.println("Heartbeat Test message with view is: " + inMsg);
            inMsg.getControllerInterface();
            System.out.println("Heartbeat Test message with getTestInterface is: "
                               + inMsg);
            inMsg.getSenderAddress();
            System.out.println("Heartbeat Test message with sender address is: "
                               + inMsg);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    public void testIt4() {
        try {
            SerializedMsg outMsg = new SerializedMsg("[This is my message]");
            System.out.println("Message for output is: " + outMsg);

            ByteBuffer wire = outMsg.toWire(bufferPool);

            SerializedMsg inMsg = (SerializedMsg) Wire.fromWire(wire);
            System.out.println("Message input is: " + inMsg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
