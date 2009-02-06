/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.shell.UserError;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

public class NodeUtil {

    /* Getters for ASTNode */

    public static ASTNodeInfo getInfo(ASTNode n) {
        return n.getInfo();
    }

    public static Span getSpan(ASTNode n) {
        return n.getInfo().getSpan();
    }

    /* Getters for Expr */

    public static Span getSpan(Expr e) {
        return e.getInfo().getSpan();
    }

    public static boolean isParenthesized(Expr e) {
        return e.getInfo().isParenthesized();
    }

    public static Option<Type> getExprType(Expr e) {
        return e.getInfo().getExprType();
    }

    /* Getters for Type */

    public static Span getSpan(Type t) {
        return t.getInfo().getSpan();
    }

    public static boolean isParenthesized(Type t) {
        return t.getInfo().isParenthesized();
    }

    public static List<StaticParam> getStaticParams(Type t) {
        return t.getInfo().getStaticParams();
    }

    public static Option<WhereClause> getWhereClause(Type t) {
        return t.getInfo().getWhereClause();
    }

    /* Getters for TraitObjectDecl */

    public static Modifiers getMods(TraitObjectDecl t) {
        return t.getHeader().getMods();
    }

    public static Id getName(TraitObjectDecl t) {
        return (Id)t.getHeader().getName();
    }

    public static List<Decl> getDecls(TraitObjectDecl t) {
        return t.getHeader().getDecls();
    }

    public static List<StaticParam> getStaticParams(TraitObjectDecl t) {
        return t.getHeader().getStaticParams();
    }

    public static Option<WhereClause> getWhereClause(TraitObjectDecl o) {
        return o.getHeader().getWhereClause();
    }

    public static List<TraitTypeWhere> getExtendsClause(TraitObjectDecl t) {
        return t.getHeader().getExtendsClause();
    }

    /* Getters for TraitDecl */
    public static List<BaseType> getExcludesClause(TraitDecl t) {
        return t.getExcludesClause();
    }

    public static Option<List<BaseType>> getComprisesClause(TraitDecl t) {
        return t.getComprisesClause();
    }

    /* Getters for ObjectDecl */
    public static Option<List<Param>> getParams(ObjectDecl o) {
        return o.getParams();
    }

    public static Option<List<BaseType>> getThrowsClause(ObjectDecl o) {
        return o.getHeader().getThrowsClause();
    }

    public static Option<Contract> getContract(ObjectDecl o) {
        return o.getHeader().getContract();
    }

    public static List<Decl> getDecls(ObjectDecl o) {
        return o.getHeader().getDecls();
    }

    public static List<TraitTypeWhere> getExtendsClause(ObjectDecl o) {
        return o.getHeader().getExtendsClause();
    }

    /* Getters for ObjectExpr */
    public static List<Decl> getDecls(ObjectExpr o) {
        return o.getHeader().getDecls();
    }

    public static List<TraitTypeWhere> getExtendsClause(ObjectExpr o) {
        return o.getHeader().getExtendsClause();
    }

    /* Getters for FnDecl */
    public static Modifiers getMods(FnDecl f) {
        return f.getHeader().getMods();
    }

    public static IdOrOpOrAnonymousName getName(FnDecl f) {
        return f.getHeader().getName();
    }

    public static List<StaticParam> getStaticParams(FnDecl f) {
        return f.getHeader().getStaticParams();
    }

    public static Option<WhereClause> getWhereClause(FnDecl f) {
        return f.getHeader().getWhereClause();
    }

    public static List<Param> getParams(FnDecl f) {
        return f.getHeader().getParams();
    }

    public static Option<Type> getReturnType(FnDecl f) {
        return f.getHeader().getReturnType();
    }

    public static Option<List<BaseType>> getThrowsClause(FnDecl f) {
        return f.getHeader().getThrowsClause();
    }

    public static Option<Contract> getContract(FnDecl f) {
        return f.getHeader().getContract();
    }

