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

package com.sun.fortress.interpreter.nodes_util;

import java.util.List;
import java.util.Iterator;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import com.sun.fortress.interpreter.useful.Fn;

public class NodeUtil {

    public static String getName(FnName n) {
        if (n instanceof DottedId) {
            String name;
            List<String> names = ((DottedId)n).getNames();
            if (names.size() == 0) {
                throw new Error("Non-empty string is expected.");
            } else {
                name = names.get(0);
            }
            for (Iterator<String> ns = names.subList(1,names.size()-1).iterator(); ns.hasNext();) {
                name += "." + ns.next();
            }
            return name;
        } else if (n instanceof Fun) {
            return ((Fun)n).getName().getName();
        } else if (n instanceof Name) {
            Option<Id> id = ((Name)n).getId();
            Option<Op> op = ((Name)n).getOp();
            if (id instanceof Some) {
                return ((Id) ((Some) id).getVal()).getName();
            } else if (op instanceof Some) {
                return ((Op) ((Some) op).getVal()).getName();
            } else {
                throw new Error("Uninitialized Name.");
            }
        } else if (n instanceof Opr) {
            return ((Opr)n).getOp().getName();
        } else if (n instanceof PostFix) {
            return ((PostFix)n).getOp().getName();
        } else if (n instanceof Enclosing) {
            return ((Enclosing)n).getOpen().getName();
        } else if (n instanceof SubscriptOp) {
            return "[]";
        } else if (n instanceof SubscriptAssign) {
            return "[]=";
        } else if (n instanceof ConstructorFnName) {
            ConstructorFnName fn = (ConstructorFnName)n;
            // TODO Auto-generated method stub
            return fn.getDef().stringName() + "#" + fn.getSerial();
        } else if (n instanceof AnonymousFnName) {
            return (n.at() == null ? n.getSpan().toString()
                                   : ((AnonymousFnName)n).getAt().stringName())
                                     + "#" + ((AnonymousFnName)n).getSerial();
        } else { throw new Error("Uncovered FnName:" + n.getClass());
        }
    }

    public static String getName(StaticParam p) {
        if (p instanceof BoolParam) {
            return ((BoolParam)p).getId().getName();
        } else if (p instanceof DimensionParam) {
            return ((DimensionParam)p).getId().getName();
        } else if (p instanceof IntParam) {
            return ((IntParam)p).getId().getName();
        } else if (p instanceof NatParam) {
            return ((NatParam)p).getId().getName();
        } else if (p instanceof OperatorParam) {
            return ((OperatorParam)p).getOp().getName();
        } else if (p instanceof SimpleTypeParam) {
            return ((SimpleTypeParam)p).getId().getName();
        } else { throw new Error("Uncovered StaticParam:" + p.getClass());
        }
    }

    public static final Fn<Id, String> IdtoStringFn = new Fn<Id, String>() {
        public String apply(Id x) {
            return x.getName();
        }
    };
}
