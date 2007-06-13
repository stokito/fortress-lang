/*
 * GreedyManager.java
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

/**
 * Tries to keep a maximal independent set running.
 * If prior transaction is
 *		waiting or lower priority, then abort it.
 *		otherwise, wait for it to commit, abort, or wait
 * Complete description in
 *		Rachid Guerraoui, Maurice Herlihy, and Sebastian Pochon, Toward a Theory of Transactional Contention Management,
 *		Proceedings of the Twenty-Fourth Annual Symposium on Principles of Distributed Computing (PODC).
 *		Las Vegas, NV July 2005.
 * @author Maurice Herlihy
 */
public class GreedyManager extends BaseManager {
  public void resolveConflict(Transaction me, Transaction other) {
    if (other.waiting || other.startTime < me.startTime) {
      other.abort();
    } else {
      me.waiting = true;		// I'm waiting
      other.waitWhileActiveNotWaiting();
      me.waiting = false;		// I'm no longer waiting
    }
  }
  /**
   * Reset priority only on commit. On abort, restart with previous priority.
   **/
  public void committed() {
    setPriority(0);
  }
  
  public void openSucceeded() {
    setPriority(getPriority() + 1);
  }
}
