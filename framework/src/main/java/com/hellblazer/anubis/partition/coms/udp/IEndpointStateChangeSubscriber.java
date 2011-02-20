package com.hellblazer.anubis.partition.coms.udp;

import java.net.InetAddress;


/**
 * This is called by an instance of the IEndpointStateChangePublisher to notify
 * interested parties about changes in the the state associated with any endpoint.
 * For instance if node A figures there is a changes in state for an endpoint B
 * it notifies all interested parties of this change. It is upto to the registered
 * instance to decide what he does with this change. Not all modules maybe interested 
 * in all state changes.
 */

public interface IEndpointStateChangeSubscriber
{
    /**
     * Use to inform interested parties about the change in the state
     * for specified endpoint
     * 
     * @param endpoint endpoint for which the state change occurred.
     * @param epState state that actually changed for the above endpoint.
     */
    public void onJoin(InetAddress endpoint, EndpointState epState); 

    public void onAlive(InetAddress endpoint, EndpointState state);

    public void onDead(InetAddress endpoint, EndpointState state);

    public void onRemove(InetAddress endpoint);
}