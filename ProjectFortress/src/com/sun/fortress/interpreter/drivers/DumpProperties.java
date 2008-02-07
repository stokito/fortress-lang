/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.drivers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

class DumpProperties {
  static public void main(String[] args) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) System.out.println("null ClassLoader");
    else System.out.println("ClassLoader " + cl.toString());

    System.out.println("Fortress home is " + ProjectProperties.FORTRESS_HOME);
    System.out.println("Fortress base is " + ProjectProperties.BASEDIR);

    java.util.Properties p = System.getProperties();
    Enumeration e = p.propertyNames();
    ArrayList<String> al = new ArrayList<String>();
    while(e.hasMoreElements()) {
      String key = (String)e.nextElement();
      al.add(key);
    }

    String[] sl = new String[al.size()];
    al.toArray(sl);

    Arrays.sort(sl, String.CASE_INSENSITIVE_ORDER);

    for (int i=0; i<sl.length; i++) {
      String key = sl[i];
      System.out.println(key + "=" + p.getProperty(key, "<NOVALUE>"));
    }
    for (int i = 0; i < args.length; i++) {
      System.out.println("Args[" + i + "] = " + args[i]);
    }
  }
}
