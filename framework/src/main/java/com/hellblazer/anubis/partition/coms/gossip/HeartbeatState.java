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
package com.hellblazer.anubis.partition.coms.gossip;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

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
public class HeartbeatState implements Heartbeat {

    public static HeartbeatState toHeartbeatState(Heartbeat heartbeat,
                                                  InetSocketAddress heartbeatAddress) {
        if (heartbeat instanceof HeartbeatState) {
            return (HeartbeatState) heartbeat;
        }
        return new HeartbeatState(heartbeat, heartbeatAddress);
    }

    private volatile Identity          candidate;
    private InetSocketAddress          heartbeatAddress;
    private volatile NodeIdSet         msgLinks;
    private volatile boolean           preferred;
    private Identity                   sender;
    private InetSocketAddress          senderAddress;
    private volatile boolean           stable        = false;
    private volatile InetSocketAddress testInterface;
    private volatile long              version       = 0;
    private NodeIdSet                  view;
    private volatile long              viewNumber    = 0;
    private volatile long              viewTimeStamp = View.undefinedTimeStamp;

    private volatile byte[]            binaryCache;

    public HeartbeatState(ByteBuffer buffer) throws UnknownHostException {
        binaryCache = new byte[GossipMessages.HEARTBEAT_STATE_BYTE_SIZE];
        buffer.get(binaryCache);
        ByteBuffer msg = ByteBuffer.wrap(binaryCache);

        candidate = new Identity(msg);
        heartbeatAddress = GossipHandler.readInetAddress(msg);
        version = msg.getLong();
        msgLinks = new NodeIdSet(msg);
        preferred = msg.get() > 0 ? true : false;
        sender = new Identity(msg);
        senderAddress = GossipHandler.readInetAddress(msg);
        stable = msg.get() > 0 ? true : false;
        testInterface = GossipHandler.readInetAddress(msg);
        view = new NodeIdSet(msg);
        viewNumber = msg.getLong();
        viewTimeStamp = msg.getLong();
    }

    public HeartbeatState(Heartbeat heartbeat, InetSocketAddress hbAddress) {
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

    public HeartbeatState(Identity candidate,
                          InetSocketAddress heartbeatAddress, long version,
                          NodeIdSet msgLinks, boolean preferred,
                          Identity sender, InetSocketAddress senderAddress,
                          boolean stable, InetSocketAddress testInterface,
                          NodeIdSet view, long viewNumber, long viewTimestamp) {
        this.candidate = candidate;
        this.heartbeatAddress = heartbeatAddress;
        this.version = version;
        this.msgLinks = msgLinks;
        this.preferred = preferred;
        this.sender = sender;
        this.senderAddress = senderAddress;
        this.stable = stable;
        this.testInterface = testInterface;
        this.view = view;
        this.viewNumber = viewNumber;
        viewTimeStamp = viewTimestamp;
    }

    public HeartbeatState(InetSocketAddress address) {
        candidate = new Identity(-1, -1, -1);
        msgLinks = new NodeIdSet(1);
        sender = new Identity(-1, -1, -1);
        heartbeatAddress = address;
        view = new NodeIdSet(1);
        version = -1;
    }

    protected HeartbeatState(InetSocketAddress address, Identity id,
                             InetSocketAddress hbAddress) {
        sender = id;
        senderAddress = address;
        view = new NodeIdSet(1);
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
        if (testInterface == null) {
            if (other.testInterface != null) {
                return false;
            }
        } else if (!testInterface.equals(other.testInterface)) {
            return false;
        }
        if (version != other.version) {
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

    public long getEpoch() {
        return sender.epoch;
    }

    public InetSocketAddress getHeartbeatAddress() {
        return heartbeatAddress;
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
    public InetSocketAddress getTestInterface() {
        return testInterface;
    }

    @Override
    public long getTime() {
        return viewTimeStamp;
    }

    public long getVersion() {
        return version;
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
        result = prime * result + (stable ? 1231 : 1237);
        result = prime * result
                 + (testInterface == null ? 0 : testInterface.hashCode());
        result = prime * result + (int) (version ^ version >>> 32);
        result = prime * result + (view == null ? 0 : view.hashCode());
        result = prime * result + (int) (viewNumber ^ viewNumber >>> 32);
        result = prime * result + (int) (viewTimeStamp ^ viewTimeStamp >>> 32);
        return result;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public void setCandidate(Identity id) {
        binaryCache = null;
        candidate = id;
    }

    @Override
    public void setIsPreferred(boolean preferred) {
        binaryCache = null;
        this.preferred = preferred;
    }

    @Override
    public void setMsgLinks(NodeIdSet ml) {
        binaryCache = null;
        msgLinks = ml;
    }

    @Override
    public void setTestInterface(InetSocketAddress address) {
        binaryCache = null;
        testInterface = address;
    }

    @Override
    public void setTime(long t) {
        binaryCache = null;
        viewTimeStamp = t;
    }

    public void setVersion(long l) {
        binaryCache = null;
        version = l;
    }

    @Override
    public void setView(View v) {
        binaryCache = null;
        view = v.toBitSet();
        stable = v.isStable();
        viewTimeStamp = v.getTimeStamp();
    }

    @Override
    public void setViewNumber(long n) {
        binaryCache = null;
        viewNumber = n;
    }

    @Override
    public Heartbeat toClose() {
        return new HeartbeatMsg(this).toClose();
    }

    @Override
    public String toString() {
        return "HeartbeatState [" + sender + " | " + heartbeatAddress
               + ", version=" + version + "]";
    }

    public void writeTo(ByteBuffer buffer) {
        fillCache();
        buffer.put(binaryCache);
    }

    private synchronized void fillCache() {
        if (binaryCache != null) {
            return;
        }
        binaryCache = new byte[GossipMessages.HEARTBEAT_STATE_BYTE_SIZE];
        ByteBuffer msg = ByteBuffer.wrap(binaryCache);

        candidate.writeTo(msg);
        GossipHandler.writeInetAddress(heartbeatAddress, msg);
        msg.putLong(version);
        msgLinks.writeTo(msg);
        if (preferred) {
            msg.put((byte) 1);
        } else {
            msg.put((byte) 0);
        }
        sender.writeTo(msg);
        GossipHandler.writeInetAddress(senderAddress, msg);
        if (stable) {
            msg.put((byte) 1);
        } else {
            msg.put((byte) 0);
        }
        GossipHandler.writeInetAddress(testInterface, msg);
        view.writeTo(msg);
        msg.putLong(viewNumber);
        msg.putLong(viewTimeStamp);
    }
}
