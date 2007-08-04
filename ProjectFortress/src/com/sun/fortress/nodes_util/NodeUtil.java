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

import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.NativeApplicable;
import com.sun.fortress.interpreter.glue.WellKnownNames;

public class NodeUtil {

    public static final String defaultSelfName = WellKnownNames.defaultSelfName;

    /* for HasAt ***********************************************************/
    /**
     * Returns the index of the 'self' parameter in the parameter list,
     * or -1 if it does not appear.
     * Only meaningful for method declarations.
     */
    public static int selfParameterIndex(HasAt d) {
        if (d instanceof FnAbsDeclOrDecl) {
            int i = 0;
            for (Param p : ((FnAbsDeclOrDecl)d).getParams()) {
                Id id = p.getId();
                if (WellKnownNames.defaultSelfName.equals(id.getName())) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    /* for Applicable ******************************************************/
    public static String nameAsMethod(Applicable app) {
        String name = getName(app.getFnName());
        if (app instanceof FnAbsDeclOrDecl) {
            int spi = selfParameterIndex((FnAbsDeclOrDecl)app);
            if (spi >= 0)
                return "rm$" + spi + "$" + name;
            else
                return name;
        } else {
            return name;
        }
    }

    public static Option<Expr> getBody(Applicable def) {
        if (def instanceof FnDef)
            return new Some<Expr>(((FnDef)def).getBody());
        else if (def instanceof FnExpr)
            return new Some<Expr>(((FnExpr)def).getBody());
        else
            return new None<Expr>();
    }

    /* for Param ***********************************************************/
    public static boolean isTransient(Param p) {
        for (Modifier m : p.getMods()) {
            if (m instanceof ModifierTransient) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMutable(Param p) {
        for (Modifier m : p.getMods()) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable) {
                return true;
            }
        }
        return false;
    }

    /* getName *************************************************************/
    public static String getName(FnName n) {
        return n.accept(new NodeAbstractVisitor<String>() {
            public String forDottedId(DottedId n) {
                String name;
                List<String> names = n.getNames();
                int size = names.size();
                if (size == 0) {
                    throw new Error("Non-empty string is expected.");
                } else if (size == 1) {
                    return names.get(0);
                } else {
                    name = names.get(0);
                }
                for (Iterator<String> ns = names.subList(1,names.size()-1).iterator(); ns.hasNext();) {
                    name += "." + ns.next();
                }
                return name;
            }
            public String forOpr(Opr n) {
                return n.getOp().getName();
            }
            public String forPostFix(PostFix n) {
                return n.getOp().getName();
            }
            public String forEnclosing(Enclosing n) {
                return n.getOpen().getName();
            }
            public String forSubscriptOp(SubscriptOp n) {
                return "[]";
            }
            public String forSubscriptAssign(SubscriptAssign n) {
                return "[]=";
            }
            public String forAnonymousFnName(AnonymousFnName n) {
                return n.getSpan().toString();
            }
            public String forConstructorFnName(ConstructorFnName n) {
            // TODO Auto-generated method stub
            return stringName(n.getDef());
            }
        });
    }

    public static String getName(StaticParam p) {
        return p.accept(new NodeAbstractVisitor<String>() {
            public String forBoolParam(BoolParam p) {
                return p.getId().getName();
            }
            public String forDimensionParam(DimensionParam p) {
                return p.getId().getName();
            }
            public String forIntParam(IntParam p) {
                return p.getId().getName();
            }
            public String forNatParam(NatParam p) {
                return p.getId().getName();
            }
            public String forOperatorParam(OperatorParam p) {
                return p.getOp().getName();
            }
            public String forSimpleTypeParam(SimpleTypeParam p) {
                return p.getId().getName();
            }
        });
    }

    /* stringName **********************************************************/
    public static String stringName(Node node) {
        return node.accept(new NodeAbstractVisitor<String>() {
            public String forDimDecl(DimDecl node) {
                return node.getId().getName();
            }
            public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
                return getName(node.getFnName());
            }
            public String forFnName(FnName node) {
                return getName(node);
            }
            public String forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl node) {
                return node.getId().getName();
            }
            public String for_RewriteObjectExpr(_RewriteObjectExpr node) {
                return node.getGenSymName();
            }
            public String forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl node) {
                return node.getId().getName();
            }
            public String forTypeAlias(TypeAlias node) {
                return node.getId().getName();
            }
            public String forUnitDecl(UnitDecl node) {
                return node.getNames().toString();
            }
            public String defaultCase(Node node) {
                return node.getClass().getSimpleName();
            }
        });
    }

    /* stringNames *********************************************************/
    public static IterableOnce<String> stringNames(LValue d) {
        return d.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forLValueBind(LValueBind d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forUnpastingBind(UnpastingBind d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forUnpastingSplit(UnpastingSplit d) {
                return new IterableOnceForLValueList(d.getElems());
            }
        });
    }

    public static IterableOnce<String> stringNames(AbsDeclOrDecl d) {
        return d.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forAbsExternalSyntax(AbsExternalSyntax d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forDimDecl(DimDecl d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forExternalSyntax(ExternalSyntax d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forFnExpr(FnExpr d) {
                return new UnitIterable<String>(NodeUtil.getName(d.getFnName()));
            }
            public IterableOnce<String> forFnAbsDeclOrDecl(FnAbsDeclOrDecl d) {
                return new UnitIterable<String>(NodeUtil.getName(d.getFnName()));
            }
            public IterableOnce<String> forGeneratedExpr(GeneratedExpr d) {
                return new UnitIterable<String>("GeneratedExpr");
            }
            public IterableOnce<String> forLetFn(LetFn d) {
                return new UnitIterable<String>(d.getClass().getSimpleName());
            }
            public IterableOnce<String> forLocalVarDecl(LocalVarDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
            public IterableOnce<String> forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> for_RewriteObjectExpr(_RewriteObjectExpr d) {
                return new UnitIterable<String>(d.getGenSymName());
            }
            public IterableOnce<String> forPropertyDecl(PropertyDecl d) {
                Option<Id> id = d.getId();
                if (id.isPresent()) {
                    Some s = (Some) id;
                    return new UnitIterable<String>(((Id)(s.getVal())).getName());
                } else {
                    return new UnitIterable<String>("_");
                }
            }
            public IterableOnce<String> forTestDecl(TestDecl d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forTypeAlias(TypeAlias d) {
                return new UnitIterable<String>(d.getId().getName());
            }
            public IterableOnce<String> forUnitDecl(UnitDecl d) {
                return new IterableOnceTranslatingList<Id, String>(d.getNames(), IdtoStringFn);
            }
            public IterableOnce<String> forVarAbsDeclOrDecl(VarAbsDeclOrDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
        });
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
    public static final com.sun.fortress.useful.Fn<Id, String> IdtoStringFn =
        new com.sun.fortress.useful.Fn<Id, String>() {
            public String apply(Id x) {
                return x.getName();
            }
        };
}
