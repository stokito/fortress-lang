/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
        if (systemHelper.cmdline.length >= n)
            return systemHelper.cmdline[n];
        else throw new RuntimeException("Can't get a command line arg because none was provided");
    }
}