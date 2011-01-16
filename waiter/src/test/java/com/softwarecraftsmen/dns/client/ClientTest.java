package com.softwarecraftsmen.dns.client;

import static com.softwarecraftsmen.dns.HostInformation.hostInformation;
import static com.softwarecraftsmen.dns.MailExchange.mailExchange;
import static com.softwarecraftsmen.dns.Seconds.seconds;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolVersion4Address;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolVersion6Address;
import static com.softwarecraftsmen.dns.ServiceInformation.serviceInformation;
import static com.softwarecraftsmen.dns.Text.text;
import static com.softwarecraftsmen.dns.labels.ServiceLabel.serviceLabel;
import static com.softwarecraftsmen.dns.labels.ServiceProtocolLabel.TCP;
import static com.softwarecraftsmen.dns.names.DomainName.domainName;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.dns.resourceRecords.CanonicalNameResourceRecord.canonicalNameResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.HostInformationResourceRecord.hostInformationResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion4AddressResourceRecord.internetProtocolVersion4AddressResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion6AddressResourceRecord.internetProtocolVersion6AddressResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.MailExchangeResourceRecord.mailExchangeResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.PointerResourceRecord.pointerResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.ServiceInformationResourceRecord.serviceInformationResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.TextResourceRecord.textResourceRecord;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.softwarecraftsmen.Optional;
import com.softwarecraftsmen.dns.HostInformation;
import com.softwarecraftsmen.dns.MailExchange;
import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.ServiceInformation;
import com.softwarecraftsmen.dns.Text;
import com.softwarecraftsmen.dns.client.resolvers.MockDnsResolver;
import com.softwarecraftsmen.dns.client.resourceRecordRepositories.NonCachingResourceRecordRepository;
import com.softwarecraftsmen.dns.labels.ServiceLabel;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.dns.names.PointerName;
import com.softwarecraftsmen.dns.names.ServiceName;

public class ClientTest {
    private static final HostName CanonicalName = hostName("www.l.google.com");
    private static final HostName AliasName = hostName("www.google.com");
    private static final DomainName SomeDomainName = domainName("google.com");
    private static final HostName ResolvedReverseLookupHostName = hostName("nf-in-f104.google.com");
    private static final SerializableInternetProtocolAddress<Inet4Address> ExampleInternetProtocolVersion4Address = serializableInternetProtocolVersion4Address(64,
                                                                                                                                                                233,
                                                                                                                                                                183,
                                                                                                                                                                104);
    private static final SerializableInternetProtocolAddress<Inet6Address> ExampleInternetProtocolVersion6Address = serializableInternetProtocolVersion6Address(0x4321,
                                                                                                                                                                0x0,
                                                                                                                                                                0x1,
                                                                                                                                                                0x2,
                                                                                                                                                                0x3,
                                                                                                                                                                0x4,
                                                                                                                                                                0x567,
                                                                                                                                                                0x89ab);

    private MockDnsResolver dnsResolver;

    private Client client;

    @Before
    public void before() {
        dnsResolver = new MockDnsResolver();
        client = new Client(new NonCachingResourceRecordRepository(dnsResolver));
    }

