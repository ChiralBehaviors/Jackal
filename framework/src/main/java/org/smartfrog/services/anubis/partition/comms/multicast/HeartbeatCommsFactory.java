package org.smartfrog.services.anubis.partition.comms.multicast;

import java.io.IOException;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;

public interface HeartbeatCommsFactory {

    HeartbeatCommsIntf create(HeartbeatReceiver cs) throws IOException; 

}