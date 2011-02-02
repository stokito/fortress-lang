/*******************************************************************************
 Copyright 2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

public class SysDescription {

    public static String getSysDescription() {
        String sep = "|";
        Runtime rt = Runtime.getRuntime();
        int cpus = rt.availableProcessors();
        String os = System.getProperty("os.name");
        os = os.replace(" ", "");
        return os + sep + System.getProperty("os.version") + sep + System.getProperty("os.arch") + sep + cpus + "-cpu";
    }
}
