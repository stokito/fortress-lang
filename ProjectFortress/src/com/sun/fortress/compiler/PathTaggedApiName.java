/*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.compiler;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.useful.MagicNumbers;

public final class PathTaggedApiName implements Comparable<PathTaggedApiName> {
    final String source_path;
    final APIName name;
    public PathTaggedApiName(String source_path, APIName name) {
        this.name = name;
        this.source_path = source_path.intern();
    }
    public int hashCode() {
        return source_path.hashCode() + MagicNumbers.a * name.hashCode();
    }
    public boolean equals(Object o) {
        if (o instanceof PathTaggedApiName) {
            PathTaggedApiName p = (PathTaggedApiName) o;
            return name.equals(p.name) && source_path.equals(p.source_path);
        }
        return false;
    }
    @Override
    public int compareTo(PathTaggedApiName p) {
        int x = 0;
        if (source_path != p.source_path) 
            x = source_path.compareTo(p.source_path);
        if (x != 0)
            return x;
        return name.getText().compareTo(p.name.getText());
    }
    
}
