/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

public class GHashMapJUTest extends com.sun.fortress.useful.TcWrapper  {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(GHashMapJUTest.class);
    }

    GHashMap<String, String> map = new GHashMap<String, String>(Hasher.CIHasher);

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.GHashMap(Hasher<K>)'
     */
    public void testGHashMap() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.clear()'
     */
    public void testClear() {
        map.clear();
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.containsKey(Object)'
     */
    public void testContainsKey() {
        assertTrue (! map.containsKey("cat"));
        map.put("cat", "meow");
        assertTrue(map.containsKey("cat"));
        assertTrue(map.containsKey("CAT"));
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.containsValue(Object)'
     */
    public void testContainsValue() {
        assertTrue (! map.containsValue("cat"));
        map.put("cat", "meow");
        assertTrue (map.containsValue("meow"));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.entrySet()'
     */
    public void testEntrySet() {
        Set<Map.Entry<String, String>> s0 = map.entrySet();
        assertTrue(s0.isEmpty());

        map.put("cat", "meow");
        s0 = map.entrySet();
        assertTrue(!s0.isEmpty());
        Map.Entry<String, String> me = s0.iterator().next();
        assertEquals("cat", me.getKey());
        assertEquals("meow", me.getValue());
        me.setValue("purr");
        assertEquals("purr", me.getValue());
        assertEquals("purr", map.get("Cat"));
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.isEmpty()'
     */
    public void testIsEmpty() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.keySet()'
     */
    public void testKeySet() {
        Set<String> s0 = map.keySet();
        assertTrue(s0.isEmpty());

        map.put("cat", "meow");
        s0 = map.keySet();
        assertTrue(!s0.isEmpty());
        String me = s0.iterator().next();
        assertEquals("cat", me);
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.put(Object, Object)'
     */
    public void testPut() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.putAll(Map)'
     */
    public void testPutAll() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.get(Object)'
     */
    public void testGet() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.remove(Object)'
     */
    public void testRemove() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.size()'
     */
    public void testSize() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.GHashMap.values()'
     */
    public void testValues() {

    }

}
