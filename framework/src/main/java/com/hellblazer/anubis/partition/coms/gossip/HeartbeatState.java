package com.hellblazer.anubis.partition.coms.gossip;

import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

public class HeartbeatState implements Heartbeat {
    private Identity candidate;
    private NodeIdSet msgLinks;
    private boolean preferred;
    private Identity sender;
    private InetSocketAddress senderAddress;
    private boolean stable;
    private InetSocketAddress testInterface;
    private NodeIdSet view;
    private long viewNumber;
    private long viewTimeStamp = View.undefinedTimeStamp;

    @Override
    public Identity getCandidate() {
        return candidate;
    }

    @Override
    public NodeIdSet getMsgLinks() {
        return msgLinks;
    }

    @Override
    public Identity getSender() {
        return sender;
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return senderAddress;
    }

    public InetSocketAddress getTestInterface() {
        return testInterface;
    }

    @Override
    public long getTime() {
        return viewTimeStamp;
    }

    @Override
    public View getView() {
        return new BitView(stable, view, viewTimeStamp);
    }

    @Override
    public long getViewNumber() {
        return viewNumber;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public void setCandidate(Identity id) {
        candidate = id;
    }

    @Override
    public void setMsgLinks(NodeIdSet ml) {
        msgLinks = ml;
    }

    public void setTestInterface(InetSocketAddress address) {
        testInterface = address;
    }

    @Override
    public void setTime(long t) {
        viewTimeStamp = t;
    }

    @Override
    public void setView(View v) {
        view = v.toBitSet();
        stable = v.isStable();
        viewTimeStamp = v.getTimeStamp();
    }

    @Override
    public void setViewNumber(long n) {
        viewNumber = n;
    }

    public boolean record(HeartbeatState remoteState) {
        assert sender.equalId(remoteState.sender);
        return remoteState.sender.epoch > sender.epoch
               || remoteState.viewNumber > viewNumber;
    }
}
