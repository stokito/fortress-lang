/*
 * EruptionManager.java
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

package dstm2.manager;

import dstm2.ContentionManager;
import dstm2.Transaction;

/**
 * Resolves conflicts by increasing pressure on the transactions that
 * a blocked transaction is waiting on, eventually causing them to
 * erupt through to completion. The way this works is that each time a
 * block is successfully opened, the transaction gains one point of
 * "momentum". When a transaction finds itself blocked by another of
 * higher priority, it adds its momentum (priority) to the other
 * transaction and then waits for the other transaction to complete.
 * Like the Karma manager, Eruption will only wait around so long
 * before clobbering the other transaction and going on anyway; the
 * maximum wait is proportional to the square of the difference in
 * priorities between the two transactions. Of course, at contention
 * time, if the other transaction has a lower priority, we just erupt
 * past it.
 *
 * The reasoning behind this management scheme is that if a particular
 * transaction is blocking resources critical to many other
 * transactions, it will gain all of their priority in addition to its
 * own and thus be much more likely to finish quickly and get out of
 * the way of all the others.
 *
 * Note that while a transaction is blocked, other transactions can
 * pile up behind it and increase its priority enough to outweigh the
 * transaction it's blocked behind.
 *
 * @author Bill Scherer
 *
 **/

public class EruptionManager extends BaseManager {
  static final int SLEEP_PERIOD = 1000;
  
  /** Creates a new instance of EruptionManager */
  public EruptionManager() {
    priority = 0;
  }
  
  public void resolveConflict(Transaction me, Transaction other) {
    long transferred = 0;
    ContentionManager otherManager = other.getContentionManager();
    for (int attempts = 0; ; attempts++) {
      long otherPriority = otherManager.getPriority();
      long delta = otherPriority - priority;
      if (delta < 0 || attempts > delta * delta) {
        transferred = 0;
        other.abort();
        return;
      }
      // Unsafe increment, but too expensive to synchronize.
      if (priority > transferred) {
        otherManager.setPriority(otherPriority + priority - transferred);
        transferred = priority;
      }
      if (attempts < delta) {
        sleep(SLEEP_PERIOD);
      }
    }
  }
  
  public void openSucceeded() {
    priority++;
  }
  
}
