/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.resourceRecords.CanonicalNameResourceRecord.canonicalNameResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.GenericResourceRecord.genericResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.HostInformationResourceRecord.hostInformationResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion4AddressResourceRecord.internetProtocolVersion4AddressResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion6AddressResourceRecord.internetProtocolVersion6AddressResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.MailExchangeResourceRecord.mailExchangeResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.NameServerResourceRecord.nameServerResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.PointerResourceRecord.pointerResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.ServiceInformationResourceRecord.serviceInformationResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.StatementOfAuthorityResourceRecord.statementOfAuthorityResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.TextResourceRecord.textResourceRecord;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Four;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Sixteen;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static java.lang.String.format;
import static java.util.Locale.UK;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.messaging.deserializer.AtomicReader;
import com.softwarecraftsmen.dns.messaging.deserializer.BadlyFormedDnsMessageException;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public enum InternetClassType implements Serializable {
    A(1, "a host address") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.checkLength(Four);
            return internetProtocolVersion4AddressResourceRecord(owner.toHostName(),
                                                                 timeToLive,
                                                                 reader.readInternetProtocolVersion4Address());
        }
    },
    NS(2, "an authoritative name server") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return nameServerResourceRecord(owner.toDomainName(), timeToLive,
                                            reader.readHostName());
        }
    },
    MD(3, "a mail destination (Obsolete - use MX)"),
    MF(4, "a mail forwarder (Obsolete - use MX)"),
    CNAME(5, "the canonical name for an alias") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return canonicalNameResourceRecord(owner.toHostName(), timeToLive,
                                               reader.readHostName());
        }
    },
    SOA(6, "marks the start of a zone of authority") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return statementOfAuthorityResourceRecord(owner.toDomainName(),
                                                      timeToLive,
                                                      reader.readStatementOfAuthority());
        }
    },
    MB(7, "a mailbox domain name (EXPERIMENTAL)"),
    MG(8, "a mail group member (EXPERIMENTAL)"),
    MR(9, "a mail rename domain name (EXPERIMENTAL"),
    NULL(10, "a null RR (EXPERIMENTAL)"),
    WKS(11, "a well known service description"),
    PTR(12, "a domain name pointer") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return pointerResourceRecord(owner.toPointerName(), timeToLive,
                                         reader.readHostName());
        }
    },
    HINFO(13, "host information") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            return hostInformationResourceRecord(owner.toHostName(),
                                                 timeToLive,
                                                 reader.readHostInformation());
        }
    },
    MINFO(14, "mailbox or mail list information"),
    MX(15, "mail exchange") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return mailExchangeResourceRecord(owner.toDomainName(), timeToLive,
                                              reader.readMailExchange());
        }
    },
    TXT(16, "text strings") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            return textResourceRecord(owner.toHostName(), timeToLive,
                                      reader.readText());
        }
    },
    RP(17, "for Responsible Person"),
    AFSDB(18, "for AFS Data Base location"),
    X25(19, "for X.25 PSDN address"),
    ISDN(20, "for ISDN address"),
    RT(21, "for Route Through"),
    NSAP(22, "for NSAP address, NSAP style A record"),
    NSAP_PTR(23, "(Unknown)"),
    SIG(24, "for security signature"),
    KEY(25, "for security key"),
    PX(26, "X.400 mail mapping information"),
    GPOS(27, "Geographical Position"),
    AAAA(28, "IP6 Address") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.checkLength(Sixteen);
            return internetProtocolVersion6AddressResourceRecord(owner.toHostName(),
                                                                 timeToLive,
                                                                 reader.readInternetProtocolVersion6Address());
        }
    },
    LOC(29, "Location Information"),
    NXT(30, "Next DomainName - OBSOLETE"),
    EID(31, "Endpoint Identifier"),
    NIMLOC(32, "Nimrod Locator"),
    SRV(33, "Server Selection") {

        @Override
        public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                           final QClass qClass,
                                                                                           final Seconds timeToLive,
                                                                                           final AtomicReader reader)
                                                                                                                     throws BadlyFormedDnsMessageException {
            reader.readLength();
            return serviceInformationResourceRecord(owner.toServiceName(),
                                                    timeToLive,
                                                    reader.readServiceInformation());
        }
    },
    ATMA(34, "ATM Address"), NAPTR(35, "Naming Authority Pointer"),
    KX(36, "Key Exchanger"), CERT(37, "CERT"), A6(38, "A6"),
    DNAME(39, "DNAME"), SINK(40, "SINK"), OPT(41, "OPT"), APL(42, "APL"),
    DS(43, "Delegation Signer"), SSHFP(44, "SSH Key Fingerprint"),
    IPSECKEY(45, "IPSECKEY"), RRSIG(46, "RRSIG"), NSEC(47, "NSEC"),
    DNSKEY(48, "DNSKEY"), DHCID(49, "DHCID"), NSEC3(50, "NSEC3"),
    NSEC3PARAM(51, "NSEC3PARAM"), HIP(55, "HostName Identity Protocol"),
    SPF(99, "(Unknown)"), UINFO(100, "(Unknown)"), UID(101, "(Unknown)"),
    GID(102, "(Unknown)"), UNSPEC(103, "(Unknown)"),
    TKEY(249, "Transaction Key"),
    TSIG(250, "Transaction Signature"),
    IXFR(251, "incremental transfer"),

    // Stictly speaking, these are QTYPE not TYPE and are only included in the superset
    AXFR(252, "transfer of an entire zone"),
    MAILB(253, "mailbox-related RRs (MB, MG or MR)"),
    MAILA(254, "mail agent RRs (Obsolete - see MX)"),
    Asterisk(255, "A request for all records"),

    TA(32768, "DNSSEC Trust Authorities"), DLV(32769,
                                               "DNSSEC Lookaside Validation");

    public static InternetClassType internetClassType(final Unsigned16BitInteger value) {
        for (InternetClassType internetClassType : values()) {
            if (internetClassType.value.equals(value)) {
                return internetClassType;
            }
        }
        throw new IllegalArgumentException(
                                           format(UK,
                                                  "Unrecognised internet class type code %1$s",
                                                  value));
    }

    private final Unsigned16BitInteger value;

    private final String description;

    private InternetClassType(final int value, final String description) {
        this.value = unsigned16BitInteger(value);
        this.description = description;
    }

    public ResourceRecord<? extends Name, ? extends Serializable> createResourceRecord(final GenericName owner,
                                                                                       final QClass qClass,
                                                                                       final Seconds timeToLive,
                                                                                       final AtomicReader reader)
                                                                                                                 throws BadlyFormedDnsMessageException {
        return genericResourceRecord(owner, this, timeToLive, reader.readData());
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeUnsigned16BitInteger(value);
    }

    @Override
    public String toString() {
        return format(UK, "%1$s (%2$s)", name(), description);
    }
}
