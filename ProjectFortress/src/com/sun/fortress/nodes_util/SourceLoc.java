/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.io.Serializable;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.MagicNumbers;

// /
// / type 'a node = 'a Node.node
// /
abstract public class SourceLoc implements Serializable, HasAt {
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

    public final String at() {
        return getFileName() + ":" + getLine() + "." + column();
    }

    public final String stringName() {
        return "";
    }

    /**
     * @return Returns the fileName.
     */
    abstract public String getFileName();

    /**
     * @return Returns the line.
     */
    abstract public int getLine();

    /**
     * @return Returns the offset.
     */
    public abstract int getOffset();
}
