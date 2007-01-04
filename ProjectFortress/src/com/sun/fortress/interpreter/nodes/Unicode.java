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

/*
 * Created on Nov 1, 2005
 *
 */
package com.sun.fortress.interpreter.nodes;

import java.util.HashMap;

public class Unicode {
    public static boolean charactersOverlap(String s1, String s2) {
        for (int i = 0; i < s1.length(); i++) {
            if (s2.indexOf(s1.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String byNameLC(String s) {
        return unicodeByName.get(s.toLowerCase());
    }

    public static int numberToValue(String s) {
        Integer I = numbers.get(s.toUpperCase());
        if (I == null) {
            return -1;
        }
        return I.intValue();
    }

    private static HashMap<String, String> unicodeByName = new HashMap<String, String>();

    private static HashMap<String, Integer> numbers = new HashMap<String, Integer>();
    static {
        HashMap<String, Integer> m = numbers;
        m.put("ZERO", new Integer(0));
        m.put("ONE", new Integer(1));
        m.put("TWO", new Integer(2));
        m.put("THREE", new Integer(3));
        m.put("FOUR", new Integer(4));
        m.put("FIVE", new Integer(5));
        m.put("SIX", new Integer(6));
        m.put("SEVEN", new Integer(7));
        m.put("EIGHT", new Integer(8));
        m.put("NINE", new Integer(9));
        m.put("TEN", new Integer(10));
        m.put("ELEVEN", new Integer(11));
        m.put("TWELVE", new Integer(12));
        m.put("THIRTEEN", new Integer(13));
        m.put("FOURTEEN", new Integer(14));
        m.put("FIFTEEN", new Integer(15));
        m.put("SIXTEEN", new Integer(16));
    }
    static {
        HashMap<String, String> m = unicodeByName;
        m.put("alpha", "\u03b1");
        m.put("beta", "\u03b2");
        m.put("gamma", "\u03b3");
        m.put("delta", "\u03b4");
        m.put("epsilon", "\u03b5");
        m.put("zeta", "\u03b6");
        m.put("eta", "\u03b7");
        m.put("theta", "\u03b8");
        m.put("iota", "\u03b9");
        m.put("kappa", "\u03ba");
        m.put("lambda", "\u03bb");
        m.put("lamda", "\u03bb");
        m.put("mu", "\u03bc");
        m.put("nu", "\u03bd");
        m.put("xi", "\u03be");
        m.put("omicron", "\u03bf");
        m.put("pi", "\u03c0");
        m.put("rho", "\u03c1");
        m.put("final sigma", "\u03c2");
        m.put("sigma", "\u03c3");
        m.put("tau", "\u03c4");
        m.put("upsilon", "\u03c5");
        m.put("phi", "\u03c6");
        m.put("chi", "\u03c7");
        m.put("psi", "\u03c8");
        m.put("omega", "\u03c9");
    }

}
