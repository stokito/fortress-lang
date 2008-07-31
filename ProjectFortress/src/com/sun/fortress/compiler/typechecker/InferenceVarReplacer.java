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

package com.sun.fortress.compiler.typechecker;

import java.util.HashMap;
import java.util.Map;

import com.sun.fortress.compiler.Types;
import com.sun.fortress.nodes._InferenceVarType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
/**
 *
 * After we have TypeChecked the program and generated constraints, this visitor
 * goes through the AST and replaces inference variables with concrete types
 *
 */
public class InferenceVarReplacer extends NodeUpdateVisitor {
	private final Map<_InferenceVarType, Type> map;
	private final Type defaultType;

	public InferenceVarReplacer(Map<_InferenceVarType, Type> _map, Type _default){
		map=new HashMap<_InferenceVarType,Type>();
		map.putAll(_map);
		defaultType=_default;
	}

	public InferenceVarReplacer(Map<_InferenceVarType, Type> _map){
		this(_map,Types.OBJECT);
	}


	@Override
	public Node for_InferenceVarTypeOnly(_InferenceVarType that) {
		if(map.containsKey(that)){
			return map.get(that);
		}
		else
			return defaultType;
	}


}
