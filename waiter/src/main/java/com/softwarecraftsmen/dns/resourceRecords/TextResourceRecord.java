package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.TXT;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.Text;
import com.softwarecraftsmen.dns.names.HostName;

public class TextResourceRecord extends AbstractResourceRecord<HostName, Text> {
    public static TextResourceRecord textResourceRecord(final HostName owner,
                                                        final Seconds timeToLive,
                                                        final Text text) {
        return new TextResourceRecord(owner, timeToLive, text);
    }

    public TextResourceRecord(final HostName owner, final Seconds timeToLive,
                              final Text text) {
        super(owner, TXT, Internet, timeToLive, text);
    }
}
