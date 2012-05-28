/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
