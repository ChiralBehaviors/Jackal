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
package org.smartfrog.services.anubis.partition.wire.msg;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.WireFormException;

import com.hellblazer.jackal.util.ByteBufferPool;

public class HeartbeatMsg extends TimedMsg implements Heartbeat {

    static final public int  MAX_BIT_SIZE            = 256;                                       // allows 2000 servers
    static final private int msgLinksSz              = MAX_BIT_SIZE + intSz;

    static final private int heartbeatInitialIdx     = TIMED_MSG_WIRE_SIZE;
    static final private int viewNumberIdx           = heartbeatInitialIdx;
    static final private int viewNumberSz            = longSz;
    static final private int viewTimeStampSz         = longSz;
    static final private int viewTimeStampIdx        = viewNumberIdx
                                                       + viewNumberSz;
    static final private int isPreferredIdx          = viewTimeStampIdx
                                                       + viewTimeStampSz;
    static final private int isPreferredSz           = booleanSz;
    static final private int candidateIdx            = isPreferredIdx
                                                       + isPreferredSz;
    static final private int candidateSz             = AddressMarshalling.connectionAddressWireSz;
    static final private int msgLinksNumberIdx       = candidateIdx
                                                       + candidateSz;
    static final private int msgLinksNumberSz        = longSz;
    static final private int msgLinksIdx             = msgLinksNumberIdx
                                                       + msgLinksNumberSz;
    static final private int stableIdx               = msgLinksIdx + msgLinksSz;

    static final private int stableSz                = booleanSz;
    static final private int viewIdx                 = stableIdx + stableSz;
    static final private int viewSz                  = MAX_BIT_SIZE + intSz;
    static final private int testInterfaceSz         = AddressMarshalling.connectionAddressWireSz;
    static final private int controllerInterfaceIdx  = viewIdx + viewSz;
    public static final int  HEARTBEAT_MSG_WIRE_SIZE = controllerInterfaceIdx
                                                       + testInterfaceSz;
    public static final int  HEARTBEAT_MSG_WIRE_TYPE = 300;

    public static HeartbeatMsg toHeartbeatMsg(Heartbeat heartbeat) {
        if (heartbeat instanceof HeartbeatMsg) {
            return (HeartbeatMsg) heartbeat;
        }
        return new HeartbeatMsg(heartbeat);
    }

    private Identity          candidate           = null;
    private NodeIdSet         msgLinks            = null;
    private long              msgLinksNumber      = 0;
    private boolean           preferred           = false;
    private boolean           stable              = true;
    private InetSocketAddress controllerInterface = null;
    private NodeIdSet         view                = null;
    private long              viewNumber          = -1;
    private long              viewTimeStamp       = View.undefinedTimeStamp;

    /**
     * Constructor - Creates a heartbeat message from the wire formatted byte
     * array. This constructor does not use the usual pattern of simply
     * extending the super(byte[]) sonstructor because it wants to retain the
     * wire and byte buffer for lazy unmarshalling later.
     * 
     * @param wireForm
     * 
     */
    public HeartbeatMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                            WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    public HeartbeatMsg(Heartbeat hb) {
        super(hb.getSender(), hb.getSenderAddress());
        setIsPreferred(hb.isPreferred());
        setCandidate(hb.getCandidate());
        setTime(hb.getTime());
        setView(hb.getView());
        setViewNumber(hb.getViewNumber());
        setMsgLinks(hb.getMsgLinks());
    }

    /**
     * Constructor - a heartbeat message is provided with the sender and the
     * sender's address
     * 
     * @param identity
     * @param address
     */
    public HeartbeatMsg(Identity identity, InetSocketAddress address) {
        super(identity, address);
    }

    protected HeartbeatMsg() {
        super();
    }

    /**
     * Candidate interface
     * 
     * @return identity
     */
    @Override
    public Identity getCandidate() {
        return candidate;
    }

    @Override
    public InetSocketAddress getControllerInterface() {
        return controllerInterface;
    }

    /**
     * Message link requirements data field
     * 
     * @return NodeIdSet
     */
    @Override
    public NodeIdSet getMsgLinks() {
        return msgLinks;
    }

    @Override
    public int getSize() {
        return HEARTBEAT_MSG_WIRE_SIZE + trailerSize;
    }

    /**
     * NumberedView interface implementation
     * 
     * @return view
     */
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

