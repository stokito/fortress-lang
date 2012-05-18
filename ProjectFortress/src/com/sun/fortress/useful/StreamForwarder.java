/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.useful;

import java.io.*;

public class StreamForwarder extends Thread {
    final BufferedReader i;

    final BufferedWriter o;

    final boolean closeOutput;

    final static String eol = System.getProperty("line.separator");

    public StreamForwarder(InputStream i, OutputStream o, boolean closeOutput) throws IOException {
        this.i = Useful.bufferedReader(i);
        this.o = Useful.bufferedWriter(o);
        this.closeOutput = closeOutput;
        this.start();
    }

    @Override
    public void run() {
        try {
            String s = i.readLine();
            while (s != null) {
                o.write(s);
                o.write(eol);
                o.flush();
                s = i.readLine();
            }
        }
        catch (IOException e) {

        }
        try {
            if (closeOutput) o.close();
        }
        catch (IOException e) {

        }
    }
}
