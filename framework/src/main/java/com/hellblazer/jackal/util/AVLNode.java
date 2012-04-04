package com.hellblazer.jackal.util;

import java.util.ArrayList;

public class AVLNode<E> {

    AVLNode<E>   _parent;
    E            _data;                           // data held by the AVLNode
    AVLNode<E>   _left;                           // His left son.
    AVLNode<E>   _right;                          // His right son.
    ArrayList<E> _myElements = new ArrayList<E>();
    int          _height;

    public AVLNode(E data) {
        if (data == null) {
            throw new NullPointerException(
                                           "Passed data to be stored cannot be Null.");
        }
        this._data = data;
        _height = 0;
    }

    public AVLNode(E data, AVLNode<E> parent) {
        _parent = parent;
        if (data == null) {
            throw new NullPointerException();
        }
        _data = data;
    }

    public AVLNode(E data, AVLNode<E> leftS, AVLNode<E> rightS) {
        if (data == null) {
            throw new NullPointerException(
                                           "Passed data to be stored cannot be Null.");
        }
        this._data = data;
        _left = leftS;
        _right = rightS;
    }

    public AVLNode(E data, AVLNode<E> parent, AVLNode<E> left, AVLNode<E> right) {
        this._parent = parent;
        this._left = left;
        this._right = right;
        _height = _parent._height + 1;
        if (data == null) {
            throw new NullPointerException(
                                           "Passed data to be stored cannot be Null.");
        }
        this._data = data;
    }

    public int balanceFactor() {
        return heightRChild() - heightLChild();
    }

    public void recalculateHeight() {
        _height = Math.max(heightLChild(), heightRChild()) + 1;
    }

    private int heightLChild() {
        return _left == null ? 0 : _left._height;
    }

    private int heightRChild() {
        return _right == null ? 0 : _right._height;
    }

    E getData() {
        return this._data;
    }
}
