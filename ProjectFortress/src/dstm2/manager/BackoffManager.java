/*
 * BackoffManager.java
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

import dstm2.util.Random;
import dstm2.ContentionManager;
import dstm2.Transaction;
import java.util.Collection;

/**
 * Contention manager employing simple exponential backoff.
 * @author Maurice Herlihy
 */
public class BackoffManager extends BaseManager {
  static final int MIN_LOG_BACKOFF = 4;
  static final int MAX_LOG_BACKOFF = 26;
  static final int MAX_RETRIES = 22;
  
  Random random;
  
  int currentAttempt = 0;
  
  public BackoffManager() {
    random = new Random();
  }
  public void openSucceeded() {
    super.openSucceeded();
    currentAttempt = 0;
  }
  public void resolveConflict(Transaction me, Transaction other) {
    if (currentAttempt <= MAX_RETRIES) {
      if (!other.isActive()) {
        return;
      }
      int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
      if (logBackoff > MAX_LOG_BACKOFF) {
        logBackoff = MAX_LOG_BACKOFF;
      }
      int sleep = random.nextInt(1 << logBackoff);
      try {
        Thread.sleep(sleep/1000000, sleep % 1000000);
      } catch (InterruptedException ex) {
      }
      currentAttempt++;
    } else {
      other.abort();
      currentAttempt = 0;
    }
  }
  public void resolveConflict(Transaction me, Collection<Transaction> others) {
    if (currentAttempt <= MAX_RETRIES) {
      int logBackoff = currentAttempt - 2 + MIN_LOG_BACKOFF;
      if (logBackoff > MAX_LOG_BACKOFF) {
        logBackoff = MAX_LOG_BACKOFF;
      }
      int sleep = random.nextInt(1 << logBackoff);
      try {
        Thread.sleep(sleep/1000000, sleep % 1000000);
      } catch (InterruptedException ex) {
      }
      currentAttempt++;
    } else {
      for (Transaction other : others) {
        if (other.isActive() && other != me) {
          other.abort();
        }
      }
      currentAttempt = 0;
    }
  }
}
