/*******************************************************************************
 Copyright 2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Very much like an ArrayList or an ArrayDeque, but remove from either end is
 * constant time, and "add" at either end is (amortized) constant time.  The
 * elements of the list are modulo-indexed within the array.
 * 
 * Not all operations are supported, not all operations are supported well.
 * 
 * @author dr2chase
 */
public class ArrayQueueList<E> extends AbstractList<E> implements List<E> {
    E[] elements;
    int offset;
    int size;
    
    private final static int SLOP = 4;

    public ArrayQueueList() {
        offset = 0;
        size = 0;
        elements = (E[]) (new Object[8 - SLOP]);
    }
    
    @Override
    public E get(int index) {
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException(index);
        int at = wrappedIndex(index);
        return elements[at];
    }
    
    @Override
    public boolean add(E e) {
        ensureSpaceForOneMore();
        int at = wrappedIndex(size++);
        elements[at] = e;
        return true;
    }

    @Override
    public void add(int index, E element) {
        if (index < 0 || index > size())
            throw new IndexOutOfBoundsException("Expected 0 <= " + index + " " + size);
        if (index == size())
            add(element);
        else {
            ensureSpaceForOneMore();
            if (index < size/2) {
                offset--;
                if (offset < 0)
                    offset += elements.length;
                // Note that we opened up a slot at "0"
                for (int i = 0; i < index; i++) {
                    setInternal(i, get(i+1));
                }
            } else {
                for (int i = size; i > index; i--) {
                    setInternal(i, get(i-1));
                }                
            }
            size++;
            int at = wrappedIndex(index);
            elements[at] = element;
      
        }
    }

    @Override
    public void clear() {
        size = 0;
        elements = (E[]) (new Object[8 - SLOP]);
    }

    /**
     * 
     */
    public void ensureSpaceForOneMore() {
        int l = elements.length;
        if (size == l) {
            int new_l = l + l + SLOP; // 5 -> 13 -> 29 -> 61 -> 125 etc
                                      // 4 -> 12 -> 28 -> 60 -> 124 etc
            E[] new_elements = (E[]) (new Object[new_l]); // expand
            if (offset == 0) {
                System.arraycopy(elements, 0, new_elements, 0, l);
            } else {
                System.arraycopy(elements, 0, new_elements, 0, offset);
                System.arraycopy(elements, offset, new_elements, offset+new_l-l, l-offset);
                offset += new_l - l;
            }
            elements = new_elements;
        }
    }

    @Override
    public int size() {
        return size;
    }
    
    @Override
    public E remove(int index) {
        int l = elements.length;
        E tmp = get(index);
        if (index == 0) {
            offset++;
        } else if (index != size-1) {
            if (index < size/2) {
                // move 0-index-1 fwd, increment offset
                for (int i = index; i > 0; i--)
                    setInternal(i,get(i-1));
                offset++;
            } else {
                // move index+1 to size backwards
                for (int i = index; i < size-1; i++) 
                    setInternal(i,get(i+1));
            }
            
        }
        if (offset >= l)
            offset -= l;
        size--;
        return tmp;
    }
    
    
    @Override
    public Iterator<E> iterator() {
        // TODO Auto-generated method stub
        return new Iterator<E>() {

            int nextIndex = 0;
            
            @Override
            public boolean hasNext() {
                return nextIndex < size;
            }

            @Override
            public E next() {
                if (hasNext())
                    return get(nextIndex++);
                throw new java.util.NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("ArrayBackedLists do not support remove");
            }
            
        };
    }

    @Override
    public E set(int index, E element) {
        if (index < 0)
            throw new ArrayIndexOutOfBoundsException(index);
        if (index >= size)
            throw new ArrayIndexOutOfBoundsException(index);
        return setInternal(index, element);
    }

    private E setInternal(int index, E element) {
        int at = wrappedIndex(index);
        E previous = elements[at];
        elements[at] = element;
        return previous;
    }

    /**
     * @param index
     * @return
     */
    public int wrappedIndex(int index) {
        int at = offset+index;
        if ( at >= elements.length)
            at -= elements.length;
        return at;
    }

    
    
}
