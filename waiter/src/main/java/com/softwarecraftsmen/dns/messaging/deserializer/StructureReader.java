package com.softwarecraftsmen.dns.messaging.deserializer;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.messaging.GenericName;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.MessageHeader;
import com.softwarecraftsmen.dns.messaging.QClass;
import com.softwarecraftsmen.dns.messaging.Question;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;

public class StructureReader {
    private final AtomicReader reader;

    public StructureReader(final AtomicReader reader) {
        this.reader = reader;
    }

    public MessageHeader readMessageHeader()
                                            throws BadlyFormedDnsMessageException,
                                            TruncatedDnsMessageException {
        return new MessageHeader(
                                 reader.readMessageId(),
                                 reader.readMessageHeaderFlags(),
                                 reader.readNumberOfEntriesInQuestionSection(),
                                 reader.readNumberOfResourceRecordsInAnswerSection(),
                                 reader.readNumberOfNameServerRecordsInAuthoritySection(),
                                 reader.readNumberOfResourceRecordsInTheAdditionalRecordsAnswerSection());
    }

    public Question readQuestion() throws BadlyFormedDnsMessageException {
        return new Question(reader.readGenericName(),
                            reader.readInternetClassType(), reader.readClass());
    }

    public ResourceRecord<? extends Name, ? extends Serializable> readResourceRecord()
                                                                                      throws BadlyFormedDnsMessageException {
        final GenericName owner = reader.readGenericName();
        final InternetClassType internetClassType = reader.readInternetClassType();
        final QClass qClass = reader.readClass();
        final Seconds timeToLive = reader.readTimeToLive();
        try {
            return internetClassType.createResourceRecord(owner, qClass,
                                                          timeToLive, reader);
        } catch (BadlyFormedDnsMessageException cause) {
            throw new BadlyFormedDnsMessageException(
                                                     "Could not read resource record data",
                                                     cause);
        }
    }
}