    /* Getters for FnExpr */
    public static IdOrOpOrAnonymousName getName(FnExpr f) {
        return f.getHeader().getName();
    }

    public static List<StaticParam> getStaticParams(FnExpr f) {
        return f.getHeader().getStaticParams();
    }

    public static Option<WhereClause> getWhereClause(FnExpr f) {
        return f.getHeader().getWhereClause();
    }

    public static List<Param> getParams(FnExpr f) {
        return f.getHeader().getParams();
    }

    public static Option<Type> getReturnType(FnExpr f) {
        return f.getHeader().getReturnType();
    }

    public static Option<List<BaseType>> getThrowsClause(FnExpr f) {
        return f.getHeader().getThrowsClause();
    }

    /* Getter for Generic */
    public static List<StaticParam> getStaticParams(Generic g) {
        return g.getHeader().getStaticParams();
    }

    /* Getters for ObjectConstructor */
    public static Option<List<Param>> getParams(ObjectConstructor g) {
        return g.getParams();
    }

    public static List<Decl> getDecls(ObjectConstructor g) {
        return g.getHeader().getDecls();
    }

    public static List<TraitTypeWhere> getExtendsClause(ObjectConstructor g) {
        return g.getHeader().getExtendsClause();
    }

    /* Getters for Applicable */
    public static IdOrOpOrAnonymousName getName(Applicable a) {
        return a.getHeader().getName();
    }

    public static List<StaticParam> getStaticParams(Applicable a) {
        return a.getHeader().getStaticParams();
    }

    public static List<Param> getParams(Applicable a) {
        return a.getHeader().getParams();
    }

    public static Option<Type> getReturnType(Applicable a) {
        return a.getHeader().getReturnType();
    }

    public static Option<WhereClause> getWhereClause(Applicable a) {
        return a.getHeader().getWhereClause();
    }

    public static boolean isOpParam(StaticParam p) {
        return p.getKind() instanceof KindOp;
    }

    public static boolean isTypeParam(StaticParam p) {
        return p.getKind() instanceof KindType;
    }

    public static boolean isIntParam(StaticParam p) {
        return ( p.getKind() instanceof KindInt ||
                 p.getKind() instanceof KindNat );
    }

    public static boolean isNatParam(StaticParam p) {
        return p.getKind() instanceof KindNat;
    }

    public static boolean isBoolParam(StaticParam p) {
        return p.getKind() instanceof KindBool;
    }

    public static boolean isDimParam(StaticParam p) {
        return p.getKind() instanceof KindDim;
    }

    public static boolean isUnitParam(StaticParam p) {
        return p.getKind() instanceof KindUnit;
    }

    public static List<_RewriteObjectExpr> getObjectExprs(Component comp) {
        for ( Decl d : comp.getDecls() ) {
            if ( d instanceof _RewriteObjectExprDecl )
                return ((_RewriteObjectExprDecl)d).getObjectExprs();
        }
        return new ArrayList<_RewriteObjectExpr>();
    }

    public static List<String> getFunctionalMethodNames(Component comp) {
        for ( Decl d : comp.getDecls() ) {
            if ( d instanceof _RewriteFunctionalMethodDecl )
                return ((_RewriteFunctionalMethodDecl)d).getFunctionalMethodNames();
        }
        return new ArrayList<String>();
    }

    private static boolean beginComment( String token ) {
        return (token.length() >= 2 &&
                token.substring(0,2).equals("(*"));
    }

    private static boolean endComment( String token ) {
        return (token.length() >= 2 &&
                token.substring(token.length()-2,token.length()).equals("*)"));
    }

    private static boolean lineComment( String token ) {
        return (token.length() == 3 &&
                token.equals("(*)"));
    }

    private static boolean blankOrComment( String line ) {
        if ( line.equals("") )
            return true;
        else {
            String[] split = line.split(" ");
            return beginComment(split[0]);
        }
    }

