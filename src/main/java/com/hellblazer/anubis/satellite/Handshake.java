package com.hellblazer.anubis.satellite;

import java.rmi.RemoteException;

import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter;

public interface Handshake {

    void setAdapter(SPLocatorAdapter adapter) throws RemoteException;

}