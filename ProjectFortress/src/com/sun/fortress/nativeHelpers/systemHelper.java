/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nativeHelpers;

public class systemHelper {

    public static volatile String[] cmdline = null;

    public static void registerArgs(String[] args) {
        cmdline = args;
    }

}
