package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.PTR;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.dns.names.PointerName;

public class PointerResourceRecord extends
        AbstractResourceRecord<PointerName, HostName> {
    public static PointerResourceRecord pointerResourceRecord(final PointerName owner,
                                                              final Seconds timeToLive,
                                                              final HostName hostName) {
        return new PointerResourceRecord(owner, timeToLive, hostName);
    }

    public PointerResourceRecord(final PointerName owner,
                                 final Seconds timeToLive,
                                 final HostName hostName) {
        super(owner, PTR, Internet, timeToLive, hostName);
    }
}
