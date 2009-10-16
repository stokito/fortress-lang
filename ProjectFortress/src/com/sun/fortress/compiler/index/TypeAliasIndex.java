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

    @Override
    public TypeConsIndex acceptNodeUpdateVisitor(NodeUpdateVisitor visitor) {
	return new TypeAliasIndex((TypeAlias) _ast.accept(visitor));
    }
}
