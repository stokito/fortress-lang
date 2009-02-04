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

package com.sun.fortress.nodes_util;

public class SourceLocRats extends SourceLoc {

    final int col;

    final int line;

    final int offset;

    final String fileName;

    public SourceLocRats(String f, int l, int c, int offset) {
        this.fileName = f;
        this.line = l;
        this.col = c;
        this.offset = offset;
    }

    @Override
    public int column() {
        return col;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int getLine() {
        return line;
    }

    public int getOffset() {
        return offset;
    }

}
