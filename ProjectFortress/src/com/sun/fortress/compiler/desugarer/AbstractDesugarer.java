/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.desugarer;

import java.util.ArrayList;
import java.util.List;

import com.sun.fortress.Shell;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.scala_src.typechecker.STypeChecker;
import com.sun.fortress.compiler.WellKnownNames;

import edu.rice.cs.plt.tuple.Option;




public class AbstractDesugarer extends NodeUpdateVisitor {
	
    public AbstractDesugarer() {}
    

    @Override
    public Node forFnDecl(FnDecl x) {
    	    	
    	if (x.getBody().isNone() && !x.getHeader().getMods().isAbstract()) {
    		FnHeader old = x.getHeader();
    		FnHeader n = new FnHeader(old.getStaticParams(),old.getMods().combine(Modifiers.Abstract),old.getName(),old.getWhereClause(),old.getThrowsClause(),old.getContract(),old.getParams(),old.getReturnType());
    	   return new FnDecl(x.getInfo(),n,x.getUnambiguousName(),x.getBody(),x.getImplementsUnambiguousName());
    	} else return x;    	
	
    }
	
}