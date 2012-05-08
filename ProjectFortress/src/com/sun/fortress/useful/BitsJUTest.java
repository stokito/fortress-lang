/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

public class BitsJUTest extends TestCaseWrapper {
    public void testCeilLogTwo() {
        assertEquals(0, Bits.ceilLogTwo(0));
        assertEquals(0, Bits.ceilLogTwo(1));
        assertEquals(1, Bits.ceilLogTwo(2));
        assertEquals(2, Bits.ceilLogTwo(3));
        assertEquals(2, Bits.ceilLogTwo(4));
        assertEquals(30, Bits.ceilLogTwo(0x40000000));
        assertEquals(31, Bits.ceilLogTwo(0x80000000));
        assertEquals(32, Bits.ceilLogTwo(0x80000001));
        assertEquals(32, Bits.ceilLogTwo(-1));

    }

    public void testMask() {
        assertEquals(1, Bits.mask(1));
        assertEquals(3, Bits.mask(2));
        assertEquals(0x7fffffff, Bits.mask(31));
        assertEquals(0xffffffffL, Bits.mask(32));
        assertEquals(0x1ffffffffL, Bits.mask(33));
        assertEquals(0xFFFFFFFFffffffffL, Bits.mask(64));
        // assertEquals(-1, Bits.mask(0));
    }
}
