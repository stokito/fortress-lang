/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.nodes.*;
import com.sun.fortress.syntax_abstractions.environments.Depth;
import com.sun.fortress.syntax_abstractions.environments.Depth.BaseDepth;
import com.sun.fortress.useful.Debug;

import java.util.Map;

public class VariableCollector extends NodeDepthFirstVisitor_void {

    private Map<Id, Depth> depthMap;
    private Depth depth;

    public VariableCollector(Map<Id, Depth> depthMap) {
        this(depthMap, new BaseDepth());
    }

    private VariableCollector(Map<Id, Depth> depthMap, Depth depth) {
        this.depthMap = depthMap;
        this.depth = depth;
    }

    @Override
    public void defaultCase(com.sun.fortress.nodes.Node that) {
        return;
    }

    @Override
    public void forPrefixedSymbol(PrefixedSymbol that) {
        this.depthMap.put(that.getId(), this.depth);
        super.forPrefixedSymbol(that);
    }

    @Override
    public void forNotPredicateSymbol(NotPredicateSymbol that) {
        return; // FIXME: ???
    }

    @Override
    public void forAndPredicateSymbol(AndPredicateSymbol that) {
        return; // FIXME: ???
    }

    @Override
    public void forRepeatSymbol(RepeatSymbol that) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Repeat symbol ", that.getSymbol());
        that.getSymbol().accept(new VariableCollector(this.depthMap, depth.addStar()));
    }

    @Override
    public void forRepeatOneOrMoreSymbol(RepeatOneOrMoreSymbol that) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Repeat One or more symbol ", that.getSymbol());
        that.getSymbol().accept(new VariableCollector(this.depthMap, depth.addPlus()));
    }

    @Override
    public void forOptionalSymbol(OptionalSymbol that) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Optional ", that.getSymbol());
        that.getSymbol().accept(new VariableCollector(this.depthMap, depth.addOptional()));
    }
}
