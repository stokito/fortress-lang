/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

import com.sun.fortress.nodes.Enclosing;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpName;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.PostFix;
import com.sun.fortress.nodes.QualifiedOpName;

public final class OprUtil {
    public static boolean isInfixOrPrefix(OpName op) {
        return (op instanceof Op);
    }

    public static boolean isInfixOrPrefix(QualifiedOpName q) {
        return isInfixOrPrefix(q.getName());
    }

    public static boolean isEnclosing(OpName op) {
        return (op instanceof Enclosing);
    }

    public static boolean isEnclosing(QualifiedOpName q) {
        return isInfixOrPrefix(q.getName());
    }

    public static Op getOp(OpName op) {
        if (isInfixOrPrefix(op)) {
            return (Op)op;
        } else if (isEnclosing(op)) {
            return ((Enclosing)op).getOpen();
        } else {
            return (PostFix)op;
        }
    }

    public static Op getOp(QualifiedOpName q) {
        return getOp(q.getName());
    }

    public static boolean hasPrefixColon(Op op) {
        String opName = op.getText();
        return (opName.length()>1 && opName.charAt(0)==':');
    }

    public static boolean hasPrefixColon(OpName n) {
        if (isInfixOrPrefix(n)) {
            return hasPrefixColon((Op)n);
        } else {
            return false;
        }
    }

    public static boolean hasPrefixColon(QualifiedOpName q) {
        return hasPrefixColon(q.getName());
    }

    public static boolean hasSuffixColon(Op op) {
        String opName = op.getText();
        int l = opName.length();
        return (l>1 && opName.charAt(l-1)==':');
    }

    public static boolean hasSuffixColon(OpName n) {
        if (isInfixOrPrefix(n)) {
            return hasSuffixColon((Op)n);
        } else {
            return false;
        }
    }

    public static boolean hasSuffixColon(QualifiedOpName q) {
        return hasSuffixColon(q.getName());
    }

    public static String noColonText(Op op) {
        String opName = op.getText();
        int l = opName.length();
        int i = 0;
        if (hasPrefixColon(op)) {
            l--;
            i++;
        }
        if (hasSuffixColon(op)) {
            l--;
        }
        if (l < opName.length()) {
            opName = opName.substring(i,l);
        }
        return opName;
    }

    public static String noColonText(OpName n) {
        if (isInfixOrPrefix(n)) {
            return noColonText((Op)n);
        } else {
            return getOp(n).getText();
        }
    }

    public static String noColonText(QualifiedOpName q) {
        return noColonText(q.getName());
    }

    public static Op noColon(Op op) {
        String opName = op.getText();
        int l = opName.length();
        int i = 0;
        if (hasPrefixColon(op)) {
            l--;
            i++;
        }
        if (hasSuffixColon(op)) {
            l--;
        }
        if (l < opName.length()) {
            opName = opName.substring(i,l);
            return NodeFactory.makeOp(op.getSpan(),noColonText(op));
        }
        return op;
    }

    public static OpName noColon(OpName n) {
        if (isInfixOrPrefix(n)) {
            Op op = noColon((Op)n);
            if (op != n) return op;
        }
        return n;
    }

    public static QualifiedOpName noColon(QualifiedOpName q) {
        OpName orig = q.getName();
        OpName n = noColon(orig);
        if (n != orig) {
            return new QualifiedOpName(q.getSpan(), q.getApi(), n);
        }
        return q;
    }

}
