package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.MX;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.MailExchange;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.names.DomainName;

public class MailExchangeResourceRecord extends
        AbstractResourceRecord<DomainName, MailExchange> {
    public static MailExchangeResourceRecord mailExchangeResourceRecord(final DomainName owner,
                                                                        final Seconds timeToLive,
                                                                        final MailExchange mailExchange) {
        return new MailExchangeResourceRecord(owner, timeToLive, mailExchange);
    }

    public MailExchangeResourceRecord(final DomainName owner,
                                      final Seconds timeToLive,
                                      final MailExchange mailExchange) {
        super(owner, MX, Internet, timeToLive, mailExchange);
    }
}
