/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.repository.graph;

import com.sun.fortress.nodes.APIName;

import java.io.File;
import java.io.IOException;

public abstract class GraphNode {

    long cacheDate = Long.MIN_VALUE; /* Missing = very old */
    long sourceDate;
    private final APIName name;
    private String canonicalSourceName;

    public GraphNode(APIName name, String canonicalSourceName, long sourceDate) {
        this.name = name;
        this.sourceDate = sourceDate;
        this.canonicalSourceName = canonicalSourceName;
    }

    public GraphNode(APIName name, File source_file) throws IOException {
        this(name, source_file.getCanonicalPath(), source_file.lastModified());
    }

    public long getCacheDate() {
        return cacheDate;
    }

    public APIName getName() {
        return name;
    }

    public long getSourceDate() {
        return sourceDate;
    }

    public String getSourcePath() {
        return canonicalSourceName;
    }

    public abstract <T, F extends Throwable> T accept(GraphVisitor<T, F> g) throws F;

    public abstract String key();

}
