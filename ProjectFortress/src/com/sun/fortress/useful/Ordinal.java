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

public class Ordinal {

    final static String[] names1th = {
            "zeroeth",
            "first",
            "second",
            "third",
            "fourth",
            "fifth",
            "sixth",
            "seventh",
            "eighth",
            "ninth",
            "tenth",
            "eleventh",
            "twelfth",
            "thirteenth",
            "fourteenth",
            "fifteenth",
            "sixteenth",
            "seventeenth",
            "eightteenth",
            "nineteenth"
    };

    final static String[] names10th = {
            "zeroeth",
            "tenth",
            "twentieth",
            "thirtieth",
            "fortieth",
            "fiftieth",
            "sixtieth",
            "seventieth",
            "eightieth",
            "ninetieth",
   };

    final static String[] names1 = {
      "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve"
    };

    final static String[] names10 = {
        "zero",
        "ten",
        "twenty",
        "thirty",
        "forty",
        "fifty",
        "sixty",
        "seventy",
        "eighty",
        "ninety",
};

    static public String ordinal(int i) {
        if (i < 20)
            return names1th[i];
        if (i < 100) {
            int tens = i / 10;
            int ones = i % 10;
            if (ones == 0)
                return names10th[tens];
            return names10[tens] + ordinal(ones);
        }
        if (i < 1300) {
            int hundreds = i / 100;
            int rest = i % 100;
            if (rest == 0)
                return names1[hundreds] + " hundredth";
            return names1[hundreds] + " hundred and " + ordinal(rest);
        }
        return "many-eth";
    }
}
