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

import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.BigFixity;
import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.EnclosingFixity;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.MultiFixity;
import com.sun.fortress.nodes.NoFixity;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.PostFixity;
import com.sun.fortress.nodes.PreFixity;
import com.sun.fortress.nodes.InFixity;
import edu.rice.cs.plt.tuple.Option;

public final class OprUtil {
    public static boolean isEnclosing(OpName op) {
        return (op instanceof Enclosing);
    }

    private static boolean isNotEnclosing(OpName op) {
        return !(isEnclosing(op));
    }

    public static boolean isUnknownFixity(Op name) {
        return name.getFixity().isNone();
    }

    public static boolean isUnknownFixity(OpName name) {
        if (name instanceof Enclosing)
            return false;
        return isUnknownFixity((Op)name);
    }

    private static boolean isPostfix(Op name) {
        return name.getFixity().unwrap(null) instanceof PostFixity;
    }

    public static boolean isPostfix(OpName name) {
        if (name instanceof Enclosing)
            return false;
        return isPostfix((Op)name);
    }

    public static Op getOp(OpName op) {
        if (isEnclosing(op)) {
            return ((Enclosing)op).getOpen();
        } else { // op instanceof Op
            return (Op)op;
        }
    }

    public static boolean hasPrefixColon(Op op) {
        String opName = op.getText();
        return (opName.length()>1 && opName.charAt(0)==':' && !opName.equals("::"));
    }

    public static boolean hasPrefixColon(OpName n) {
        if (isNotEnclosing(n)) {
            return hasPrefixColon((Op)n);
        } else {
            return false;
        }
    }

    public static boolean hasSuffixColon(Op op) {
        String opName = op.getText();
        int l = opName.length();
        return (l>1 && opName.charAt(l-1)==':' && !opName.equals("::"));
    }

    public static boolean hasSuffixColon(OpName n) {
        if (isNotEnclosing(n)) {
            return hasSuffixColon((Op)n);
        } else {
            return false;
        }
    }

    public static String noColonText(Op op) {
        String opName = op.getText();
        int l = opName.length();
        int i = 0;
        if (hasSuffixColon(op)) {
            l--;
        }
        if (hasPrefixColon(op)) {
            i++;
        }
        if (l < opName.length()) {
            opName = opName.substring(i,l);
        }
        return opName;
    }

    public static String noColonText(OpName n) {
        if (isNotEnclosing(n)) {
            return noColonText((Op)n);
        } else {
            return getOp(n).getText();
        }
    }

    public static Op noColon(Op op) {
        if (hasSuffixColon(op) || hasPrefixColon(op)) {
            return NodeFactory.makeOp(op.getSpan(),noColonText(op),
                                      op.getFixity());
        }
        return op;
    }

    public static OpName noColon(OpName n) {
        if (isNotEnclosing(n)) {
            Op op = noColon((Op)n);
            if (op != n) return op;
        }
        return n;
    }

    public static String fixityDecorator(final Option<Fixity> of, final String s) {
        if (of.isNone()) {
            return s;
        }
        return of.unwrap().accept(new NodeAbstractVisitor<String>() {
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
        return new Op(o.getSpan(), fixityDecorator(o.getFixity(), o.getText()), o.getFixity());
    }

    /** Return a new operator with the fixity stripped from the text. */
    public static Op undecorateOperator(Op o) {
        int i = o.getText().indexOf(" ");
        if (i < 0) {
            return o;
        }
        return new Op(o.getSpan(), o.getText().substring(i+1), o.getFixity());
    }

}
