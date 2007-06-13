/*
 * ListSnap.java
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
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.GracefulException;
import dstm2.atomic;
import dstm2.benchmark.IntSetBenchmark;
import dstm2.Thread;
import dstm2.factory.Factory;
import dstm2.factory.Snapable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * @author Maurice Herlihy
 */
public class ListSnap extends IntSetBenchmark {
  
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
   * Tests wheter a value is in an the integer set.
   * @param v the integer value to insert into the set
   * @return true iff presence was confirmed.
   */
  public boolean contains(int v) {
    INode last = null;
    INode lastSnap = null;
    INode prev = this.first;
    INode prevSnap = ((Snapable<INode>)prev).snapshot();
    INode curr = prevSnap.getNext();
    INode currSnap = ((Snapable<INode>)curr).snapshot();
    while (currSnap.getValue()< v) {
      if (last != null) {
        ((Snapable<INode>)last).validate(lastSnap);
      }
      last = prev;
      lastSnap = prevSnap;
      prev = curr;
      prevSnap = currSnap;
      curr = prevSnap.getNext();
      currSnap = ((Snapable<INode>)curr).snapshot();
    }
    if (lastSnap != null) {
      ((Snapable<INode>)last).upgrade(lastSnap);
    }
    ((Snapable<INode>)prev).upgrade(prevSnap);
    ((Snapable<INode>)curr).upgrade(currSnap);
    return (curr.getValue()== v);
  }
  
  /**
   * Removes an element from the integer set, if it is there.
   * @param v the integer value to delete from the set
   * @return true iff v was removed
   */
  public boolean remove(int v) {
    INode last = null;
    INode lastSnap = null;
    INode prev = this.first;
    INode prevSnap = ((Snapable<INode>)prev).snapshot();
    INode curr = prevSnap.getNext();
    INode currSnap = ((Snapable<INode>)curr).snapshot();
    while (currSnap.getValue()< v) {
      if (last != null) {
        ((Snapable<INode>)last).validate(lastSnap);
      }
      last = prev;
      lastSnap = prevSnap;
      prev = curr;
      prevSnap = currSnap;
      curr = prevSnap.getNext();
      currSnap = ((Snapable<INode>)curr).snapshot();
    }
    if (lastSnap != null) {
      ((Snapable<INode>)last).upgrade(lastSnap);
    }
    ((Snapable<INode>)prev).upgrade(prevSnap);
    ((Snapable<INode>)curr).upgrade(currSnap);
    if (curr.getValue()!= v) {
      return false;
    } else {
      prev.setNext(curr.getNext());
      return true;
    }
  }
  
  public boolean insert(int v) {
    INode last = null;
    INode lastSnap = null;
    INode prev = this.first;
    Snapable<INode> prevS = (Snapable<INode>)prev;
    INode prevSnap = prevS.snapshot();
    INode curr = prevSnap.getNext();
    INode currSnap = ((Snapable<INode>)curr).snapshot();
    INode newNode = factory.create();
    while (currSnap.getValue()< v) {
      if (last != null) {
        ((Snapable<INode>)last).validate(lastSnap);
      }
      last = prev;
      lastSnap = prevSnap;
      prev = curr;
      prevSnap = currSnap;
      curr = prevSnap.getNext();
      currSnap = ((Snapable<INode>)curr).snapshot();
    }
    if (lastSnap != null) {
      ((Snapable<INode>)last).upgrade(lastSnap);
    }
    ((Snapable<INode>)prev).upgrade(prevSnap);
    ((Snapable<INode>)curr).upgrade(currSnap);
    if (currSnap.getValue()== v) {
      return false;
    } else {
      newNode.setNext(curr);
      prev.setNext(newNode);
      return true;
    }
  }
  
  public Iterator<Integer> iterator() {
    return new Iterator<Integer>() {
      INode cursor = ListSnap.this.first.getNext();
      public boolean hasNext() {
        return cursor.getNext().getValue() == Integer.MAX_VALUE;
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
  
  @atomic public interface INode {
    int getValue();
    void setValue(int value);
    INode getNext();
    void setNext(INode value);
  }
  
  
  public static void main(String [] a) {
      String[] myArgs = {
        "-m", "dstm2.manager.GreedyManager",
        "-a", "dstm2.factory.ofree.invisible.Adapter",
        "-b", "dstm2.benchmark.ListSnap",
        "-t", "32",
        "-n", "60000",
        "-e", "100"
      };
      Main.main(myArgs);
  }
}

