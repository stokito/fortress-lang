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

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;

public class SourceLocRats extends SourceLoc {

    int col;

    int line;

    String fileName;

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

    @Override
    void setColumn(int column) {
        this.col = column;

    }

    @Override
    public void setFileName(String s) {
        this.fileName = s;
    }

    @Override
    void setLine(int x) {
        this.line = x;

    }

    public SourceLocRats() {
        fileName = "";
    }

    public SourceLocRats(SourceLoc x) {
        this(x.getFileName(), x.getLine(), x.column());
    }

    public SourceLocRats(String f, int l, int c) {
        this.fileName = f;
        this.line = l;
        this.col = c;
    }

}
