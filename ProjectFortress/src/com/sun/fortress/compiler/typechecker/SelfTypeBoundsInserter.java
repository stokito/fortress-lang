/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.compiler.typechecker;

import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.scala_src.useful.SNodeUtil;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

/**
 * This class is responsible for simplifying 
 * the comprises clauses of self-type idioms:
 *  - For each naked type variable V in a trait T's comprises clause
 *    the instance of T by using all its static parameter names as
 *      corresponding static arguments to the trait is implicitly
 *      regarded as one of the bounds on that static parameter V
 *      (in addition to any other bounds it might have).
 *    (checked by SNodeUtil.checkSparams)
 */
public class SelfTypeBoundsInserter extends NodeUpdateVisitor {

    public Node forTraitDeclOnly(TraitDecl that,
				 ASTNodeInfo info,
				 TraitTypeHeader header,
				 Option<SelfType> selfType,
				 List<BaseType> excludesClause,
				 Option<List<NamedType>> comprises) {
	Id name = (Id)header.getName();
	List<StaticParam> sparams = header.getStaticParams();
	sparams = SNodeUtil.checkSparams(name, sparams, comprises);

	return new TraitDecl(info,
			     new TraitTypeHeader(sparams, header.getMods(), name,
						 header.getWhereClause(),
						 header.getThrowsClause(),
						 header.getContract(),
						 header.getExtendsClause(),
                         header.getParams(),
						 header.getDecls()),
			     selfType, excludesClause, comprises,
			     that.isComprisesEllipses());
    }
}
