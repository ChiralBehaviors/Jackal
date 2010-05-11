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
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.WireFormException;

public class HeartbeatMsg extends TimedMsg implements Heartbeat {

    private long              viewNumber    = -1;
    private long              viewTimeStamp = View.undefinedTimeStamp;
	private boolean           preferred     = false;
    private Identity          candidate     = null;
    private long              msgLinksNumber = 0;
    private NodeIdSet            msgLinks      = null;
    private boolean           stable        = true;
    private NodeIdSet            view          = null;
    private ConnectionAddress testInterface = null;

    private boolean           candidateUnmarshalled;
    private boolean           viewUnmarshalled;
    private boolean           msgLinksUnmarshalled;
    private boolean           testInterfaceUnmarshalled;

    static final public int MAX_BIT_SIZE = 256; // allows 2000 servers
    static final private int heartbeatInitialIdx = TIMED_MSG_WIRE_SIZE;

    static final private int viewNumberIdx = heartbeatInitialIdx;
    static final private int viewNumberSz = longSz;
    static final private int viewTimeStampIdx = viewNumberIdx + viewNumberSz;
    static final private int viewTimeStampSz = longSz;

    static final private int isPreferredIdx = viewTimeStampIdx + viewTimeStampSz; 
    static final private int isPreferredSz = booleanSz;
    
    static final private int candidateIdx = isPreferredIdx + isPreferredSz;
    static final private int candidateSz = ConnectionAddress.connectionAddressWireSz;

    static final private int msgLinksNumberIdx = candidateIdx + candidateSz;
    static final private int msgLinksNumberSz = longSz;

    static final private int msgLinksIdx = msgLinksNumberIdx + msgLinksNumberSz;
    static final private int msgLinksSz = MAX_BIT_SIZE + intSz;

    static final private int stableIdx = msgLinksIdx + msgLinksSz;
    static final private int stableSz = booleanSz;

    static final private int viewIdx = stableIdx + stableSz;
    static final private int viewSz = MAX_BIT_SIZE + intSz;

    static final private int testInterfaceIdx = viewIdx + viewSz;
    static final private int testInterfaceSz  = ConnectionAddress.connectionAddressWireSz;

    public static final int HEARTBEAT_MSG_WIRE_SIZE = testInterfaceIdx + testInterfaceSz;
    public static final int HEARTBEAT_MSG_WIRE_TYPE = 300;

    protected int getType() { return HEARTBEAT_MSG_WIRE_TYPE; }
    public int getSize() { return HEARTBEAT_MSG_WIRE_SIZE; }

    protected HeartbeatMsg() {
        super();
    }

