package com.hellblazer.jackal.util;

import java.util.Comparator;

public interface SortedCollection<E> extends Iterable<E> {
    void add(E element);

    void addAll(SortedCollection<? extends E> col);

    Comparator<? super E> comparator();

    boolean contains(E element);

    E find(E element);

    E first();

    boolean isEmpty();

    E last();

    void remove(E element);

    int size();

    Iterable<E> subset(E from, E to);

}