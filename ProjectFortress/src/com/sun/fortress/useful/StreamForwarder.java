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
