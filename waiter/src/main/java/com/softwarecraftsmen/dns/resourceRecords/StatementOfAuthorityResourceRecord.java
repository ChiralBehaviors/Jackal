package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.SOA;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.StatementOfAuthority;
import com.softwarecraftsmen.dns.names.DomainName;

public class StatementOfAuthorityResourceRecord extends
        AbstractResourceRecord<DomainName, StatementOfAuthority> {
    public static StatementOfAuthorityResourceRecord statementOfAuthorityResourceRecord(final DomainName owner,
                                                                                        final Seconds timeToLive,
                                                                                        final StatementOfAuthority statementOfAuthority) {
        return new StatementOfAuthorityResourceRecord(owner, timeToLive,
                                                      statementOfAuthority);
    }

    public StatementOfAuthorityResourceRecord(final DomainName owner,
                                              final Seconds timeToLive,
                                              final StatementOfAuthority statementOfAuthority) {
        super(owner, SOA, Internet, timeToLive, statementOfAuthority);
    }
}