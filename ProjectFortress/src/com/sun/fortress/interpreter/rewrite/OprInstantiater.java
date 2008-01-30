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

import java.util.Map;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes_util.RewriteHackList;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.NodeFactory;

public class OprInstantiater extends Rewrite {

    Map<String, String> subst;

    public OprInstantiater(Map<String, String> subst) {
        this.subst = subst;
    }

    @Override
    public AbstractNode visit(AbstractNode node) {

        if (node instanceof Op) {
            // Replace instance of Op with substitution.
            Op op = (Op) node;
            String repl = subst.get(op.getText());
            if (repl == null)
                return node;
            return NodeFactory.makeOp(op, repl);
        } else if (node instanceof OperatorParam) {
            // Nested operator params with same name (e.g., ObjectExpr) -- is that legal?
            // For now, remove it, no matter what.
            OperatorParam opp = (OperatorParam) node;
            if (subst.containsKey(NodeUtil.nameString(opp.getName()))) {
                return new RewriteHackList();
            }
        }

        return visitNode(node);
    }

}