    /* get the declared name of a component or api */
    public static APIName apiName( String path ) throws UserError, IOException {
        try {
            File file = new File(path).getCanonicalFile();
            String filename = new File(path).getName();
            filename = filename.substring(0, filename.length()-4);
            BufferedReader br = Useful.utf8BufferedFileReader(file);
            // skip comments and blank lines
            int lineNo = 0;
            int commentDepth = 0;
            String line = br.readLine(); lineNo++;
            while ( blankOrComment(line) || commentDepth != 0 ) {
                if ( line.equals("") ) {
                    line = br.readLine(); lineNo++;
                } else {
                    String[] split = line.split(" ");
                    for ( String token : split ) {
                        if ( lineComment(token) ) {
                            line = "";
                            break;
                        } else if ( beginComment(token) ) {
                            commentDepth++; line = "";
                        } else if ( endComment(token) ) {
                            commentDepth--; line = "";
                        } else if ( commentDepth == 0 ) {
                            line += token;
                            line += " ";
                        }
                    }
                    if ( commentDepth == 0 && ! line.equals("") ) break;
                    else line = br.readLine(); lineNo++;
                }
            }
            br.close();
            // line is the first non-comment/non-blank line
            String[] split = line.split(" ");
            String name;
            if ( split[0].equals("component") || split[0].equals("api") ) {
                Span span = NodeFactory.makeSpan(path, lineNo, split[0].length()+2,
                                                 split[0].length()+split[1].length()+1);
                name = split[1];
                if ( ! name.equals(filename) )
                    return error(ExprFactory.makeVoidLiteralExpr(span),
                                 "    Component/API names must match their enclosing file names." +
                                 "\n    File name: " + path +
                                 "\n    Component/API name: " + name);
            }
           else
               name = filename;
            return NodeFactory.makeAPIName(NodeFactory.parserSpan, name);
        } catch (FileNotFoundException ex) {
            throw new UserError("Can't find file " + path);
        }
    }

