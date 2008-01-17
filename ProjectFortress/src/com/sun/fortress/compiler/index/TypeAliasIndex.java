/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import java.util.Map;
import java.util.List;

import com.sun.fortress.nodes.TypeAlias;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.Type;

import com.sun.fortress.useful.NI;

public class TypeAliasIndex extends TypeConsIndex {
    
    private final TypeAlias _ast;
    
    public TypeAliasIndex(TypeAlias ast) {
        _ast = ast;
    }
    
    public List<StaticParam> staticParameters() { return _ast.getStaticParams(); }
    
    public Type type() { return _ast.getType(); }

}
