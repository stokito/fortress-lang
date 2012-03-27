/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class ListJUTest extends com.sun.fortress.useful.TestCaseWrapper {
    public void testMake() {
        PureList<Integer> test = PureList.make(0, 1, 2);
        assertEquals(test, new Cons<Integer>(0, new Cons<Integer>(1, new Cons<Integer>(2, new Empty<Integer>()))));
    }

    public void testReverse() {
        PureList<String> test = PureList.make("this", "is", "a", "test");
        PureList<String> expected = PureList.make("test", "a", "is", "this");
        assertEquals(expected, test.reverse());
    }

    public void testSize() {
        assertEquals(4, PureList.make(5, 6, 7, 8).size());
    }

    public void testMap() {
        assertEquals(PureList.make(2, 3, 4), PureList.make("ab", "cde", "fghi").
                map(new Fn<String, Integer>() {
            public Integer apply(String x) {
                return Integer.valueOf(x.length());
            }
        }));
    }

    public void testContains() {
        PureList<String> test = PureList.make("a", "b", "c");
        assert (test.contains("a"));
        assert (test.contains("b"));
        assert (test.contains("c"));
    }

    public void testFromJavaList() {
        java.util.List<String> test = Useful.list("ef", "gh", "ij");

        assertEquals(PureList.make("ef", "gh", "ij"), PureList.fromJavaList(test));
    }

    public void testToArray() {
        PureList<Integer> test = PureList.make(0, 1, 2, 3, 4, 5, 6);
        Object[] result = test.toArray(4);
        assertEquals(4, result.length);

        for (int i = 0; i < 4; i++) {
            assertEquals(i, result[i]);
        }
        assertEquals(PureList.make(0, 1, 2, 3, 4, 5, 6), test);
    }

    public void testToJavaList() {
        PureList<Integer> test = PureList.make(144, 121, 100, 81);
        java.util.List<Integer> expected = Useful.list(144, 121, 100, 81);

        assertEquals(expected, test.toJavaList());
    }

}
