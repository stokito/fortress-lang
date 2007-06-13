/*
 * IntSetBenchmark.java
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

import dstm2.exceptions.AbortedException;
import dstm2.exceptions.GracefulException;
import dstm2.exceptions.PanicException;
import dstm2.Thread;
import dstm2.atomic;
import dstm2.benchmark.Benchmark;
import dstm2.benchmark.Benchmark;
import dstm2.util.Random;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.concurrent.Callable;

/**
 * This abstract class is the superclass for the integer set benchmarks.
 * @author Maurice Herlihy
 * @date April 2004
 */
public abstract class IntSetBenchmark implements Benchmark, Iterable<Integer> {
  
  /**
   * How large to initialize the integer set.
   */
  protected final int INITIAL_SIZE = 8;
  
  /**
   * After the run is over, synchronize merging statistics with other threads.
   */
  static final Object lock = new Object();
  /**
   * local variable
   */
  int element;
  /**
   * local variable
   */
  int value;
  
  /**
   * Number of calls to insert()
   */
  int insertCalls = 0;
  /**
   * number of calls to contains()
   */
  int containsCalls = 0;
  /**
   * number of calls to remove()
   */
  int removeCalls = 0;
  /**
   * amount by which the set size has changed
   */
  int delta = 0;
  
  /**
   * Give subclass a chance to intialize private fields.
   */
  protected abstract void init();
  
  /**
   * Give subclass a chance to intialize thread-local fields.
   */
  protected void threadLocalInit() {
    Class adapterClass = Thread.adapterClass;
    // if adapter has static onCommit method, schedule it for commits
    try {
      Thread.onBegin((Runnable)adapterClass.getMethod("onBegin").invoke(null));
    } catch (IllegalArgumentException ex) {
      throw new PanicException("Adapter onBegin failed: " + ex);
    } catch (InvocationTargetException ex) {
      throw new PanicException("Adapter onBegin failed: " + ex);
    } catch (IllegalAccessException ex) {
      throw new PanicException("Adapter onBegin failed: " + ex);
    } catch (NoSuchMethodException e) {
    }
    
    // if adapter has static onCommit method, schedule it for commits
    try {
      Thread.onCommit((Runnable)adapterClass.getMethod("onCommit").invoke(null));
    } catch (IllegalArgumentException ex) {
      throw new PanicException("Adapter onCommit failed: " + ex);
    } catch (InvocationTargetException ex) {
      throw new PanicException("Adapter onCommit failed: " + ex);
    } catch (IllegalAccessException ex) {
      throw new PanicException("Adapter onCommit failed: " + ex);
    } catch (NoSuchMethodException e) {
    }
    // if adapter has static onAbort method, schedule it for aborts
    try {
      Thread.onAbort((Runnable)adapterClass.getMethod("onAbort").invoke(null));
    } catch (IllegalArgumentException ex) {
      throw new PanicException("Adapter onAbort failed: " + ex);
    } catch (InvocationTargetException ex) {
      throw new PanicException("Adapter onAbort failed: " + ex);
    } catch (IllegalAccessException ex) {
      throw new PanicException("Adapter onAbort failed: " + ex);
    } catch (NoSuchMethodException e) {
    }
    // if adapter has static onValidate method, schedule it for validation
    try {
      Thread.onValidate((Callable<Boolean>)adapterClass.getMethod("onValidate").invoke(null));
    } catch (IllegalArgumentException ex) {
      throw new PanicException("Adapter onValidate failed: " + ex);
    } catch (InvocationTargetException ex) {
      throw new PanicException("Adapter onValidate failed: " + ex);
    } catch (IllegalAccessException ex) {
      throw new PanicException("Adapter onValidate failed: " + ex);
    } catch (NoSuchMethodException e) {
    }
  };
  
  /**
   * Iterate through set. Not necessarily thread-safe.
   */
  public abstract Iterator<Integer> iterator();
  
  /**
   * Add an element to the integer set, if it is not already there.
   * @param v the integer value to add from the set
   * @return true iff value was added.
   */
  public abstract boolean insert(int v);
  
