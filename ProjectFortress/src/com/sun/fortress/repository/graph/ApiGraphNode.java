/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.repository.graph;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.nodes.APIName;
import edu.rice.cs.plt.tuple.Option;

import java.io.File;
import java.io.IOException;

public class ApiGraphNode extends GraphNode {
    private Option<ApiIndex> api;
    private final String k;

    public ApiGraphNode(APIName name, String canonicalSourceName, long sourceDate) {
        super(name, canonicalSourceName, sourceDate);
        k = key(name);
        this.api = Option.none();
    }

    public ApiGraphNode(APIName name, File source_file) throws IOException {
        super(name, source_file);
        k = key(name);
        this.api = Option.none();
    }

    public int hashCode() {
        return super.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof ApiGraphNode) {
            ApiGraphNode a = (ApiGraphNode) o;
            return a.getName().equals(getName());
        }
        return false;
    }

    public Option<ApiIndex> getApi() {
        return this.api;
    }

    public void setApi(ApiIndex api, long cacheDate) {
        this.api = Option.wrap(api);
        //this.cacheDate = cacheDate;
    }

    public String toString() {
        return "Api " + getName().toString();
    }

    public <T, F extends Throwable> T accept(GraphVisitor<T, F> g) throws F {
        return g.visit(this);
    }

    public String key() {
        return k;
    }

    public static String key(APIName k) {
        return "api " + k.getText();
    }

}
