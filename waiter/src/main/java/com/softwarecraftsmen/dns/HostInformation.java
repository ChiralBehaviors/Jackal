package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.toString.ToString.string;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;

public class HostInformation implements Serializable {
    public static HostInformation hostInformation(final String cpuType,
                                                  final String operatingSystemType) {
        return new HostInformation(cpuType, operatingSystemType);
    }

    public final String cpuType;

    public final String operatingSystemType;

    public HostInformation(final String cpuType,
                           final String operatingSystemType) {
        this.cpuType = cpuType;
        this.operatingSystemType = operatingSystemType;
        if (cpuType.length() > 255) {
            throw new IllegalArgumentException(
                                               "cpuType is a character strign which DNS restricts to a maximum length of 255 characters");
        }
        if (operatingSystemType.length() > 255) {
            throw new IllegalArgumentException(
                                               "operatingSystemType is a character strign which DNS restricts to a maximum length of 255 characters");
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final HostInformation that = (HostInformation) o;
        return cpuType.equals(that.cpuType)
               && operatingSystemType.equals(that.operatingSystemType);
    }

    @Override
    public int hashCode() {
        int result;
        result = cpuType.hashCode();
        result = 31 * result + operatingSystemType.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeCharacterString(cpuType);
        writer.writeCharacterString(operatingSystemType);
    }

    @Override
    public String toString() {
        return string(this, cpuType, operatingSystemType);
    }
}
