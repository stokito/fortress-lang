/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.useful;

import java.io.*;
//import org.apache.tools.ant.*;
//import org.apache.tools.ant.taskdefs.*;
//import org.apache.tools.ant.types.selectors.*;

/*
 * This convenience class provides a simple API for common file actions.
 */
public class Files {
    public static void rm(String name) throws IOException {
        if (! new File(name).delete())
            throw new IOException();
    }

    public static void mkdir(String name) throws IOException {
        if (! new File(name).mkdir())
            throw new IOException();
    }

    public static void mv(String src, String dest) throws IOException {
        if (! new File(src).renameTo(new File(dest)))
            throw new IOException();
    }

    public static File[] ls(String name) {
        return new File(name).listFiles();
    }

    public static void cp(String src, String dest) throws FileNotFoundException, IOException {
        FileInputStream input = null;
        FileOutputStream output = null;
        try {
            input = new FileInputStream(new File(src));
            output = new FileOutputStream(new File(dest));

            for (int next = input.read(); next != -1; next = input.read()) {
                output.write(next);
            }
        } finally {
            try {
                if (input != null) input.close();
            } finally {
                if (output != null) output.close();
            }
        }
    }

    /* Convenience method for creating a BufferedReader from a file name. */
    public static BufferedReader reader(String fileName) throws IOException {
        return new BufferedReader(new FileReader(fileName));
    }

    /* Convenience method for creating a BufferedReader from a file name. */
    public static BufferedWriter writer(String fileName) throws IOException {
        return new BufferedWriter(new FileWriter(fileName));
    }
}
