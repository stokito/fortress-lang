/*
 * PanicException.java
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A.  All rights reserved.  
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document.  In particular, and without limitation, these
 * intellectual property rights may include one or more of the
 * U.S. patents listed at http://www.sun.com/patents and one or more
 * additional patents or pending patent applications in the U.S. and
 * in other countries.
 * 
 * U.S. Government Rights - Commercial software.
 * Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its
 * supplements.  Use is subject to license terms.  Sun, Sun
 * Microsystems, the Sun logo and Java are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.  
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime
 * end uses or end users, whether direct or indirect, are strictly
 * prohibited.  Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion
 * lists, including, but not limited to, the denied persons and
 * specially designated nationals lists is strictly prohibited.
 */

package dstm2.exceptions;

/**
 * Thrown to indicate an error in the use of the transactional memory;
 * that is, a violation of the assumptions of use.
 */
public class PanicException extends java.lang.RuntimeException {

    /**
     * Creates new <code>PanicException</code> with no detail message.
     */
    public PanicException() {
    }

    public PanicException(String format, Object ... args) {
      super(String.format(format, args));
    }

    /**
     * Creates a new <code>PanicException</code> with the specified detail message.
     * 
     * @param msg the detail message.
     */
    public PanicException(String msg) {
        super(msg);
    }

    /**
     * Creates an <code>PanicException</code> with the specified cause.
     * 
     * @param cause Throwable that caused PanicException to be thrown
     */
    public PanicException(Throwable cause) {
        super(cause);
    }
}

