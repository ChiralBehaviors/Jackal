package com.softwarecraftsmen.dns.messaging.deserializer;

import static com.softwarecraftsmen.dns.MailBox.mailBox;
import static com.softwarecraftsmen.dns.Seconds.seconds;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolAddress;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.internetClassType;
import static com.softwarecraftsmen.dns.messaging.OperationCode.operationCode;
import static com.softwarecraftsmen.dns.messaging.QClass.qclass;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.responseCode;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Four;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Sixteen;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.CanNeverHappenException;
import com.softwarecraftsmen.dns.HostInformation;
import com.softwarecraftsmen.dns.MailBox;
import com.softwarecraftsmen.dns.MailExchange;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.ServiceInformation;
import com.softwarecraftsmen.dns.StatementOfAuthority;
import com.softwarecraftsmen.dns.Text;
import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.messaging.GenericName;
import com.softwarecraftsmen.dns.messaging.GenericResourceRecordData;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.MessageHeaderFlags;
import com.softwarecraftsmen.dns.messaging.MessageId;
import com.softwarecraftsmen.dns.messaging.OperationCode;
import com.softwarecraftsmen.dns.messaging.QClass;
import com.softwarecraftsmen.dns.messaging.ResponseCode;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger;

public class AtomicReader {
    private final ByteArrayReader reader;

    public AtomicReader(final ByteArrayReader reader) {
        this.reader = reader;
    }

    public void checkLength(final Unsigned16BitInteger expectedLength)
                                                                      throws BadlyFormedDnsMessageException {
        final Unsigned16BitInteger lengthOfData = readLength();
        if (!lengthOfData.equals(expectedLength)) {
            throw new BadlyFormedDnsMessageException(
                                                     format(UK,
                                                            "Expected length %1$s but was %2$s",
                                                            expectedLength,
                                                            lengthOfData));
        }
    }

    public void moveToOffset(final int offset) {
        reader.moveToOffset(offset);
    }

    public QClass readClass() throws BadlyFormedDnsMessageException {
        try {
            return qclass(reader.readUnsigned16BitInteger());
        } catch (IllegalArgumentException e) {
            throw new BadlyFormedDnsMessageException(
                                                     "Could not understand QClass value",
                                                     e);
        }
    }

    public GenericResourceRecordData readData() {
        final Unsigned16BitInteger lengthOfData = readLength();
        return new GenericResourceRecordData(
                                             reader.readRawByteArray(lengthOfData));
    }

    public DomainName readDomainName() {
        return new DomainName(readLabels());
    }

    public GenericName readGenericName() {
        return new GenericName(readLabels());
    }

    public HostInformation readHostInformation() {
        readLength();
        return new HostInformation(reader.readAsciiString(readLength()),
                                   reader.readAsciiString(readLength()));
    }

    public HostName readHostName() {
        return new HostName(readLabels());
    }

    public InternetClassType readInternetClassType()
                                                    throws BadlyFormedDnsMessageException {
        try {
            return internetClassType(reader.readUnsigned16BitInteger());
        } catch (IllegalArgumentException e) {
            throw new BadlyFormedDnsMessageException(
                                                     "Could not understand QClass value",
                                                     e);
        }
    }

