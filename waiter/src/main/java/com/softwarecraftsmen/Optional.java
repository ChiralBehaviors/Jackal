package com.softwarecraftsmen;

import static java.util.Collections.emptySet;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Optional<T> implements Set<T> {
    public static <T> Optional<T> empty() {
        return new Optional<T>();
    }

    private Set<T> internalSet;

    private final T singleValue;

    public Optional(final T singleValue) {
        internalSet = new LinkedHashSet<T>(1);
        this.singleValue = singleValue;
        internalSet.add(this.singleValue);
    }

    private Optional() {
        internalSet = emptySet();
        singleValue = null;
    }

    public boolean add(final T t) {
        throw new UnsupportedOperationException("add");
    }

    public boolean addAll(final Collection<? extends T> c) {
        throw new UnsupportedOperationException("addAll");
    }

    public void clear() {
        throw new UnsupportedOperationException("clear");
    }

    public boolean contains(final Object o) {
        return internalSet.contains(o);
    }

    public boolean containsAll(final Collection<?> c) {
        return internalSet.containsAll(c);
    }

    public boolean isEmpty() {
        return internalSet.isEmpty();
    }

    public Iterator<T> iterator() {
        return internalSet.iterator();
    }

    public boolean remove(final Object o) {
        throw new UnsupportedOperationException("remove");
    }

    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException("removeAll");
    }

    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException("retainAll");
    }

    public int size() {
        return internalSet.size();
    }

    public Object[] toArray() {
        return internalSet.toArray();
    }

    public <T> T[] toArray(final T[] a) {
        return internalSet.toArray(a);
    }

    public T value() {
        if (isEmpty()) {
            throw new IllegalStateException("IsEmpty");
        }
        return singleValue;
    }
}
