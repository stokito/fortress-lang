/*
 * ContentionManager.java
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

package dstm2;

import dstm2.*;
import java.util.Collection;
/**
 * Interface satisfied by all contention managers
 */
public interface ContentionManager {
  /**
   * Either give the writer a chance to finish it, abort it, or both.
   * @param me Calling transaction.
   * @param other Transaction that's in my way.
   */
  void resolveConflict(Transaction me, Transaction other);
  
  /**
   * Either give the writer a chance to finish it, abort it, or both.
   * @param me Calling transaction.
   * @param other set of transactions in my way
   */
  void resolveConflict(Transaction me, Collection<Transaction> other);
  
  /**
   * Assign a priority to caller. Not all managers assign meaningful priorities.
   * @return Priority of conflicting transaction.
   */
  long getPriority();
  
  /**
   * Change this manager's priority.
   * @param value new priority value
   */
  void setPriority(long value);
  
  /**
   * Notify manager that object was opened.
   */
  void openSucceeded();
  
  /**
   * Notify manager that transaction committed.
   */
  void committed();
  
};
