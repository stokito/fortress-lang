/*
 * ListRelease.java
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A.  All rights reserved.  
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document.  In particular, and without limitation, these
 * intellectual property rights may include one or more of the
 * U.S. patents listed at http://www.sun.com/patents and one or more
 * additional patents or pending patent applications in the U.S. and
 * in other countries.
 * 
 * U.S. Government Rights - Commercial software.
 * Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its
 * supplements.  Use is subject to license terms.  Sun, Sun
 * Microsystems, the Sun logo and Java are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.  
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime
 * end uses or end users, whether direct or indirect, are strictly
 * prohibited.  Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion
 * lists, including, but not limited to, the denied persons and
 * specially designated nationals lists is strictly prohibited.
 */

package dstm2.benchmark;

import dstm2.Main;
import dstm2.atomic;
import dstm2.factory.Factory;
import dstm2.Thread;
import dstm2.factory.Releasable;
import java.lang.reflect.Method;
import java.util.Iterator;

/**
 * @author Maurice Herlihy
 */
public class ListRelease extends IntSetBenchmark {
  
  static Factory<INode> factory;
  
  protected INode first;
  
  protected void init() {
    factory = Thread.makeFactory(INode.class);
    INode firstList  = factory.create();
    firstList.setValue(Integer.MIN_VALUE);
    this.first = firstList;
    INode firstNext = factory.create();
    firstNext.setValue(Integer.MAX_VALUE);
    firstList.setNext(firstNext);
  }
  
  /**
   * This method does all the work. It returns a
   * <code>dstm.benchmark.IntSetBenchmark.Neighborhood</code> object containing
   * the transactional node with maximal value strictly less than v, and the
   * non-transactional TestFactory element containing v (or null, if none exists).
   * @param v value sought
   * @return neighborhood of value
   */
  protected Neighborhood find(int v) {
    INode last = null;
    INode prev = this.first;
    INode curr = prev.getNext();
    while (curr.getValue() < v) {
      if (last != null) {
        ((Releasable)last).release();
      }
      last = prev;
      prev = curr;
      curr = prev.getNext();
    }
    if (curr.getValue() == v)
      return new Neighborhood(prev, curr);
    else
      return new Neighborhood(prev);
  }
  
  /**
   * Add an element to the integer set, if it is not already there.
   * @param v the integer value to add from the set
   * @return true iff value was added.
   */
  public boolean insert(int v) {
    INode newNode = factory.create();
    newNode.setValue(v);
    Neighborhood hood = find(v);
    if (hood.curr != null) {
      return false;
    } else {
      INode prev = hood.prev;
      newNode.setNext(prev.getNext());
      prev.setNext(newNode);
      return true;
    }
  }
  
  /**
   * Tests wheter a value is in an the integer set.
   * @param v the integer value to insert into the set
   * @return true iff presence was confirmed.
   */
  public boolean contains(int v) {
    Neighborhood hood = find(v);
    return hood.curr != null;
  }
  
  /**
   * Removes an element from the integer set, if it is there.
   * @param v the integer value to delete from the set
   * @return true iff v was removed
   */
  public boolean remove(int v) {
    INode newNode = factory.create();
    newNode.setValue(v);
    Neighborhood hood = find(v);
    if (hood.curr == null) {
      return false;
    } else {
      INode prev = hood.prev;
      prev.setNext(hood.curr.getNext());
      return true;
    }
  }
  
  @atomic public interface INode {
    int getValue();
    void setValue(int value);
    INode getNext();
    void setNext(INode value);
  }
  
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      INode cursor = ListRelease.this.first.getNext();
      public boolean hasNext() {
        return cursor.getNext().getValue() != Integer.MAX_VALUE;
      }
      public Integer next() {
        INode node = cursor;
        cursor = cursor.getNext();
        return node.getValue();
      }
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  } 
  protected class Neighborhood {
    public INode prev;
    public INode curr;
    public Neighborhood(INode prev, INode curr) {
      this.prev = prev;
      this.curr = curr;
    }
    public Neighborhood(INode prev) {
      this.prev = prev;
    }
  }
  public static void main(String [] a) {
      String[] myArgs = {
        "-m", "dstm2.manager.GreedyManager",
        "-a", "dstm2.factory.shadow.invisible.Adapter",
        "-b", "dstm2.benchmark.ListRelease",
        "-t", "1",
        "-n", "60000",
        "-e", "100"
      };
      Main.main(myArgs);
  }
}
