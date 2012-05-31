/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress excludes clause.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes_util.Span;

import java.util.Collections;
import java.util.List;

public class Excludes extends TraitClause {
    private List<BaseType> excludes = Collections.<BaseType>emptyList();

    public Excludes(Span span, List<BaseType> excludes) {
        super(span);
        this.excludes = excludes;
    }

    public List<BaseType> getExcludes() {
        return excludes;
    }

    public String message() {
        return "excludes";
    }

    public Span span() {
        return span;
    }
}
