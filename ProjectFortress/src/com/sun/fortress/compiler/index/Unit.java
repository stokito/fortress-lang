/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.UnitDecl;

import java.util.ArrayList;
import java.util.List;

public class Unit extends TypeConsIndex {
    private final UnitDecl ast;

    public Unit(UnitDecl _ast) {
        ast = _ast;
    }

    public UnitDecl ast() {
        return ast;
    }

    public List<StaticParam> staticParameters() {
        return new ArrayList<StaticParam>();
    }
    
}
