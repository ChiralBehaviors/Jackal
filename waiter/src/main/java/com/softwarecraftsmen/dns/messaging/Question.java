/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.QClass.Internet;
import static com.softwarecraftsmen.toString.ToString.string;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;

public class Question implements Serializable {
    public static Question internetQuestion(final Name name,
                                            final InternetClassType internetClassType) {
        return new Question(name, internetClassType, Internet);
    }

    private final Name name;
    private final InternetClassType internetClassType;

    private final QClass qClass;

    public Question(final Name name, final InternetClassType internetClassType,
                    final QClass qClass) {
        this.name = name;
        this.internetClassType = internetClassType;
        this.qClass = qClass;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Question question = (Question) o;
        if (internetClassType != question.internetClassType) {
            return false;
        }
        if (!name.equals(question.name)) {
            return false;
        }
        //noinspection RedundantIfStatement
        if (qClass != question.qClass) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = name.hashCode();
        result = 31 * result + internetClassType.hashCode();
        result = 31 * result + qClass.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        name.serialize(writer);
        internetClassType.serialize(writer);
        qClass.serialize(writer);
    }

    @Override
    public String toString() {
        return string(this, name, internetClassType, qClass);
    }
}
