/*
 * Adapter.java
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

package dstm2.factory.twophase;
import dstm2.ContentionManager;
import dstm2.Transaction;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.PanicException;
import dstm2.exceptions.SnapshotException;
import dstm2.factory.Factory;
import dstm2.Thread;
import dstm2.factory.shadow.Recoverable;
import dstm2.factory.shadow.RecoverableFactory;
import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

/**
 * Simple two-phase locking implementation.
 * <b>Warning</b>
 * This class is intended for tutorial purposes only. It is inefficient, but
 * does a pretty good job of illustrating basic mechanisms.
 * @author Maurice Herlihy
 */
public class Adapter<T> implements dstm2.factory.Adapter<T> {
  T version;
  Lock lock;
  boolean firstTime;
  private final String FORMAT = "Unexpected transaction state: %s";
  private static Map<Class,Factory> map = new HashMap<Class,Factory>();
  
  /**
   * Creates a new instance of Adapter
   */
  public Adapter(Class<T> _class) {
    lock = new ReentrantLock();
    Factory<T> factory = map.get(_class);
    if (factory == null) {
      factory = new RecoverableFactory(_class);
      map.put(_class, factory);
    }
    version = factory.create();
    firstTime = true;
  }
  
  public <V> Adapter.Getter<V> makeGetter(String methodName, Class<V> _class)  {
    try {
      final Method method = version.getClass().getMethod(methodName);
      return new Adapter.Getter<V>() {
        public V call() {
          try{
            lock.lock();
            if (firstTime) {
              ((Recoverable)version).backup();
              firstTime = false;
            }
    Thread.onCommitOnce( new Runnable() {
      public void run() {
        lock.unlock();
      }
    });
    Thread.onAbortOnce( new Runnable() {
      public void run() {
        lock.unlock();
        ((Recoverable)version).recover();
      }
    });
            return (V)method.invoke(version);
          } catch (IllegalArgumentException ex) {
            throw new PanicException(ex);
          } catch (IllegalAccessException ex) {
            throw new PanicException(ex);
          } catch (InvocationTargetException ex) {
            throw new PanicException(ex);
          }
        }};
    } catch (NoSuchMethodException e) {
      throw new PanicException(e);
    }
  }
  
  public <V> Adapter.Setter<V> makeSetter(String methodName, Class<V> _class)  {
    try {
      final Method method = version.getClass().getMethod(methodName, _class);
      return new Adapter.Setter<V>() {
        public void call(V value) {
          try{
            lock.lock();
            if (firstTime) {
              ((Recoverable)version).backup();
              firstTime = false;
            }
    Thread.onCommitOnce( new Runnable() {
      public void run() {
        lock.unlock();
      }
    });
    Thread.onAbortOnce( new Runnable() {
      public void run() {
        lock.unlock();
        ((Recoverable)version).recover();
      }
    });
            method.invoke(version, value);
          } catch (IllegalArgumentException ex) {
            throw new PanicException(ex);
          } catch (IllegalAccessException ex) {
            throw new PanicException(ex);
          } catch (InvocationTargetException ex) {
            throw new PanicException(ex);
          }
        }};
    } catch (NoSuchMethodException e) {
      throw new PanicException(e);
    }
  }
}

