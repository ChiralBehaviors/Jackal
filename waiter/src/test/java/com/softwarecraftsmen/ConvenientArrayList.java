package com.softwarecraftsmen;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;

public class ConvenientArrayList<T> extends ArrayList<T> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static <T> List<T> toList(final T... values) {
        return new ConvenientArrayList<T>(values);
    }

    public static List<ResourceRecord<? extends Name, ? extends Serializable>> toResourceRecordList(final ResourceRecord<? extends Name, ? extends Serializable>... values) {
        return new ConvenientArrayList<ResourceRecord<? extends Name, ? extends Serializable>>(
                                                                                               values);
    }

    public ConvenientArrayList(final T... values) {
        super(java.util.Arrays.asList(values));
    }
}
