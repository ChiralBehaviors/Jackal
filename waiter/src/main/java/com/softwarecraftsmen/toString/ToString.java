package com.softwarecraftsmen.toString;

import java.io.StringWriter;
import java.util.Arrays;

public final class ToString {
    public static String string(final Object object, final Object... fields) {
        final StringWriter writer = new StringWriter();
        writer.write(object.getClass().getSimpleName());
        writer.write("(");
        boolean afterFirst = false;
        for (Object field : fields) {
            if (afterFirst) {
                writer.write(", ");
            }
            if (field.getClass().isArray()) {
                writer.write(arrayToString(field));
            } else {
                writer.write(field.toString());
            }
            afterFirst = true;
        }
        writer.write(")");
        return writer.toString();
    }

    private static String arrayToString(final Object objects) {
        final Class<?> arrayType = objects.getClass().getComponentType();
        if (arrayType.equals(byte.class)) {
            return Arrays.toString((byte[]) objects);
        } else if (arrayType.equals(boolean.class)) {
            return Arrays.toString((boolean[]) objects);
        } else if (arrayType.equals(short.class)) {
            return Arrays.toString((short[]) objects);
        } else if (arrayType.equals(char.class)) {
            return Arrays.toString((char[]) objects);
        } else if (arrayType.equals(int.class)) {
            return Arrays.toString((int[]) objects);
        } else if (arrayType.equals(long.class)) {
            return Arrays.toString((long[]) objects);
        } else if (arrayType.equals(float.class)) {
            return Arrays.toString((float[]) objects);
        } else if (arrayType.equals(double.class)) {
            return Arrays.toString((double[]) objects);
        } else {
            return Arrays.toString((Object[]) objects);
        }
    }

    private ToString() {
    }
}
