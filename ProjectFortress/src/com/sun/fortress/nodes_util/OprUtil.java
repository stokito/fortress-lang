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

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.UnknownFixity;
import com.sun.fortress.nodes.PostFixity;
import com.sun.fortress.nodes.PreFixity;
import edu.rice.cs.plt.tuple.Option;

public final class OprUtil {
    public static boolean isEnclosing(Op op) {
        return op.isEnclosing();
    }

    private static boolean isNotEnclosing(Op op) {
        return !(isEnclosing(op));
    }

    public static boolean isUnknownFixity(Op name) {
        if (name.isEnclosing())
            return false;
        return name.getFixity() instanceof UnknownFixity;
    }

    public static boolean isPostfix(Op name) {
        if (name.isEnclosing())
            return false;
        return name.getFixity() instanceof PostFixity;
    }

    public static Op getOp(Op op) {
        if (isEnclosing(op)) {
            String open  = op.getText().split(" ")[0];
            return NodeFactory.makeOpInfix(op.getSpan(), open);
        } else { // op instanceof Op
            return (Op)op;
        }
    }

    public static boolean hasPrefixColon(Op n) {
        if (isNotEnclosing(n)) {
            String opName = n.getText();
            return (opName.length()>1 && opName.charAt(0)==':' && !opName.equals("::"));
        } else {
            return false;
        }
    }

    public static boolean hasSuffixColon(Op n) {
        if (isNotEnclosing(n)) {
            String opName = n.getText();
            int l = opName.length();
            return (l>1 && opName.charAt(l-1)==':' && !opName.equals("::"));
        } else {
            return false;
        }
    }

    public static String noColonText(Op n) {
        if (isNotEnclosing(n)) {
            String opName = n.getText();
            int l = opName.length();
            int i = 0;
            if (hasSuffixColon(n)) {
                l--;
            }
            if (hasPrefixColon(n)) {
                i++;
            }
            if (l < opName.length()) {
                opName = opName.substring(i,l);
            }
            return opName;
        } else {
            return getOp(n).getText();
        }
    }

    public static Op noColon(Op n) {
        if (isNotEnclosing(n)) {
            Op op = n;
            if (hasSuffixColon(op) || hasPrefixColon(op)) {
                op = NodeFactory.makeOp(op.getSpan(),noColonText(op),
                                        op.getFixity());
            }
            if (op != n) return op;
        }
        return n;
    }

    public static String fixityDecorator(final Fixity of, final String s) {
        return of.accept(new NodeAbstractVisitor<String>() {
//            @Override public String forInFixity(InFixity that) {
//                return "infix "+s;
//            }
            @Override public String forPreFixity(PreFixity that) {
                return "prefix "+s;
            }
            @Override public String forPostFixity(PostFixity that) {
                return "postfix "+s;
            }
//            @Override public String forNoFixity(NoFixity that) {
//                return "nofix "+s;
//            }
//            @Override public String forMultiFixity(MultiFixity that) {
//                return "multifix "+s;
//            }
//            @Override public String forBigFixity(BigFixity that) {
//                return "big "+s;
//            }
//            @Override public String forEnclosingFixity(EnclosingFixity that) {
//                return "enclosing "+s;
//            }
            @Override public String defaultCase(Node that) {
                return s;
            }
        });
    }

    /** Return a new operator with the fixity prepended to the text. */
    public static Op decorateOperator(Op o) {
        return new Op(o.getSpan(), Option.<APIName>none(),
                      fixityDecorator(o.getFixity(), o.getText()),
                      o.getFixity(), o.isEnclosing());
    }

    /** Return a new operator with the fixity stripped from the text. */
    public static Op undecorateOperator(Op o) {
        int i = o.getText().indexOf(" ");
        if (i < 0) {
            return o;
        }
        return new Op(o.getSpan(), Option.<APIName>none(),
                      o.getText().substring(i+1),
                      o.getFixity(), o.isEnclosing());
    }

}
