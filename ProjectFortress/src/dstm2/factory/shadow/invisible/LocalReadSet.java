/*
 * LocalReadSet.java
 *
 * Created on April 27, 2007, 9:55 AM
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

import dstm2.factory.ofree.Locator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Thread-local read set used for invisible reads
 * @author Maurice Herlihy
 */
public class LocalReadSet {
  static ThreadLocal<Map<Adapter,Long>> local = new ThreadLocal<Map<Adapter,Long>>() {
    protected Map<Adapter,Long> initialValue() {
      return new HashMap<Adapter,Long>();
    }
  };
  
  Map<Adapter,Long> map;
  
  private LocalReadSet() {
    map = local.get();
  }
  public static LocalReadSet getLocal() {
    return new LocalReadSet();
  }
  
  public void add(Adapter key, long value) {
    map.put(key, value);
  }
  
  public boolean release(Adapter key) {
    return map.remove(key) != null;
  }
  
  public static boolean validate() {
    Map<Adapter,Long> localMap = local.get();
    try {
      for (Adapter key : localMap.keySet()) {
        long value = localMap.get(key);
        if (value != key.versionNumber) {
          return false;
        }
      }
      return true;
    } finally {
      localMap.clear();
    }
  }
  
  public static void cleanup() {
    local.get().clear();
  }
}
