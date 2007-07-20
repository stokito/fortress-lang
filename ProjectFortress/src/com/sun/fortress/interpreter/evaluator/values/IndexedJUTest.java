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

package com.sun.fortress.interpreter.evaluator.values;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.types.FTypeInt;

public class IndexedJUTest extends com.sun.fortress.useful.TcWrapper {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        Init.initializeEverything();
    }

    static IUOTuple A3(int x, int l, int m, int n) {
        List<FValue> list = new ArrayList<FValue>(n);
        for (int i = 0; i < n; i++) {
            list.add(A2(x, l, m));
            x += 10000;

        }
        return new IUOTuple(list, null);

    }
    static IUOTuple A2(int x, int l, int m) {
        List<FValue> list = new ArrayList<FValue>(m);
        for (int i = 0; i < m; i++) {
            list.add(A1(x, l));
            x += 100;

        }
        return new IUOTuple(list, null);

    }

    static IUOTuple A1(int x, int l) {
        List<FValue> list = new ArrayList<FValue>(l);
        for (int i = 0; i < l; i++) {
            list.add(FInt.make(x));
            x += 1;

        }
        return new IUOTuple(list, null);
    }

    public void testIUOTuple() {
        IUOTuple x = A3(0, 4,3,2);
        x.finish();
        System.out.println(x);
        assertEquals(3, x.dim());
        assertEquals(4, x.size(0));
        assertEquals(3, x.size(1));
        assertEquals(2, x.size(2));
     }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(IndexedJUTest.class);
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.values.Indexed.Indexed(IndexedShape, FType)'
     */
    public void testIndexed() {
        IUOTuple y = A3(0, 4,3,2);
        y.finish();
        Indexed x = new Indexed(y, FTypeInt.ONLY);
        assertEquals(3, x.dim());
        assertEquals(4, x.size(0));
        assertEquals(3, x.size(1));
        assertEquals(2, x.size(2));
        assertEquals(24, x.size());
     }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.values.Indexed.put(FValue, int[], int)'
     */
    public void testPut() {
        IUOTuple y = A3(0, 4,3,2);
        y.finish();
        Indexed x = new Indexed(y, FTypeInt.ONLY);
        int[] indices = new int[3];

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    x.put(FInt.make(i + j * 100 + k * 10000), indices, 3);
                }
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.values.Indexed.get(FValue, int[], int)'
     */
    public void testGet() {
        IUOTuple y = A3(0, 4,3,2);
        y.finish();
        Indexed x = new Indexed(y, FTypeInt.ONLY);
        int[] indices = new int[3];

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    x.put(FInt.make(i + j * 100 + k * 10000), indices, 3);
                }

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    assertEquals(i + j * 100 + k * 10000,  ((FInt)x.get(indices, 3)).getInt());
                }
        System.out.println(x.getString());
    }

    /*
     * Test method for 'com.sun.fortress.interpreter.evaluator.values.Indexed.copyTo(Indexed, int[], int)'
     */
    public void testCopyToIndexedIntArrayInt() {
        IUOTuple y = A3(0, 4,3,2);
        y.finish();
        Indexed x = new Indexed(y, FTypeInt.ONLY);
        Indexed z = new Indexed(y, FTypeInt.ONLY);
        int[] indices = new int[3];

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    x.put(FInt.make(i + j * 100 + k * 10000), indices, 3);
                }

        indices[0] = 0;
        indices[1] = 0;
        indices[2] = 0;

        System.out.println("x="+x);

        x.copyTo(z, indices, 3);

        System.out.println("z="+z);

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    assertEquals(i + j * 100 + k * 10000,  ((FInt)z.get(indices, 3)).getInt());
                }
    }

    public void testCopyToIUOTuple() {
        IUOTuple y = A3(0,4,3,2);
        y.finish();
        Indexed x = new Indexed(y, FTypeInt.ONLY);
        Indexed z = new Indexed(y, FTypeInt.ONLY);
        int[] indices = new int[3];

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    x.put(FInt.make(i + j * 100 + k * 10000), indices, 3);
                }

        indices[0] = 0;
        indices[1] = 0;
        indices[2] = 0;

        System.out.println("x="+x);

        y.copyTo(z, indices, 3);

        System.out.println("z="+z);

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 2; k++) {
                    indices[0] = i; indices[1] = j; indices[2] = k;
                    assertEquals(i + j * 100 + k * 10000,  ((FInt)z.get(indices, 3)).getInt());
                }
    }


}
