/*******************************************************************************
 Copyright 2009,2010, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class simpleSystem {

    public static int nativeArgcount() {
        if (systemHelper.cmdline != null) {
            return systemHelper.cmdline.length;
        } else {
            return 0;
        }
    }

    public static String nativeArg(int n) {
        if (n >= 0 && n < nativeArgcount()) return systemHelper.cmdline[n];
        else throw new RuntimeException("Can't get command line arg " + n + " because it wasn't provided");
    }
    
 }
