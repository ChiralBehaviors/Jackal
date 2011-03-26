package com.hellblazer.jackal.util;

import java.util.Comparator;

public interface SortedCollection<E> extends Iterable<E> {
    boolean isEmpty();

    int size();

    E first();

    E last();

    boolean contains(E element);

    E find(E element);

    void add(E element);

    void remove(E element);

    void addAll(SortedCollection<? extends E> col);

    Comparator<? super E> comparator();

    Iterable<E> subset(E from, E to);

}