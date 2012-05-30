/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

class DumpProperties {
    static public void main(String[] args) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) System.out.println("null ClassLoader");
        else System.out.println("ClassLoader " + cl.toString());

        System.out.println("Fortress autohome is " + ProjectProperties.FORTRESS_AUTOHOME);
        System.out.println("Fortress base is " + ProjectProperties.BASEDIR);

        java.util.Properties p = System.getProperties();
        Enumeration e = p.propertyNames();
        ArrayList<String> al = new ArrayList<String>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            al.add(key);
        }

        String[] sl = new String[al.size()];
        al.toArray(sl);

        Arrays.sort(sl, String.CASE_INSENSITIVE_ORDER);

        for (int i = 0; i < sl.length; i++) {
            String key = sl[i];
            System.out.println(key + "=" + p.getProperty(key, "<NOVALUE>"));
        }
        for (int i = 0; i < args.length; i++) {
            System.out.println("Args[" + i + "] = " + args[i]);
        }
    }
}
