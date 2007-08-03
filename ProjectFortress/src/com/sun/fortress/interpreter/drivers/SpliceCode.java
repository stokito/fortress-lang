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

package com.sun.fortress.interpreter.drivers;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sun.fortress.useful.Useful;

public class SpliceCode {
    
    static StringBuffer readReader(BufferedReader br) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            String s = br.readLine();
            while (s != null) {
                sb.append(s);sb.append("\n");
                s = br.readLine();
            }
            return sb;
        }
        finally { br.close(); }
    }
    
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java SpliceCode Original.java < inserted > modified");
            return;
        }
        BufferedReader br = Useful.utf8BufferedFileReader(args[0]);
        StringBuffer sb = readReader(br);
        int i = sb.lastIndexOf("}");
        if (i == -1) {
            System.err.println("No closing brace in a Java file?");
        } else {
            int j = sb.lastIndexOf("\n", i);
            if (j == -1)
                j = i;
            else j = j + 1;
            StringBuffer splice = readReader(new BufferedReader(new InputStreamReader(System.in)));
            sb.insert(j, splice);
        }
        System.out.print(sb.toString());
    }
}
