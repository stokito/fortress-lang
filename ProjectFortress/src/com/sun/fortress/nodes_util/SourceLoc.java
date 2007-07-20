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

import com.sun.fortress.useful.MagicNumbers;

// /
// / type 'a node = 'a Node.node
// /
abstract public class SourceLoc {
    public abstract int column();

    @Override
    public final int hashCode() {
        return getLine() * MagicNumbers.S + column() * MagicNumbers.o
                + getFileName().hashCode() + MagicNumbers.u;
    }

    @Override
    public final boolean equals(Object o) {
        if (o instanceof SourceLoc) {
            SourceLoc sl = (SourceLoc) o;
            return getLine() == sl.getLine() && column() == sl.column()
                    && getFileName().equals(sl.getFileName());
        }
        return false;
    }

    abstract void setColumn(int column);

    public final String at() {
        return getFileName() + ":" + getLine() + "." + column();
    }

    /**
     * @return Returns the fileName.
     */
    abstract public String getFileName();

    abstract public void setFileName(String s);

    /**
     * @return Returns the line.
     */
    abstract public int getLine();

    /**
     * @return Sets the line.
     */
    abstract void setLine(int x);

}
