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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.IndexBuilder;
import com.sun.fortress.compiler.StaticChecker;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.GeneratedExpr;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.Label;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeDepthFirstVisitor_void;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.Spawn;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.While;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Pair;

/**
 * This type holds output from the TypeChecking phase that could reasonably used by
 * later passes. For instance, you can retrieve a {@code TypeEnv} that is in scope
 * at a particular node.
 */
public class TypeCheckerOutput {
    // FIXME: Change visibility to private
    private final Map<Pair<Node,Span>, TypeEnv> nodeTypeEnvs;
    
    // Package private: Only the typechecker should need to create these.
    TypeCheckerOutput(Map<Pair<Node,Span>, TypeEnv> node_type_envs) {
        this.nodeTypeEnvs = Collections.unmodifiableMap(node_type_envs);
    }
    
    public TypeCheckerOutput(TypeCheckerOutput that, TypeCheckerOutput the_other) {
        this(CollectUtil.union(that.nodeTypeEnvs, the_other.nodeTypeEnvs));
    }

    /**
     * Given an ast node, return the TypeEnv that was in scope _at_ that node. In particular,
     * this type environment will not include any new variables that this node might
     * introduce.
     */
    public TypeEnv getTypeEnv(Node n) {
        Span s = n.getSpan();
        Pair<Node,Span> p = Pair.make(n, s);
        return this.nodeTypeEnvs.get(p);
    }
    
    /**
     * Given a component ast with one of its subnodes overwritten, and
     * the ast of the component that it was to replace, this method
     * recreates both the ast and this object, so that they fulfill any invariants. It is
     * reasonable to think of this method as re-running the typechecker and returning the
     * result. The performance on this is poor so try and run it once per component.
     */
    public Pair<Node,TypeCheckerOutput> populateType(Node old_component_ast,Node new_component_ast, 
            GlobalEnvironment env) {
    	/* Create a new copy of TypeCheckerOutput that does not contain any TypeEnvs for subnodes
    	 * of old_comonent_ast.
    	 */
    	final Map<Pair<Node,Span>,TypeEnv> removed = new HashMap<Pair<Node,Span>,TypeEnv>();
    	NodeDepthFirstVisitor_void remover=new NodeDepthFirstVisitor_void(){

			@Override
			public void forCatch(Catch that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forCatch(that);
			}

			@Override
			public void forFnDecl(FnDecl that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forFnDecl(that);
			}

			@Override
			public void forFnExpr(FnExpr that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forFnExpr(that);
			}

			@Override
			public void forForOnly(For that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forForOnly(that);
			}

			@Override
			public void forGeneratedExpr(GeneratedExpr that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forGeneratedExpr(that);
			}

			@Override
			public void forIfClause(IfClause that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forIfClause(that);
			}

			@Override
			public void forLabel(Label that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forLabel(that);
			}

			@Override
			public void forLetFn(LetFn that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forLetFn(that);
			}

			@Override
			public void forLocalVarDecl(LocalVarDecl that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forLocalVarDecl(that);
			}

			@Override
			public void forObjectDecl(ObjectDecl that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forObjectDecl(that);
			}

			@Override
			public void forObjectExpr(ObjectExpr that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forObjectExpr(that);
			}

			@Override
			public void forSpawn(Spawn that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forSpawn(that);
			}

			@Override
			public void forTraitDecl(TraitDecl that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forTraitDecl(that);
			}

			@Override
			public void forTypecase(Typecase that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forTypecase(that);
			}

			@Override
			public void forWhile(While that) {
				// TODO Auto-generated method stub
				removed.remove(Pair.make(that,that.getSpan()));
				super.forWhile(that);
			}
    		
    	};
    	old_component_ast.accept(remover);
    	TypeCheckerOutput oldremoved = new TypeCheckerOutput(removed);
    	/* Accumulate the necessary structures to rerun the typechecker on new_component_ast. Unfortunately you
    	 * can't run the typechecker on new_node because the TypeEnv from nodeTypeEnvs won't contain any top-level
    	 * functions that you define during closure conversion
    	 */
        ComponentIndex component = IndexBuilder.builder.buildComponentIndex((Component)new_component_ast, System.currentTimeMillis());
        TypeCheckerResult result = StaticChecker.checkComponent(component, env);
        TypeCheckerOutput newoutput = result.getTypeCheckerOutput();
        // Get the new ast out of result, merge the TypeCheckerOutput, and then return
        return Pair.make(result.ast(), new TypeCheckerOutput(oldremoved, newoutput)); 
    }

    public static TypeCheckerOutput emptyOutput() {
        return new TypeCheckerOutput(Collections.<Pair<Node,Span>, TypeEnv>emptyMap());
    }
    
}
