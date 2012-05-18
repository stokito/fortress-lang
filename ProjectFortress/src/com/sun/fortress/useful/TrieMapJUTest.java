/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

public class TrieMapJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(BATJUTest.class);
    }

    static final String[] animals = {
            "alpaca", "ant", "auk", "bat", "beetle", "bison", "buffalo", "camel", "cat", "cavy", "crab", "deer",
            "dingo", "dodo", "dog", "dove", "eagle", "eel", "eland", "elephant", "elk", "emu", "finch", "gar",
            "giraffe", "gnu", "guanaco", "gull", "hawk", "hedgehog", "hyena", "ibis", "iguana", "jackal", "jaguar",
            "kangaroo", "koala", "lemur", "leopard", "llama", "manatee", "mule", "narwhal", "nutria", "octopus",
            "osprey", "ostrich", "owl", "penguin", "pigeon", "piranha", "puffin", "quagga", "quail", "quokka", "rat",
            "ray", "seal", "shark", "snake", "spider", "tern", "tiger", "turtle", "unicorn", "vicuna", "vole",
            "vulture", "walrus", "warthog", "worm", "xiphias", "yak", "zebra"
    };

    static final String[] screws = {
            "", "e", "ee", "eee", "eeee", "eeeee", "eeeeee"
    };

    TrieMap<Integer> screwsMap = null;
    TrieMap<Integer> animalsMap = null;
    TrieMap<Integer> bothMap = null;

    synchronized void initScrewsMap() {
        if (screwsMap != null) return;
        screwsMap = new TrieMap<Integer>();
        for (int i = 0; i < screws.length; i++) {
            assertEquals(screwsMap.size(), i);
            screwsMap.put(screws[i], i);
            screwsMap.check();
        }
        assertEquals(screwsMap.size(), screws.length);
    }

    synchronized void initAnimalsMap() {
        if (animalsMap != null) return;
        animalsMap = new TrieMap<Integer>();
        for (int i = 0; i < animals.length; i++) {
            animalsMap.put(animals[i], i);
        }
        assertEquals(animalsMap.size(), animals.length);
    }

    synchronized void initBothMap() {
        if (bothMap != null) return;
        initScrewsMap();
        initAnimalsMap();
        bothMap = (TrieMap<Integer>) screwsMap.clone();
        bothMap.putAll(animalsMap);
        assertEquals(bothMap.size(), screws.length + animals.length);
    }

    public void testEmpty() {
        TrieMap<Integer> t = new TrieMap<Integer>();
        assertEquals(t.size(), 0);
        assert (t.isEmpty());
        for (int i = 0; i < animals.length; i++) {
            assertFalse(t.containsKey(animals[i]));
            assertNull(t.get(animals[i]));
            assertNull(t.remove(animals[i]));
        }
        for (int i = 0; i < screws.length; i++) {
            assertFalse(t.containsKey(screws[i]));
            assertNull(t.get(screws[i]));
            assertNull(t.remove(screws[i]));
        }
        for (Map.Entry<String, Integer> e : t) {
            fail("iterator yielded " + e);
        }
        for (Map.Entry<String, Integer> e : t.entrySet()) {
            fail("entry iterator yielded " + e);
        }
    }

    public void testScrewsMap() {
        initScrewsMap();
        assertTrue(screwsMap != null);
        int n = 0;
        for (Map.Entry<String, Integer> e : screwsMap) {
            assertEquals(screws[e.getValue()], e.getKey());
            assertTrue(n++ < screwsMap.size());
        }
        assertEquals(n, screwsMap.size());
        for (int i = 0; i < screws.length; i++) {
            boolean b = screwsMap.containsKey(screws[i]);
            Useful.use(b);
            assertEquals(Integer.valueOf(i), screwsMap.get(screws[i]));
        }
        for (int i = 0; i < animals.length; i++) {
            assertFalse(screwsMap.containsKey(animals[i]));
            assertNull(screwsMap.get(animals[i]));
        }
        assertEquals(screwsMap, screwsMap);
    }

    public void testBothMap() {
        initBothMap();
        assertTrue(bothMap != null);
        int n = 0;
        for (Map.Entry<String, Integer> e : bothMap) {
            if (e.getValue() < screws.length) {
                assertTrue(animals[e.getValue()].equals(e.getKey()) || screws[e.getValue()].equals(e.getKey()));
            } else {
                assertEquals(animals[e.getValue()], e.getKey());
            }
            assertTrue(n++ < bothMap.size());
        }
        assertEquals(n, bothMap.size());
        for (int i = 0; i < animals.length; i++) {
            boolean b = bothMap.containsKey(animals[i]);
            Useful.use(b);
            assertEquals(Integer.valueOf(i), bothMap.get(animals[i]));
        }
        for (int i = 0; i < screws.length; i++) {
            boolean b = bothMap.containsKey(screws[i]);
            Useful.use(b);
            assertEquals(Integer.valueOf(i), bothMap.get(screws[i]));
        }
        assertEquals(bothMap, bothMap);
    }

    public void testAnimalsMap() {
        initAnimalsMap();
        assertTrue(animalsMap != null);
        int n = 0;
        for (Map.Entry<String, Integer> e : animalsMap) {
            assertEquals(animals[e.getValue()], e.getKey());
            assertTrue(n++ < animalsMap.size());
        }
        assertEquals(n, animalsMap.size());
        for (int i = 0; i < animals.length; i++) {
            boolean b = animalsMap.containsKey(animals[i]);
            Useful.use(b);
            assertEquals(Integer.valueOf(i), animalsMap.get(animals[i]));
        }
        for (int i = 0; i < screws.length; i++) {
            assertFalse(animalsMap.containsKey(screws[i]));
            assertNull(animalsMap.get(screws[i]));
        }
        assertEquals(animalsMap, animalsMap);
        // throw new Error("animalsMap: "+animalsMap);
    }

    // Permutes entries in place, returns array permutation (original indices).
    private int[] shuffle(String[] entries) {
        int[] indices = new int[entries.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        Random g = new Random();
        for (int i = entries.length - 1; i > 0; i--) {
            int j = g.nextInt(i);
            int o = indices[j];
            String e = entries[j];
            indices[j] = indices[i];
            entries[j] = entries[i];
            indices[i] = o;
            entries[i] = e;
        }
        return indices;
    }

    // Undo permutation given in perm.
    private void unpermute(String[] entries, int[] perm) {
        String[] orig = Arrays.copyOf(entries, entries.length);
        for (int i = 0; i < perm.length; i++) {
            entries[perm[i]] = orig[i];
        }
    }

    private void permTest(String[] initial, TrieMap<Integer> initialMap) {
        String[] src = Arrays.copyOf(initial, initial.length);
        int[] perm = shuffle(src);
        TrieMap<Integer> srcMap = new TrieMap<Integer>();
        for (int i = 0; i < src.length; i++) {
            assertFalse(initialMap.equals(srcMap));
            srcMap.put(src[i], Integer.valueOf(perm[i]));
            srcMap.check();
        }
        // System.out.println("permuted: "+srcMap);
        assertEquals(initialMap, srcMap);
        TrieMap<Integer> srcCpy = initialMap.copy();
        shuffle(src);
        for (int i = src.length - 1; i >= 0; i--) {
            srcMap.remove(src[i]);
            srcCpy.remove(src[i]);
            srcMap.check();
            srcCpy.check();
            assertEquals(srcCpy, srcMap);
            assertEquals(i, srcCpy.size());
        }
    }

    public void testPermutationsScrew() {
        initScrewsMap();
        // System.out.println("Initial " + screwsMap);
        for (int j = 0; j < 20; j++) {
            permTest(screws, screwsMap);
        }
        // throw new Error("Hand inspection.");
    }

    public void testPermutationsAnimals() {
        initAnimalsMap();
        // System.out.println("Initial " + animalsMap);
        for (int j = 0; j < 100; j++) {
            permTest(animals, animalsMap);
        }
        // throw new Error("Hand inspection.");
    }
}
