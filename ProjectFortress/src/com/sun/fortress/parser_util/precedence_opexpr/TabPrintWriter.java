/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

/**
 * An extension of PrintWriter to support indenting levels.
 */
public class TabPrintWriter extends java.io.PrintWriter {
    private final int _tabSize;
    private final java.lang.String _tabString;
    private int _numIndents = 0;

    public TabPrintWriter(java.io.Writer writer, int tabSize) {
        super(writer);
        char[] c = new char[tabSize];
        for (int i = 0; i < tabSize; i++) {
            c[i] = ' ';
        }
        _tabString = new java.lang.String(c);
        _tabSize = tabSize;
    }

    /**
     * ups indent for any future new lines.
     */
    public void indent() {
        _numIndents++;
    }

    public void unindent() {
        _numIndents--;
    }

    public void startLine(Object s) {
        startLine();
        print(s);
    }

    public void startLine() {
        println();
        for (int i = 0; i < _numIndents; i++) {
            print(_tabString);
        }
    }
}
