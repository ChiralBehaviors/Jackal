package com.softwarecraftsmen.dns.names;

import java.util.List;

import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;

public interface Name<L extends Label> extends Serializable {

    List<L> toLabels();
}
