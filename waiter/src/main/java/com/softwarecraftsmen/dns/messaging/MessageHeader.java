/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.MessageHeaderFlags.Query;
import static com.softwarecraftsmen.toString.ToString.string;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.One;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Zero;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;

import java.util.List;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public class MessageHeader implements Serializable {
    public static MessageHeader emptyReply(final MessageHeader messageHeader) {
        return new MessageHeader(
                                 messageHeader.messageId,
                                 MessageHeaderFlags.emptyReply(messageHeader.messageHeaderFlags),
                                 One, Zero, Zero, Zero);
    }

    public static MessageHeader outboundMessageHeader(final MessageId messageId,
                                                      final List<Question> questions) {
        return new MessageHeader(messageId, Query,
                                 unsigned16BitInteger(questions.size()), Zero,
                                 Zero, Zero);
    }

    private final MessageId messageId;
    private final MessageHeaderFlags messageHeaderFlags;
    private final Unsigned16BitInteger numberOfEntriesInQuestionSection;
    private final Unsigned16BitInteger numberOfResourceRecordsInAnswerSection;
    private final Unsigned16BitInteger numberOfNameServerRecordsInAuthoritySection;

    private final Unsigned16BitInteger numberOfResourceRecordsInAdditionalRecordsAnswerSection;

    public static final int SizeOfDnsMessageHeader = 12;

    public MessageHeader(final MessageId messageId,
                         final MessageHeaderFlags messageHeaderFlags,
                         final Unsigned16BitInteger numberOfEntriesInQuestionSection,
                         final Unsigned16BitInteger numberOfResourceRecordsInAnswerSection,
                         final Unsigned16BitInteger numberOfNameServerRecordsInAuthoritySection,
                         final Unsigned16BitInteger numberOfResourceRecordsInAdditionalRecordsAnswerSection) {
        this.messageId = messageId;
        this.messageHeaderFlags = messageHeaderFlags;
        this.numberOfEntriesInQuestionSection = numberOfEntriesInQuestionSection;
        this.numberOfResourceRecordsInAnswerSection = numberOfResourceRecordsInAnswerSection;
        this.numberOfNameServerRecordsInAuthoritySection = numberOfNameServerRecordsInAuthoritySection;
        this.numberOfResourceRecordsInAdditionalRecordsAnswerSection = numberOfResourceRecordsInAdditionalRecordsAnswerSection;
    }

    @Override
    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final MessageHeader that = (MessageHeader) o;

        if (!messageHeaderFlags.equals(that.messageHeaderFlags)) {
            return false;
        }
        if (!messageId.equals(that.messageId)) {
            return false;
        }
        if (!numberOfEntriesInQuestionSection.equals(that.numberOfEntriesInQuestionSection)) {
            return false;
        }
        if (!numberOfNameServerRecordsInAuthoritySection.equals(that.numberOfNameServerRecordsInAuthoritySection)) {
            return false;
        }
        if (!numberOfResourceRecordsInAdditionalRecordsAnswerSection.equals(that.numberOfResourceRecordsInAdditionalRecordsAnswerSection)) {
            return false;
        }
        if (!numberOfResourceRecordsInAnswerSection.equals(that.numberOfResourceRecordsInAnswerSection)) {
            return false;
        }
        return true;
    }

    public Unsigned16BitInteger getNumberOfEntriesInQuestionSection() {
        return numberOfEntriesInQuestionSection;
    }

    public Unsigned16BitInteger getNumberOfNameServerRecordsInAuthoritySection() {
        return numberOfNameServerRecordsInAuthoritySection;
    }

    public Unsigned16BitInteger getNumberOfResourceRecordsInAdditionalRecordsAnswerSection() {
        return numberOfResourceRecordsInAdditionalRecordsAnswerSection;
    }

    public Unsigned16BitInteger getNumberOfResourceRecordsInAnswerSection() {
        return numberOfResourceRecordsInAnswerSection;
    }

    @Override
    public int hashCode() {
        int result;
        result = messageId.hashCode();
        result = 31 * result + messageHeaderFlags.hashCode();
        result = 31 * result + numberOfEntriesInQuestionSection.hashCode();
        result = 31 * result
                 + numberOfResourceRecordsInAnswerSection.hashCode();
        result = 31 * result
                 + numberOfNameServerRecordsInAuthoritySection.hashCode();
        result = 31
                 * result
                 + numberOfResourceRecordsInAdditionalRecordsAnswerSection.hashCode();
        return result;
    }

    @SuppressWarnings({ "SimplifiableIfStatement" })
    public boolean matchesReply(final MessageHeader reply) {
        if (!messageId.equals(reply.messageId)) {
            return false;
        }
        if (!messageHeaderFlags.matchesReply(reply.messageHeaderFlags)) {
            return false;
        }
        return numberOfEntriesInQuestionSection.equals(reply.numberOfEntriesInQuestionSection);
    }

    public void serialize(final AtomicWriter writer) {
        messageId.serialize(writer);
        messageHeaderFlags.serialize(writer);
        writer.writeUnsigned16BitInteger(numberOfEntriesInQuestionSection);
        writer.writeUnsigned16BitInteger(numberOfResourceRecordsInAnswerSection);
        writer.writeUnsigned16BitInteger(numberOfNameServerRecordsInAuthoritySection);
        writer.writeUnsigned16BitInteger(numberOfResourceRecordsInAdditionalRecordsAnswerSection);
    }

    @Override
    public String toString() {
        return string(this, messageId, messageHeaderFlags,
                      numberOfEntriesInQuestionSection,
                      numberOfResourceRecordsInAnswerSection,
                      numberOfNameServerRecordsInAuthoritySection,
                      numberOfResourceRecordsInAdditionalRecordsAnswerSection);
    }
}
