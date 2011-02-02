/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;


public class TypeRangeJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(TypeRangeJUTest.class);
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.hashCode()'
     */
    public void testHashCode() {
        TypeRange x = new TypeRange(0, 1);
        TypeRange y = new TypeRange(0, 1);
        TypeRange z = new TypeRange(1, 1);
        TypeRange w = new TypeRange(1, 0);
        assertEquals(x.hashCode(), y.hashCode());
        assertEquals(x.hashCode(), x.hashCode());
        assertFalse(x.hashCode() == w.hashCode());
        assertFalse(x.hashCode() == z.hashCode());
        assertFalse(z.hashCode() == w.hashCode());

        System.out.println("w.hashCode()=" + w.hashCode());
        System.out.println("y.hashCode()=" + y.hashCode());
        System.out.println("z.hashCode()=" + z.hashCode());
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.getEvaluatedBase()'
     */
    public void testGetEvaluatedBase() {
        TypeRange y = new TypeRange(0, 1);
        TypeRange z = new TypeRange(1, 1);
        assertEquals(0, y.getEvaluatedBase().longValue());
        assertEquals(1, z.getEvaluatedBase().longValue());
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.getEvaluatedSize()'
     */
    public void testGetEvaluatedSize() {
        TypeRange y = new TypeRange(0, 1);
        TypeRange z = new TypeRange(0, 2);
        assertEquals(1, y.getEvaluatedSize().longValue());
        assertEquals(2, z.getEvaluatedSize().longValue());
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.compatible(TypeRange)'
     */
    public void testCompatible() {
        TypeRange x = new TypeRange(0, 1);
        TypeRange y = new TypeRange(0, 1);
        TypeRange z = new TypeRange(1, 1);
        TypeRange w = new TypeRange(1, 0);

        assertTrue(x.compatible(x));
        assertTrue(x.compatible(y));
        assertTrue(x.compatible(z));
        assertTrue(y.compatible(x));
        assertTrue(z.compatible(x));
        assertFalse(w.compatible(z));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.equals(Object)'
     */
    public void testEqualsObject() {
        TypeRange x = new TypeRange(0, 1);
        TypeRange y = new TypeRange(0, 1);
        TypeRange z = new TypeRange(1, 1);
        TypeRange w = new TypeRange(1, 0);
        assertEquals(x, y);
        assertEquals(x, x);
        assertEquals(y, x);
        assertFalse(x.equals(z));
        assertFalse(x.equals(w));
        assertFalse(w.equals(z));

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.TypeRange.toString()'
     */
    public void testToString() {
        TypeRange x = new TypeRange(0, 1);
        TypeRange z = new TypeRange(1, 1);
        TypeRange w = new TypeRange(1, 0);
        assertEquals("1", x.toString());
        assertEquals("1#1", z.toString());
        assertEquals("1#0", w.toString());
    }

}
