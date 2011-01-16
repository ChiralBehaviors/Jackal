package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.SRV;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.ServiceInformation;
import com.softwarecraftsmen.dns.names.ServiceName;

public class ServiceInformationResourceRecord extends
        AbstractResourceRecord<ServiceName, ServiceInformation> {
    public static ServiceInformationResourceRecord serviceInformationResourceRecord(final ServiceName owner,
                                                                                    final Seconds timeToLive,
                                                                                    final ServiceInformation serviceInformation) {
        return new ServiceInformationResourceRecord(owner, timeToLive,
                                                    serviceInformation);
    }

    public ServiceInformationResourceRecord(final ServiceName owner,
                                            final Seconds timeToLive,
                                            final ServiceInformation serviceInformation) {
        super(owner, SRV, Internet, timeToLive, serviceInformation);
    }
}
