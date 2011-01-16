package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.messaging.GenericName;
import com.softwarecraftsmen.dns.messaging.GenericResourceRecordData;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.QClass;

public class GenericResourceRecord extends
        AbstractResourceRecord<GenericName, GenericResourceRecordData> {
    public static GenericResourceRecord genericResourceRecord(final GenericName owner,
                                                              final InternetClassType internetClassType,
                                                              final Seconds timeToLive,
                                                              final GenericResourceRecordData data) {
        return new GenericResourceRecord(owner, internetClassType, Internet,
                                         timeToLive, data);
    }

    public GenericResourceRecord(final GenericName owner,
                                 final InternetClassType internetClassType,
                                 final QClass qClass, final Seconds timeToLive,
                                 final GenericResourceRecordData data) {
        super(owner, internetClassType, qClass, timeToLive, data);
    }
}
