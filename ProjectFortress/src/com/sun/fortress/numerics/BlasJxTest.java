/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.numerics;

import junit.framework.TestCase;

public class BlasJxTest extends TestCase {

    public BlasJxTest() {
        new Blas();
    }

    public void testDot1() {
        double[] x = new double[]{1.0};
        double[] y = new double[]{1.0};

        assertEquals(1.0, Blas.dotProduct(x.length, x, 1, 0, y, 1, 0));
    }

    public void testDot2() {
        double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        double[] y = new double[]{3.0, 3.0, 3.0, 3.0, 3.0};

        assertEquals(45.0, Blas.dotProduct(x.length, x, 1, 0, y, 1, 0));
    }

    public void testDot3() {
        double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        assertEquals(55.0, Blas.dotProduct(x.length, x, 1, 0, x, 1, 0));
    }

    public void testNorm() {
        double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        double norm = Blas.norm(x.length, x, 1, 0);
        assertEquals(55.0, norm * norm);
    }

    public void testAdd() {
        double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        double[] y = new double[]{3.0, 3.0, 3.0, 3.0, 3.0};
        double[] result = Blas.add(x.length, 1.0, x, 1, 0, y, 1, 0);
        assertEquals(4.0, result[0]);
        assertEquals(5.0, result[1]);
        assertEquals(6.0, result[2]);
        assertEquals(7.0, result[3]);
        assertEquals(8.0, result[4]);
    }

    public void testScale() {
        double[] x = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        double[] result = Blas.scale(x.length, 2, x, 1, 0);
        assertEquals(110.0, Blas.dotProduct(x.length, x, 1, 0, result, 1, 0));
    }

    public void testVectorMatrixMultiply1() {
        double[] matrix = new double[]{
                2.0, 0.0, 0.0, 7.0, 0.0, 3.0, 0.0, 11.0, 0.0, 0.0, 5.0, 13.0,
        };
        double[] vector = new double[]{2.0, 2.0, 2.0, 2.0};

        double[] result = Blas.vectorMatrixMultiply(Blas.Order.RowMajor,
                                                    Blas.Transpose.NoTranspose,
                                                    3,
                                                    4,
                                                    1.0,
                                                    matrix,
                                                    4,
                                                    0,
                                                    vector,
                                                    1,
                                                    0);

        double[] expected = new double[]{18.0, 28.0, 36.0};
        for (int i = 0; i < result.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    public void testMatrixMatrixMultiply1() {
        double[] m0 = new double[]{
                2.0, 0.0, 0.0, 7.0, 0.0, 3.0, 0.0, 11.0, 0.0, 0.0, 5.0, 13.0,
        };

        double[] m1 = new double[]{
                4, 0, 0, 14, 0, 9, 0, 33, 0, 0, 25, 65, 14, 33, 65, 339,
        };
        double[] result = Blas.matrixMatrixMultiply(Blas.Order.RowMajor,
                                                    Blas.Transpose.Transpose,
                                                    Blas.Transpose.NoTranspose,
                                                    4,
                                                    4,
                                                    3,
                                                    1.0,
                                                    m0,
                                                    4,
                                                    0,
                                                    m0,
                                                    4,
                                                    0);

        for (int i = 0; i < result.length; i++) {
            assertEquals(m1[i], result[i]);
        }
    }
}