  /**
   * Tests wheter a value is in an the integer set.
   * @param v the integer value to insert into the set
   * @return true iff presence was confirmed.
   */
  public abstract boolean contains(int v);
  
  /**
   * Removes an element from the integer set, if it is there.
   * @param v the integer value to delete from the set
   * @return true iff v was removed
   */
  public abstract boolean remove(int v);
  
  /**
   * Creates a new test thread.
   * @param percent Mix of mutators and observers.
   * @return Thread to run.
   */
  public Thread createThread(int percent) {
    try {
      TestThread testThread = new TestThread(this, percent);
      return testThread;
    } catch (Exception e) {
      e.printStackTrace(System.out);
      return null;
    }
  }
  
  /**
   * Prints an error message to <code>System.out</code>, including a
   *  standard header to identify the message as an error message.
   * @param s String describing error
   */
  protected static void reportError(String s) {
    System.out.println(" ERROR: " + s);
    System.out.flush();
  }
  
  public void report() {
    System.out.println("Insert/Remove calls:\t" + (insertCalls + removeCalls));
    System.out.println("Contains calls:\t" + containsCalls);
  }
  
  private class TestThread extends Thread {
    IntSetBenchmark intSet;
    /**
     * Thread-local statistic.
     */
    int myInsertCalls = 0;
    /**
     * Thread-local statistic.
     */
    int myRemoveCalls = 0;
    /**
     * Thread-local statistic.
     */
    int myContainsCalls = 0;
    /**
     * Thread-local statistic.
     */
    int myDelta = 0;        // net change
    public int percent = 0; // percent inserts
    
    TestThread(IntSetBenchmark intSet, int percent) {
      this.intSet = intSet;
      this.percent = percent;
    }
    
    public void run() {
      Random random = new Random(this.hashCode());
      random.setSeed(System.currentTimeMillis()); // comment out for determinstic
      intSet.threadLocalInit();
      
      boolean toggle = true;
      try {
        while (true) {
          boolean result = true;
          element = random.nextInt();
          if (Math.abs(element) % 100 < percent) {
            if (toggle) {        // insert on even turns
              value = element / 100;
              result = Thread.doIt(new Callable<Boolean>() {
                public Boolean call() {
                  return intSet.insert(value);
                }
              });
              if (result)
                myDelta++;
            } else {              // remove on odd turns
              result = Thread.doIt(new Callable<Boolean>() {
                public Boolean call() {
                  return intSet.remove(value);
                }
              });
              myRemoveCalls++;
              if (result)
                this.myDelta--;
            }
            toggle = !toggle;
          } else {
            Thread.doIt(new Callable<Void>() {
              public Void call() {
                intSet.contains(element / 100);
                return null;
              }
            });
            myContainsCalls++;
          }
        }
      } catch (GracefulException g) {
        // update statistics
        synchronized (lock) {
          insertCalls   += myInsertCalls;
          removeCalls   += myRemoveCalls;
          containsCalls += myContainsCalls;
          delta         += myDelta;
        }
        return;
      }
    }
  }
  
  public void sanityCheck() {
    long expected = INITIAL_SIZE + delta;
    int length = 1;
    
    int prevValue = Integer.MIN_VALUE;
    for (int value : this) {
      length++;
      if (value < prevValue) {
        System.out.println("ERROR: set  not sorted");
        System.exit(0);
      }
      if (value == prevValue) {
        System.out.println("ERROR: set has duplicates!");
        System.exit(0);
      }
      if (length == expected) {
        System.out.println("ERROR: set has bad length: found " + length + ", expected " + expected);
        System.exit(0);
      }
    }
    System.out.println("Integer Set OK");
  }
  
  /**
   * Creates a new IntSetBenchmark
   */
  public IntSetBenchmark() {
    int size = 2;
    init();
    Random random = new Random(this.hashCode());
    while (size < INITIAL_SIZE) {
      if (insert(random.nextInt())) {
        size++;
      }
    }
  }
  
}
