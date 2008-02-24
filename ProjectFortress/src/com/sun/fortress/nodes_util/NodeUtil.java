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

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.InterpreterBug;
import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.WellKnownNames;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class NodeUtil {

    public static final String defaultSelfName = WellKnownNames.defaultSelfName;


    public static Iterable<Id> getIds(final QualifiedIdName qName) {
        return qName.getApi().apply(new OptionVisitor<APIName, Iterable<Id>>() {
            public Iterable<Id> forSome(APIName apiName) {
                return IterUtil.compose(apiName.getIds(), qName.getName());
            }
            public Iterable<Id> forNone() {
                return IterUtil.singleton(qName.getName());
            }
        });
    }

    /* for HasAt ***********************************************************/
    /**
     * Returns the index of the 'self' parameter in the parameter list,
     * or -1 if it does not appear.
     * Only meaningful for method declarations.
     */
    public static int selfParameterIndex(Applicable d) {
        int i = 0;
        for (Param p : d.getParams()) {
            Id name = p.getName();
                if (WellKnownNames.defaultSelfName.equals(nameString(name))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /* for Applicable ******************************************************/
    public static String nameAsMethod(Applicable app) {
        String name = nameString(app.getName());

            int spi = selfParameterIndex(app);
            if (spi >= 0)
                return "rm$" + spi + "$" + name;
            else
                return name;

    }

    public static Option<Expr> getBody(Applicable def) {
        if (def instanceof FnDef) { return Option.some(((FnDef)def).getBody()); }
        else if (def instanceof FnExpr) { return Option.some(((FnExpr)def).getBody()); }
        else { return Option.none(); }
    }

    /* for Param ***********************************************************/
    public static boolean isMultifix(List<Param> params) {
        for (Param p : params) {
            if (p instanceof VarargsParam) return true;
        }
        return false;
    }

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

    private final static String fixityDecorator(Option<Fixity> of, String s) {
        if (of.isNone())
            return s;
        Fixity f = Option.unwrap(of);
        if (f instanceof PreFixity)
            return "prefix "+s;
        if (f instanceof PostFixity)
            return "postfix "+s;

        return s;
    }

    private final static NodeVisitor<String> nameGetter =
        new NodeAbstractVisitor<String>() {

        @Override public String forAPIName(APIName n) {
            return nameString(n);
            }
        @Override public String forQualifiedName(QualifiedName n) {
            return nameString(n);
            }
        public String forId(Id n) { return n.getText(); }
        public String forOp(Op n) { return fixityDecorator(n.getFixity(), n.getText()); }
        public String forEnclosing(Enclosing n) {
            return n.getOpen().getText() + " " + n.getClose().getText();
        }
        public String forAnonymousFnName(AnonymousFnName n) {
            return n.getSpan().toString();
        }
        public String forConstructorFnName(ConstructorFnName n) {
        // TODO Auto-generated method stub
        return stringName(n.getDef());
        }
    };

    /* nameString *************************************************************/
    public static String nameString(Name n) {
        return n.accept(nameGetter);
    }

    public static String nameString(Id n) {
        return n.getText();
    }

    public static String nameString(Op n) {
        return n.getText();
    }

    public static String nameString(APIName n) {
        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
        return IterUtil.toString(ns, "", ".", "");
    }

    public static String nameString(QualifiedName n) {
        final String last = n.getName().accept(nameGetter);
//        return n.getApi().apply(new OptionVisitor<APIName, String>() {
//            public String forSome(APIName api) {
//                return nameString(api) + "." + last;
//            }
//            public String forNone() { return last; }
//        });
        Option<APIName> odn = n.getApi();
        return odn.isSome() ? nameString(Option.unwrap(odn)) + "." + last : last;
    }

    public static String namesString(Iterable<? extends Name> names) {
        return IterUtil.toString(IterUtil.map(names, NameToStringFn), "", ", ", "");
    }


    /* getName *************************************************************/
    public static String getName(StaticParam param) {
        return param.accept(new NodeAbstractVisitor<String>() {
            public String forBoolParam(BoolParam p) {
                return p.getName().getText();
            }
            public String forDimensionParam(DimensionParam p) {
                return p.getName().getText();
            }
            public String forIntParam(IntParam p) {
                return p.getName().getText();
            }
            public String forNatParam(NatParam p) {
                return p.getName().getText();
            }
            public String forOperatorParam(OperatorParam p) {
                return nameString(p.getName());
            }
            public String forSimpleTypeParam(SimpleTypeParam p) {
                return p.getName().getText();
            }
            public String forUnitParam(UnitParam p) {
                return p.getName().getText();
            }
        });
    }

    private final static NodeAbstractVisitor<String> stringNameVisitor =
        new NodeAbstractVisitor<String>() {
        public String forDimDecl(DimDecl node) {
            return nameString(node.getDim());
        }
        public String forUnitDecl(UnitDecl node) {
            List<Id> ids = node.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return nameString(ids.get(0));
        }
        public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
            return nameString(node.getName());
        }
        public String forSimpleName(SimpleName node) {
            return nameString(node);
        }
        public String forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl node) {
            return node.getName().getText();
        }
        public String for_RewriteObjectExpr(_RewriteObjectExpr node) {
            return node.getGenSymName();
        }
        public String forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl node) {
            return node.getName().getText();
        }
        public String forTypeAlias(TypeAlias node) {
            return node.getName().getText();
        }
        public String defaultCase(Node node) {
            return node.getClass().getSimpleName();
        }
    };


    /* stringName **********************************************************/
    public static String stringName(Node the_node) {
        return the_node.accept(stringNameVisitor);
    }

    /* stringNames *********************************************************/
    public static IterableOnce<String> stringNames(LValue lv) {
        return lv.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forLValueBind(LValueBind d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forUnpastingBind(UnpastingBind d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forUnpastingSplit(UnpastingSplit d) {
                return new IterableOnceForLValueList(d.getElems());
            }
        });
    }

    public static IterableOnce<String> stringNames(AbsDeclOrDecl decl) {
        return decl.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forAbsExternalSyntax(AbsExternalSyntax d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forDimDecl(DimDecl d) {
                return new UnitIterable<String>(d.getDim().getText());
            }
            public IterableOnce<String> forUnitDecl(UnitDecl d) {
            List<Id> ids = d.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return new UnitIterable<String>(nameString(ids.get(0)));
            }
            public IterableOnce<String> forExternalSyntax(ExternalSyntax d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forFnExpr(FnExpr d) {
                return new UnitIterable<String>(nameString(d.getName()));
            }
            public IterableOnce<String> forFnAbsDeclOrDecl(FnAbsDeclOrDecl d) {
                return new UnitIterable<String>(nameString(d.getName()));
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
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> for_RewriteObjectExpr(_RewriteObjectExpr d) {
                return new UnitIterable<String>(d.getGenSymName());
            }
            public IterableOnce<String> forPropertyDecl(PropertyDecl d) {
                return d.getName().apply(new OptionVisitor<Id, IterableOnce<String>>() {
                    public IterableOnce<String> forSome(Id name) {
                        return new UnitIterable<String>(name.getText());
                    }
                    public IterableOnce<String> forNone() {
                        return new UnitIterable<String>("_");
                    }
                });
            }
            public IterableOnce<String> forTestDecl(TestDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forTypeAlias(TypeAlias d) {
                return new UnitIterable<String>(d.getName().getText());
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
        return (T)bug("AST." + s + " NYI");
    }

    /* function ************************************************************/
    public static final Fn<Id, String> IdToStringFn = new Fn<Id, String>() {
            public String apply(Id x) {
                return x.getText();
            }
        };

    public static final Fn<Name, String> NameToStringFn = new Fn<Name, String>() {
        public String apply(Name n) { return nameString(n); }
    };

    /* for APIName ******************************************************/
    public static List<String> toStrings(APIName n) {
        return IterUtil.asList(IterUtil.map(n.getIds(), IdToStringFn));
    }

    /* for TraitTypeWhere **************************************************/
    public static List<TraitType> getTypes(List<TraitTypeWhere> l) {
        List<TraitType> t = new ArrayList<TraitType>(l.size());
        for (TraitTypeWhere tw : l) {
            t.add(tw.getType());
        }
        return t;
    }

    /* for Type and StaticExpr **********************************************/
    public static boolean isExponentiation(Type type) {
        return (type instanceof ArrayType ||
                type instanceof MatrixType ||
                type instanceof ExponentType ||
                type instanceof ExponentDim);
    }
    public static boolean isExponentiation(IntExpr staticExpr) {
        return (staticExpr instanceof ExponentConstraint);
    }
}
