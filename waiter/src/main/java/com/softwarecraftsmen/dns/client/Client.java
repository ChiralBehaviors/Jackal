package com.softwarecraftsmen.dns.client;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.A;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.AAAA;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.CNAME;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.HINFO;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.MX;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.PTR;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.SRV;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.TXT;
import static com.softwarecraftsmen.dns.names.ServiceName.serviceName;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.LinkedHashSet;
import java.util.Set;

import com.softwarecraftsmen.CanNeverHappenException;
import com.softwarecraftsmen.Optional;
import com.softwarecraftsmen.dns.HostInformation;
import com.softwarecraftsmen.dns.MailExchange;
import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.ServiceInformation;
import com.softwarecraftsmen.dns.Text;
import com.softwarecraftsmen.dns.client.resourceRecordRepositories.ResourceRecordRepository;
import com.softwarecraftsmen.dns.labels.ServiceLabel;
import com.softwarecraftsmen.dns.labels.ServiceProtocolLabel;
import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.dns.names.Name;

public class Client {
    private final ResourceRecordRepository resourceRecordRepository;

    public Client(final ResourceRecordRepository resourceRecordRepository) {
        this.resourceRecordRepository = resourceRecordRepository;
    }

    public Set<Inet4Address> findAllInternetProtocolVersion4Addresses(final HostName hostName) {
        final Set<SerializableInternetProtocolAddress<Inet4Address>> set = resourceRecordRepository.findData(hostName,
                                                                                                             A);
        final Set<Inet4Address> addresses = new LinkedHashSet<Inet4Address>(
                                                                            set.size());
        for (SerializableInternetProtocolAddress<Inet4Address> inet4AddressSerializableInternetProtocolAddress : set) {
            addresses.add(inet4AddressSerializableInternetProtocolAddress.address);
        }
        return addresses;
    }

    public Set<Inet6Address> findAllInternetProtocolVersion6Addresses(final HostName hostName) {
        final Set<SerializableInternetProtocolAddress<Inet6Address>> set = resourceRecordRepository.findData(hostName,
                                                                                                             AAAA);
        final Set<Inet6Address> addresses = new LinkedHashSet<Inet6Address>(
                                                                            set.size());
        for (SerializableInternetProtocolAddress<Inet6Address> inet4AddressSerializableInternetProtocolAddress : set) {
            addresses.add(inet4AddressSerializableInternetProtocolAddress.address);
        }
        return addresses;
    }

    public Optional<HostName> findCanonicalName(final HostName hostName) {
        return findOptionalData(hostName, CNAME);
    }

    public Optional<HostInformation> findHostInformation(final HostName hostName) {
        return findOptionalData(hostName, HINFO);
    }

    public Set<MailExchange> findMailServers(final DomainName domainName) {
        return resourceRecordRepository.findData(domainName, MX);
    }

    public Optional<HostName> findNameFromInternetProtocolVersion4Address(Inet4Address internetProtocolVersion4Address) {
        return findNameFromInternetProtocolVersion4Address(new SerializableInternetProtocolAddress<Inet4Address>(
                                                                                                                 internetProtocolVersion4Address));
    }

    public Optional<HostName> findNameFromInternetProtocolVersion4Address(final SerializableInternetProtocolAddress<Inet4Address> internetProtocolVersion4Address) {
        return findOptionalData(internetProtocolVersion4Address.toInternetProtocolName(),
                                PTR);
    }

    public Optional<HostName> findNameFromInternetProtocolVersion6Address(Inet6Address internetProtocolVersion6Address) {
        return findNameFromInternetProtocolVersion6Address(new SerializableInternetProtocolAddress<Inet6Address>(
                                                                                                                 internetProtocolVersion6Address));
    }

    public Optional<HostName> findNameFromInternetProtocolVersion6Address(final SerializableInternetProtocolAddress<Inet6Address> internetProtocolVersion6Address) {
        return findOptionalData(internetProtocolVersion6Address.toInternetProtocolName(),
                                PTR);
    }

    public Set<ServiceInformation> findServiceInformation(final ServiceLabel serviceLabel,
                                                          final ServiceProtocolLabel serviceProtocolLabel,
                                                          final DomainName domainName) {
        return resourceRecordRepository.findData(serviceName(serviceLabel,
                                                             serviceProtocolLabel,
                                                             domainName), SRV);
    }

    public Optional<Text> findText(final HostName hostName) {
        return findOptionalData(hostName, TXT);
    }

    private <T extends Serializable> Optional<T> findOptionalData(final Name<SimpleLabel> name,
                                                                  final InternetClassType internetClassType) {
        final Set<T> set = resourceRecordRepository.findData(name,
                                                             internetClassType);
        if (set.isEmpty()) {
            return com.softwarecraftsmen.Optional.empty();
        } else {
            for (T serializable : set) {
                return new Optional<T>(serializable);
            }
        }
        throw new CanNeverHappenException();
    }
}
