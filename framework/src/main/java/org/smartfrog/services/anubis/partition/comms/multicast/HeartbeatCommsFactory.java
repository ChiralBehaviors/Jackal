package org.smartfrog.services.anubis.partition.comms.multicast;

import java.io.IOException;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;

public interface HeartbeatCommsFactory {

    HeartbeatCommsIntf create(MulticastAddress address, ConnectionAddress inf,
                              HeartbeatReceiver cs, String threadName,
                              Identity id) throws IOException;

    HeartbeatCommsIntf create(MulticastAddress address, HeartbeatReceiver cs,
                              String threadName, Identity id)
                                                             throws IOException;

}