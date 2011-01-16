/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.toString.ToString.string;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.MaximumValue;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public final class MessageId implements Serializable {
    public static MessageId messageId() {
        return new MessageId(getMessageId());
    }

    private final Unsigned16BitInteger messageId;

    private static final Object lockObject = new Object();

    private static Unsigned16BitInteger lastMessageId = MaximumValue;

    private static Unsigned16BitInteger getMessageId() {
        synchronized (lockObject) {
            lastMessageId = lastMessageId.increment();
        }
        return lastMessageId;
    }

    public MessageId(final Unsigned16BitInteger messageId) {
        this.messageId = messageId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MessageId that = (MessageId) o;
        return messageId.equals(that.messageId);
    }

    @Override
    public int hashCode() {
        return messageId.hashCode();
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeUnsigned16BitInteger(messageId);
    }

    @Override
    public String toString() {
        return string(this, messageId);
    }
}
