package com.softwarecraftsmen.dns.client.resolvers;

import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.Message;
import com.softwarecraftsmen.dns.names.Name;

public interface DnsResolver {

    Message resolve(final Name<SimpleLabel> name, final InternetClassType internetClassType);
}
