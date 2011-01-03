package org.smartfrog.services.anubis.partition.diagnostics.msg;

import java.io.Serializable;

import org.smartfrog.services.anubis.partition.util.Identity;

public class ConnectionStateMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    public Identity identity;
    public Identity[] msgConnections;
    public Identity[] msgLinks;
    public Identity[] connections;
    
}