    @Test
    public void findAllInternetProtocolVersion4AddressesForCanonicalName() {
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion4Address(1,
                                                                                                                      2,
                                                                                                                      3,
                                                                                                                      4)));
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion4Address(2,
                                                                                                                      2,
                                                                                                                      3,
                                                                                                                      4)));
        final Set<Inet4Address> version4Addresses = client.findAllInternetProtocolVersion4Addresses(CanonicalName);
        assertThat(version4Addresses.size(), is(greaterThan(0)));
    }

    @Test
    public void findAllInternetProtocolVersion4AddressesForNonCanonicalName() {
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion4Address(1,
                                                                                                                      2,
                                                                                                                      3,
                                                                                                                      4)));
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion4Address(2,
                                                                                                                      2,
                                                                                                                      3,
                                                                                                                      4)));
        dnsResolver.program(canonicalNameResourceRecord(AliasName,
                                                        seconds(1000),
                                                        CanonicalName));
        final Set<Inet4Address> version4Addresses = client.findAllInternetProtocolVersion4Addresses(AliasName);
        assertThat(version4Addresses.size(), is(greaterThan(0)));
    }

    @Test
    public void findAllInternetProtocolVersion6AddressesForCanonicalName() {
        dnsResolver.program(internetProtocolVersion6AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion6Address(2001,
                                                                                                                      0x0db8,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x1428,
                                                                                                                      0x57ab)));
        dnsResolver.program(internetProtocolVersion6AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion6Address(2001,
                                                                                                                      0x0db8,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x1428,
                                                                                                                      0x57ac)));
        final Set<Inet6Address> version6Addresses = client.findAllInternetProtocolVersion6Addresses(CanonicalName);
        assertThat(version6Addresses.size(), is(greaterThan(0)));
    }

    @Test
    public void findAllInternetProtocolVersion6AddressesForNonCanonicalName() {
        dnsResolver.program(internetProtocolVersion6AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion6Address(2001,
                                                                                                                      0x0db8,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x1428,
                                                                                                                      0x57ab)));
        dnsResolver.program(internetProtocolVersion6AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          serializableInternetProtocolVersion6Address(2001,
                                                                                                                      0x0db8,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x0000,
                                                                                                                      0x1428,
                                                                                                                      0x57ac)));
        dnsResolver.program(canonicalNameResourceRecord(AliasName,
                                                        seconds(1000),
                                                        CanonicalName));

        final Set<Inet6Address> version6Addresses = client.findAllInternetProtocolVersion6Addresses(AliasName);
        assertThat(version6Addresses.size(), is(greaterThan(0)));
    }

    @Test
    public void findCanonicalName() {
        dnsResolver.program(canonicalNameResourceRecord(AliasName,
                                                        seconds(1000),
                                                        CanonicalName));
        final Optional<HostName> canonicalName = client.findCanonicalName(AliasName);
        assertThat(canonicalName.value(), is(equalTo(CanonicalName)));
    }

    // Does not have any values returned. Odd.
    // How do we distinguish a canonical name from no name - ?SOA?
    @Test
    public void findCanonicalNameHasAValueIfNameIsAlsoCanonical() {
        final Optional<HostName> canonicalName = client.findCanonicalName(CanonicalName);
        assertThat(canonicalName.size(), is(equalTo(0)));
    }

    @Test
    public void findHostInformation() {
        final HostInformation hostInformation = hostInformation("i386", "Linux");
        dnsResolver.program(hostInformationResourceRecord(CanonicalName,
                                                          seconds(1000),
                                                          hostInformation));
        final Optional<HostInformation> hostInformations = client.findHostInformation(CanonicalName);
        assertThat(hostInformations.value(), is(equalTo(hostInformation)));
    }

    @Test
    public void findMailServers() {
        dnsResolver.program(mailExchangeResourceRecord(SomeDomainName,
                                                       seconds(1000),
                                                       mailExchange(unsigned16BitInteger(10),
                                                                    hostName("smtp1.google.com"))));
        dnsResolver.program(mailExchangeResourceRecord(SomeDomainName,
                                                       seconds(1000),
                                                       mailExchange(unsigned16BitInteger(10),
                                                                    hostName("smtp2.google.com"))));
        final Set<MailExchange> mailExchanges = client.findMailServers(SomeDomainName);
        assertThat(mailExchanges.size(), is(greaterThan(0)));
    }

    @Test
    public void findNameFromInternetProtocolVersion4Address() {
        dnsResolver.program(pointerResourceRecord(PointerName.pointerName(ExampleInternetProtocolVersion4Address.address),
                                                  seconds(1000),
                                                  ResolvedReverseLookupHostName));
        final Optional<HostName> resolvedName = client.findNameFromInternetProtocolVersion4Address(ExampleInternetProtocolVersion4Address.address);
        assertThat(resolvedName.value(),
                   is(equalTo(ResolvedReverseLookupHostName)));
    }

    @Test
    public void findNameFromInternetProtocolVersion4AddressFromSerializable() {
        dnsResolver.program(pointerResourceRecord(PointerName.pointerName(ExampleInternetProtocolVersion4Address.address),
                                                  seconds(1000),
                                                  ResolvedReverseLookupHostName));
        final Optional<HostName> resolvedName = client.findNameFromInternetProtocolVersion4Address(ExampleInternetProtocolVersion4Address);
        assertThat(resolvedName.value(),
                   is(equalTo(ResolvedReverseLookupHostName)));
    }

    @Test
    public void findNameFromInternetProtocolVersion6Address() {
        // 4321:0:1:2:3:4:567:89ab
        dnsResolver.program(pointerResourceRecord(PointerName.pointerName(ExampleInternetProtocolVersion6Address.address),
                                                  seconds(1000),
                                                  ResolvedReverseLookupHostName));
        final Optional<HostName> resolvedName = client.findNameFromInternetProtocolVersion6Address(ExampleInternetProtocolVersion6Address.address);
        assertThat(resolvedName.value(),
                   is(equalTo(ResolvedReverseLookupHostName)));
    }

    @Test
    public void findNameFromInternetProtocolVersion6AddressFromSerializable() {
        // 4321:0:1:2:3:4:567:89ab
        dnsResolver.program(pointerResourceRecord(PointerName.pointerName(ExampleInternetProtocolVersion6Address.address),
                                                  seconds(1000),
                                                  ResolvedReverseLookupHostName));
        final Optional<HostName> resolvedName = client.findNameFromInternetProtocolVersion6Address(ExampleInternetProtocolVersion6Address);
        assertThat(resolvedName.value(),
                   is(equalTo(ResolvedReverseLookupHostName)));
    }

    @Test
    public void findNonExistentMailServers() {
        final Set<MailExchange> mailExchanges = client.findMailServers(domainName("doesnotexist.google.com"));
        assertThat(mailExchanges.size(), is(equalTo(0)));
    }

    @Test
    public void findServiceInformation() {
        final ServiceLabel serviceLabel = serviceLabel("_ldap");
        final ServiceInformation expectedServiceInformation = serviceInformation(unsigned16BitInteger(100),
                                                                                 unsigned16BitInteger(10),
                                                                                 unsigned16BitInteger(8080),
                                                                                 AliasName);
        dnsResolver.program(serviceInformationResourceRecord(ServiceName.serviceName(serviceLabel,
                                                                                     TCP,
                                                                                     SomeDomainName),
                                                             seconds(1000),
                                                             expectedServiceInformation));
        final Set<ServiceInformation> actualServiceInformation = client.findServiceInformation(serviceLabel,
                                                                                               TCP,
                                                                                               SomeDomainName);
        assertThat(actualServiceInformation.size(), is(greaterThan(0)));
    }

    @Test
    public void findText() {
        final Text text = text("hello=world");
        dnsResolver.program(textResourceRecord(CanonicalName, seconds(1000),
                                               text));
        final Optional<Text> texts = client.findText(CanonicalName);
        assertThat(texts.value(), is(equalTo(text)));
    }
}