package com.softwarecraftsmen.dns.messaging.deserializer;

import static com.softwarecraftsmen.ConvenientArrayList.toList;
import static com.softwarecraftsmen.ConvenientArrayList.toResourceRecordList;
import static com.softwarecraftsmen.dns.Seconds.seconds;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolVersion4Address;
import static com.softwarecraftsmen.dns.messaging.GenericName.genericName;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.A;
import static com.softwarecraftsmen.dns.messaging.OperationCode.Status;
import static com.softwarecraftsmen.dns.messaging.Question.internetQuestion;
import static com.softwarecraftsmen.dns.messaging.ResponseCode.NoErrorCondition;
import static com.softwarecraftsmen.dns.names.DomainName.domainName;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.dns.resourceRecords.CanonicalNameResourceRecord.canonicalNameResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion4AddressResourceRecord.internetProtocolVersion4AddressResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.NameServerResourceRecord.nameServerResourceRecord;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger.unsigned3BitInteger;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.UnknownHostException;

import org.junit.Test;

import com.softwarecraftsmen.dns.messaging.GenericName;
import com.softwarecraftsmen.dns.messaging.Message;
import com.softwarecraftsmen.dns.messaging.MessageHeader;
import com.softwarecraftsmen.dns.messaging.MessageHeaderFlags;
import com.softwarecraftsmen.dns.messaging.MessageId;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger;

public class MessageDeserializerTest {
    private static final GenericName AliasName = genericName("mail.google.com");
    private static final HostName CanonicalName = hostName("googlemail.l.google.com");
    private static final DomainName DomainName = domainName("l.google.com");
    private static final HostName NameServer1 = hostName("a.l.google.com");
    private static final HostName NameServer2 = hostName("b.l.google.com");
    private static final HostName NameServer3 = hostName("c.l.google.com");
    private static final HostName NameServer4 = hostName("d.l.google.com");
    private static final HostName NameServer5 = hostName("e.l.google.com");
    private static final HostName NameServer6 = hostName("f.l.google.com");
    private static final HostName NameServer7 = hostName("g.l.google.com");

    private static final byte[] typicalResponseMessage = new byte[] {
                                                                     // Bytes 0 - 11: Header
                                                                     0,
                                                                     10,
                                                                     -127,
                                                                     -128,
                                                                     0,
                                                                     1,
                                                                     0,
                                                                     5,
                                                                     0,
                                                                     7,
                                                                     0,
                                                                     7,
                                                                     // Bytes 12-32: Question
                                                                     4,
                                                                     109,
                                                                     97,
                                                                     105,
                                                                     108,
                                                                     6,
                                                                     103,
                                                                     111,
                                                                     111,
                                                                     103,
                                                                     108,
                                                                     101,
                                                                     3,
                                                                     99,
                                                                     111,
                                                                     109,
                                                                     0,
                                                                     0,
                                                                     1,
                                                                     0,
                                                                     1,
                                                                     // Byte 33: First resource record byte
                                                                     // AbstractName: Offset to labels [mail.google.com]
                                                                     -64,
                                                                     12,

                                                                     0,
                                                                     5,
                                                                     0,
                                                                     1,
                                                                     0,
                                                                     0,
                                                                     -78,
                                                                     106,
                                                                     0,
                                                                     15,
                                                                     // label: googlemail
                                                                     10,
                                                                     103,
                                                                     111,
                                                                     111,
                                                                     103,
                                                                     108,
                                                                     101,
                                                                     109,
                                                                     97,
                                                                     105,
                                                                     108,
                                                                     // label: l
                                                                     1,
                                                                     108,
                                                                     // label: offset [google.com]
                                                                     -64,
                                                                     17,
                                                                     // label: offset [googlemail.l.[google.com] ] - offset to name, which has an offset to name...
                                                                     -64,
                                                                     45,
                                                                     // ?
                                                                     0, 1, 0,
                                                                     1, 0, 0,
                                                                     0, -87, 0,
                                                                     4, 64,
                                                                     -23, -73,
                                                                     83, -64,
                                                                     45, 0, 1,
                                                                     0, 1, 0,
                                                                     0, 0, -87,
                                                                     0, 4, 64,
                                                                     -23, -73,
                                                                     17, -64,
                                                                     45, 0, 1,
                                                                     0, 1, 0,
                                                                     0, 0, -87,
                                                                     0, 4, 64,
                                                                     -23, -73,
                                                                     18, -64,
                                                                     45, 0, 1,
                                                                     0, 1, 0,
                                                                     0, 0, -87,
                                                                     0, 4, 64,
                                                                     -23, -73,
                                                                     19, -64,
                                                                     56, 0, 2,
                                                                     0, 1, 0,
                                                                     0, 65,
                                                                     -100, 0,
                                                                     4, 1, 100,
                                                                     -64, 56,
                                                                     -64, 56,
                                                                     0, 2, 0,
                                                                     1, 0, 0,
                                                                     65, -100,
                                                                     0, 4, 1,
                                                                     101, -64,
                                                                     56, -64,
                                                                     56, 0, 2,
                                                                     0, 1, 0,
                                                                     0, 65,
                                                                     -100, 0,
                                                                     4, 1, 102,
                                                                     -64, 56,
                                                                     -64, 56,
                                                                     0, 2, 0,
                                                                     1, 0, 0,
                                                                     65, -100,
                                                                     0, 4, 1,
                                                                     103, -64,
                                                                     56, -64,
                                                                     56, 0, 2,
                                                                     0, 1, 0,
                                                                     0, 65,
                                                                     -100, 0,
                                                                     4, 1, 97,
                                                                     -64, 56,
                                                                     -64, 56,
                                                                     0, 2, 0,
                                                                     1, 0, 0,
                                                                     65, -100,
                                                                     0, 4, 1,
                                                                     98, -64,
                                                                     56, -64,
                                                                     56, 0, 2,
                                                                     0, 1, 0,
                                                                     0, 65,
                                                                     -100, 0,
                                                                     4, 1, 99,
                                                                     -64, 56,
                                                                     -64, -56,
                                                                     0, 1, 0,
                                                                     1, 0, 0,
                                                                     -37, 23,
                                                                     0, 4, -47,
                                                                     85, -117,
                                                                     9, -64,
                                                                     -40, 0, 1,
                                                                     0, 1, 0,
                                                                     0, 80, 91,
                                                                     0, 4, 64,
                                                                     -23, -77,
                                                                     9, -64,
                                                                     -24, 0, 1,
                                                                     0, 1, 0,
                                                                     0, 80, 91,
                                                                     0, 4, 64,
                                                                     -23, -95,
                                                                     9, -64,
                                                                     -120, 0,
                                                                     1, 0, 1,
                                                                     0, 0, 68,
                                                                     38, 0, 4,
                                                                     66, -7,
                                                                     93, 9,
                                                                     -64, -104,
                                                                     0, 1, 0,
                                                                     1, 0, 0,
                                                                     74, 75, 0,
                                                                     4, -47,
                                                                     85, -119,
                                                                     9, -64,
                                                                     -88, 0, 1,
                                                                     0, 1, 0,
                                                                     0, -35,
                                                                     -123, 0,
                                                                     4, 72, 14,
                                                                     -21, 9,
                                                                     -64, -72,
                                                                     0, 1, 0,
                                                                     1, 0, 0,
                                                                     68, 38, 0,
                                                                     4, 64,
                                                                     -23, -89,
                                                                     9 };

