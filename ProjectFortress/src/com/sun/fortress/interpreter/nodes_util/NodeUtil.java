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

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.NativeApplicable;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import com.sun.fortress.interpreter.useful.IterableOnce;
import com.sun.fortress.interpreter.useful.IterableOnceTranslatingList;
import com.sun.fortress.interpreter.useful.UnitIterable;

public class NodeUtil {

    /* for AbstractNode ****************************************************/
    /**
     *
     * @return String representation of the location, suitable for error
     *         messages.
     */
    public static String getAt(HasAt thing) {
        if (thing instanceof AbstractNode) {
            return ((AbstractNode)thing).getSpan().begin.at();
            // + ":(" + getClass().getSimpleName() + ")";
        } else if (thing instanceof Overload) {
            return ((Overload)thing).at();
        } else if (thing instanceof NativeApp) {
            return ((NativeApp)thing).at();
        } else if (thing instanceof NativeApplicable) {
            return "Native stub for " + ((NativeApplicable)thing).stringName();
        } else if (thing instanceof HasAt.FromString) {
            return ((HasAt.FromString)thing).at();
        } else {
            throw new Error("NodeUtil.getAt() is not defined for "
                            + thing.getClass());
        }
    }

    public static Span getSpan(HasAt thing) {
        if (thing instanceof AbstractNode) {
            return ((AbstractNode)thing).getSpan();
        } else {
            throw new Error("NodeUtil.getSpan() is not defined for "
                            + thing.getClass());
        }
    }

    /**
     * Returns the index of the 'self' parameter in the list,
     * or -1 if it does not appear.
     */
    public static int selfParameterIndex(HasAt d) {
        if (d instanceof FnDefOrDecl) {
            return ((FnDefOrDecl)d).selfParameterIndex();
        } else {
            return -1;
        }
    }

    /* for Applicable ******************************************************/
    public static String nameAsMethod(Applicable app) {
        if (app instanceof FnExpr) {
            return getName(((FnExpr)app).getFnName());
        } else if (app instanceof FnDefOrDecl) {
            int spi = ((FnDefOrDecl)app).selfParameterIndex();
            if (spi >= 0)
                return "rm$" + spi + "$" + getName(((FnDefOrDecl)app).getFnName());
            else
                return getName(((FnDefOrDecl)app).getFnName());
        } else if (app instanceof NativeApp) {
            return getName(((NativeApp)app).getFnName());
        } else if (app instanceof NativeApplicable) {
            return getName(((NativeApplicable)app).getFnName());
        } else {
            throw new Error("NodeUtil.nameAsMethod(" + app.getClass());
        }
    }

    /* for Param ***********************************************************/
    public static boolean isTransient(Param p) {
        for (Modifier m : p.getMods()) {
            if (m instanceof Modifier.Transient) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMutable(Param p) {
        for (Modifier m : p.getMods()) {
            if (m instanceof Modifier.Var || m instanceof Modifier.Settable) {
                return true;
            }
        }
        return false;
    }

    /* getName *************************************************************/
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
        } else if (n instanceof AnonymousFnName) {
            return (getAt(n) == null ? n.getSpan().toString()
                                     : stringName(((AnonymousFnName)n).getAt()))
                                       + "#" + ((AnonymousFnName)n).getSerial();
        } else if (n instanceof ConstructorFnName) {
            ConstructorFnName fn = (ConstructorFnName)n;
            // TODO Auto-generated method stub
            return stringName(fn.getDef()) + "#" + fn.getSerial();
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

    /* stringName **********************************************************/
    public static String stringName(HasAt node) {
        if (node instanceof Dimension) {
            return ((Dimension)node).getId().getName();
        } else if (node instanceof FnDefOrDecl) {
            return NodeUtil.getName(((FnDefOrDecl)node).getFnName());
        } else if (node instanceof FnName) {
            return NodeUtil.getName((FnName)node);
        } else if (node instanceof ObjectDefOrDecl) {
            return ((ObjectDefOrDecl)node).getName().getName();
        } else if (node instanceof ObjectExpr) {
            return ((ObjectExpr)node).getGenSymName();
        } else if (node instanceof TraitDefOrDecl) {
            return ((TraitDefOrDecl)node).getName().getName();
        } else if (node instanceof TypeAlias) {
            return ((TypeAlias)node).getName().getName();
        } else if (node instanceof UnitVar) {
            return ((UnitVar)node).getNames().toString();
        } else {
            return node.getClass().getSimpleName();
        }
    }

    public static String stringName(ObjectExpr expr) {
        return expr.getGenSymName();
    }

    /* stringNames *********************************************************/
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

    public static IterableOnce<String> stringNames(DefOrDecl d) {
        if (d instanceof AbsExternalSyntax) {
            return new UnitIterable<String>(((AbsExternalSyntax)d).getId().getName());
        } else if (d instanceof AbsTypeAlias) {
            return new UnitIterable<String>(((AbsTypeAlias)d).getName().getName());
        } else if (d instanceof Dimension) {
            return new UnitIterable<String>(((Dimension)d).getId().getName());
        } else if (d instanceof ExternalSyntax) {
            return new UnitIterable<String>(((ExternalSyntax)d).getId().getName());
        } else if (d instanceof FnExpr) {
            return new UnitIterable<String>(NodeUtil.getName(((FnExpr)d).getFnName()));
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

    /* dump ****************************************************************/
    public static String dump(AbstractNode n) {
        try {
            StringBuffer sb = new StringBuffer();
            dump(sb, n);
            return sb.toString();
        } catch (Throwable ex) {
            return "Exception " + ex + " during dump";
        }
    }

    /**
     * @throws IOException
     */
    public static void dump(Appendable appendable, AbstractNode n) throws IOException {
        Printer p = new Printer(true, true, true);
        p.dump(n, appendable, 0);
    }

    public static <T> T NYI(String s) {
        throw new Error("AST." + s + " NYI");
    }

    /* function ************************************************************/
    public static final com.sun.fortress.interpreter.useful.Fn<Id, String> IdtoStringFn =
        new com.sun.fortress.interpreter.useful.Fn<Id, String>() {
            public String apply(Id x) {
                return x.getName();
            }
        };
}
