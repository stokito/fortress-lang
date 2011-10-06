/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.DimDecl;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticParam;

import java.util.ArrayList;
import java.util.List;

public class Dimension extends TypeConsIndex {
    private final DimDecl ast;

    public Dimension(DimDecl _ast) {
        ast = _ast;
    }

    public DimDecl ast() {
        return ast;
    }

    @Override
    public List<StaticParam> staticParameters() {
        return new ArrayList<StaticParam>();
    }
}
