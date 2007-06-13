/*
 * Locator.java
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

package dstm2.factory.ofree;
import dstm2.exceptions.AbortedException;
import dstm2.ContentionManager;
import dstm2.exceptions.PanicException;
import dstm2.Transaction.Status;
import dstm2.Transaction;
import dstm2.factory.Copyable;
import dstm2.factory.SequentialFactory;
import dstm2.factory.ofree.ReadSet;

/**
 * A locator points to an old version, a new version, and transactional
 * bookkeeping information. A transaction opens an object by creating
 * and installing a new locator.
 *
 * @author Maurice Herlihy
 */
public final class Locator {
  /**
   * Transaction that last opened this object for writing.
   */
  public Transaction writer;
  /**
   * Set of transactions currently reading this object.
   */
  public final ReadSet readers;
  /**
   * Prior version of object. Meaningless if last writer committed.
   */
  public volatile Copyable oldVersion;
  /**
   * Newer version of object. Tentative it writer is active, meaningless
   * if writer is aborted, and otherwise the current value.
   */
  public volatile Copyable newVersion;
  
  /**
   * Creates a new instance of Locator
   * @param version Current version of object.
   */
  public Locator() {
    writer = Transaction.COMMITTED;
    readers = new ReadSet();
    oldVersion = null;
    newVersion = null;
  }
  /**
   * Open object for writing.
   * @param me Calling transaction.
   * @param version Version to be modified.
   */
  public Locator(Transaction me, Copyable version) {
    writer = me;
    readers = new ReadSet(0);
    oldVersion = null;
    newVersion = version;
  }
  
  /**
   * Checks whether object is alread opened (for writing).
   * @param me calling transaction
   * @return Returns version if already open, null otherwise.
   */
  public Copyable fastPath(Transaction me) {
    // not in a transaction, update in place
    if (me == null) {
      return getVersion(me, null);
    } else if (writer == me) {  // already
      return newVersion;
    } else {
      return null;
    }
  }
  
  public Copyable getVersion(Transaction me, ContentionManager manager) {
    while (true) {
      if (me != null && me.getStatus() == Status.ABORTED) {
        throw new AbortedException();
      }
      switch (writer.getStatus()) {
        case ACTIVE:
          if (manager == null) {
            throw new PanicException("Transactional/Non-Tranactional race");
          }
          manager.resolveConflict(me, writer);
          continue;
        case COMMITTED:
          return newVersion;
        case ABORTED:
          return oldVersion;
        default:
          throw new PanicException("Unexpected transaction state: " + writer.getStatus());
      }
    }
  }
  
  /**
   * Prepare a new locator to be used to open object for reading.
   * @param me calling transaction
   * @param manager caller's contention manager
   * @param newLocator Prepare this locator for reading the object.
   */
  public void readPath(Transaction me,
      ContentionManager manager,
      Locator newLocator) {
    Copyable version = getVersion(me, manager);
    newLocator.oldVersion = newLocator.newVersion = version;
    newLocator.readers.copyFrom(readers);
    newLocator.readers.add(me);
    return;
  }
  
  /**
   * Prepare a new locator to be used to release prior read access.
   * @param me calling transaction
   * @param newLocator Prepare this locator to replace current locator.
   */
  public void releasePath(Transaction me,
      Locator newLocator) {
    newLocator.oldVersion = oldVersion;
    newLocator.newVersion = newVersion;
    newLocator.writer = writer;
    newLocator.readers.copyFrom(readers);
    boolean present = newLocator.readers.remove(me);
    if (!present && me.isActive()) {
      throw new PanicException("illegal release attempt");
    }
  }
  
  /**
   * Prepare a new locator to be used to open object for writing.
   * @param me caller
   * @param manager caller's contention manager
   * @param newLocator locator to prepare
   */
  public void writePath(Transaction me,
      ContentionManager manager,
      Locator newLocator) {
    retry:
      while (true) {
        Copyable version = getVersion(me, manager);
        newLocator.oldVersion = version;
        newLocator.newVersion.copyFrom(version);
        for (Transaction reader : readers) {
          if (reader.isActive() && reader != me) {
            manager.resolveConflict(me, readers);
            continue retry;
          }
        }
        return;
      }
  }
  
  public Copyable snapshot(Transaction me, ContentionManager manager) {
    return getVersion(me, manager);
  }
}
