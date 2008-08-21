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
import java.util.Map;

import com.sun.fortress.nodes.Node;
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
    public final Map<Pair<Node,Span>, TypeEnv> nodeTypeEnvs;
    
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
     * Given a new ast node, and the node that it was to replace in the ast, this method
     * recreates both the node and this object, so that they fulfill any invariants. It is
     * reasonable to think of this method as re-running the typechecker and returning the
     * result.
     */
    public Pair<Node,TypeCheckerOutput> populateType(Node original_node,Node new_node) {
        return NI.nyi();
    }

    public static TypeCheckerOutput emptyOutput() {
        return new TypeCheckerOutput(Collections.<Pair<Node,Span>, TypeEnv>emptyMap());
    }
    
}
