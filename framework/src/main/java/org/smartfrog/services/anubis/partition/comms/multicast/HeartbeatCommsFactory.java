package org.smartfrog.services.anubis.partition.comms.multicast;

import java.io.IOException;

import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionManager;

public interface HeartbeatCommsFactory {

    HeartbeatCommsIntf create(ConnectionManager cs) throws IOException;

}