    public SerializableInternetProtocolAddress<Inet4Address> readInternetProtocolVersion4Address() {
        final byte[] rawBytes = reader.readRawByteArray(Four);
        try {
            return serializableInternetProtocolAddress((Inet4Address) InetAddress.getByAddress(rawBytes));
        } catch (UnknownHostException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public SerializableInternetProtocolAddress<Inet6Address> readInternetProtocolVersion6Address() {
        final byte[] rawBytes = reader.readRawByteArray(Sixteen);
        try {
            return serializableInternetProtocolAddress((Inet6Address) InetAddress.getByAddress(rawBytes));
        } catch (UnknownHostException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public Unsigned16BitInteger readLength() {
        return reader.readUnsigned16BitInteger();
    }

    public MailBox readMailBox() throws BadlyFormedDnsMessageException {
        final List<SimpleLabel> labels = readLabels();
        if (labels.size() < 2) {
            throw new BadlyFormedDnsMessageException(
                                                     "A mailbox must have more than one label");
        }
        final Label userName = labels.get(0);
        return mailBox(userName.toStringRepresentation(),
                       new DomainName(labels.subList(1, labels.size() - 1)));
    }

    public MailExchange readMailExchange() {
        return new MailExchange(reader.readUnsigned16BitInteger(),
                                readHostName());
    }

    public MessageHeaderFlags readMessageHeaderFlags()
                                                      throws BadlyFormedDnsMessageException,
                                                      TruncatedDnsMessageException {
        reader.moveToOffset(2);
        final Unsigned16BitInteger unsigned16BitInteger = reader.readUnsigned16BitInteger();
        final boolean isResponse = unsigned16BitInteger.getBitIetf(0);
        final OperationCode operationCode;
        try {
            operationCode = operationCode(unsigned16BitInteger.getUnsigned4BitIntegerIetf(1));
        } catch (IllegalArgumentException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     "Could not deserialize header flag operation code",
                                                     exception);
        }
        final boolean authoritativeAnswer = unsigned16BitInteger.getBitIetf(5);
        final boolean truncation = unsigned16BitInteger.getBitIetf(6);
        if (isResponse && truncation) {
            throw new TruncatedDnsMessageException();
        }
        final boolean recursionDesired = unsigned16BitInteger.getBitIetf(7);
        final boolean recursionAvailable = unsigned16BitInteger.getBitIetf(8);
        final Unsigned3BitInteger z = unsigned16BitInteger.getThreeBitsIetf(9);

        final ResponseCode responseCode;
        try {
            responseCode = responseCode(unsigned16BitInteger.getUnsigned4BitIntegerIetf(12));
        } catch (IllegalArgumentException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     "Could not deserialize header flag response code",
                                                     exception);
        }
        return new MessageHeaderFlags(isResponse, operationCode,
                                      authoritativeAnswer, truncation,
                                      recursionDesired, recursionAvailable, z,
                                      responseCode);
    }

    public MessageId readMessageId() {
        reader.moveToOffset(0);
        return new MessageId(reader.readUnsigned16BitInteger());
    }

    public Unsigned16BitInteger readNumberOfEntriesInQuestionSection() {
        reader.moveToOffset(4);
        return readLength();
    }

    public Unsigned16BitInteger readNumberOfNameServerRecordsInAuthoritySection() {
        reader.moveToOffset(8);
        return readLength();
    }

    public Unsigned16BitInteger readNumberOfResourceRecordsInAnswerSection() {
        reader.moveToOffset(6);
        return readLength();
    }

    public Unsigned16BitInteger readNumberOfResourceRecordsInTheAdditionalRecordsAnswerSection() {
        reader.moveToOffset(10);
        return readLength();
    }

    public ServiceInformation readServiceInformation() {
        return new ServiceInformation(reader.readUnsigned16BitInteger(),
                                      reader.readUnsigned16BitInteger(),
                                      reader.readUnsigned16BitInteger(),
                                      readHostName());
    }

    public StatementOfAuthority readStatementOfAuthority()
                                                          throws BadlyFormedDnsMessageException {
        return new StatementOfAuthority(readHostName(), readMailBox(),
                                        reader.readUnsigned32BitInteger(),
                                        readTimeToLive(), readTimeToLive(),
                                        readTimeToLive());
    }

    public Text readText() {
        final List<String> lines = new ArrayList<String>();
        final Unsigned16BitInteger lengthOfData = readLength();
        final long finalPosition = reader.currentPosition()
                                   + lengthOfData.toLong();
        while (finalPosition > reader.currentPosition()) {
            lines.add(reader.readAsciiString(readLength()));
        }
        return new Text(lines);
    }

    public Seconds readTimeToLive() {
        return seconds(reader.readUnsigned32BitInteger());
    }

    private List<SimpleLabel> readLabels() {
        final LabelsReader labelsReader = new LabelsReader(reader);
        final List<SimpleLabel> labels = new ArrayList<SimpleLabel>();
        labelsReader.readLabels(labels);
        return labels;
    }

}
