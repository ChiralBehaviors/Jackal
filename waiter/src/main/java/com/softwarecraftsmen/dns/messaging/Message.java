/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.MessageHeader.outboundMessageHeader;
import static com.softwarecraftsmen.dns.messaging.MessageId.messageId;
import static com.softwarecraftsmen.toString.ToString.string;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;

public class Message implements Serializable {
    @SuppressWarnings({ "unchecked" })
    public static <T extends Serializable> Set<T> allAnswersMatching(final Collection<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecords,
                                                                     final InternetClassType internetClassType) {
        final Set<T> set = new LinkedHashSet<T>(resourceRecords.size());
        for (ResourceRecord<? extends Name, ? extends Serializable> answer : resourceRecords) {
            answer.appendDataIfIs(internetClassType, (Set) set);
        }
        return set;
    }

    public static Message emptyReply(final Message request) {
        return new Message(MessageHeader.emptyReply(request.messageHeader),
                           request.questions, NoResourceRecords,
                           NoResourceRecords, NoResourceRecords);
    }

    public static Message query(final MessageId messageId,
                                final Question... questions) {
        final List<Question> questionList = asList(questions);
        return new Message(outboundMessageHeader(messageId, questionList),
                           questionList, NoResourceRecords, NoResourceRecords,
                           NoResourceRecords);
    }

    public static Message query(final Question... questions) {
        return query(messageId(), questions);
    }

    private final MessageHeader messageHeader;
    private final List<Question> questions;

    private final List<ResourceRecord<? extends Name, ? extends Serializable>> answers;

    private final List<ResourceRecord<? extends Name, ? extends Serializable>> nameServerAuthorities;

    private final List<ResourceRecord<? extends Name, ? extends Serializable>> additionalRecords;

    public static final List<ResourceRecord<? extends Name, ? extends Serializable>> NoResourceRecords = emptyList();

    public Message(final MessageHeader messageHeader,
                   final List<Question> questions,
                   final List<ResourceRecord<? extends Name, ? extends Serializable>> answers,
                   final List<ResourceRecord<? extends Name, ? extends Serializable>> nameServerAuthorities,
                   final List<ResourceRecord<? extends Name, ? extends Serializable>> additionalRecords) {
        this.messageHeader = messageHeader;
        this.questions = questions;
        this.answers = answers;
        this.nameServerAuthorities = nameServerAuthorities;
        this.additionalRecords = additionalRecords;
    }

    @SuppressWarnings({ "unchecked" })
    public <T extends Serializable> Set<T> allAnswersMatching(final InternetClassType internetClassType) {
        return allAnswersMatching(answers, internetClassType);
    }

    public Set<ResourceRecord<? extends Name, ? extends Serializable>> allResourceRecords() {
        return new LinkedHashSet<ResourceRecord<? extends Name, ? extends Serializable>>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                addAll(answers);
                addAll(nameServerAuthorities);
                addAll(additionalRecords);
            }
        };
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

        final Message message = (Message) o;

        if (!additionalRecords.equals(message.additionalRecords)) {
            return false;
        }
        if (!answers.equals(message.answers)) {
            return false;
        }
        if (!messageHeader.equals(message.messageHeader)) {
            return false;
        }
        if (!nameServerAuthorities.equals(message.nameServerAuthorities)) {
            return false;
        }
        if (!questions.equals(message.questions)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = messageHeader.hashCode();
        result = 31 * result + questions.hashCode();
        result = 31 * result + answers.hashCode();
        result = 31 * result + nameServerAuthorities.hashCode();
        result = 31 * result + additionalRecords.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        messageHeader.serialize(writer);
        writeDnsQuestions(writer);
        writeResourceRecordsWhichAnswerTheQuestion(writer);
        writeResourceRecordsWhichPointToTheDomainAuthority(writer);
        writeResourceRecordsWhichMayHoldAdditionalInformation(writer);
    }

    @Override
    public String toString() {
        return string(this, messageHeader, questions, answers,
                      nameServerAuthorities, additionalRecords);
    }

    private void writeDnsQuestions(final AtomicWriter writer) {
        for (Question question : questions) {
            question.serialize(writer);
        }
    }

    private void writeResourceRecordsWhichAnswerTheQuestion(final AtomicWriter writer) {
        for (ResourceRecord<? extends Name, ? extends Serializable> answer : answers) {
            answer.serialize(writer);
        }
    }

    private void writeResourceRecordsWhichMayHoldAdditionalInformation(final AtomicWriter writer) {
        for (ResourceRecord<? extends Name, ? extends Serializable> additionalRecord : additionalRecords) {
            additionalRecord.serialize(writer);
        }
    }

    // These MUST always (on send or receive) be of InternetClassType.NS
    private void writeResourceRecordsWhichPointToTheDomainAuthority(final AtomicWriter writer) {
        for (ResourceRecord<? extends Name, ? extends Serializable> nameServerAuthority : nameServerAuthorities) {
            nameServerAuthority.serialize(writer);
        }
    }
}
