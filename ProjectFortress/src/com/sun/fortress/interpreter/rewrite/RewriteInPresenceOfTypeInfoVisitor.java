/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.rewrite;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;

import java.util.List;

public class RewriteInPresenceOfTypeInfoVisitor extends NodeUpdateVisitor {

    public static final RewriteInPresenceOfTypeInfoVisitor Only = new RewriteInPresenceOfTypeInfoVisitor();

    public Node visit(Node n) {
        return n.accept(this);
    }

    @Override
    public Node forFnRef(FnRef fr) {

        List<IdOrOp> fns = fr.getNames(); // ignore this for now.
        List<StaticArg> sargs = fr.getStaticArgs();
        IdOrOp idn = fns.get(0);
        if (!(idn instanceof Id)) {
            bug(idn, "The name field of FnRef should be Id.");
        }
        Id id = (Id) idn;

        if (sargs.size() > 0) return (ExprFactory.make_RewriteFnRef(NodeUtil.getSpan(fr),
                                                                    NodeUtil.isParenthesized(fr),
                                                                    NodeUtil.getExprType(fr),
                                                                    ExprFactory.makeVarRef(id),
                                                                    sargs)).accept(this);

        else {
            //throw new Error("Unexpected FnRef " + fr);
            return (ExprFactory.makeVarRef(NodeUtil.getSpan(id),
                                           NodeUtil.isParenthesized(fr),
                                           NodeUtil.getExprType(fr),
                                           id)).accept(this);
        }

    }

    @Override
    public Node forTraitType(TraitType it) {
        if (it.getArgs().size() == 0) {
            return (NodeFactory.makeVarType(NodeUtil.getSpan(it), it.getName())).accept(this);
        }
        return super.forTraitType(it);
    }

    private RewriteInPresenceOfTypeInfoVisitor() {

    }

}
