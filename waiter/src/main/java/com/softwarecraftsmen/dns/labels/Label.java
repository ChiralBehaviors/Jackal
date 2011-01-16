/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.labels;

import com.softwarecraftsmen.dns.messaging.serializer.Serializable;

public interface Label extends Serializable {

    boolean isEmpty();

    int length();

    String toStringRepresentation();
}
