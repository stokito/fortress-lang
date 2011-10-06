/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.index;

import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeAlias;

import java.util.List;

public class TypeAliasIndex extends TypeConsIndex {

    private final TypeAlias _ast;

    public TypeAliasIndex(TypeAlias ast) {
        _ast = ast;
    }

    public TypeAlias ast() {
        return _ast;
    }

    public List<StaticParam> staticParameters() {
        return _ast.getStaticParams();
    }

    public Type type() {
        return _ast.getTypeDef();
    }

}
