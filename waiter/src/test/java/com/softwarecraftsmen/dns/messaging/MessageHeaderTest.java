/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.OperationCode.InverseQuery;
import static com.softwarecraftsmen.dns.messaging.OperationCode.Query;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.NameError;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.NoErrorCondition;
import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger.Zero;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger;

public class MessageHeaderTest {
    @Test
    public void replyHeaderIsForRequestHeader() {
        final Unsigned3BitInteger z = Zero;
        final MessageHeader request = new MessageHeader(
                                                        new MessageId(
                                                                      unsigned16BitInteger(0x0F0F)),
                                                        new MessageHeaderFlags(
                                                                               false,
                                                                               InverseQuery,
                                                                               false,
                                                                               false,
                                                                               true,
                                                                               false,
                                                                               z,
                                                                               NoErrorCondition),
                                                        unsigned16BitInteger(2),
                                                        unsigned16BitInteger(0),
                                                        unsigned16BitInteger(0),
                                                        unsigned16BitInteger(0));
        final MessageHeader matchingReply = new MessageHeader(
                                                              new MessageId(
                                                                            unsigned16BitInteger(0x0F0F)),
                                                              new MessageHeaderFlags(
                                                                                     true,
                                                                                     InverseQuery,
                                                                                     true,
                                                                                     false,
                                                                                     true,
                                                                                     true,
                                                                                     z,
                                                                                     NameError),
                                                              unsigned16BitInteger(2),
                                                              unsigned16BitInteger(0),
                                                              unsigned16BitInteger(0),
                                                              unsigned16BitInteger(0));
        final MessageHeader nonMatchingReply1 = new MessageHeader(
                                                                  new MessageId(
                                                                                unsigned16BitInteger(0x0F00)),
                                                                  new MessageHeaderFlags(
                                                                                         true,
                                                                                         InverseQuery,
                                                                                         true,
                                                                                         false,
                                                                                         true,
                                                                                         true,
                                                                                         z,
                                                                                         NameError),
                                                                  unsigned16BitInteger(2),
                                                                  unsigned16BitInteger(0),
                                                                  unsigned16BitInteger(0),
                                                                  unsigned16BitInteger(0));
        final MessageHeader nonMatchingReply2 = new MessageHeader(
                                                                  new MessageId(
                                                                                unsigned16BitInteger(0x0F0F)),
                                                                  new MessageHeaderFlags(
                                                                                         true,
                                                                                         Query,
                                                                                         true,
                                                                                         false,
                                                                                         true,
                                                                                         true,
                                                                                         z,
                                                                                         NameError),
                                                                  unsigned16BitInteger(2),
                                                                  unsigned16BitInteger(0),
                                                                  unsigned16BitInteger(0),
                                                                  unsigned16BitInteger(0));

        assertTrue(request.matchesReply(matchingReply));
        assertFalse(request.matchesReply(nonMatchingReply1));
        assertFalse(request.matchesReply(nonMatchingReply2));
    }

    @Test
    public void serializesHeaderAsExpectedForAClientTalkingToAServer() {
        final MessageHeader header = new MessageHeader(
                                                       new MessageId(
                                                                     unsigned16BitInteger(0x0F0F)),
                                                       MessageHeaderFlags.Query,
                                                       unsigned16BitInteger(1),
                                                       unsigned16BitInteger(2),
                                                       unsigned16BitInteger(3),
                                                       unsigned16BitInteger(4));

        assertThat(serialize(header),
                   is(equalTo(new byte[] { 0x0F, 0x0F, 0x01, 0x00, 0x00, 0x01,
                                          0x00, 0x02, 0x00, 0x03, 0x00, 0x04 })));
    }
}
