package com.hellblazer.anubis.partition.coms.udp;

import static org.smartfrog.services.anubis.basiccomms.connectiontransport.AddressMarshalling.connectionAddressWireSz;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.AddressMarshalling;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

public class MembershipMessage extends WireMsg {
    public static final int MEMBERSHIP_MSG_WIRE_TYPE = 666;
    private static final int payloadCountIdx = WIRE_SIZE;
    private static final int payloadIdx = payloadCountIdx + intSz;
    private List<InetSocketAddress> members;

    public MembershipMessage(ByteBuffer wireForm)
                                                 throws ClassNotFoundException,
                                                 WireFormException, IOException {
        super(wireForm);
    }

    public MembershipMessage(List<InetSocketAddress> members) {
        this.members = members;
    }

    public List<InetSocketAddress> getMembers() {
        return members;
    }

    @Override
    public int getSize() throws WireFormException {
        return payloadIdx + (members.size() * connectionAddressWireSz);
    }

    @Override
    public String toString() {
        return members.toString();
    }

    @Override
    protected int getType() {
        return MEMBERSHIP_MSG_WIRE_TYPE;
    }

    @Override
    protected void readWireForm(ByteBuffer buf) throws IOException,
                                               WireFormException,
                                               ClassNotFoundException {
        super.readWireForm(buf);
        int count = wireForm.getInt(payloadCountIdx);
        members = new ArrayList<InetSocketAddress>(count);
        int idx = payloadIdx;
        for (int i = 0; i < count; i++) {
            members.add(AddressMarshalling.readWireForm(wireForm, idx));
            idx += AddressMarshalling.connectionAddressWireSz;
        }
    }

    @Override
    protected void writeWireForm() throws WireFormException {
        super.writeWireForm();
        wireForm.putInt(payloadCountIdx, members.size());
        int idx = payloadIdx;
        for (InetSocketAddress address : members) {
            AddressMarshalling.writeWireForm(address, wireForm, idx);
            idx += AddressMarshalling.connectionAddressWireSz;
        }
    }
}
