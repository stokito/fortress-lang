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

package com.sun.fortress.repository.graph;

import java.io.File;
import java.io.IOException;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.repository.GraphRepository;
import com.sun.fortress.repository.ProjectProperties;

public abstract class GraphNode{

    long sourceDate;
    long cacheDate = Long.MIN_VALUE; /* Missing = very old */
    private final APIName name;
    private String canonicalSourceName;

    public GraphNode(APIName name, String canonicalSourceName, long sourceDate){
        this.name = name;
        this.sourceDate = sourceDate;
        this.canonicalSourceName = canonicalSourceName;
    }

    public GraphNode(APIName name, File source_file) throws IOException{
        this(name, source_file.getCanonicalPath(), source_file.lastModified());
    }
    
    public APIName getName(){
        return name;
    }

    public long getSourceDate() {
        return sourceDate;
    }
    
    public String getSourcePath() {
        return canonicalSourceName;
    }

    public abstract <T,F extends Throwable> T accept( GraphVisitor<T,F> g ) throws F;

    public abstract String key();

}
