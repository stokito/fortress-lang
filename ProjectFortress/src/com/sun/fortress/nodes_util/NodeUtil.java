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
import java.util.ArrayList;
import java.util.Iterator;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.interpreter.evaluator.values.Overload;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.NativeApplicable;
import com.sun.fortress.interpreter.glue.WellKnownNames;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class NodeUtil {

    public static final String defaultSelfName = WellKnownNames.defaultSelfName;

    
    public static Iterable<Id> getIds(final QualifiedIdName qName) {
        return qName.getApi().apply(new OptionVisitor<DottedName, Iterable<Id>>() {
            public Iterable<Id> forSome(DottedName apiName) {
                return IterUtil.compose(apiName.getIds(), qName.getName().getId());
            }
            public Iterable<Id> forNone() {
                return IterUtil.singleton(qName.getName().getId());
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
        // Bit of a hack, we want to get rid NativeApplicable if we can
        if (d instanceof NativeApplicable)
            return -1;
        int i = 0;
        for (Param p : d.getParams()) {
            IdName name = p.getName();
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

    private final static NodeVisitor<String> nameGetter =
        new NodeAbstractVisitor<String>() {
        
        @Override public String forDottedName(DottedName n) {
            return nameString(n);
            }
        @Override public String forQualifiedName(QualifiedName n) {
            return nameString(n);
            }
        public String forIdName(IdName n) { return n.getId().getText(); }
        public String forOpr(Opr n) { return n.getOp().getText(); }
        public String forPostFix(PostFix n) { return n.getOp().getText(); }
        public String forEnclosing(Enclosing n) { return n.getOpen().getText(); }
        public String forSubscriptOp(SubscriptOp n) { return "[]"; }
        public String forSubscriptAssign(SubscriptAssign n) { return "[]="; }
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

    public static String nameString(IdName n) {
        return n.getId().getText();
    }
    
    public static String nameString(Opr n) {
        return n.getOp().getText();
    }
    
    public static String nameString(DottedName n) {
        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
        return IterUtil.toString(ns, "", ".", "");
    }
    
    public static String nameString(QualifiedName n) {
        final String last = n.getName().accept(nameGetter);
        return n.getApi().apply(new OptionVisitor<DottedName, String>() {
            public String forSome(DottedName api) {
                return nameString(api) + "." + last;
            }
            public String forNone() { return last; }
        });
    }
    
    public static String namesString(Iterable<? extends Name> names) {
        return IterUtil.toString(IterUtil.map(names, NameToStringFn), "", ", ", "");
    }
    

    /* getName *************************************************************/
    public static String getName(StaticParam param) {
        return param.accept(new NodeAbstractVisitor<String>() {
            public String forBoolParam(BoolParam p) {
                return p.getName().getId().getText();
            }
            public String forDimensionParam(DimensionParam p) {
                return p.getName().getId().getText();
            }
            public String forIntParam(IntParam p) {
                return p.getName().getId().getText();
            }
            public String forNatParam(NatParam p) {
                return p.getName().getId().getText();
            }
            public String forOperatorParam(OperatorParam p) {
                return nameString(p.getName());
            }
            public String forSimpleTypeParam(SimpleTypeParam p) {
                return p.getName().getId().getText();
            }
            public String forUnitParam(UnitParam p) {
                return p.getName().getId().getText();
            }
        });
    }

    /* stringName **********************************************************/
    public static String stringName(Node the_node) {
        return the_node.accept(new NodeAbstractVisitor<String>() {
            public String forDimUnitDecl(DimUnitDecl node) {
                if (node.getDim().isSome()) {
                    if (node.getUnits().isEmpty())
                        return Option.unwrap(node.getDim()).getId().getText();
                    else
                        return Option.unwrap(node.getDim()).getId().getText() + " and " +
                               Useful.listInDelimiters("", node.getUnits(), "");
                } else
                    return Useful.listInDelimiters("", node.getUnits(), "");
            }
            public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
                return nameString(node.getName());
            }
            public String forFnName(FnName node) {
                return nameString(node);
            }
            public String forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl node) {
                return node.getName().getId().getText();
            }
            public String for_RewriteObjectExpr(_RewriteObjectExpr node) {
                return node.getGenSymName();
            }
            public String forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl node) {
                return node.getName().getId().getText();
            }
            public String forTypeAlias(TypeAlias node) {
                return node.getName().getId().getText();
            }
            public String defaultCase(Node node) {
                return node.getClass().getSimpleName();
            }
        });
    }

    /* stringNames *********************************************************/
    public static IterableOnce<String> stringNames(LValue lv) {
        return lv.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forLValueBind(LValueBind d) {
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> forUnpastingBind(UnpastingBind d) {
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> forUnpastingSplit(UnpastingSplit d) {
                return new IterableOnceForLValueList(d.getElems());
            }
        });
    }

    public static IterableOnce<String> stringNames(AbsDeclOrDecl decl) {
        return decl.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forAbsExternalSyntax(AbsExternalSyntax d) {
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> forDimUnitDecl(DimUnitDecl d) {
                if (d.getDim().isSome()) {
                    if (d.getUnits().isEmpty())
                        return new UnitIterable<String>(
                           Option.unwrap(d.getDim()).getId().getText());
                    else
                        return bug(d, "DimUnitDecl represents both a dimension declaration and a unit declaration.");
                } else
                    return new IterableOnceTranslatingList<Name, String>(
                           d.getUnits(), NameToStringFn);
            }
            public IterableOnce<String> forExternalSyntax(ExternalSyntax d) {
                return new UnitIterable<String>(d.getName().getId().getText());
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
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> for_RewriteObjectExpr(_RewriteObjectExpr d) {
                return new UnitIterable<String>(d.getGenSymName());
            }
            public IterableOnce<String> forPropertyDecl(PropertyDecl d) {
                return d.getName().apply(new OptionVisitor<IdName, IterableOnce<String>>() {
                    public IterableOnce<String> forSome(IdName name) {
                        return new UnitIterable<String>(name.getId().getText());
                    }
                    public IterableOnce<String> forNone() {
                        return new UnitIterable<String>("_");
                    }
                });
            }
            public IterableOnce<String> forTestDecl(TestDecl d) {
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getName().getId().getText());
            }
            public IterableOnce<String> forTypeAlias(TypeAlias d) {
                return new UnitIterable<String>(d.getName().getId().getText());
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

    /* for DottedName ******************************************************/
    public static List<String> toStrings(DottedName n) {
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
}
