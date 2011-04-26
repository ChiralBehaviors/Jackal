package org.smartfrog.services.anubis.partition.protocols.partitionmanager;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;

public interface ConnectionManager extends HeartbeatReceiver {
    void connectTo(Identity peer);
}
