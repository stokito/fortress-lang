/*
 * LocalReadSet.java
 *
 * Created on April 27, 2007, 9:55 AM
 *
 * From "Multiprocessor Synchronization and Concurrent Data Structures",
 * by Maurice Herlihy and Nir Shavit.
 * Copyright 2007 Elsevier Inc. All rights reserved.
 */

package dstm2.factory.ofree.invisible;

import dstm2.factory.ofree.Locator;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Thread-local read set used for invisible reads
 * @author Maurice Herlihy
 */
public class LocalReadSet {
  static ThreadLocal<List<Entry>> local = new ThreadLocal<List<Entry>>() {
    protected List<Entry> initialValue() {
      return new ArrayList<Entry>();
    }
  };
  
  List<Entry> list;
  
  private LocalReadSet() {
    list = local.get();
  }
  public static LocalReadSet getLocal() {
    return new LocalReadSet();
  }
  
  public void add(Adapter key, Locator value) {
    list.add(new Entry(key, value));
  }
  
  public void release(Adapter key) {
    List<Entry> list = local.get();
    ListIterator<Entry> iterator = list.listIterator();
    while (iterator.hasNext()) {
      Entry e = iterator.next();
      if (e.key != key) {
        iterator.remove();
      }
    }
  }
  
  public static boolean validate() {
    List<Entry> localList = local.get();
    try {
      for (Entry e : localList) {
        if (e.value != e.key.start.get()) {
          return false;
        }
      }
      return true;
    } finally {
      localList.clear();
    }
  }
  
  public static void cleanup() {
    local.get().clear();
  }
  
  private class Entry {
    Adapter key;
    Locator value;
    Entry(Adapter k, Locator v) {
      key = k; value = v;
    }
  }
}
