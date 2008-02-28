/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * A class that performs two different kinds of topological sort, given
 * a List of elements that implement Item.
 * <p>
 * In a breadth first topological sort, unprocessed items with zero predecessors
 * are stored in a queue (FIFO), so that first all the roots are visited, then
 * all items at distance 1, then 2, and so on.
 * <p>
 * In a depth first topological sort, unprocessed items with zero predecessors
 * are stored in a stack (LIFO), so that all items reachable only from the
 * first root are visited before any other root is processed, and so on.
 *
 */
public class TopSort {
    /**
     * @param unsorted Unordered list of items
     * @return breadth-first topologically ordered list of items.
     */
    public static <T extends TopSortItem<T>> List<T> breadthFirst(Iterable<T> unsorted) {
        Iterator<T> i = unsorted.iterator();

        ArrayList<T> sorted = new ArrayList<T>();
        ArrayList<T> pending = new ArrayList<T>();
        int n = 0;

        while (i.hasNext()) {
            T it = i.next();
            n++;
            if (it.predecessorCount() == 0)
                pending.add(it);
        }

        int j = 0;
        while (j < pending.size()) {
            T it = pending.get(j);
            pending.set(j, null); // optional, really.
            j++;
            sorted.add(it);
            Iterator<T> iti = it.successors();
            while (iti.hasNext()) {
                T succ = iti.next();
                if (succ.decrementPredecessors() == 0) {
                    pending.add(succ);
                }
            }
        }

        if (n != sorted.size())
            throw new IllegalArgumentException("No order exists; input contains cycle");

        return sorted;
    }

    public static <T extends TopSortItem<T> > List<T> depthFirst(Iterable<T> unsorted) {
        Iterator<T> i = unsorted.iterator();

        int n = 0;
        ArrayList<T> sorted = new ArrayList<T>();
        ArrayList<T> pending = new ArrayList<T>();

        while (i.hasNext()) {
            T it =  i.next();
            n++;
            if (it.predecessorCount() == 0)
                pending.add(it);
        }

        int j = pending.size();
        while (j > 0) {
            T it = pending.remove(j-1);
            sorted.add(it);
            Iterator<T> iti = it.successors();
            while (iti.hasNext()) {
                T succ = iti.next();
                if (succ.decrementPredecessors() == 0) {
                    pending.add(succ);
                }
            }
            j = pending.size();
        }

        if (n != sorted.size())
            throw new IllegalArgumentException("No order exists; input contains cycle");

        return sorted;
    }
}
