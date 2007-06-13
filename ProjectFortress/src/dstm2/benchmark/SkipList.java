/*
 * SkipList.java
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

package dstm2.benchmark;

import dstm2.AtomicArray;
import dstm2.atomic;
import dstm2.factory.Factory;
import dstm2.Thread;
import dstm2.util.Random;
import java.util.Iterator;

/**
 * @author Maurice Herlihy
 */
public class SkipList extends IntSetBenchmark {
  /**
   * Transactional Node factory.
   */
  static Factory<Node> factory;
  
  // Maximum level any node in a skip list can have
  private final int MaxLevel = 32;
  
  // Probability factor used to determine the node level
  private final double Probability = 0.5;
  
  // The skip list header. It also serves as the NIL node.
  private Node header;
  
  // Random number generator for generating random node levels.
  private Random random;
  
  // Current maximum list level.
  private int listLevel;
  
  protected void init() {
    factory = Thread.makeFactory(Node.class);
    int size = 0;
    header = factory.create();
    random = new Random();
    initNode(header, MaxLevel);
    listLevel = 1;
    AtomicArray<Node> forward = header.getForward();
    for (int i = 0; i < MaxLevel; i++) {
      forward.set(i, header);
    }
  }
  
  public java.util.Iterator<Integer> iterator() {
    return new MyIterator();
  }
  
  public boolean insert(int key) {
    Node[] update = new Node[MaxLevel];
    Node curr = search(key, update);
    // If key does not already exist in the skip list.
    if(curr.getKey() != key) {
      insert(key, update);
      return true;
    } else {
      return false;
    }
  }
  
  public boolean contains(int key) {
    Node[] dummy = new Node[MaxLevel];
    Node curr = search(key, dummy);
    return curr.getKey() == key;
  }
  
  public boolean remove(int key) {
    Node[] update = new Node[MaxLevel];
    Node curr = search(key, update);
    if(curr.getKey() == key) {
      // Redirect references to victim node to successors.
      for (int i = 0; i < listLevel && update[i].getForward().get(i) == curr; i++) {
        update[i].getForward().set(i, curr.getForward().get(i));
      }
      return true;
    }
    return false;
  }
  
  private class MyIterator implements Iterator<Integer> {
    Node node = header.getForward().get(0);
    public boolean hasNext() {
      return node != header;
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
    public Integer next() {
      node = node.getForward().get(0);
      return node.getKey();
    }
  }
  /**
   * Initializes an instant of a Node with its node level.
   **/
  public void initNode(Node node, int level) {
    node.setForward(new AtomicArray<Node>(Node.class, level));
  }
  
  /**
   * Initializes an instant of a Node with its node level and
   * key/value pair.
   **/
  public void initNode(Node node, int level, int key) {
    node.setForward(new AtomicArray<Node>(Node.class, level));
    node.setKey(key);
  }
  
  /// <summary>
  /// Returns a level value for a new SkipList node.
  /// <returns>
  /// The level value for a new SkipList node.
  /// </returns>
  /// </summary>
  private int getNewLevel() {
    int level = 1;
    while(random.nextDouble() < Probability && level < MaxLevel && level <= listLevel) {
      level++;
    }
    return level;
  }
  
  /// <summary>
  /// Inserts a keykey into the SkipList.
  /// <param name="key">
  /// The key to insert into the SkipList.
  /// </param>
  /// <param name="update">
  /// An array of nodes holding references to places in the SkipList in
  /// which the search for the place to insert the new key/value pair
  /// dropped down one level.
  /// </param>
  /// </summary>
  private void insert(int key, Node[] update) {
    // Get the level for the new node.
    int newLevel = getNewLevel();
    // If new node level greater than skip list level.
    if (newLevel > listLevel) {
      // Make sure our update references above the current skip list
      // level point to the header.
      for (int i = listLevel; i < newLevel; i++) {
        update[i] = header;
      }
      // The current skip list level is now the new node level.
      listLevel++;
    }
    // Create the new node.
    Node newNode = factory.create();
    initNode(newNode, newLevel, key);
    // Insert the new node into the skip list.
    for (int i = 0; i < newLevel; i++) {
      // Initialize new node forward references to update forward references
      newNode.getForward().set(i, update[i].getForward().get(i));
      // Set update forward references to new node.
      update[i].getForward().set(i, newNode);
    }
  }
  
  /// <summary>
  /// Search for the specified key.
  /// <param name="_key">
  /// The key to search for.
  /// </param>
  /// <param name="curr">
  /// A SkipList node to hold the results of the search.
  /// </param>
  /// <param name="update">
  /// An array of nodes holding references to the places in the SkipList
  /// search in which the search dropped down one level.
  /// </param>
  /// <returns>
  /// Returns node with least key greater than or equal to search key.
  /// </returns>
  /// </summary>
  private Node search(int key, Node[] update) {
    int comp;
    // Begin at the start of the skip list.
    Node curr = header;
    // Work our way down from the top of the skip list to the bottom.
    for(int i = listLevel - 1; i >= 0; i--) {
      comp = curr.getForward().get(i).getKey();
      // While we haven't reached the end of the skip list and the
      // current key is less than the search key.
      while(curr.getForward().get(i) != header && comp < key) {
        // Move forward in the skip list.
        curr = curr.getForward().get(i);
        // Get the current key.
        comp = curr.getForward().get(i).getKey();
      }
      // Keep track of each node where we move down a level. Used later to rearrange
      // node references when inserting a new element.
      update[i] = curr;
    }
    // Move ahead in the skip list. If the key isn't there, we end up at a node with a
    // key greater key, and otherwise at a node with the same key.
    curr = curr.getForward().get(0);
    return curr;
  }
  
  @atomic public interface Node {
    /**
     * Get array of nodes further along in the skip list.
     **/
    public AtomicArray<Node> getForward();
    /**
     * Set array of nodes further along in the skip list.
     **/
    public void setForward(AtomicArray<Node> value);
    
    /**
     * Get node value.
     **/
    public int getKey();
    /**
     * Set node value.
     **/
    public void setKey(int value);
  }
}
