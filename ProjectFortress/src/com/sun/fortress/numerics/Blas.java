/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.numerics;

public class Blas {

    static {
        load();
    }

    public Blas() {
    }

    private static void load() {
        try {
            System.out.println("Loading blas");
            System.loadLibrary("blas");
            System.out.println("Loaded blas");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public enum Order {
        RowMajor(101),
        ColumnMajor(102);

        public final int value;

        private Order(int i) {
            this.value = i;
        }
    }

    public enum Transpose {
        NoTranspose(111), Transpose(112), ConjugateTranspose(113), Conjugate(114);
        public final int value;

        private Transpose(int i) {
            this.value = i;
        }
    }

    public enum Triangularity {
        Upper(121), Lower(122);
        public int value;

        private Triangularity(int i) {
            this.value = i;
        }
    }

    public enum Diagonal {
        NonUnit(131), Unit(132);
        public final int value;

        private Diagonal(int i) {
            this.value = i;
        }
    }

    public enum Side {
        Left(141), Right(142);
        public final int value;

        private Side(int i) {
            this.value = i;
        }
    }

    public static native double dotProduct(int length,
                                           double[] x,
                                           int xStride,
                                           int xOffset,
                                           double[] y,
                                           int yStride,
                                           int yOffset);

    public static native double norm(int length, double[] x, int xStride, int xOffset);

    public static native void internal_add(int length,
                                           double alpha,
                                           double[] x,
                                           int xStride,
                                           int xOffset,
                                           double[] y,
                                           int yStride,
                                           int yOffset);

    public static native void internal_dcopy(int length,
                                             double[] x,
                                             int xStride,
                                             int xOffset,
                                             double[] result,
                                             int resultStride,
                                             int resultOffset);

    public static native void internal_scale(int length, double alpha, double[] x, int stride, int offset);

    public static native void internal_vectorMatrixMultiply(int order,
                                                            int transpose,
                                                            int m,
                                                            int n,
                                                            double alpha,
                                                            double[] matrix,
                                                            int matrixStride,
                                                            int matrixOffset,
                                                            double[] vector,
                                                            int stride,
                                                            int offset,
                                                            double beta,
                                                            double[] result,
                                                            int resultStride,
                                                            int resultOffset);

    public static native void internal_matrixMatrixMultiply(int order,
                                                            int transpose1,
                                                            int transpose2,
                                                            int m,
                                                            int n,
                                                            int k,
                                                            double alpha,
                                                            double[] matrix1,
                                                            int matrix1Stride,
                                                            int matrix1Offset,
                                                            double[] matrix2,
                                                            int matrix2Stride,
                                                            int matrix2Offset,
                                                            double beta,
                                                            double[] result,
                                                            int resultStride,
                                                            int resultOffset);

    public static double[] scale(int length, double alpha, double[] x, int stride, int offset) {
        double[] result = new double[length];
        internal_dcopy(length, x, stride, offset, result, 1, 0);
        internal_scale(length, alpha, result, 1, 0);
        return result;
    }

    public static double[] add(int length,
                               double alpha,
                               double[] x,
                               int xStride,
                               int xOffset,
                               double[] y,
                               int yStride,
                               int yOffset) {
        double[] result = new double[length];
        internal_dcopy(length, y, yStride, yOffset, result, 1, 0);
        internal_add(length, alpha, x, xStride, xOffset, result, 1, 0);
        return result;
    }

    public static double[] vectorMatrixMultiply(Order order,
                                                Transpose transpose,
                                                int m,
                                                int n,
                                                double alpha,
                                                double[] matrix,
                                                int matrixStride,
                                                int matrixOffset,
                                                double[] vector,
                                                int stride,
                                                int offset) {
        double[] result = new double[m];
        internal_vectorMatrixMultiply(order.value,
                                      transpose.value,
                                      m,
                                      n,
                                      alpha,
                                      matrix,
                                      matrixStride,
                                      matrixOffset,
                                      vector,
                                      stride,
                                      offset,
                                      1.0,
                                      result,
                                      1,
                                      0);
        return result;
    }

    public static double[] matrixMatrixMultiply(Order order,
                                                Transpose transpose1,
                                                Transpose transpose2,
                                                int m,
                                                int n,
                                                int k,
                                                double alpha,
                                                double[] matrix1,
                                                int matrix1Stride,
                                                int matrix1Offset,
                                                double[] matrix2,
                                                int matrix2Stride,
                                                int matrix2Offset) {
        double[] result = new double[m * n];
        internal_matrixMatrixMultiply(order.value,
                                      transpose1.value,
                                      transpose2.value,
                                      m,
                                      n,
                                      k,
                                      alpha,
                                      matrix1,
                                      matrix1Stride,
                                      matrix1Offset,
                                      matrix2,
                                      matrix2Stride,
                                      matrix2Offset,
                                      1.0,
                                      result,
                                      n,
                                      0);

        return result;
    }

}
