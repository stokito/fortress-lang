/*******************************************************************************
 Copyright 2011, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

import com.sun.fortress.useful.Useful;

public class stringOps {

    public static int compareTo(String s, String t) {
        return s.compareTo(t);
    }
    
    public static String substring(String s, int start, int finish) {
        return Useful.substring(s, start, finish);
    }
    
    public static int charAt(String s, int at) {
        return s.charAt(at);
    }
    
    public static String asString(fortress.AnyType.Any a) { // this can't be right! DRC
        //        return "<" + a.getClass() + ">";
        return a.toString(); // I think this is better CHF
    }
}
