/*
 * KarmaManager.java
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
import dstm2.Transaction;
import dstm2.ContentionManager;

/**
 * Uses "karmic debt management" to resolve conflicts.  Roughly, a
 * thread gains "karma" for every object it successfully opens, and
 * threads with greater karma can abort transactions of other threads.
 * A thread's karma is reset every time it successfully commits a
 * transaction, but not when it is aborted (hence the name).
 *
 * When conflict occurs between two transactions, the one with the
 * greater accumulated karma wins. If the other transaction holds a
 * block, it gets aborted immediately. Otherwise, the "lesser"
 * transaction backs off for a fixed interval and up to the square of
 * the difference in karma bethere the two.
 *
 * The key idea behind this policy is that it allows long transactions
 * to eventually finish even if mixed with lots of competing shorter
 * transactions. This happens because the longer transaction will
 * accumulate more and more karma each time it gets aborted, so it
 * will eventually reach "critical mass" and be able to bulldoze its
 * way through to get its work done.
 *
 * @author Bill Scherer
 **/

public class KarmaManager extends BaseManager {
  static final int SLEEP_PERIOD = 1000;
  
  public void resolveConflict(Transaction me, Transaction other) {
    ContentionManager otherManager = other.getContentionManager();
    for (int attempts = 0; ; attempts++) {
      long delta = otherManager.getPriority() - priority;
      if (attempts > delta) {
        other.abort();
      }
    }
  }
  
  /**
   * Reset priority only on commit. On abort, restart with previous priority.
   * "Cosmic debt"?. More like cosmic credit.
   **/
  public void committed() {
    setPriority(0);
  }
  
  public void openSucceeded() {
    setPriority(getPriority() + 1);
  }
}
