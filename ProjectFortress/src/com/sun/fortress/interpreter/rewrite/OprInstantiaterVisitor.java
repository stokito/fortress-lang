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

package com.sun.fortress.interpreter.rewrite;

import java.util.List;
import java.util.Map;

import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeUpdateVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpParam;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitObjectAbsDeclOrDecl;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.RewriteHackList;


public class OprInstantiaterVisitor extends NodeUpdateVisitor {
    Map<String, String> subst;

    public TraitObjectAbsDeclOrDecl visit(TraitObjectAbsDeclOrDecl toadod) {
        return (TraitObjectAbsDeclOrDecl) toadod.accept(this);
    }
    
    public OprInstantiaterVisitor(Map<String, String> subst) {
        this.subst = subst;
    }

    @Override
    public Node forOp(Op op) {
        // Replace instance ofs Op with substitution.
        String repl = subst.get(op.getText());
        if (repl == null)
            return op;
        return NodeFactory.makeOp(op, repl);
    }

    @Override
    public Node forOpParam(OpParam opp) {
        // Replace instance ofs Op with substitution.
        if (subst.containsKey(NodeUtil.nameString(opp.getName()))) {
            // throw new Error();
            return new RewriteHackList();
        } else return opp;
    }
    
    
    @Override
    public List<StaticParam> recurOnListOfStaticParam(List<StaticParam> that) {
        List<StaticParam> accum = new java.util.ArrayList<StaticParam>();
        boolean unchanged = true;
        for (StaticParam elt : that) {
            Node node = recur(elt);
            if (node instanceof RewriteHackList) {
                unchanged = false;
                for (Node nnode : ((RewriteHackList) node).getNodes()) {
                    accum.add((StaticParam) nnode);
                }
            } else {
                StaticParam update_elt = (StaticParam) node;
                unchanged &= (elt == update_elt);
                accum.add(update_elt);
            }
        }
        return unchanged ? that : accum;
    }
}
