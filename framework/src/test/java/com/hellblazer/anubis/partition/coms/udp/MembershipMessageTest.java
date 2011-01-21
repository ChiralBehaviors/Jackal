package com.hellblazer.anubis.partition.coms.udp;

import static java.util.Arrays.asList;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;

import junit.framework.TestCase;

public class MembershipMessageTest extends TestCase {
    public void testMarshalling() throws Exception {
        List<InetSocketAddress> addresses = asList(new InetSocketAddress[] {
                                                                            new InetSocketAddress(
                                                                                                  1),
                                                                            new InetSocketAddress(
                                                                                                  3),
                                                                            new InetSocketAddress(
                                                                                                  2) });
        byte[] wireForm = new MembershipMessage(addresses).toWire();
        assertNotNull(wireForm);
        MembershipMessage message = new MembershipMessage(ByteBuffer.wrap(wireForm));
        List<InetSocketAddress> members = message.getMembers();
        assertNotNull(members);
        assertEquals(addresses.size(), members.size());
        int i = 0;
        for (InetSocketAddress address: addresses) {
            assertEquals(address, members.get(i++));
        }
    }
}
