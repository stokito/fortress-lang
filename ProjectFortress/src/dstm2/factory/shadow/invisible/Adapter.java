/*
 * Adapter.java
 *
 * Created on April 27, 2007, 4:03 PM
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

package dstm2.factory.shadow.invisible;
import dstm2.ContentionManager;
import dstm2.Transaction;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.PanicException;
import dstm2.exceptions.SnapshotException;
import dstm2.factory.Copyable;
import dstm2.factory.Factory;
import dstm2.Thread;
import dstm2.factory.Releasable;
import dstm2.factory.Snapable;
import dstm2.factory.ofree.CopyableFactory;
import dstm2.factory.ofree.Locator;
import dstm2.factory.shadow.Recoverable;
import dstm2.factory.shadow.RecoverableFactory;
import java.lang.Class;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Shadow-field atomic object implementation. Visible reads.
 * Supports snapshots and early release.
 * @author Maurice Herlihy
 */
public class Adapter<T> implements dstm2.factory.Adapter<T>, Releasable {
  Class<T> iface;
  T version;
  Recoverable rVersion;
  ContentionManager manager;
  Transaction writer;
  long versionNumber;
  private final String FORMAT = "Unexpected transaction state: %s";
  /**
   * A transaction switches to exclusive mode after being aborted this many times.
   */
  public static final int CONFLICT_THRESHOLD = 8;
  
  static Map<Class, RecoverableFactory> factoryMap
      = new HashMap<Class, RecoverableFactory>();
  
  /**
   * Creates a new instance of Adapter
   */
  public Adapter(Class<T> _class) {
    iface = _class;
    RecoverableFactory<T> factory = factoryMap.get(iface);
    if (factory == null) {
      factory = new RecoverableFactory<T>(iface);
      factoryMap.put(iface, factory);
    }
    version = factory.create();
    rVersion = (Recoverable)version;
    manager = Thread.getContentionManager();
    writer = Transaction.COMMITTED;
    versionNumber = 0;
  }
  
  public <V> Adapter.Setter<V> makeSetter(String methodName, Class<V> _class) {
    try {
      final Method method = version.getClass().getMethod(methodName, _class);
      return new Adapter.Setter<V>() {
        public void call(V value) {
          try {
            Transaction me  = Thread.getTransaction();
            Transaction other = null;
            Set<Transaction> others = null;
            while (true) {
              synchronized (Adapter.this) {
                other = openWrite(me);
                if (other == null) {
                  method.invoke(version, value);
                  versionNumber++;
                  return;
                }
              }
              if (others != null) {
                manager.resolveConflict(me, others);
              } else if (other != null) {
                manager.resolveConflict(me, other);
              }
            }
          } catch (IllegalAccessException e) {
            throw new PanicException(e);
          } catch (InvocationTargetException e) {
            throw new PanicException(e);
          }
        }};
    } catch (NoSuchMethodException e) {
      throw new PanicException(e);
    }
  }
  
  public <V> Adapter.Getter<V> makeGetter(String methodName, Class<V> _class)  {
    try {
      final Method method = version.getClass().getMethod(methodName);
      return new Adapter.Getter<V>() {
        public V call() {
          try {
            Transaction me  = Thread.getTransaction();
            Transaction other = null;
            while (true) {
              synchronized (Adapter.this) {
                other = openRead(me);
                if (other == null) {
                  return (V)method.invoke(version);
                }
              }
              manager.resolveConflict(me, other);
            }
          } catch (SecurityException e) {
            throw new PanicException(e);
          } catch (IllegalAccessException e) {
            throw new PanicException(e);
          } catch (InvocationTargetException e) {
            throw new PanicException(e);
          }
        }};
    } catch (NoSuchMethodException e) {
      throw new PanicException(e);
    }
  }
  
  public synchronized void release() {
    Transaction me = Thread.getTransaction();
    if (me != null) {
      boolean ok = LocalReadSet.getLocal().release(this);
      if (!ok) {
        throw new PanicException("illegal release attempt");
      }
    }
  }
  /**
   * Tries to open object for reading. Returns reference to conflictin transaction, if one exists
   **/
  public Transaction openRead(Transaction me) {
    // don't try read sharing if contention seems high
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        rVersion.recover();
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    if (me.attempts > CONFLICT_THRESHOLD) {
      return openWrite(me);
    }
    // Am I still active?
    if (!me.isActive()) {
      throw new AbortedException();
    }
    // Have I already opened this object?
    if (writer == me) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        break;
      case ABORTED:
        rVersion.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = Transaction.COMMITTED;
    LocalReadSet.getLocal().add(this, versionNumber);
    manager.openSucceeded();
    return null;
  }
  
  /**
   * Tries to open object for reading.
   * Returns reference to conflicting transaction, if one exists
   **/
  Transaction openWrite(Transaction me) {
    boolean cacheHit = false;  // already open for read?
    // not in a transaction
    if (me == null) {	// restore object if latest writer aborted
      if (writer.isAborted()) {
        rVersion.recover();
        writer = Transaction.COMMITTED;
      }
      return null;
    }
    if (!me.isActive()) {
      throw new AbortedException();
    }
    if (me == writer) {
      return null;
    }
    switch (writer.getStatus()) {
      case ACTIVE:
        return writer;
      case COMMITTED:
        rVersion.backup();
        break;
      case ABORTED:
        rVersion.recover();
        break;
      default:
        throw new PanicException(FORMAT, writer.getStatus());
    }
    writer = me;
    if (!cacheHit) {
      me.memRefs++;
      manager.openSucceeded();
    }
    LocalReadSet.getLocal().release(this);
    return null;
  }
  public static Callable<Boolean> onValidate() {
    return new Callable<Boolean>() {
      public Boolean call() {
        return LocalReadSet.validate();
      }
    };
  }
  public static Runnable onCommit() {
    return new Runnable() {
      public void run() {
        LocalReadSet.cleanup();
      }
    };
  }
  public static Runnable onAbort() {
    return new Runnable() {
      public void run() {
        LocalReadSet.cleanup();
      }
    };
  }
  
}