    /**
     * controllerInterface get and set
     * 
     * @param address
     */
    @Override
    public void setController(InetSocketAddress address) {
        controllerInterface = address;
    }

    @Override
    public void setIsPreferred(boolean preferred) {
        this.preferred = preferred;
    }

    @Override
    public void setMsgLinks(NodeIdSet l) {
        msgLinks = l;
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

    /**
     * generate close message from this heartbeat
     */
    @Override
    public HeartbeatMsg toClose() {
        return new CloseMsg(this);
    }

    @Override
    public String toString() {
        String str = "[" + super.toString() + " | ";
        str += "view#=" + viewNumber + ", viewTS=" + viewTimeStamp + ", ";
        str += "isPreferred#=" + preferred + ", ";
        str += "cand=" + candidate + ", ";
        str += "links#=" + msgLinksNumber + ", ";
        str += "links=" + msgLinks + ", ";
        str += "view=" + view + ", ";
        str += "testIF=" + controllerInterface;
        str += "]";
        return str;
    }

    @Override
    protected int getType() {
        return HEARTBEAT_MSG_WIRE_TYPE;
    }

    /**
     * Read the attributes from the wire format. Note that some of the
     * attributes (views, msgLinks and test interface) are not read here. These
     * are read on demand. These attributes are not likely to be needed often -
     * so we only unmarshall them when we have to.
     * 
     * @param wireForm
     *            byte[]
     */
    @Override
    protected void readWireForm(ByteBuffer wireForm) throws IOException,
                                                    WireFormException,
                                                    ClassNotFoundException {
        super.readWireForm(wireForm);

        /**
         * view number, view time stamp and msgLinksNumber
         */
        viewNumber = wireForm.getLong(viewNumberIdx);
        viewTimeStamp = wireForm.getLong(viewTimeStampIdx);
        msgLinksNumber = wireForm.getLong(msgLinksNumberIdx);
        preferred = wireForm.getInt(isPreferredIdx) == 1;
        candidate = Identity.readWireForm(wireForm, candidateIdx);
        controllerInterface = AddressMarshalling.readWireForm(wireForm,
                                                              controllerInterfaceIdx);
        msgLinks = NodeIdSet.readWireForm(wireForm, msgLinksIdx, viewSz);
        stable = wireForm.getInt(stableIdx) == booleanTrueValue;
        view = NodeIdSet.readWireForm(wireForm, viewIdx, viewSz);
    }

    /**
     * Write the message attributes to the
     * 
     * @param bufferPool
     * 
     * @throws IOException
     */
    protected ByteBuffer writeWireForm(ByteBufferPool bufferPool)
                                                                 throws WireFormException,
                                                                 IOException {
        ByteBuffer wireForm = bufferPool.allocate(getSize());

        /**
         * view number, view time stamp, isPreferred, candidate, msgLinksNumber
         */
        wireForm.putLong(viewNumberIdx, viewNumber);
        wireForm.putLong(viewTimeStampIdx, viewTimeStamp);
        if (preferred) {
            wireForm.putInt(isPreferredIdx, 1);
        } else {
            wireForm.putInt(isPreferredIdx, 0);
        }
        candidate.writeWireForm(wireForm, candidateIdx);
        wireForm.putLong(msgLinksNumberIdx, msgLinksNumber);

        /**
         * msgLinks
         */
        msgLinks.writeWireForm(wireForm, msgLinksIdx, msgLinksSz);

        /**
         * stable and view
         */
        wireForm.putInt(stableIdx, stable ? booleanTrueValue
                                         : booleanFalseValue);
        view.writeWireForm(wireForm, viewIdx, msgLinksSz);

        /**
         * Test interface (if there is one)
         */
        if (controllerInterface == null) {
            AddressMarshalling.writeNullWireForm(wireForm,
                                                 controllerInterfaceIdx);
        } else {
            AddressMarshalling.writeWireForm(controllerInterface, wireForm,
                                             controllerInterfaceIdx);
        }
        writeWireForm(wireForm);
        return wireForm;
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.WireMsg#toWire(com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    public ByteBuffer toWire(ByteBufferPool bufferPool)
                                                       throws WireFormException,
                                                       IOException {
        ByteBuffer wireForm = writeWireForm(bufferPool);
        wireForm.putInt(0, getType());
        wireForm.rewind();
        return wireForm;
    }

}
