/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.numerics;

class DirectedRoundingTest {

    public static void main(String[] args) {
        int k;
        float f1, f2, fdown, fup;
        double d1, d2, ddown, dup;
        for (k = 0; k < DirectedRoundingTestData.float_add_data.length; k++) {
            f1 = DirectedRoundingTestData.float_add_data[k][0];
            f2 = DirectedRoundingTestData.float_add_data[k][1];
            fdown = DirectedRoundingTestData.float_add_data[k][2];
            fup = DirectedRoundingTestData.float_add_data[k][3];
            checkEqual(DirectedRounding.addDown(f1, f2), fdown, "addDown", f1, f2);
            checkEqual(DirectedRounding.addDown(f2, f1), fdown, "addDown", f2, f1);
            checkEqual(DirectedRounding.addUp(f1, f2), fup, "addUp", f1, f2);
            checkEqual(DirectedRounding.addUp(f2, f1), fup, "addUp", f2, f1);
            checkEqual(DirectedRounding.subtractDown(f1, -f2), fdown, "subtractDown", f1, f2);
            checkEqual(DirectedRounding.subtractDown(f2, -f1), fdown, "subtractDown", f2, f1);
            checkEqual(DirectedRounding.subtractUp(f1, -f2), fup, "subtractUp", f1, f2);
            checkEqual(DirectedRounding.subtractUp(f2, -f1), fup, "subtractUp", f2, f1);
        }
        for (k = 0; k < DirectedRoundingTestData.float_multiply_data.length; k++) {
            f1 = DirectedRoundingTestData.float_multiply_data[k][0];
            f2 = DirectedRoundingTestData.float_multiply_data[k][1];
            fdown = DirectedRoundingTestData.float_multiply_data[k][2];
            fup = DirectedRoundingTestData.float_multiply_data[k][3];
            checkEqual(DirectedRounding.multiplyDown(f1, f2), fdown, "multiplyDown", f1, f2);
            checkEqual(DirectedRounding.multiplyDown(f2, f1), fdown, "multiplyDown", f2, f1);
            checkEqual(DirectedRounding.multiplyUp(f1, f2), fup, "multiplyUp", f1, f2);
            checkEqual(DirectedRounding.multiplyUp(f2, f1), fup, "multiplyUp", f2, f1);
        }
        for (k = 0; k < DirectedRoundingTestData.float_divide_data.length; k++) {
            f1 = DirectedRoundingTestData.float_divide_data[k][0];
            f2 = DirectedRoundingTestData.float_divide_data[k][1];
            fdown = DirectedRoundingTestData.float_divide_data[k][2];
            fup = DirectedRoundingTestData.float_divide_data[k][3];
            checkEqual(DirectedRounding.divideDown(f1, f2), fdown, "divideDown", f1, f2);
            checkEqual(DirectedRounding.divideUp(f1, f2), fup, "divideUp", f1, f2);
        }
        for (k = 0; k < DirectedRoundingTestData.float_sqrt_data.length; k++) {
            f1 = DirectedRoundingTestData.float_sqrt_data[k][0];
            fdown = DirectedRoundingTestData.float_sqrt_data[k][1];
            fup = DirectedRoundingTestData.float_sqrt_data[k][2];
            checkEqual(DirectedRounding.sqrtDown(f1), fdown, "sqrtDown", f1);
            checkEqual(DirectedRounding.sqrtUp(f1), fup, "sqrtUp", f1);
        }
        for (k = 0; k < DirectedRoundingTestData.double_add_data.length; k++) {
            d1 = DirectedRoundingTestData.double_add_data[k][0];
            d2 = DirectedRoundingTestData.double_add_data[k][1];
            ddown = DirectedRoundingTestData.double_add_data[k][2];
            dup = DirectedRoundingTestData.double_add_data[k][3];
            checkEqual(DirectedRounding.addDown(d1, d2), ddown, "addDown", d1, d2);
            checkEqual(DirectedRounding.addDown(d2, d1), ddown, "addDown", d2, d1);
            checkEqual(DirectedRounding.addUp(d1, d2), dup, "addUp", d1, d2);
            checkEqual(DirectedRounding.addUp(d2, d1), dup, "addUp", d2, d1);
            checkEqual(DirectedRounding.subtractDown(d1, -d2), ddown, "subtractDown", d1, d2);
            checkEqual(DirectedRounding.subtractDown(d2, -d1), ddown, "subtractDown", d2, d1);
            checkEqual(DirectedRounding.subtractUp(d1, -d2), dup, "subtractUp", d1, d2);
            checkEqual(DirectedRounding.subtractUp(d2, -d1), dup, "subtractUp", d2, d1);
        }
        for (k = 0; k < DirectedRoundingTestData.double_multiply_data.length; k++) {
            d1 = DirectedRoundingTestData.double_multiply_data[k][0];
            d2 = DirectedRoundingTestData.double_multiply_data[k][1];
            ddown = DirectedRoundingTestData.double_multiply_data[k][2];
            dup = DirectedRoundingTestData.double_multiply_data[k][3];
            checkEqual(DirectedRounding.multiplyDown(d1, d2), ddown, "multiplyDown", d1, d2);
            checkEqual(DirectedRounding.multiplyDown(d2, d1), ddown, "multiplyDown", d2, d1);
            checkEqual(DirectedRounding.multiplyUp(d1, d2), dup, "multiplyUp", d1, d2);
            checkEqual(DirectedRounding.multiplyUp(d2, d1), dup, "multiplyUp", d2, d1);
        }
        for (k = 0; k < DirectedRoundingTestData.double_divide_data.length; k++) {
            d1 = DirectedRoundingTestData.double_divide_data[k][0];
            d2 = DirectedRoundingTestData.double_divide_data[k][1];
            ddown = DirectedRoundingTestData.double_divide_data[k][2];
            dup = DirectedRoundingTestData.double_divide_data[k][3];
            checkEqual(DirectedRounding.divideDown(d1, d2), ddown, "divideDown", d1, d2);
            checkEqual(DirectedRounding.divideUp(d1, d2), dup, "divideUp", d1, d2);
        }
        for (k = 0; k < DirectedRoundingTestData.double_sqrt_data.length; k++) {
            d1 = DirectedRoundingTestData.double_sqrt_data[k][0];
            ddown = DirectedRoundingTestData.double_sqrt_data[k][1];
            dup = DirectedRoundingTestData.double_sqrt_data[k][2];
            checkEqual(DirectedRounding.sqrtDown(d1), ddown, "sqrtDown", d1);
            checkEqual(DirectedRounding.sqrtUp(d1), dup, "sqrtUp", d1);
        }
    }

