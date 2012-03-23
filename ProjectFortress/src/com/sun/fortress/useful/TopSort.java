/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A class that performs two different kinds of topological sort, given
 * a List of elements that implement Item.
 * <p/>
 * In a breadth first topological sort, unprocessed items with zero predecessors
 * are stored in a queue (FIFO), so that first all the roots are visited, then
 * all items at distance 1, then 2, and so on.
 * <p/>
 * In a depth first topological sort, unprocessed items with zero predecessors
 * are stored in a stack (LIFO), so that all items reachable only from the
 * first root are visited before any other root is processed, and so on.
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
            if (it.predecessorCount() == 0) pending.add(it);
        }

        int j = 0;
        while (j < pending.size()) {
            T it = pending.get(j);
            pending.set(j, null);
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

        if (n != sorted.size()) {
            ArrayList a = new ArrayList();
            for (int k=0; k<pending.size(); k++) {
                if (pending.get(k) != null) {
                    a.add(pending.get(k));
                }
            }
           
            throw new CycleInRelation("No topological order exists; input contains cycle", a);
        }

        return sorted;
    }

    public static <T extends TopSortItem<T>> List<T> depthFirstArray(T[] unsorted) {
        return TopSort.<T>depthFirst(Useful.<T>list(unsorted));
    }

    public static <T extends TopSortItem<T>> List<T> breadthFirstArray(T[] unsorted) {
        return TopSort.<T>breadthFirst(Useful.<T>list(unsorted));
    }

    /**
     * @param unsorted Unordered list of items
     * @return topologically ordered list of items, prioritized by comparator comp
     */
    public static <T extends TopSortItem<T>> List<T> prioritized(Iterable<T> unsorted, Comparator<T> comp) {
        Iterator<T> i = unsorted.iterator();

        ArrayList<T> sorted = new ArrayList<T>();
        BASet<T> pending = new BASet<T>(comp);
        int n = 0;

        while (i.hasNext()) {
            T it = i.next();
            n++;
            if (it.predecessorCount() == 0) pending.add(it);
        }

        int j = 0;
        while (pending.size() > 0) {
            T it = pending.first();
            pending.remove(it);
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

        if (n != sorted.size()) {
            ArrayList a = new ArrayList();

            while (pending.size() > 0) {
                T it = pending.first();
                pending.remove(it);
                a.add(it);
            }
            throw new CycleInRelation("No topological order exists; input contains cycle", a);
        }

        return sorted;
    }


    public static <T extends TopSortItem<T>> List<T> depthFirst(Iterable<T> unsorted) {
//         {
// 	    System.err.println(">>TopSort");
// 	    Iterator<T> baz = unsorted.iterator();
// 	    while (baz.hasNext()) {
// 		System.err.println(">> " + baz.next());
// 	    }
//         }
        Iterator<T> i = unsorted.iterator();

        int n = 0;
        ArrayList<T> sorted = new ArrayList<T>();
        ArrayList<T> pending = new ArrayList<T>();

        while (i.hasNext()) {
            T it = i.next();
            n++;
            if (it.predecessorCount() == 0) pending.add(it);
        }

        int j = pending.size();
        while (j > 0) {
            T it = pending.remove(j - 1);
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

        if (n != sorted.size()) {
            ArrayList a = new ArrayList();
            Iterator<T> q = unsorted.iterator();
            while (q.hasNext()) {
                T it = q.next();
                if (!sorted.contains(it)) {
                    a.add(it);
                }
            }
            throw new CycleInRelation("No topological order exists; input contains cycle: ", a);
            
        }

        return sorted;
    }
}
