/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.rewrite;

import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.RewriteHackList;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;
import java.util.Map;


public class OprInstantiaterVisitor extends NodeUpdateVisitor {
    Map<String, String> subst;

    public TraitObjectDecl visit(TraitObjectDecl toadod) {
        return (TraitObjectDecl) toadod.accept(this);
    }

    public OprInstantiaterVisitor(Map<String, String> subst) {
        this.subst = subst;
    }

    @Override
    public Node forNamedOp(NamedOp op) {
        // Replace instance ofs Op with substitution.
        String repl = subst.get(op.getText());
        if (repl == null) return op;
        return NodeFactory.makeOp(op, repl);
    }

    @Override
    public Node forOpRef(OpRef op) {
        final IdOrOp originalName = op.getOriginalName();
        final List<IdOrOp> ops = op.getNames();
        final List<StaticArg> args = op.getStaticArgs();
        final Option<Type> otype = NodeUtil.getExprType(op);
        final Type type = otype.isNone() ? null : otype.unwrap();

        Op n_originalName = (Op) recur(originalName);
        List<IdOrOp> n_ops = recurOnListOfIdOrOp(ops);
        List<StaticArg> n_args = recurOnListOfStaticArg(args);
        Type n_type = type == null ? (Type) null : (Type) recur(type);

        if (args != n_args || originalName != n_originalName || ops != n_ops || type != n_type) {
            return ExprFactory.makeOpRef(NodeUtil.getSpan(op),
                                         NodeUtil.isParenthesized(op),
                                         Option.wrap(n_type),
                                         n_args,
                                         Environment.TOP_LEVEL,
                                         n_originalName,
                                         n_ops,
                                         op.getInterpOverloadings(),
                                         op.getNewOverloadings(),
                                         op.getOverloadingType());
        }

        return op;
    }

    @Override
    public Node forStaticParam(StaticParam opp) {
        if (NodeUtil.isOpParam(opp)) {
            // Replace instance ofs Op with substitution.
            if (subst.containsKey(NodeUtil.nameString(opp.getName()))) {
                return new RewriteHackList();
            } else return opp;
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