    private static void checkEqual(float actual, float desired, String op, float op1, float op2) {
        if (Float.isNaN(actual) ? !Float.isNaN(desired) : (actual != desired)) squawk(actual, desired, op, op1, op2);
    }

    private static void checkEqual(double actual, double desired, String op, double op1, double op2) {
        if (Double.isNaN(actual) ? !Double.isNaN(desired) : (actual != desired)) squawk(actual, desired, op, op1, op2);
        // else System.out.println("Float operation " + op + " on " + op1 + " and " + op2 + " OK!");
    }

    private static void checkEqual(float actual, float desired, String op, float op1) {
        if (Float.isNaN(actual) ? !Float.isNaN(desired) : (actual != desired)) squawk(actual, desired, op, op1);
    }

    private static void checkEqual(double actual, double desired, String op, double op1) {
        if (Double.isNaN(actual) ? !Double.isNaN(desired) : (actual != desired)) squawk(actual, desired, op, op1);
    }

    private static void squawk(float actual, float desired, String op, float op1, float op2) {
        System.out.println(
                "Float operation " + op + " on " + op1 + " and " + op2 + " produced " + actual + " instead of " +
                desired);
    }

    private static void squawk(double actual, double desired, String op, double op1, double op2) {
        System.out.println(
                "Double operation " + op + " on " + op1 + " and " + op2 + " produced " + actual + " instead of " +
                desired);
    }

    private static void squawk(float actual, float desired, String op, float op1) {
        System.out.println("Float operation " + op + " on " + op1 + " produced " + actual + " instead of " + desired);
    }

    private static void squawk(double actual, double desired, String op, double op1) {
        System.out.println("Double operation " + op + " on " + op1 + " produced " + actual + " instead of " + desired);
    }

}
