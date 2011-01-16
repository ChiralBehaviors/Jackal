package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.toString.ToString.string;
import static java.util.Arrays.asList;

import java.util.List;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;

public class Text implements Serializable {
    public static Text text(final String... lines) {
        return new Text(asList(lines));
    }

    private final List<String> lines;

    public Text(final List<String> lines) {
        this.lines = lines;
        for (String line : lines) {
            if (line.length() > 255) {
                throw new IllegalArgumentException(
                                                   "Maximum length of a character string in DNS is 255 characters");
            }
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

        final Text text = (Text) o;
        return lines.equals(text.lines);
    }

    @Override
    public int hashCode() {
        return lines.hashCode();
    }

    public void serialize(final AtomicWriter writer) {
        for (String line : lines) {
            writer.writeCharacterString(line);
        }
    }

    @Override
    public String toString() {
        return string(this, lines);
    }
}
