/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging.serializer;

public interface Serializable {
    void serialize(final AtomicWriter writer);
}
