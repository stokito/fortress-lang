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

package com.sun.fortress.interpreter.useful;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class WireTappedPrintStream extends PrintStream {

    PrintStream tappee;
    int limit;
    ByteArrayOutputStream s;
    boolean postponePassthrough;
    int postponeStart;

    public static final int DEFAULT_BYTE_LIMIT = 65536;

    public static WireTappedPrintStream make(PrintStream tappee) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        return new WireTappedPrintStream(bos, tappee, false);
    }

    public static WireTappedPrintStream make(PrintStream tappee, boolean postponePassthrough) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        return new WireTappedPrintStream(bos, tappee, postponePassthrough);
    }

    private WireTappedPrintStream(ByteArrayOutputStream s, PrintStream tappee, boolean postponePassthrough) {
        super(s, true);
        this.s = s;
        this.tappee = tappee;
        this.postponePassthrough = postponePassthrough;
    }

    public String getString() {
        flush();
        return s.toString();
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#close()
     */
    @Override
    public void close() {
        super.close();
        tappee.close();
    }

    public void flush(boolean releasePostponed) throws IOException {
        super.flush();
        if (releasePostponed && postponePassthrough) {
            byte[] released = s.toByteArray();
            tappee.write(released, postponeStart, released.length - postponeStart);
            postponeStart = released.length;
        }
        tappee.flush();
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#flush()
     */
    @Override
    public void flush() {
        super.flush();
        tappee.flush();
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] arg0, int arg1, int arg2) {
        super.write(arg0, arg1, arg2);
        if (!postponePassthrough)
            tappee.write(arg0, arg1, arg2);
    }

    /* (non-Javadoc)
     * @see java.io.PrintStream#write(int)
     */
    @Override
    public void write(int arg0) {
        super.write(arg0);
        if (!postponePassthrough)
            tappee.write(arg0);
    }

    /* (non-Javadoc)
     * @see java.io.FilterOutputStream#write(byte[])
     */
    @Override
    public void write(byte[] arg0) throws IOException {
        super.write(arg0);
        if (!postponePassthrough)
            tappee.write(arg0);
    }


}
