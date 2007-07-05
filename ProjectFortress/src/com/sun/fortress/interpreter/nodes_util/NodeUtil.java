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
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.IterableOnceTranslatingList;
import com.sun.fortress.interpreter.useful.UnitIterable;

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
        } else { throw new Error("NodeUtil.getName: uncovered FnName " + n.getClass());
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
        } else { throw new Error("NodeUtil.getName: uncovered StaticParam " + p.getClass());
        }
    }

    /*
    public static IterableOnce<String> stringNames(LValue d) {
        if (d instanceof LValueBind) {
            return new UnitIterable<String>(((LValueBind)d).getName().getName());
        } else if (d instanceof UnpastingBind) {
            return new UnitIterable<String>(((UnpastingBind)d).getName().getName());
        } else if (d instanceof UnpastingSplit) {
            return new IterableOnceForLValueList(((UnpastingSplit)d).getElems());
        } else {
            throw new Error("NodeUtil.stringNames: Uncovered LValue " + d.getClass());
        }
    }
    */

    public static IterableOnce<String> stringNames(DefOrDecl d) {
        if (d instanceof AbsExternalSyntax) {
            return new UnitIterable<String>(((AbsExternalSyntax)d).getId().getName());
        } else if (d instanceof AbsTypeAlias) {
            return new UnitIterable<String>(((AbsTypeAlias)d).getName().getName());
        } else if (d instanceof Dimension) {
            return new UnitIterable<String>(((Dimension)d).getId().getName());
        } else if (d instanceof ExternalSyntax) {
            return new UnitIterable<String>(((ExternalSyntax)d).getId().getName());
        } else if (d instanceof Fn) {
            return new UnitIterable<String>(NodeUtil.getName(((Fn)d).getFnName()));
        } else if (d instanceof FnDefOrDecl) {
            return new UnitIterable<String>(NodeUtil.getName(((FnDefOrDecl)d).getFnName()));
        } else if (d instanceof GeneratedExpr) {
            return new UnitIterable<String>("GeneratedExpr");
        } else if (d instanceof LetFn) {
            return new UnitIterable<String>(((LetFn)d).getClass().getSimpleName());
        } else if (d instanceof LocalVarDecl) {
                   return new IterableOnceForLValueList(((LocalVarDecl)d).getLhs());
        } else if (d instanceof ObjectDefOrDecl) {
            return new UnitIterable<String>(((ObjectDefOrDecl)d).getName().getName());
        } else if (d instanceof ObjectExpr) {
            return new UnitIterable<String>(((ObjectExpr)d).getGenSymName());
        } else if (d instanceof PropertyDecl) {
            Option<Id> id = ((PropertyDecl)d).getId();
            if (id.isPresent()) {
                Some s = (Some) id;
                return new UnitIterable<String>(((Id)(s.getVal())).getName());
            } else {
                return new UnitIterable<String>("_");
            }
        } else if (d instanceof TestDecl) {
            return new UnitIterable<String>(((TestDecl)d).getId().getName());
        } else if (d instanceof TraitDefOrDecl) {
            return new UnitIterable<String>(((TraitDefOrDecl)d).getName().getName());
        } else if (d instanceof TypeAlias) {
            return new UnitIterable<String>(((TypeAlias)d).getName().getName());
        } else if (d instanceof UnitVar) {
            return new IterableOnceTranslatingList<Id, String>(((UnitVar)d).
                                                    getNames(), IdtoStringFn);
        } else if (d instanceof VarDefOrDecl) {
            return new IterableOnceForLValueList(((VarDefOrDecl)d).getLhs());
        } else {
            throw new Error("NodeUtil.stringNames: Uncovered DefOrDecl " + d.getClass());
        }
    }

    public static String stringName(AbstractNode node) {
        return node.getClass().getSimpleName();
    }

    public static String stringName(ObjectExpr expr) {
        return expr.getGenSymName();
    }

    public static final com.sun.fortress.interpreter.useful.Fn<Id, String> IdtoStringFn =
        new com.sun.fortress.interpreter.useful.Fn<Id, String>() {
            public String apply(Id x) {
                return x.getName();
            }
        };
}
