/*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import com.sun.fortress.nodes.*;
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
            return NodeFactory.makeOpInfix(NodeUtil.getSpan(op), open);
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
                op = NodeFactory.makeOp(NodeUtil.getSpan(op),noColonText(op),
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
        return NodeFactory.makeOp(NodeUtil.getSpan(o), Option.<APIName>none(),
                      fixityDecorator(o.getFixity(), o.getText()),
                      o.getFixity(), o.isEnclosing());
    }
    
    public static boolean equal(Fixity l, Fixity r) {
        return ((l instanceof    InFixity && r instanceof    InFixity) ||
                (l instanceof   PreFixity && r instanceof   PreFixity) ||
                (l instanceof  PostFixity && r instanceof  PostFixity) ||
                (l instanceof    NoFixity && r instanceof    NoFixity) ||
                (l instanceof MultiFixity && r instanceof MultiFixity) ||
                (l instanceof   BigFixity && r instanceof   BigFixity) ||
                (l instanceof EnclosingFixity && r instanceof EnclosingFixity));
    }
}
