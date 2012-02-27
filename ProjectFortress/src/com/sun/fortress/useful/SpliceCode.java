/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.useful;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SpliceCode {

    static StringBuilder readReader(BufferedReader br) throws IOException {
        try {
            StringBuilder sb = new StringBuilder();
            String s = br.readLine();
            while (s != null) {
                sb.append(s);
                sb.append("\n");
                s = br.readLine();
            }
            return sb;
        }
        finally {
            br.close();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java SpliceCode Original.java < inserted > modified");
            return;
        }
        BufferedReader br = Useful.utf8BufferedFileReader(args[0]);
        StringBuilder sb = readReader(br);
        int i = sb.lastIndexOf("}");
        if (i == -1) {
            System.err.println("No closing brace in a Java file?");
        } else {
            int j = sb.lastIndexOf("\n", i);
            if (j == -1) j = i;
            else j = j + 1;
            StringBuilder splice = readReader(new BufferedReader(new InputStreamReader(System.in)));
            sb.insert(j, splice);
        }
        System.out.print(sb.toString());
    }
}
