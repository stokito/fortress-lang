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

import java.util.List;

import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.Modifier;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.tuple.Option;

/** 
 * Inserts inference type variables into the AST where explicit types were not
 * given. This change will be performed before typechecking, so that the
 * typechecker will always expect types to exist.
 */
public class InferenceVarInserter extends NodeUpdateVisitor {

	@Override
	public Node forLValueBindOnly(LValueBind that, Id name_result,
			Option<Type> type_result, List<Modifier> mods_result) {
		if( type_result.isNone() ) {
			Option<Type> new_type = Option.<Type>some(NodeFactory.make_InferenceVarType());
			return new LValueBind(that.getSpan(),name_result,new_type,mods_result,that.isMutable());
		}
		else {
			return that;
		}
	}

	
	
}
