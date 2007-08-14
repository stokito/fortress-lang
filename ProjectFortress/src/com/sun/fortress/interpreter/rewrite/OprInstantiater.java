/*
 * Created on Aug 13, 2007
 *
 */
package com.sun.fortress.interpreter.rewrite;

import java.util.Map;

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OperatorParam;
import com.sun.fortress.nodes_util.RewriteHackList;

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
            String repl = subst.get(op.getName());
            if (repl == null)
                return node;
            return new Op(op.getSpan(), repl);
        } else if (node instanceof OperatorParam) {
            // Nested operator params with same name (e.g., ObjectExpr) -- is that legal?
            // For now, remove it, no matter what.
            OperatorParam opp = (OperatorParam) node;
            if (subst.containsKey(opp.getOp().getName())) {
                return new RewriteHackList();
            }
        }
        
        return visitNode(node);
    }

}
