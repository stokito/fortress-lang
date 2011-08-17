/*******************************************************************************
 Copyright 2008,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class UsefulJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public UsefulJUTest() {
        super("UsefulJUTest");
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(UsefulJUTest.class);
    }

    static List<String> wordsList = Useful.list("cheese", "mouse", "cat", "dog");
    static List<String> moreWordsList = Useful.list("kelp", "urchin", "cod", "shark");
    static List<String> noWordsList = Collections.<String>emptyList();

    static Set<List<String>> house = Useful.<List<String>>set(wordsList);
    static Set<List<String>> sea = Useful.<List<String>>set(moreWordsList);
    static Set<List<String>> empty = Useful.<List<String>>set(noWordsList);
    static Set<List<String>> houseSea = Useful.<List<String>>set(wordsList, moreWordsList);
    static Set<List<String>> houseSeaEmpty = Useful.<List<String>>set(wordsList, moreWordsList, noWordsList);

    static Set<String> wordsSet = new HashSet<String>(wordsList);

    
    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.setProduct(Set<List<T>>, Set<T>) <T>'
     */
    public void testSetProductSetOfListOfTSetOfT() {
        Set<List<String>> a = Useful.setProduct(house, Collections.<String>emptySet());
        assertEquals(Collections.emptySet(), a);

    }

    public final static Fn2<List<String>, List<String>, List<String>> listAppender = Fn2.<String>listAppender();

    /*
    * Test method for 'com.sun.fortress.interpreter.useful.Useful.setProduct(Set<T>, Set<U>, Fn2<T, U, V>) <T, U, V>'
    */
    public void testSetProductSetOfTSetOfUFn2OfTUV() {
        Set<List<String>> x = Useful.<List<String>, List<String>, List<String>>setProduct(empty,
                                                                                          Collections.<List<String>>emptySet(),
                                                                                          listAppender);
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.<List<String>, List<String>, List<String>>setProduct(Collections.<List<String>>emptySet(),
                                                                        empty,
                                                                        listAppender);
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.<List<String>, List<String>, List<String>>setProduct(Collections.<List<String>>emptySet(),
                                                                        Collections.<List<String>>emptySet(),
                                                                        listAppender);
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.setProduct(sea, empty, listAppender);
        assertEquals(sea, x);
        System.out.println(x);

        x = Useful.setProduct(empty, empty, listAppender);
        assertEquals(empty, x);
        System.out.println(x);

        x = Useful.setProduct(Useful.set(Useful.list("a"), Useful.list("b")), Useful.set(Useful.list("c"), Useful.list(
                "d")), listAppender);
        assertEquals(Useful.set(VarArgs.make(Useful.list("a", "c"),
                                             Useful.list("a", "d"),
                                             Useful.list("b", "c"),
                                             Useful.list("b", "d"))), x);
        System.out.println(x);

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.singleton(T) <T>'
     */
    public void testSingleton() {

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.listInParens(List<T>) <T>'
     */
    public void testListInParens() {
        assertEquals("(cheese,mouse,cat,dog)", Useful.listInParens(wordsList));
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.listInCurlies(List<T>) <T>'
     */
    public void testListInCurlies() {
        assertEquals("{cheese,mouse,cat,dog}", Useful.listInCurlies(wordsList));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.listInDelimiters(String, List<T>, String) <T>'
     */
    public void testListInDelimiters() {
        assertEquals("<<<cheese,mouse,cat,dog>>>", Useful.listInDelimiters("<<<", wordsList, ">>>"));
        assertEquals("<<<>>>", Useful.listInDelimiters("<<<", noWordsList, ">>>"));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.listsInParens(List<T>, List<U>) <T, U>'
     */
    public void testListsInParens() {
        assertEquals("(cheese,mouse,cat,dog,kelp,urchin,cod,shark)", Useful.listsInParens(wordsList, moreWordsList));
        assertEquals("(kelp,urchin,cod,shark)", Useful.listsInParens(noWordsList, moreWordsList));
        assertEquals("(cheese,mouse,cat,dog)", Useful.listsInParens(wordsList, noWordsList));
        assertEquals("()", Useful.listsInParens(noWordsList, noWordsList));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.dottedList(List<T>) <T>'
     */
    public void testDottedList() {
        assertEquals("cheese.mouse.cat.dog", Useful.dottedList(wordsList));

    }

    public void testConcat() {
        List<String> both = Useful.concat(wordsList, moreWordsList);
        assertEquals("(cheese,mouse,cat,dog,kelp,urchin,cod,shark)", Useful.listInParens(both));
        assertEquals("(kelp,urchin,cod,shark)", Useful.listInParens(Useful.concat(noWordsList, moreWordsList)));
        assertEquals("(cheese,mouse,cat,dog)", Useful.listInParens(Useful.concat(wordsList, noWordsList)));
        assertEquals("()", Useful.listInParens(Useful.concat(noWordsList, noWordsList)));
        assertEquals(noWordsList, Useful.concat());
        assertEquals(noWordsList, Useful.concat(noWordsList));
        assertEquals(moreWordsList, Useful.concat(moreWordsList));
    }

    public void testUnion() {
        Collection<List<String>> nullSet = Useful.<List<String>>set();
        assertEquals(nullSet, Useful.union(nullSet, nullSet));
        assertEquals(houseSeaEmpty, Useful.union(house, sea, empty));
        assertEquals(houseSeaEmpty, Useful.union(houseSea, empty));
        assertEquals(houseSeaEmpty, Useful.union(house, Useful.union(sea, empty)));
        assertEquals(houseSea, Useful.union(nullSet, houseSea));
        assertEquals(houseSea, Useful.union(houseSea, nullSet));
    }

    public void testCountMatch() {
        assertEquals(3, Useful.countMatches("aaa", "a"));
        assertEquals(0, Useful.countMatches("", "a"));
        assertEquals(0, Useful.countMatches("aaa", "b"));
        assertEquals(3, Useful.countMatches("ababa", "a"));
    }

    public void testReplaceCount() {
        assertEquals("aaa", Useful.replace("aaa", "a", "b", 0));
        assertEquals("aaa", Useful.replace("aaa", "c", "b", 3));
        assertEquals("ba", Useful.replace("aaa", "aa", "b", 3));
        assertEquals("bb", Useful.replace("aaaa", "aa", "b", 3));
        assertEquals("baa", Useful.replace("aaa", "a", "b", 1));
        assertEquals("bba", Useful.replace("aaa", "a", "b", 2));
        assertEquals("bbb", Useful.replace("aaa", "a", "b", 3));
        assertEquals("bbb", Useful.replace("aaa", "a", "b", 4));
        assertEquals("cba", Useful.replace("caa", "a", "b", 1));
    }

    public void testHashBijection() {
        HashBijection<String, Integer> map = new HashBijection<String, Integer>();
        Integer one = 1;
        Integer two = 2;
        Integer three = 3;
        Integer threeA = 3;

        map.put("one", one);
        map.put("two", two);
        map.put("three", three);

        assertEquals(true, map.validate());

        map.put("one", one);
        map.put("two", two);
        map.put("three", threeA);
        assertEquals(true, map.validate());
        assertEquals(true, map.get("three") == threeA);

        map.put("too", two);
        assertEquals(true, map.validate());

        map.put("too", two);
        assertEquals(true, map.validate());

        map.put("too", three);
        assertEquals(true, map.validate());

        map.remove("too");
        assertEquals(true, map.validate());
    }

    public void testMOMOS() {
        MapOfMapOfSet<String, String, String> reln = new MapOfMapOfSet<String, String, String>();

        reln.putItem("eats", "cow", "grass");
        reln.putItem("eats", "cow", "corn");
        reln.putItem("eats", "rat", "cheese");
        reln.putItem("chases", "dog", "cat");
        reln.putItem("chases", "cat", "rat");

        IMultiMap<String, String> eats = reln.get("eats");
        IMultiMap<String, String> chases = reln.get("chases");

        assertEquals(eats.get("cow"), Useful.set("grass", "corn"));
        assertEquals(eats.get("rat"), Useful.set("cheese"));
        assertEquals(chases.get("dog"), Useful.set("cat"));
        assertEquals(chases.get("cat"), Useful.set("rat"));

    }

    public void testMOMO() throws IOException, VersionMismatch {
        MapOfMap<String, String, String> reln = new MapOfMap<String, String, String>();

        reln.putItem("eats", "cow", "grass");
        reln.putItem("eats", "cow", "corn");
        reln.putItem("eats", "rat", "cheese");
        reln.putItem("chases", "dog", "cat");
        reln.putItem("chases", "cat", "rat");

        Map<String, String> eats = reln.get("eats");
        Map<String, String> chases = reln.get("chases");

        assertEquals(eats.get("cow"), "corn");
        assertEquals(eats.get("rat"), "cheese");
        assertEquals(chases.get("dog"), "cat");
        assertEquals(chases.get("cat"), "rat");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        CheapSerializer<Map<String, Map<String, String>>> ser = MapOfMap.serializer(CheapSerializer.STRING,
                                                                                    CheapSerializer.STRING,
                                                                                    CheapSerializer.STRING);

        ser.version(bos);
        ser.write(bos, reln);

        byte[] b = bos.toByteArray();
        String s = new String(b);

        ByteArrayInputStream bis = new ByteArrayInputStream(b);

        ser.version(bis);
        Map<String, Map<String, String>> reln2 = ser.read(bis);

        assertEquals(reln, reln2);


        System.out.println(reln);
        System.out.println(reln2);
        System.out.println(s);


    }

}