    /**
     * Constructor - Creates a heartbeat message from the wire formatted
     *               byte array. This constructor does not use the usual
     *               pattern of simply extending the super(byte[])
     *               sonstructor because it wants to retain the wire and
     *               byte buffer for lazy unmarshalling later.
     * @param wireForm
     *
     */
    public HeartbeatMsg(ByteBuffer wireForm) throws ClassNotFoundException, WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }


    /**
     * Constructor - a heartbeat message is provided with the sender and
     *               the sender's address
     *
     * @param identity
     * @param address
     */
    public HeartbeatMsg(Identity identity, ConnectionAddress address) {
        super(identity, address);
        candidateUnmarshalled = true;
        viewUnmarshalled = true;
        msgLinksUnmarshalled = true;
        testInterfaceUnmarshalled = true;
    }

    public HeartbeatMsg(HeartbeatMsg hb) {
        super( hb.getSender(), hb.getSenderAddress() );
        this.setIsPreferred(hb.getIsPreferred());
        this.setCandidate(hb.getCandidate());
        this.setTime(hb.getTime());
        this.setView(hb.getView());
        this.setViewNumber(hb.getViewNumber());
        this.setMsgLinks(hb.getMsgLinks());
    }

    /**
     * NumberedView interface implementation
     * @return view
     */
    public View getView() {
        if( !viewUnmarshalled )
            viewFromWire();
        return new BitView(stable, view, viewTimeStamp);
    }

    public void setView(View v) {
        view = v.toBitSet();
        stable = v.isStable();
        viewTimeStamp = v.getTimeStamp();
    }

    public long getViewNumber() {
        return viewNumber;
    }

    public void setViewNumber(long n) {
        viewNumber = n;
    }
    
    public void setIsPreferred(boolean preferred) {
    	this.preferred = preferred;
    }
    
    public boolean getIsPreferred() {
    	return preferred;
    }


    /**
     * Candidate interface
     * @return  identity
     */
    public Identity getCandidate() {
        if( !candidateUnmarshalled )
            candidateFromWire();
        return candidate;
    }

    public void setCandidate(Identity id) {
        candidate = id;
    }

    /**
     * Message link requirements data field
     * @return NodeIdSet
     */
    public NodeIdSet getMsgLinks() {
        if( !msgLinksUnmarshalled )
            msgLinksFromWire();
        return msgLinks;
    }
    public void setMsgLinks(NodeIdSet l) {
        msgLinks = l;
    }

    /**
     * testInterface get and set
     * @param address
     */
    public void setTestInterface(ConnectionAddress address) {
        testInterface = address;
    }
    public ConnectionAddress getTestInterface() {
        if( !testInterfaceUnmarshalled )
            testInterfaceFromWire();
        return testInterface;
    }

    /**
     * generate close message from this heartbeat
     */
    public HeartbeatMsg toClose() {
        return new CloseMsg(this);
    }


    private void candidateFromWire() {
        candidateUnmarshalled = true;
        candidate = Identity.readWireForm(wireForm, candidateIdx);
    }

    private void viewFromWire() {
        viewUnmarshalled = true;
        stable = ( wireForm.getInt(stableIdx) == booleanTrueValue );
        try {
            view = NodeIdSet.readWireForm(wireForm, viewIdx, viewSz);
        } catch (WireFormException ex) {
            ex.printStackTrace();
            view = new NodeIdSet();
        }
    }

    private void msgLinksFromWire() {
        msgLinksUnmarshalled = true;
        try {
            msgLinks = NodeIdSet.readWireForm(wireForm, msgLinksIdx, viewSz);
        } catch (WireFormException ex) {
            ex.printStackTrace();
            msgLinks = new NodeIdSet();
        }
    }

    private void testInterfaceFromWire() {
        testInterfaceUnmarshalled = true;
        testInterface = ConnectionAddress.readWireForm(wireForm, testInterfaceIdx);
    }



    /**
     * Write the message attributes to the
     */
    protected void writeWireForm() throws WireFormException {
        super.writeWireForm();

        /**
         * view number, view time stamp, isPreferred, candidate, msgLinksNumber
         */
        wireForm.putLong(viewNumberIdx, viewNumber);
        wireForm.putLong(viewTimeStampIdx, viewTimeStamp);
        if( preferred ) {
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
        wireForm.putInt(stableIdx, (stable ? booleanTrueValue : booleanFalseValue));
        view.writeWireForm(wireForm, viewIdx, msgLinksSz);

        /**
         * Test interface (if there is one)
         */
        if (testInterface == null) {
            ConnectionAddress.writeNullWireForm(wireForm, testInterfaceIdx);
        } else {
            testInterface.writeWireForm(wireForm, testInterfaceIdx);
        }
    }

    /**
     * Read the attributes from the wire format. Note that some of the attributes
     * (views, msgLinks and test interface) are not read here. These are read
     * on demand. These attributes are not likely to be needed often - so we
     * only unmarshall them when we have to.
     *
     * @param buf byte[]
     */
    protected void readWireForm(ByteBuffer buf) throws IOException, WireFormException, ClassNotFoundException {
        super.readWireForm(buf);

        /**
         * view number, view time stamp and msgLinksNumber
         */
        viewNumber = wireForm.getLong(viewNumberIdx);
        viewTimeStamp = wireForm.getLong(viewTimeStampIdx);
        msgLinksNumber = wireForm.getLong(msgLinksNumberIdx);
        preferred = (wireForm.getInt(isPreferredIdx) == 1);

        /**
         * Do not unmarshall candidate, msgLinks, view or test interface
         * These are done on demand only
         */
        candidateUnmarshalled = false;
        msgLinksUnmarshalled = false;
        viewUnmarshalled = false;
        testInterfaceUnmarshalled = false;
    }

    public String toString() {
        String str = "[" + super.toString() + " | ";
        str += "view#=" + viewNumber +", viewTS=" + viewTimeStamp + ", ";
        str += "isPreferred#=" + preferred + ", ";
        str += ( candidateUnmarshalled ? "cand=" + candidate : "CANDIDATE_MARSHALLED" ) + ", ";
        str += "links#=" + msgLinksNumber + ", ";
        str += ( msgLinksUnmarshalled ? "links=" + msgLinks : "LINKS_MARSHALLED" ) + ", ";
        str += ( viewUnmarshalled ? "view=" + view : "VIEW_MARSHALLED" ) + ", ";
        str += ( testInterfaceUnmarshalled ? "testIF=" + testInterface : "TEST_IF_MARSHALLED" );
        str += "]";
        return str;
    }



}
