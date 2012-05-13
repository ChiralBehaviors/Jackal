/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.gossip;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

/**
 * The heartbeat state replicated by the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class HeartbeatState implements Heartbeat, Cloneable {

    public static InetSocketAddress readInetAddress(ByteBuffer msg)
                                                                   throws UnknownHostException {
        int length = msg.get();
        if (length == 0) {
            return null;
        }

        byte[] address = new byte[length];
        msg.get(address);
        int port = msg.getInt();

        InetAddress inetAddress = InetAddress.getByAddress(address);
        return new InetSocketAddress(inetAddress, port);
    }

    public static HeartbeatState toHeartbeatState(Heartbeat heartbeat,
                                                  InetSocketAddress heartbeatAddress) {
        if (heartbeat instanceof HeartbeatState) {
            return (HeartbeatState) heartbeat;
        }
        return new HeartbeatState(heartbeat, heartbeatAddress);
    }

    public static void writeInetAddress(InetSocketAddress ipaddress,
                                        ByteBuffer bytes) {
        if (ipaddress == null) {
            bytes.put((byte) 0);
            return;
        }
        byte[] address = ipaddress.getAddress().getAddress();
        bytes.put((byte) address.length);
        bytes.put(address);
        bytes.putInt(ipaddress.getPort());
    }

    private volatile Identity          candidate;
    private final InetSocketAddress    heartbeatAddress;
    private final boolean              discoveryOnly;
    private final NodeIdSet            msgLinks;
    private volatile boolean           preferred;
    private final Identity             sender;
    private InetSocketAddress          senderAddress;
    private AtomicBoolean              stable        = new AtomicBoolean();
    private volatile InetSocketAddress controllInterface;
    private volatile long              time          = -1;
    private final NodeIdSet            view          = new NodeIdSet();
    private AtomicLong                 viewNumber    = new AtomicLong();

    private volatile long              viewTimeStamp = View.undefinedTimeStamp;

    private volatile byte[]            binaryCache;

    public HeartbeatState(ByteBuffer buffer) throws UnknownHostException {
        binaryCache = new byte[GossipMessages.HEARTBEAT_STATE_BYTE_SIZE];
        buffer.get(binaryCache);
        ByteBuffer msg = ByteBuffer.wrap(binaryCache);

        candidate = new Identity(msg);
        discoveryOnly = msg.get() > 0 ? true : false;
        heartbeatAddress = HeartbeatState.readInetAddress(msg);
        time = msg.getLong();
        msgLinks = new NodeIdSet(msg);
        preferred = msg.get() > 0 ? true : false;
        sender = new Identity(msg);
        senderAddress = HeartbeatState.readInetAddress(msg);
        stable.set(msg.get() > 0 ? true : false);
        controllInterface = HeartbeatState.readInetAddress(msg);
        view.copyFrom(new NodeIdSet(msg));
        viewNumber.set(msg.getLong());
        viewTimeStamp = msg.getLong();
    }

    public HeartbeatState(Heartbeat heartbeat, InetSocketAddress hbAddress) {
        discoveryOnly = false;
        candidate = heartbeat.getCandidate();
        heartbeatAddress = hbAddress;
        msgLinks = heartbeat.getMsgLinks();
        preferred = heartbeat.isPreferred();
        sender = heartbeat.getSender();
        senderAddress = heartbeat.getSenderAddress();
        setView(heartbeat.getView());
        setViewNumber(heartbeat.getViewNumber());
        fillCache();
    }

    public HeartbeatState(Identity candidate, boolean discoveryOnly,
                          InetSocketAddress heartbeatAddress,
                          NodeIdSet msgLinks, boolean preferred,
                          Identity sender, InetSocketAddress senderAddress,
                          boolean stable, InetSocketAddress testInterface,
                          NodeIdSet view, long viewNumber, long viewTimestamp) {
        this.candidate = candidate;
        this.discoveryOnly = discoveryOnly;
        this.heartbeatAddress = heartbeatAddress;
        this.msgLinks = msgLinks;
        this.preferred = preferred;
        this.sender = sender;
        this.senderAddress = senderAddress;
        this.stable.set(stable);
        controllInterface = testInterface;
        this.view.copyFrom(view);
        this.viewNumber.set(viewNumber);
        viewTimeStamp = viewTimestamp;
    }

    public HeartbeatState(InetSocketAddress address) {
        discoveryOnly = false;
        candidate = new Identity(-1, -1, -1);
        msgLinks = new NodeIdSet(1);
        sender = new Identity(-1, -1, -1);
        heartbeatAddress = address;
    }

    public HeartbeatState(InetSocketAddress heartbeatAddress, long version,
                          Identity sender) {
        discoveryOnly = true;
        this.heartbeatAddress = heartbeatAddress;
        candidate = new Identity(-1, -1, -1);
        msgLinks = new NodeIdSet(1);
        this.sender = sender;
    }

    protected HeartbeatState(InetSocketAddress address, Identity id,
                             InetSocketAddress hbAddress) {
        assert id.id >= 0;
        discoveryOnly = false;
        sender = id;
        senderAddress = address;
        candidate = new Identity(-1, -1, -1);
        msgLinks = new NodeIdSet(1);
        heartbeatAddress = hbAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        HeartbeatState other = (HeartbeatState) obj;
        if (!Arrays.equals(binaryCache, other.binaryCache)) {
            return false;
        }
        if (candidate == null) {
            if (other.candidate != null) {
                return false;
            }
        } else if (!candidate.equals(other.candidate)) {
            return false;
        }
        if (heartbeatAddress == null) {
            if (other.heartbeatAddress != null) {
                return false;
            }
        } else if (!heartbeatAddress.equals(other.heartbeatAddress)) {
            return false;
        }
        if (msgLinks == null) {
            if (other.msgLinks != null) {
                return false;
            }
        } else if (!msgLinks.equals(other.msgLinks)) {
            return false;
        }
        if (preferred != other.preferred) {
            return false;
        }
        if (sender == null) {
            if (other.sender != null) {
                return false;
            }
        } else if (!sender.equals(other.sender)) {
            return false;
        }
        if (senderAddress == null) {
            if (other.senderAddress != null) {
                return false;
            }
        } else if (!senderAddress.equals(other.senderAddress)) {
            return false;
        }
        if (stable != other.stable) {
            return false;
        }
        if (controllInterface == null) {
            if (other.controllInterface != null) {
                return false;
            }
        } else if (!controllInterface.equals(other.controllInterface)) {
            return false;
        }
        if (view == null) {
            if (other.view != null) {
                return false;
            }
        } else if (!view.equals(other.view)) {
            return false;
        }
        if (viewNumber != other.viewNumber) {
            return false;
        }
        if (viewTimeStamp != other.viewTimeStamp) {
            return false;
        }
        return true;
    }

    @Override
    public Identity getCandidate() {
        return candidate;
    }

    @Override
    public InetSocketAddress getControllerInterface() {
        return controllInterface;
    }

    public long getEpoch() {
        return sender.epoch;
    }

    public InetSocketAddress getHeartbeatAddress() {
        return heartbeatAddress;
    }

    public NodeIdSet getMembers() {
        return view;
    }

    public String getMemberString() {
        return "[" + getSender() + " : " + getHeartbeatAddress() + "]";
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

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public View getView() {
        return new BitView(stable.get(), view, viewTimeStamp);
    }

    @Override
    public long getViewNumber() {
        return viewNumber.get();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(binaryCache);
        result = prime * result
                 + (candidate == null ? 0 : candidate.hashCode());
        result = prime * result
                 + (heartbeatAddress == null ? 0 : heartbeatAddress.hashCode());
        result = prime * result + (msgLinks == null ? 0 : msgLinks.hashCode());
        result = prime * result + (preferred ? 1231 : 1237);
        result = prime * result + (sender == null ? 0 : sender.hashCode());
        result = prime * result
                 + (senderAddress == null ? 0 : senderAddress.hashCode());
        result = prime * result + (stable.get() ? 1231 : 1237);
        result = prime
                 * result
                 + (controllInterface == null ? 0
                                             : controllInterface.hashCode());
        result = prime * result + (view == null ? 0 : view.hashCode());
        result = prime * result
                 + (int) (viewNumber.get() ^ viewNumber.get() >>> 32);
        result = prime * result + (int) (viewTimeStamp ^ viewTimeStamp >>> 32);
        return result;
    }

    public boolean isDiscoveryOnly() {
        return discoveryOnly;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public void setCandidate(Identity id) {
        candidate = id;
        invalidateCache();
    }

    @Override
    public void setController(InetSocketAddress address) {
        controllInterface = address;
        invalidateCache();
    }

    @Override
    public void setIsPreferred(boolean preferred) {
        this.preferred = preferred;
        invalidateCache();
    }

    @Override
    public void setMsgLinks(NodeIdSet ml) {
        msgLinks.copyFrom(ml);
        invalidateCache();
    }

    @Override
    public void setTime(long t) {
        time = t;
        invalidateCache();
    }

    @Override
    public void setView(View v) {
        view.copyFrom(v.toBitSet());
        stable.set(v.isStable());
        viewTimeStamp = v.getTimeStamp();
        invalidateCache();
    }

    @Override
    public void setViewNumber(long n) {
        viewNumber.set(n);
        invalidateCache();
    }

    @Override
    public Heartbeat toClose() {
        return new HeartbeatMsg(this).toClose();
    }

    @Override
    public String toString() {
        return "HeartbeatState [" + sender + " | " + heartbeatAddress
               + ", time=" + time + "]";
    }

    public synchronized void writeTo(ByteBuffer buffer) {
        fillCache();
        buffer.put(binaryCache);
    }

    private void fillCache() {
        if (binaryCache != null) {
            return;
        }
        binaryCache = new byte[GossipMessages.HEARTBEAT_STATE_BYTE_SIZE];
        ByteBuffer msg = ByteBuffer.wrap(binaryCache);

        candidate.writeTo(msg);
        if (discoveryOnly) {
            msg.put((byte) 1);
        } else {
            msg.put((byte) 0);
        }
        HeartbeatState.writeInetAddress(heartbeatAddress, msg);
        msg.putLong(time);
        msgLinks.writeTo(msg);
        if (preferred) {
            msg.put((byte) 1);
        } else {
            msg.put((byte) 0);
        }
        sender.writeTo(msg);
        HeartbeatState.writeInetAddress(senderAddress, msg);
        if (stable.get()) {
            msg.put((byte) 1);
        } else {
            msg.put((byte) 0);
        }
        HeartbeatState.writeInetAddress(controllInterface, msg);
        view.writeTo(msg);
        msg.putLong(viewNumber.get());
        msg.putLong(viewTimeStamp);
    }

    private synchronized void invalidateCache() {
        binaryCache = null;
    }

    @Override
    protected HeartbeatState clone() {
        try {
            return (HeartbeatState) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException();
        }
    }
}
