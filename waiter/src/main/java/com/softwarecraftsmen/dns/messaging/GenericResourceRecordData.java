package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.toString.ToString.string;

import java.util.Arrays;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;

public class GenericResourceRecordData implements Serializable {
    private final byte[] data;

    public GenericResourceRecordData(final byte[] data) {
        this.data = data;
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeBytes(data);
    }

    @Override
    public String toString() {
        return string(this, Arrays.toString(data));
    }
}
