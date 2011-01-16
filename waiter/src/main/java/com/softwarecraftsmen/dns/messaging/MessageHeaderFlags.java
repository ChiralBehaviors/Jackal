/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.OperationCode.Status;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.NoErrorCondition;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.ServerFailure;
import static com.softwarecraftsmen.toString.ToString.string;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Zero;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger;

public class MessageHeaderFlags implements Serializable {
    public static final MessageHeaderFlags Query = new MessageHeaderFlags(
                                                                          false,
                                                                          OperationCode.Query,
                                                                          false,
                                                                          false,
                                                                          true,
                                                                          false,
                                                                          Unsigned3BitInteger.Zero,
                                                                          NoErrorCondition);

    public static MessageHeaderFlags emptyReply(final MessageHeaderFlags messageHeaderFlags) {
        return new MessageHeaderFlags(true, Status, false, false,
                                      messageHeaderFlags.recursionDesired,
                                      false, Unsigned3BitInteger.Zero,
                                      ServerFailure);
    }

    public static MessageHeaderFlags reply(final boolean recursionDesired) {
        return new MessageHeaderFlags(true, Status, false, false,
                                      recursionDesired, false,
                                      Unsigned3BitInteger.Zero,
                                      NoErrorCondition);
    }

    private final boolean isResponse;
    private final OperationCode operationCode;
    private final boolean authoritativeAnswer;
    private final boolean truncation;
    private final boolean recursionDesired;
    private final boolean recursionAvailable;

    private final Unsigned3BitInteger z;

    private final ResponseCode responseCode;

    public MessageHeaderFlags(final boolean isResponse,
                              final OperationCode operationCode,
                              final boolean authoritativeAnswer,
                              final boolean truncation,
                              final boolean recursionDesired,
                              final boolean recursionAvailable,
                              final Unsigned3BitInteger z,
                              final ResponseCode responseCode) {
        this.isResponse = isResponse;
        this.operationCode = operationCode;
        this.authoritativeAnswer = authoritativeAnswer;
        this.truncation = truncation;
        this.recursionDesired = recursionDesired;
        this.recursionAvailable = recursionAvailable;
        this.z = z;
        this.responseCode = responseCode;
        if (!isResponse) {
            if (authoritativeAnswer) {
                throw new IllegalArgumentException(
                                                   "Queries (isReponse=false) can not have authoritativeAnswer=true");
            }
            if (truncation) {
                throw new IllegalArgumentException(
                                                   "Queries (isReponse=false) can not have truncation=true");
            }
            if (recursionAvailable) {
                throw new IllegalArgumentException(
                                                   "Queries (isReponse=false) can not have recursionAvailable=true");
            }
            if (responseCode.isError()) {
                throw new IllegalArgumentException(
                                                   "Queries (isReponse=false) can not have responseCode.isError()=true");
            }
        }
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

        final MessageHeaderFlags that = (MessageHeaderFlags) o;

        if (authoritativeAnswer != that.authoritativeAnswer) {
            return false;
        }
        if (isResponse != that.isResponse) {
            return false;
        }
        if (recursionAvailable != that.recursionAvailable) {
            return false;
        }
        if (recursionDesired != that.recursionDesired) {
            return false;
        }
        if (truncation != that.truncation) {
            return false;
        }
        if (operationCode != that.operationCode) {
            return false;
        }
        if (!z.equals(that.z)) {
            return false;
        }
        if (responseCode != that.responseCode) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = isResponse ? 1 : 0;
        result = 31 * result + operationCode.hashCode();
        result = 31 * result + (authoritativeAnswer ? 1 : 0);
        result = 31 * result + (truncation ? 1 : 0);
        result = 31 * result + (recursionDesired ? 1 : 0);
        result = 31 * result + (recursionAvailable ? 1 : 0);
        result = 31 * result + responseCode.hashCode();
        return result;
    }

    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean matchesReply(final MessageHeaderFlags reply) {
        if (isResponse) {
            return false;
        }
        if (!reply.isResponse) {
            return false;
        }
        if (!operationCode.equals(reply.operationCode)) {
            return false;
        }
        if (recursionDesired != reply.recursionDesired) {
            return false;
        }
        return true;
    }

    public void serialize(final AtomicWriter writer) {
        Unsigned16BitInteger unsigned16BitInteger = Zero.setBitIetf(isResponse,
                                                                    1);
        unsigned16BitInteger = operationCode.set4Bits(unsigned16BitInteger);
        unsigned16BitInteger = unsigned16BitInteger.setBitIetf(authoritativeAnswer,
                                                               5);
        unsigned16BitInteger = unsigned16BitInteger.setBitIetf(truncation, 6);
        unsigned16BitInteger = unsigned16BitInteger.setBitIetf(recursionDesired,
                                                               7);
        unsigned16BitInteger = unsigned16BitInteger.setBitPowerOfTwo(recursionAvailable,
                                                                     1);
        unsigned16BitInteger = responseCode.set4Bits(unsigned16BitInteger);
        writer.writeUnsigned16BitInteger(unsigned16BitInteger);
    }

    @Override
    public String toString() {
        return string(this, isResponse, operationCode, authoritativeAnswer,
                      truncation, recursionDesired, recursionAvailable,
                      responseCode);
    }
}
