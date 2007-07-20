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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

public class UsefulJUTest extends com.sun.fortress.useful.TcWrapper  {

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

    public final static Fn2<List<String>, List<String>, List<String>> listAppender =
        Fn2.<String>listAppender();

   /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.setProduct(Set<T>, Set<U>, Fn2<T, U, V>) <T, U, V>'
     */
    public void testSetProductSetOfTSetOfUFn2OfTUV() {
        Set<List<String>> x = Useful.<List<String>,List<String>,List<String>>setProduct(
                empty, Collections.<List<String>>emptySet(), listAppender
                );
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.<List<String>,List<String>,List<String>>setProduct(
                Collections.<List<String>>emptySet(), empty, listAppender
                );
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.<List<String>,List<String>,List<String>>setProduct(
                Collections.<List<String>>emptySet(), Collections.<List<String>>emptySet(), listAppender
                );
        assertEquals(Collections.emptySet(), x);
        System.out.println(x);

        x = Useful.setProduct(sea, empty, listAppender);
        assertEquals(sea, x);
        System.out.println(x);

        x = Useful.setProduct(empty, empty, listAppender);
        assertEquals(empty, x);
        System.out.println(x);

        x = Useful.setProduct( Useful.set(Useful.list("a"), Useful.list("b")),
                               Useful.set(Useful.list("c"), Useful.list("d")) ,
                               listAppender);
        assertEquals( Useful.set(VarArgs.make(Useful.list("a", "c"),
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
        assertEquals("(cheese,mouse,cat,dog,kelp,urchin,cod,shark)",
                Useful.listsInParens(wordsList, moreWordsList));
        assertEquals("(kelp,urchin,cod,shark)",
                Useful.listsInParens(noWordsList, moreWordsList));
        assertEquals("(cheese,mouse,cat,dog)",
                Useful.listsInParens(wordsList, noWordsList));
        assertEquals("()",
                Useful.listsInParens(noWordsList, noWordsList));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.useful.Useful.dottedList(List<T>) <T>'
     */
    public void testDottedList() {
        assertEquals("cheese.mouse.cat.dog", Useful.dottedList(wordsList));

    }

    public void testConcat() {
        List<String> both = Useful.concat(wordsList, moreWordsList);
        assertEquals("(cheese,mouse,cat,dog,kelp,urchin,cod,shark)",
            Useful.listInParens(both));
        assertEquals("(kelp,urchin,cod,shark)",
            Useful.listInParens(Useful.concat(noWordsList, moreWordsList)));
        assertEquals("(cheese,mouse,cat,dog)",
            Useful.listInParens(Useful.concat(wordsList, noWordsList)));
        assertEquals("()",
            Useful.listInParens(Useful.concat(noWordsList, noWordsList)));
        assertEquals(noWordsList, Useful.concat());
        assertEquals(noWordsList, Useful.concat(noWordsList));
        assertEquals(moreWordsList, Useful.concat(moreWordsList));
    }

    public void testUnion() {
        Collection<List<String>> nullSet = Useful.<List<String>>set();
        assertEquals(nullSet, Useful.union(nullSet, nullSet));
        assertEquals(houseSeaEmpty, Useful.union(house,sea,empty));
        assertEquals(houseSeaEmpty, Useful.union(houseSea,empty));
        assertEquals(houseSeaEmpty, Useful.union(house,Useful.union(sea,empty)));
        assertEquals(houseSea, Useful.union(nullSet, houseSea));
        assertEquals(houseSea, Useful.union(houseSea, nullSet));
    }
}