    // Additional records are usually the A name records for the NS records returned in the NS authorities; for MX requests they are A records for the servers
    // AbstractName server authorities section can contain NS (for A requests) or SOA (for weirder requests)
    @Test
    public void deserializeResourceRecordsInAnswers()
                                                     throws BadlyFormedDnsMessageException,
                                                     UnknownHostException,
                                                     TruncatedDnsMessageException {
        final Unsigned3BitInteger z = unsigned3BitInteger(6);
        final Message expectedMessage = new Message(
                                                    new MessageHeader(
                                                                      new MessageId(
                                                                                    unsigned16BitInteger(10)),
                                                                      new MessageHeaderFlags(
                                                                                             true,
                                                                                             Status,
                                                                                             false,
                                                                                             false,
                                                                                             true,
                                                                                             true,
                                                                                             z,
                                                                                             NoErrorCondition),
                                                                      unsigned16BitInteger(1),
                                                                      unsigned16BitInteger(5),
                                                                      unsigned16BitInteger(7),
                                                                      unsigned16BitInteger(7)),
                                                    toList(internetQuestion(AliasName,
                                                                            A)),
                                                    toResourceRecordList(canonicalNameResourceRecord(AliasName.toHostName(),
                                                                                                     seconds(45674),
                                                                                                     CanonicalName),
                                                                         internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                                                                       seconds(169),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   183,
                                                                                                                                                                   83)),
                                                                         internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                                                                       seconds(169),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   183,
                                                                                                                                                                   17)),
                                                                         internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                                                                       seconds(169),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   183,
                                                                                                                                                                   18)),
                                                                         internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                                                                       seconds(169),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   183,
                                                                                                                                                                   19))),
                                                    toResourceRecordList(nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer4),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer5),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer6),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer7),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer1),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer2),
                                                                         nameServerResourceRecord(DomainName,
                                                                                                  seconds(16796),
                                                                                                  NameServer3)),
                                                    toResourceRecordList(internetProtocolVersion4AddressResourceRecord(NameServer1,
                                                                                                                       seconds(56087),
                                                                                                                       serializableInternetProtocolVersion4Address(209,
                                                                                                                                                                   85,
                                                                                                                                                                   139,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer2,
                                                                                                                       seconds(20571),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   179,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer3,
                                                                                                                       seconds(20571),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   161,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer4,
                                                                                                                       seconds(17446),
                                                                                                                       serializableInternetProtocolVersion4Address(66,
                                                                                                                                                                   249,
                                                                                                                                                                   93,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer5,
                                                                                                                       seconds(19019),
                                                                                                                       serializableInternetProtocolVersion4Address(209,
                                                                                                                                                                   85,
                                                                                                                                                                   137,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer6,
                                                                                                                       seconds(56709),
                                                                                                                       serializableInternetProtocolVersion4Address(72,
                                                                                                                                                                   14,
                                                                                                                                                                   235,
                                                                                                                                                                   9)),
                                                                         internetProtocolVersion4AddressResourceRecord(NameServer7,
                                                                                                                       seconds(17446),
                                                                                                                       serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                   233,
                                                                                                                                                                   167,
                                                                                                                                                                   9))));
        final Message actualMessage = new MessageDeserializer(
                                                              typicalResponseMessage).readMessage();
        assertThat(actualMessage, is(equalTo(expectedMessage)));
    }
}
