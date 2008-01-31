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

package com.sun.fortress.syntax_abstractions.util;

import java.util.Collection;
import java.util.LinkedList;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.NodeFactory;

public class SyntaxAbstractionUtil {

	/**
	 * Returns a qualified id name where the grammar name is added to the api.
	 * E.g. api: Foo.Bar, grammar name Baz, and production Gnu gives 
	 * APIName: Foo.Bar.Baz and id: Gnu.
	 */
	public static QualifiedIdName qualifyProductionName(APIName api, Id grammarName, Id productionName) {
		Collection<Id> names = new LinkedList<Id>();
		names.addAll(api.getIds());
		names.add(grammarName);
		APIName apiGrammar = NodeFactory.makeAPIName(names);
		return NodeFactory.makeQualifiedIdName(apiGrammar, productionName);
	}

}
