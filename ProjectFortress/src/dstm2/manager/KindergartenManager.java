/*
 * KindergartenManager.java
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
import dstm2.util.Random;
import java.util.TreeSet;

/**
 * Transactions take turns playing with blocks.
 *
 * @author Bill Scherer
 */
public class KindergartenManager extends BaseManager {
  static final int SLEEP_PERIOD = 1000; // was 100
  static final int MAX_RETRIES = 100; // was 10
  TreeSet<KindergartenManager> otherChildren;
  Random random;
  
  /** Creates new <code>Kindergarten</code> manager */
  public KindergartenManager() {
    super();
    otherChildren = new TreeSet<KindergartenManager>();
    otherChildren.add(this);
    random = new Random();
  }
  
  public void resolveConflict(Transaction me, Transaction other) {
    try {
      KindergartenManager otherManager =
          (KindergartenManager) other.getContentionManager();
      // first, check sharing records.
      if (otherChildren.contains(otherManager)) {
        otherChildren.remove(otherManager);
        other.abort();                      // My turn! My turn!
        return;
      }
      for (int i = 0; i < MAX_RETRIES; i++) {
        this.sleep(SLEEP_PERIOD);
        if (!other.isActive()) {
          return;
        }
      }
      me.abort(); // give up
      return;
    } catch (ClassCastException e) {
      other.abort(); // Oh, other not a Kindergartener. Kill it.
      return;
    }
  }
}
