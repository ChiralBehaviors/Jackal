/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.dns.MailExchange.mailExchange;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MailExchangeTest {
    @Test
    public void comparesUsingPreferencesFirstAndLowerPreferenceWins() {
        final MailExchange lowerPreference = mailExchange(unsigned16BitInteger(10),
                                                          hostName("mail.google.com"));
        final MailExchange higherPreference = mailExchange(unsigned16BitInteger(20),
                                                           hostName("mail.google.com"));
        assertThat(lowerPreference, lessThan(higherPreference));
        assertThat(higherPreference, greaterThan(lowerPreference));
    }

    @Test
    public void comparesUsingPreferencesFirstThenHostNamesAndEqualHostNamesAreEqual() {
        final MailExchange identicalPreference1 = mailExchange(unsigned16BitInteger(10),
                                                               hostName("mail.google.com"));
        final MailExchange identicalPreference2 = mailExchange(unsigned16BitInteger(10),
                                                               hostName("mail.google.com"));
        final int operand = identicalPreference1.compareTo(identicalPreference2);
        assertThat(0, equalTo(operand));
    }

    @Test
    public void comparesUsingPreferencesFirstThenHostNamesAndHostNamesWithLessLabelsAreFirst() {
        final MailExchange shorterHostName = mailExchange(unsigned16BitInteger(10),
                                                          hostName("squid.com"));
        final MailExchange longerHostName = mailExchange(unsigned16BitInteger(10),
                                                         hostName("mail.google.com"));
        assertThat(shorterHostName, lessThan(longerHostName));
        assertThat(longerHostName, greaterThan(shorterHostName));
    }

    @Test
    public void comparesUsingPreferencesFirstThenHostNamesAndHostNamesWithSameLabelsAreFirst() {
        final MailExchange shorterHostName = mailExchange(unsigned16BitInteger(10),
                                                          hostName("first.google.com"));
        final MailExchange longerHostName = mailExchange(unsigned16BitInteger(10),
                                                         hostName("mail.google.com"));
        assertThat(shorterHostName, lessThan(longerHostName));
        assertThat(longerHostName, greaterThan(shorterHostName));
    }

    @Test
    public void comparesUsingPreferencesFirstThenHostNamesAndHostNamesWithSameLabelsAreFirstCheckReversesCorrectly() {
        final MailExchange shorterHostName = mailExchange(unsigned16BitInteger(10),
                                                          hostName("mail.google.com"));
        final MailExchange longerHostName = mailExchange(unsigned16BitInteger(10),
                                                         hostName("mail.google.org"));
        assertThat(shorterHostName, lessThan(longerHostName));
        assertThat(longerHostName, greaterThan(shorterHostName));
    }
}