    public static Iterable<Id> getIds(final Id qName) {
        return qName.getApiName().apply(new OptionVisitor<APIName, Iterable<Id>>() {
            public Iterable<Id> forSome(APIName apiName) {
                return IterUtil.compose(apiName.getIds(), qName);
            }
            public Iterable<Id> forNone() {
                return IterUtil.singleton(qName);
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
        for (Param p : NodeUtil.getParams(d)) {
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
        String name = nameString(NodeUtil.getName(app));
            int spi = selfParameterIndex(app);
            if (spi >= 0)
                return "rm$" + spi + "$" + name;
            else
                return name;

    }

    public static Option<Expr> getBody(Applicable def) {
        if (def instanceof FnDecl) { return ((FnDecl)def).getBody(); }
        else if (def instanceof FnExpr) { return Option.some(((FnExpr)def).getBody()); }
        else { return Option.none(); }
    }

    public static boolean hasVarargs(TupleType t) {
        return (! t.getVarargs().isNone() );
    }

    public static boolean isVarargsParam(Param p) {
        return (! p.getVarargsType().isNone());
    }

    public static boolean isSingletonObject(VarRef v) {
        return (! v.getStaticArgs().isEmpty());
    }

    public static boolean isGenericSingletonType(TraitType t) {
        return (! t.getStaticParams().isEmpty());
    }

    public static boolean isVoidType(Type t) {
        if ( t instanceof TupleType ) {
            TupleType _t = (TupleType)t;
            return ( _t.getElements().isEmpty() &&
                     _t.getVarargs().isNone() &&
                     _t.getKeywords().isEmpty() );
        } else
            return false;
    }

    /* for Param ***********************************************************/
    public static boolean isMultifix(List<Param> params) {
        for (Param p : params) {
            if ( isVarargsParam(p) )
                return true;
        }
        return (params.size() > 2);
    }

    public static boolean isMutable(Param p) {
        return p.getMods().isMutable();
    }

    public final static NodeVisitor<String> nameSuffixGetter =
        new NodeAbstractVisitor<String>() {
        @Override public String forAPIName(APIName n) {
            return n.getText();
            }
        public String forId(Id n) {
            return n.getText();
        }
        public String forOp(Op n) {
            if ( n.isEnclosing() )
                return n.getText();
            else return OprUtil.fixityDecorator(n.getFixity(), n.getText());
        }
        public String forAnonymousFnName(AnonymousFnName n) {
            return getSpan(n).toString();
        }
    };

    public static String nameSuffixString(AbstractNode n) {
        return n.accept(nameSuffixGetter);
    }

    private final static NodeVisitor<String> nameGetter =
        new NodeAbstractVisitor<String>() {

        @Override public String forAPIName(APIName n) {
            return n.getText();
            }
        @Override public String forIdOrOpOrAnonymousName(IdOrOpOrAnonymousName n) {
            return nameString(n);
            }
        public String forId(Id n) {
            final String last = n.getText();
            Option<APIName> odn = n.getApiName();
            return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
        }
        public String forOp(Op n) {
            if ( n.isEnclosing() ) {
                Option<APIName> odn = n.getApiName();
                String last = n.getText();
                return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
            } else {
                Option<APIName> odn = n.getApiName();
                String last = OprUtil.fixityDecorator(n.getFixity(), n.getText());
                return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
            }
        }
        public String forAnonymousFnName(AnonymousFnName n) {
            return getSpan(n).toString();
        }
        public String forConstructorFnName(ConstructorFnName n) {
            // TODO Auto-generated method stub
            return nameString(n);
        }
    };

    /* nameString *************************************************************/
    public static String nameString(Name n) {
        return n.accept(nameGetter);
    }

    public static String nameString(IdOrOpOrAnonymousName n) {
        return n.accept(nameGetter);
    }

    public static String nameString(APIName n) {
        return n.getText();
//        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
//      return IterUtil.toString(ns, "", ".", "");
    }

    public static String dirString(APIName n) {
        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
        // NOT File.separator -- that is unnecessary and confusing.
        return IterUtil.toString(ns, "", "/", "");
    }

    public static String shortNameString(Id n) {
        final String last = n.getText();
        Option<APIName> odn = n.getApiName();
        if (odn.isSome()) {
            APIName _odn = odn.unwrap();
            if (_odn.getText().equals(WellKnownNames.fortressBuiltin())
                || _odn.getText().equals(WellKnownNames.fortressLibrary())) {
                return last;
            } else {
                return nameString(odn.unwrap()) + "." + last;
            }
        } else {
            return last;
        }
    }

    public static String nameString(Id n) {
        final String last = n.getText();
        Option<APIName> odn = n.getApiName();
        return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
    }

    public static String nameString(Op n) {
        if ( n.isEnclosing() )
            return n.getText();
        else return OprUtil.fixityDecorator(n.getFixity(), n.getText());
    }
    public static String nameString(AnonymousFnName n) {
        return getSpan(n).toString();
    }
    public static String nameString(ConstructorFnName n) {
        // TODO Auto-generated method stub
        return stringName(n.getConstructor());
    }

    private static final Fn<Name, String> NameToStringFn = new Fn<Name, String>() {
        public String apply(Name n) { return nameString(n); }
    };

    public static String namesString(Iterable<? extends Name> names) {
        return IterUtil.toString(IterUtil.map(names, NameToStringFn), "", ", ", "");
    }

    /* getName *************************************************************/
    public static String getName(StaticParam param) {
        if ( isOpParam(param) )
            return nameString(param.getName());
        else
            return param.getName().getText();
    }

    private final static NodeAbstractVisitor<String> stringNameVisitor =
        new NodeAbstractVisitor<String>() {
        @Override
            public String forId(Id that) {
                return that.getText();
            }
            @Override
            public String forOp(Op that) {
                if ( that.isEnclosing() )
                    return that.getText();
                else return OprUtil.fixityDecorator(that.getFixity(), that.getText());
            }
            @Override
        public String forDimDecl(DimDecl node) {
            return nameString(node.getDimId());
        }
            @Override
        public String forUnitDecl(UnitDecl node) {
            List<Id> ids = node.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return nameString(ids.get(0));
        }
            @Override
        public String forFnDecl(FnDecl node) {
            return nameString(getName(node));
        }
            @Override
        public String forIdOrOpOrAnonymousName(IdOrOpOrAnonymousName node) {
            return nameString(node);
        }
            @Override
        public String forObjectDecl(ObjectDecl node) {
            return getName(node).getText();
        }
            @Override
        public String for_RewriteObjectExpr(_RewriteObjectExpr node) {
            return node.getGenSymName();
        }
            @Override
        public String forTraitDecl(TraitDecl node) {
            return getName(node).getText();
        }
            @Override
        public String forTypeAlias(TypeAlias node) {
            return node.getName().getText();
        }
            @Override
        public String forOpRef(OpRef node) {
            return node.getOriginalName().accept(this);
        }
            @Override
        public String forVarRef(VarRef node) {
            return node.getVarId().accept(this);
        }

            @Override
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
        return new UnitIterable<String>(lv.getName().getText());
    }

    public static IterableOnce<String> stringNames(Decl decl) {
        return decl.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forDimDecl(DimDecl d) {
                return new UnitIterable<String>(d.getDimId().getText());
            }
            public IterableOnce<String> forUnitDecl(UnitDecl d) {
            List<Id> ids = d.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return new UnitIterable<String>(nameString(ids.get(0)));
            }
            public IterableOnce<String> forFnExpr(FnExpr d) {
                return new UnitIterable<String>(nameString(d.getHeader().getName()));
            }
            public IterableOnce<String> forFnDecl(FnDecl d) {
                return new UnitIterable<String>(nameString(getName(d)));
            }
            public IterableOnce<String> forLetFn(LetFn d) {
                return new UnitIterable<String>(d.getClass().getSimpleName());
            }
            public IterableOnce<String> forLocalVarDecl(LocalVarDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
            public IterableOnce<String> forObjectDecl(ObjectDecl d) {
                return new UnitIterable<String>(getName(d).getText());
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
            public IterableOnce<String> forTraitDecl(TraitDecl d) {
                return new UnitIterable<String>(getName(d).getText());
            }
            public IterableOnce<String> forTypeAlias(TypeAlias d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forVarDecl(VarDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
            public IterableOnce<String> forGrammarDecl(GrammarDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl d) {
                return new UnitIterable<String>(nameString(d.getName()));
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

    /* Boolean functions for FnDecls. */
    public static boolean isGetter(FnDecl decl) {
        return getMods(decl).isGetter();
    }

    public static boolean isSetter(FnDecl decl) {
        return getMods(decl).isSetter();
    }

    public static boolean isOp(FnDecl decl) {
        return getName(decl) instanceof Op;
    }

    /* for APIName ******************************************************/
    private static final Fn<Id, String> IdToStringFn = new Fn<Id, String>() {
        public String apply(Id x) {
            return stringName(x);
        }
    };

    public static List<String> toStrings(APIName n) {
        return Useful.applyToAll(n.getIds(), IdToStringFn);
    }

    /* for TraitTypeWhere **************************************************/
    public static List<BaseType> getTypes(List<TraitTypeWhere> l) {
        List<BaseType> t = new ArrayList<BaseType>(l.size());
        for (TraitTypeWhere tw : l) {
            t.add(tw.getBaseType());
        }
        return t;
    }

    /* for Type and StaticExpr **********************************************/
    public static boolean isExponentiation(Type type) {
        return (type instanceof ArrayType ||
                type instanceof MatrixType ||
                type instanceof DimExponent);
    }
    public static boolean isExponentiation(IntExpr staticExpr) {
        return (staticExpr instanceof IntBinaryOp &&
                ((IntBinaryOp)staticExpr).getOp().getText().equals("^"));
    }

    public static String nameString(BoolRef vre) {
        return nameString(vre.getName());
    }
    public static String nameString(IntRef vre) {
        return nameString(vre.getName());
    }
}
