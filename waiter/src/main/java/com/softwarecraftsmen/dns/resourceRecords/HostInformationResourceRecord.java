package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.HINFO;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.HostInformation;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.names.HostName;

public class HostInformationResourceRecord extends
        AbstractResourceRecord<HostName, HostInformation> {
    public static HostInformationResourceRecord hostInformationResourceRecord(final HostName owner,
                                                                              final Seconds timeToLive,
                                                                              final HostInformation hostInformation) {
        return new HostInformationResourceRecord(owner, timeToLive,
                                                 hostInformation);
    }

    public HostInformationResourceRecord(final HostName owner,
                                         final Seconds timeToLive,
                                         final HostInformation hostInformation) {
        super(owner, HINFO, Internet, timeToLive, hostInformation);
    }
}