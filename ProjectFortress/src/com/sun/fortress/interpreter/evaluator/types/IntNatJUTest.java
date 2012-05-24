/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.evaluator.types;


public class IntNatJUTest extends com.sun.fortress.useful.TestCaseWrapper {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(IntNatJUTest.class);
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.hashCode()'
     */
    public void testHashCode() {
        IntNat x = IntNat.make(3);
        IntNat y = IntNat.make(3);
        IntNat z = IntNat.make(4);

        assertEquals(x.hashCode(), y.hashCode());
        assertFalse(x.hashCode() == z.hashCode());

        System.out.println("IntNat(3).hashCode() = " + x.hashCode());
        System.out.println("IntNat(4).hashCode() = " + z.hashCode());

        x = IntNat.make(123456789);
        y = IntNat.make(123456789);

        assertEquals(x.hashCode(), y.hashCode());
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.toString()'
     */
    public void testToString() {
        IntNat x = IntNat.make(0);
        IntNat y = IntNat.make(1);
        IntNat z = IntNat.make(123456789);
        assertEquals("0", x.toString());
        assertEquals("1", y.toString());
        assertEquals("123456789", z.toString());

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.equals(Object)'
     */
    public void testEqualsObject() {
        IntNat x = IntNat.make(3);
        IntNat y = IntNat.make(3);
        IntNat z = IntNat.make(4);

        assertEquals(x, y);
        assertEquals(x, x);
        assertFalse(x.equals(z));

        x = IntNat.make(123456789);
        y = IntNat.make(123456789);

        assertEquals(x, y);
        assertEquals(x, x);

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.make(Long)'
     */
    public void testMakeLong() {
        IntNat x = IntNat.make(3);
        IntNat y = IntNat.make(Long.valueOf(3));

        assertEquals(x, y);
        assertEquals(x, x);
        assertEquals(x.hashCode(), y.hashCode());

        x = IntNat.make(123456789);
        y = IntNat.make(Long.valueOf(123456789));
        assertEquals(x, y);
        assertEquals(x, x);
        assertEquals(x.hashCode(), y.hashCode());

    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.getValue()'
     */
    public void testGetValue() {
        IntNat x = IntNat.make(3);
        IntNat y = IntNat.make(4);
        IntNat z = IntNat.make(123456789);
        assertEquals(x.getValue(), 3L);
        assertEquals(y.getValue(), 4L);
        assertEquals(z.getValue(), 123456789L);
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.types.IntNat.getNumber()'
     */
    public void testGetNumber() {
        IntNat x = IntNat.make(3);
        IntNat y = IntNat.make(4);
        IntNat z = IntNat.make(123456789);
        assertEquals(x.getNumber(), Long.valueOf(3L));
        assertEquals(y.getNumber(), Long.valueOf(4L));
        assertEquals(z.getNumber(), Long.valueOf(123456789L));

    }

}
