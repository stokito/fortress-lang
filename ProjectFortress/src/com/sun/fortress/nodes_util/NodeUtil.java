/*******************************************************************************
    Copyright 2008,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.ObjectTraitIndex;
import com.sun.fortress.compiler.index.ProperTraitIndex;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.parser_util.FnHeaderFront;
import com.sun.fortress.parser_util.FnHeaderClause;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;

import com.sun.fortress.parser.Fortress;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

public class NodeUtil {

    public static boolean isKeyword(Id id) {
        String name = id.getText();
        return ( ! name.equals("self") &&
                 Fortress.FORTRESS_KEYWORDS.contains(name) );
    }

    public static void log(BufferedWriter writer, Span span, String msg) {
        try {
            if ( writer == null ) {
                String file = span.getFileName();
                writer = Useful.filenameToBufferedWriter( ProjectProperties.preparserErrorLog(file) );
            }
            writer.write( span + ":\n    " + msg + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the parser failed!");
        }
    }

// let join (one : span) (two : span) : span =
//   match one, two with
//     | None, span | span, None -> span
//     | Some (left,_), Some (_,right) -> Some (left,right)

// let span_two (one : 'a node) (two : 'b node) : span =
//   join one.node_span two.node_span

    public static Span spanTwo(Span s1, Span s2) {
        return new Span(s1.getBegin(), s2.getEnd());
    }

    public static Span spanTwo(ASTNode s1, ASTNode s2) {
        return spanTwo(getSpan(s1), getSpan(s2));
    }

    public static Span spanTwo(Expr e1, Expr e2) {
        return spanTwo(getSpan(e1), getSpan(e2));
    }

// let rec span_all (com.sun.fortress.interpreter.nodes : 'a node list) : span =
//   match com.sun.fortress.interpreter.nodes with
//     | [] -> None
//     | node :: rest -> join node.node_span (span_all rest)
    public static Span spanAll(Object[] nodes, int size) {
        if (size == 0)
            return bug("Cannot make a span from an empty list of nodes.");
        else { // size != 0
            return new Span(getSpan((ASTNode)Array.get(nodes,0)).getBegin(),
                            getSpan((ASTNode)Array.get(nodes,size-1)).getEnd());
        }
    }

    public static Span spanAll(Iterable<? extends ASTNode> nodes) {
        if (IterUtil.isEmpty(nodes))
            return bug("Cannot make a span from an empty list of nodes.");
        else {
            return new Span(getSpan(IterUtil.first(nodes)).getBegin(),
                            getSpan(IterUtil.last(nodes)).getEnd());
        }
    }

    public static Span spanAll(SourceLoc defaultLoc, Iterable<? extends ASTNode> nodes) {
        if (IterUtil.isEmpty(nodes)) { return new Span(defaultLoc, defaultLoc); }
        else {
            return new Span(getSpan(IterUtil.first(nodes)).getBegin(),
                            getSpan(IterUtil.last(nodes)).getEnd());
        }
    }

    /* Methods for ASTNode */

    public static ASTNodeInfo getInfo(ASTNode n) {
        return n.getInfo();
    }

    public static Span getSpan(ASTNode n) {
        return n.getInfo().getSpan();
    }

    public static boolean isTraitObjectDecl(ASTNode n) {
        return (n instanceof TraitObjectDecl);
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

    /* Getters for TypeOrPattern */

    public static Span getSpan(TypeOrPattern t) {
        if (t instanceof Type)
            return ((Type)t).getInfo().getSpan();
        else
            return ((Pattern)t).getInfo().getSpan();
    }

    public static Option<Type> optTypeOrPatternToType(Option<TypeOrPattern> tpopt) {
        if (tpopt.isNone()) return Option.<Type>none();
        else {
            TypeOrPattern tp = tpopt.unwrap();
            if (tp instanceof Type)
                return Option.<Type>some((Type)tp);
            else return bug(tp, "Type is expected.");
        }
    }

    public static Option<TypeOrPattern> optTypeToTypeOrPattern(Option<Type> tyopt) {
        if (tyopt.isNone()) return Option.<TypeOrPattern>none();
        else return Option.<TypeOrPattern>some(tyopt.unwrap());
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

    // TODO: we want a way to convert a decl into the corresponding type.
    // However, this requires first having a graceful way to turn staticParams
    // into StaticArgs.
    // public static Type getAsType(TraitObjectDecl t) {
    //     List<StaticParam> params = getStaticParams(t);
    //     List<StatigArg> args = new ArrayList(params.size());
    //     Id name = getName(t);
    //     for (StaticParam sp : params) {
    //         args.add(sp.getName());
    //     }
    //     if (params.size() == 0) {

    // }

    public static Option<WhereClause> getWhereClause(TraitObjectDecl o) {
        return o.getHeader().getWhereClause();
    }

    public static List<TraitTypeWhere> getExtendsClause(TraitObjectDecl t) {
        return t.getHeader().getExtendsClause();
    }

    public static List<BaseType> getExcludesClause(TraitObjectDecl t) {
        if ( t instanceof TraitDecl )
            return ((TraitDecl)t).getExcludesClause();
        else
            return bug("TraitDecl expected, but got " + t);
    }

    public static Option<List<NamedType>> getComprisesClause(TraitObjectDecl t) {
        if ( t instanceof TraitDecl )
            return ((TraitDecl)t).getComprisesClause();
        else
            return bug("TraitDecl expected, but got " + t);
    }

    public static boolean isComprisesEllipses(TraitObjectDecl t) {
        if ( t instanceof TraitDecl )
            return ((TraitDecl)t).isComprisesEllipses();
        else {
            bug("TraitDecl expected, but got " + t);
            return false;
        }
    }

    public static Option<List<Param>> getParams(TraitObjectDecl o) {
        return o.getHeader().getParams();
    }

    /* Getters for TraitDecl */
    public static List<BaseType> getExcludesClause(TraitDecl t) {
        return t.getExcludesClause();
    }

    public static Option<List<NamedType>> getComprisesClause(TraitDecl t) {
        return t.getComprisesClause();
    }

    /* Getters for ObjectDecl */
    public static Option<List<Param>> getParams(ObjectDecl o) {
        return o.getHeader().getParams();
    }

    public static Option<List<Type>> getThrowsClause(ObjectDecl o) {
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

    public static IdOrOp getUnambiguousName(FnDecl f) {
        return f.getUnambiguousName();
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

    public static List<Param> getParams(FnHeader h) {
        return h.getParams();
    }

    public static Type getParamType(Param p) {
        if ( p.getIdType().isSome() && p.getIdType().unwrap() instanceof Type )
            return (Type)p.getIdType().unwrap();
        else if ( p.getVarargsType().isSome() )
            return NodeFactory.makeVarargsType(getSpan(p),
                                               p.getVarargsType().unwrap());
        else
            return bug(getSpan(p) + "\n    Type is not inferred.");
    }

    public static Type getParamType(List<Param> params, Span span) {
        if ( params.isEmpty() ) return NodeFactory.makeVoidType(span);
        else if ( params.size() == 1 ) return getParamType(params.get(0));
        else { // if ( params.size() > 1 )
            List<Type> types = new ArrayList<Type>( params.size() );
            for ( Param p : params ) {
                types.add( getParamType(p) );
            }
            return NodeFactory.makeTupleType(span, types);
        }
    }

    public static Option<Type> getParamType(ObjectDecl o) {
        Option<List<Param>> params = getParams(o);
        if ( params.isSome() ) {
            return Option.<Type>some(getParamType(params.unwrap(), getSpan(o)));
        } else return Option.<Type>none();
    }

    public static Type getHeaderParamType(FnHeader f, Span s) {
        return getParamType(getParams(f), s);
    }

    public static Type getParamType(FnDecl d) {
        return getParamType(getParams(d), getSpan(d));
    }

    public static Option<Type> getReturnType(FnDecl f) {
        return f.getHeader().getReturnType();
    }

    public static Option<Type> getReturnType(FnHeader h) {
        return h.getReturnType();
    }

    public static Option<List<Type>> getThrowsClause(FnDecl f) {
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

    public static Option<List<Type>> getThrowsClause(FnExpr f) {
        return f.getHeader().getThrowsClause();
    }

    /* Getter for Generic */
    public static List<StaticParam> getStaticParams(Generic g) {
        return g.getHeader().getStaticParams();
    }

    /* Getters for ObjectConstructor */
    public static Option<List<Param>> getParams(ObjectConstructor g) {
        return g.getHeader().getParams();
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

    public static Option<Type> getSelfType(ArrowType a) {
        Option<MethodInfo> mi = a.getMethodInfo();
        if (mi.isNone()) return Option.<Type>none();
        return Option.some(mi.unwrap().getSelfType());
    }

    public static Option<Integer> getSelfPosition(ArrowType a) {
        Option<MethodInfo> mi = a.getMethodInfo();
        if (mi.isNone()) return Option.<Integer>none();
        return Option.some(mi.unwrap().getSelfPosition());
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
        if ( line.trim().equals("") )
            return true;
        else {
            String[] split = line.split(" ");
            return beginComment(split[0]);
        }
    }

    /* Getters for VarDecl */
    public static Modifiers getMods(BufferedWriter writer, VarDecl v) {
        if ( v.getLhs().isEmpty() ) {
            log(writer, getSpan(v),
                "Variable declaration does not declare any: " + v);
            return Modifiers.None;
        } else
            return v.getLhs().get(0).getMods();
    }

    /* Getters for LocalVarDecl */
    public static Modifiers getMods(BufferedWriter writer, LocalVarDecl v) {
        if ( v.getLhs().isEmpty() ) {
            log(writer, getSpan(v),
                "Variable declaration does not declare any: " + v);
            return Modifiers.None;
        } else
            return v.getLhs().get(0).getMods();
    }

    /* get the declared name of a component or api */
    public static APIName apiName( String path ) throws UserError, IOException {
        String absolutePath = new File(path).getCanonicalPath();
        try {
            File file = new File(path).getCanonicalFile();
            String filename = new File(path).getName();
            try {
                filename = filename.substring(0, filename.length()-4);
                BufferedReader br = Useful.utf8BufferedFileReader(file);
                // skip comments and blank lines
                int lineNo = 0;
                int commentDepth = 0;
                String line = br.readLine(); lineNo++;
                while ( blankOrComment(line) || commentDepth != 0 ) {
                    if ( line.trim().equals("") ) {
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
                    Span span = NodeFactory.makeSpan(absolutePath,
                                                     lineNo, split[0].length()+2,
                                                     split[0].length()+split[1].length()+1);
                    name = split[1];
                    if ( ! name.equals(filename) )
                        return error(span,
                                     "    Component/API names must match their enclosing file names." +
                                     "\n    File name: " + absolutePath +
                                     "\n    Component/API name: " + name);
                } else name = filename;
                return NodeFactory.makeAPIName(NodeFactory.parserSpan, name);
            } catch (NullPointerException ex) {
                return NodeFactory.makeAPIName(NodeFactory.parserSpan, filename);
            }
        } catch (FileNotFoundException ex) {
            throw new UserError("Cannot find file " + absolutePath);
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
        for (Param p : getParams(d)) {
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
        String name = nameString(getName(app));
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

    public static boolean hasVarargs(Type t) {
        if ( isTupleType(t) )
            return hasVarargs((TupleType)t);
        else {
            bug("TupleType expected, but got " + t);
            return false;
        }
    }

    public static boolean hasKeywords(Type t) {
        if ( isTupleType(t) )
            return hasKeywords((TupleType)t);
        else {
            bug("TupleType expected, but got " + t);
            return false;
        }
    }

    public static boolean hasVarargs(TupleType t) {
        return t.getVarargs().isSome();
    }

    public static boolean hasKeywords(TupleType t) {
        return ! t.getKeywords().isEmpty();
    }

    public static boolean isVarargsParam(Param p) {
        return p.getVarargsType().isSome();
    }

    public static boolean isKeywordParam(Param p) {
        return p.getDefaultExpr().isSome();
    }

    public static boolean isSingletonObject(VarRef v) {
        return (! v.getStaticArgs().isEmpty());
    }

    public static boolean isGenericSingletonType(TraitType t) {
        return (! t.getTraitStaticParams().isEmpty());
    }

    public static boolean isUnderscore(Id id) {
        return (id.getText().equals("_"));
    }

    public static boolean isVoidExpr(Expr e) {
        return ( e instanceof VoidLiteralExpr );
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

    public static boolean isTupleType(Type t) {
        return ( (t instanceof TupleType) && (!isVoidType(t)) );
    }

    public static int getTupleTypeSize(Type t) {
        if ( isTupleType(t) )
            return ((TupleType)t).getElements().size();
        else {
            bug("TupleType expected, but got " + t);
            return 0;
        }
    }

    public static Type getTupleTypeElem(Type t, int i) {
        if ( isTupleType(t) )
            return ((TupleType)t).getElements().get(i);
        else
            return bug("TupleType expected, but got " + t);
    }

    public static boolean differentArity(TupleType first, TupleType second) {
        return ( isVoidType(first) && ! isVoidType(second) ) ||
               ( isVoidType(second) && ! isVoidType(first) ) ||
               ( first.getVarargs().isNone()   && second.getVarargs().isNone() &&
                 first.getKeywords().isEmpty() && second.getKeywords().isEmpty() &&
                 first.getElements().size() != second.getElements().size() );
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

    public static Modifiers getMods(LValue f) {
        return f.getMods();
    }

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
            StringBuilder sb = new StringBuilder();
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
    public static void validTraitTypes(BufferedWriter writer,
                                       List<? extends BaseType> types) {
        for (BaseType ty: types) {
            if ( ty instanceof NamedType &&
                 validOp(((NamedType)ty).getName().getText()) )
                log(writer, getSpan(ty), "Operator name, " +
                    ((NamedType)ty).getName().getText() +
                    ", is not a valid type name.");
        }
    }

    public static void validTraitTypeWheres(BufferedWriter writer,
                                            List<TraitTypeWhere> types) {
        for (TraitTypeWhere typ: types) {
            BaseType ty = typ.getBaseType();
            if ( ty instanceof NamedType &&
                 validOp(((NamedType)ty).getName().getText()) )
                log(writer, getSpan(ty), "Operator name, " +
                    ((NamedType)ty).getName().getText() +
                    ", is not a valid type name.");
        }
    }

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

    /* for Literals ********************************************************/
    public static boolean validRadix(BufferedWriter writer,
                                     Span span, String radix) {
        String[] all = new String[]{"2","3","4","5","6","7","8","9","10",
                                    "11","12","13","14","15","16"};
        List<String> validRadix = new LinkedList<String>(java.util.Arrays.asList(all));
        if (! validRadix.contains( radix )) {
            log(writer, span, "Syntax Error: the radix of " +
                "a numeral must be an integer from 2 to 16.");
            return false;
        } else return true;
    }

    public static boolean validIntLiteral(String numeral) {
        for (int index = 0; index < numeral.length(); index++) {
            if (numeral.charAt(index) == '.')
                return false;
        }
        return true;
    }

    public static boolean validNumericLiteral(BufferedWriter writer,
                                           Span span, String numeral) {
	boolean result = true;
        int numberOfDots = 0;
        for (int index = 0; index < numeral.length(); index++) {
            char c = numeral.charAt(index);
            if (Character.isLetter(c)) {
                log(writer, span, "Syntax Error: a numeral contains " +
                    "letters and does not have a radix specifier.");
		result = false;
		break;
	    }
            if (c == '.') numberOfDots++;
        }
        if (numberOfDots > 1) {
            log(writer, span, "Syntax Error: a numeral contains more " +
                "than one `.' character.");
	    result = false;
	}
	return result;
    }

    public static boolean validNumericLiteral(BufferedWriter writer,
                                           Span span, String numeral,
                                           String radix) {
	boolean result = true;
        int radixNumber = radix2Number(radix);
        if (radixNumber == -1) {
            log(writer, span, "Syntax Error: the radix of " +
                "a numeral should be an integer from 2 to 16.");
	    result = false;
	}
        boolean sawUpperCase = false;
        boolean sawLowerCase = false;
        boolean sawAb = false;
        boolean sawXe = false;
        int numberOfDots = 0;
        for (int index = 0; index < numeral.length(); index++) {
            char c = numeral.charAt(index);
            if (c == '.') numberOfDots++;
            if (Character.isUpperCase(c)) {
                if (sawLowerCase) {
                    log(writer, span, "Syntax Error: a numeral " +
                        "contains both uppercase and lowercase letters.");
		    result = false;
		}
                else sawUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                if (sawUpperCase) {
                    log(writer, span, "Syntax Error: a numeral " +
                        "contains both uppercase and lowercase letters.");
		    result = false;
		}
                else sawLowerCase = true;
            }
            if (radixNumber == 12) {
                if (!validDigitOrLetterIn12(c)
                    && c != '.' && c != '\'' && c != '\u202F') {
		    log(writer, span, "Syntax Error: a numeral " +
                        "has radix 12 and contains letters other " +
                        "than A, B, X, E, a, b, x or e.");
		    result = false;
                }
                if (c == 'A' || c == 'a' || c == 'B' || c == 'b') {
                    if (sawXe) {
                        log(writer, span, "Syntax Error: a numeral " +
                            "has radix 12 and contains at least one " +
                            "A, B, a or b and at least one X, E, x or e.");
			result = false;
		    }
                    else sawAb = true;
                } else if (c == 'X' || c == 'x' || c == 'E' || c == 'e') {
                    if (sawAb) {
                        log(writer, span, "Syntax Error: a numeral " +
                            "has radix 12 and contains at least one " +
                            "A, B, a or b and at least one X, E, x or e.");
			result = false;
		    }
                    else sawXe = true;
                }
            }
            // The numeral has a radix other than 12.
            else if (!validDigitOrLetter(c, radixNumber)
                     && c != '.' && c != '\'' && c != '\u202F') {
                log(writer, span, "Syntax Error: a numeral has a radix " +
                    "specifier and contains a digit or letter that " +
                    "denotes a value greater than or equal to the " +
                    "numeral's radix.");
		result = false;
            }
        }
        if (numberOfDots > 1) {
            log(writer, span, "Syntax Error: a numeral contains more " +
                "than one `.' character.");
	    result = false;
	}
	return result;
    }

    public static int radix2Number(String radix) {
        if (radix.equals("2") || radix.equals("TWO")) {
            return 2;
        } else if (radix.equals("3") || radix.equals("THREE")) {
            return 3;
        } else if (radix.equals("4") || radix.equals("FOUR")) {
            return 4;
        } else if (radix.equals("5") || radix.equals("FIVE")) {
            return 5;
        } else if (radix.equals("6") || radix.equals("SIX")) {
            return 6;
        } else if (radix.equals("7") || radix.equals("SEVEN")) {
            return 7;
        } else if (radix.equals("8") || radix.equals("EIGHT")) {
            return 8;
        } else if (radix.equals("9") || radix.equals("NINE")) {
            return 9;
        } else if (radix.equals("10") || radix.equals("TEN")) {
            return 10;
        } else if (radix.equals("11") || radix.equals("ELEVEN")) {
            return 11;
        } else if (radix.equals("12") || radix.equals("TWELVE")) {
            return 12;
        } else if (radix.equals("13") || radix.equals("THIRTEEN")) {
            return 13;
        } else if (radix.equals("14") || radix.equals("FOURTEEN")) {
            return 14;
        } else if (radix.equals("15") || radix.equals("FIFTEEN")) {
            return 15;
        } else if (radix.equals("16") || radix.equals("SIXTEEN")) {
            return 16;
        } else {
            /* radix is not valid. */
            return -1;
        }
    }

    private static boolean validDigitOrLetterIn12(char c) {
        if (Character.isLetter(c)) {
            switch (c) {
                case 'A':
                case 'a':
                case 'B':
                case 'b':
                case 'X':
                case 'x':
                case 'E':
                case 'e': { break; }
                default: {
                    /* c is not valid in radix 12. */
                    return false;
                }
            }
        }
        return true;
    }

    // radix is not 12.
    private static boolean validDigitOrLetter(char c, int radix) {
        if ((radix < 10 && Character.digit(c, radix) > -1) ||
            (radix >= 10 && Character.isDigit(c)))
            return true;
        switch (c) {
            case 'A':
            case 'a': {
                if (radix <= 10) return false;
                break;
            }
            case 'B':
            case 'b': {
                if (radix <= 11) return false;
                break;
            }
            case 'C':
            case 'c': {
                if (radix <= 12) return false;
                break;
            }
            case 'D':
            case 'd': {
                if (radix <= 13) return false;
                break;
            }
            case 'E':
            case 'e': {
                if (radix <= 14)
                    return false;
                break;
            }
            case 'F':
            case 'f': {
                if (radix <= 15) return false;
                break;
            }
            default: {
                /* c is not valid in a numeral of the radix. */
                return false;
            }
        }
        return true;
    }

    public static void checkSubscriptedAssignment(BufferedWriter writer, Span span,
                                                  FnHeaderFront fhf, FnHeaderClause fhc) {
        if ( fhf.isSubscriptedAssignment && fhc.getReturnType().isSome() &&
             ! isVoidType(fhc.getReturnType().unwrap()) )
            log(writer, span,
                "If a return type is given in a subscripted assignment operator declaration,\n    it must be ().");
    }

    /* Each modifier cannot appear more than once.
     * The modifier abstract should be the last one.
     * The modifiers getter/setter should be the last one.
     */
    public static Modifiers checkModifiers(BufferedWriter writer, final Span span,
                                           List<Modifiers> mods) {
        Modifiers res = Modifiers.None;
        int index = 0;
        for (Modifiers mod : mods) {
            if ( mod.isAbstract() && index > 0 )
                log(writer, span,
                    "Modifier " + mod + " should come first.");
            if ( (mod.isGetter() || mod.isSetter()) &&
                 index != mods.size()-1 )
                log(writer, span,
                    "Modifier " + mod + " should come last.");
            if (res.containsAny(mod)) {
                log(writer, span,
                    "Modifier " + mod + " must not occur multiple times.");
            }
            res = res.combine(mod);
            index++;
        }
        return res;
    }

    public static void checkNoWrapped(BufferedWriter writer,
                                      Option<List<Param>> optParams) {
        if ( optParams.isSome() ) {
            List<Param> params = optParams.unwrap();
            for ( Param param : params ) {
                if (param.getMods().isWrapped()) {
                    log(writer, getSpan(param),
                        "The modifier \"wrapped\" cannot " +
                        "appear in an API.");
                }
            }
        }
    }

    /* true if there exists a self parameter in the parameter list of a given FnDecl */
    public static boolean isFunctionalMethod(FnDecl f) {
        for (Param p : getParams(f)) {
            if (p.getName().getText().equals("self")) return true;
        }
        return false;
    }

    /* true if there exists a self parameter in a given parameter list */
    public static boolean isFunctionalMethod(List<Param> params) {
        for (Param p : params) {
            if (p.getName().getText().equals("self")) return true;
        }
        return false;
    }

    public static void validId(final BufferedWriter writer, Id name) {
        name.accept(new NodeDepthFirstVisitor_void(){
            public void forIdOnly(Id id){
                if (id.getText().equals("outcome"))
                    log(writer, getSpan(id),
                        "Invalid variable name: 'outcome' is a reserved word.");
            }

            public void defaultTemplateGap(TemplateGap g){
                /* nothing */
            }

            public void for_EllipsesIdOnly(_EllipsesId e){
                /* nothing */
            }
        });
    }

    public static void validId(BufferedWriter writer, List<? extends LValue> lvs) {
        for (LValue lv : lvs) {
            validId(writer, lv.getName());
        }
    }

    private static boolean allDigits(String s) {
        for (int index = 0; index < s.length(); index++) {
            if ( ! Character.isDigit(s.charAt(index)) )
                return false;
        }
        return true;
    }

    public static boolean validId(String s) {
        String[] words = s.split("_");
        boolean isNumeral = (words.length == 2 &&
                             (radix2Number(words[1]) != -1 ||
                              allDigits(words[1])));
        return (! validOp(s) && !s.equals("_") &&
                !isNumeral && !s.equals("SUM") && !s.equals("PROD"));
    }

    private static boolean compoundOp(String s) {
        return (s.length() > 1 && s.endsWith("=")
                && !s.equals("<=") && !s.equals(">=")
                && !s.equals("=/=") && !s.equals("==="));
    }
    private static boolean validOpChar(char c) {
        return (c == '_' || java.lang.Character.isUpperCase(c));
    }
    public static boolean validOp(String s) {
        if (s.equals("juxtaposition") || s.equals("in") || s.equals("per") ||
            s.equals("square") || s.equals("cubic") || s.equals("inverse") ||
            s.equals("squared") || s.equals("cubed"))
            return true;
        if (s.equals("SUM") || s.equals("PROD")) return false;
        int length = s.length();
        if (length < 2 || compoundOp(s)) return false;
        char start = s.charAt(0);
        if (length == 2 && start == s.charAt(1)) return false;
        if (length > 2 && start == s.charAt(1) && s.charAt(2) == '_')
            return false;
        if (start == '_' || s.endsWith("_")) return false;
        for (int i = 0; i < length; i++) {
            if (!validOpChar(s.charAt(i))) return false;
        }
        return true;
    }

    public static void checkParams(BufferedWriter writer,
                                   List<Param> params) {
        boolean seenKeyword = false;
        boolean seenVarargs = false;
        for ( Param p : params ) {
            if ( isVarargsParam(p) ) {
                seenVarargs = true;
                if ( seenKeyword )
                    log(writer, getSpan(p),
                        "Keyword parameters should come after varargs parameters.");
            } else if ( isKeywordParam(p) ) {
                seenKeyword = true;
            } else { // normal parameter
                if ( seenVarargs || seenKeyword )
                    log(writer, getSpan(p),
                        "Keyword parameters and varargs parameters should come after normal parameters.");
            }
        }
    }

    public static boolean hasKeywordParam(List<Param> params) {
        for ( Param p : params ) {
            if ( isKeywordParam(p) ) return true;
        }
        return false;
    }

    public static Param checkAbsParam(BufferedWriter writer, Param p) {
        if ( p.getIdType().isNone() && p.getVarargsType().isNone() )
            return NodeFactory.makeAbsParam(NodeFactory.makeVarType(getSpan(p), p.getName()));
        else
            return p;
    }

    public static List<Param> checkAbsParams(BufferedWriter writer,
                                             List<Param> params) {
        List<Param> absParams = new ArrayList<Param>(params.size());
        for ( Param p : params ) {
            absParams.add(checkAbsParam(writer, p));
        }
        return absParams;
    }

    // AbsVarDecl / GetterSetterDecl / PropertyDecl
    private static boolean isFieldLike(Decl d) {
        return ( d instanceof VarDecl ||
                 d instanceof FnDecl &&
                 (isGetter((FnDecl)d) || isSetter((FnDecl)d)) ||
                 d instanceof PropertyDecl );
    }

    public static boolean isCoercion(Decl d) {
        return ( d instanceof FnDecl &&
                 getName((FnDecl)d) instanceof Id &&
                 ((Id)getName((FnDecl)d)).getText().equals(NamingCzar.COERCION_NAME));
    }

    /**
     * Coercion declarations are moved and renamed to top-level functions. This
     * method will determine if the given decl is one such function.
     */
    public static boolean isLiftedCoercion(Decl d) {
      return ( d instanceof FnDecl &&
          getName((FnDecl)d) instanceof Id &&
          ((Id)getName((FnDecl)d)).getText().startsWith(NamingCzar.LIFTED_COERCION_PREFIX));
    }

    public static void checkMembers(BufferedWriter writer, List<Decl> members) {
        boolean seenField = false;
        boolean seenMethod = false;
        for ( Decl d : members ) {
            if ( isCoercion(d) ) {
                if ( seenField || seenMethod ) {
                    log(writer, getSpan(d),
                        "Coercion declarations should come first.");
                }
            } else if ( isFieldLike(d) ) {
                seenField = true;
                if ( seenMethod )
                    log(writer, getSpan(d),
                        "Field/getter/setter declarations should come " +
                        "before method declarations.");
            } else // method and property declarations
                seenMethod = true;
        }
    }

    public static void checkAbsMembers(BufferedWriter writer, List<Decl> members) {
        checkMembers(writer, members);
        // checked by the parser
        // absDecls(writer, members);
    }

    private static void absDecls(BufferedWriter writer, List<Decl> members) {
        for ( Decl d : members ) {
            if ( d instanceof VarDecl &&
                 ((VarDecl)d).getInit().isSome() ||
                 d instanceof FnDecl &&
                 ((FnDecl)d).getBody().isSome() )
                log(writer, getSpan(d),
                    "Declarations in APIs should not have defining expressions.");
        }
    }

    /* No throws clause
     * No requires clause
     */
    public static void checkCoercionClauses(BufferedWriter writer, Span span,
                                            FnHeaderClause fnh) {
        if ( fnh.getThrowsClause().isSome() )
            log(writer, span, "Coercion declarations are not allowed to have " +
                "throws clauses.");
        if ( fnh.getContractClause().isSome() &&
             fnh.getContractClause().unwrap().getRequiresClause().isSome() )
            log(writer, span, "Coercion declarations are not allowed to have " +
                "requires clauses.");
    }

    /* No coercion constraint: Type widens or coerces Type */
    public static void checkWhereClauses(BufferedWriter writer,
                                         Option<WhereClause> where) {
        if ( where.isSome() ) checkWhereClauses(writer, where.unwrap());
    }
    public static void checkWhereClauses(BufferedWriter writer,
                                         WhereClause where) {
        for ( WhereConstraint c : where.getConstraints() ) {
            if ( c instanceof WhereCoerces &&
                 ((WhereCoerces)c).isCoerces() &&
                 ((WhereCoerces)c).isWidens() )
                log(writer, getSpan(c), "This where clause constraint is " +
                    "allowed only for coercion declarations.");
        }
    }

    public static FnHeaderFront makeOpHeaderFront(BufferedWriter writer, Span span,
                                                  Op leftOp, String right,
                                                  Option<String> big,
                                                  List<StaticParam> sparams,
                                                  List<Param> params) {
        return makeOpHeaderFront(writer, span, leftOp, right, big, sparams, params, false, Option.<Param>none());
    }

    public static FnHeaderFront makeOpHeaderFront(BufferedWriter writer, Span span,
                                                  Op leftOp, String right,
                                                  Option<String> big,
                                                  List<StaticParam> sparams,
                                                  List<Param> params,
						  boolean subscript,
                                                  Option<Param> opparam) {
        String left  = leftOp.getText();
        if (PrecedenceMap.ONLY.matchedBrackets(left, right) ||
            (left.equals("{|->") || left.equals("{\u21a6") ||
             left.equals("\u007b|->") || left.equals("\u007b\u21a6")) &&
            (right.equals("}") || right.equals("\u007d"))) {
            if (big.isSome()) {
                left  = "BIG " + left;
                right = "BIG " + right;
            }
        } else
            log(writer, getSpan(leftOp),
                "Mismatched enclosing operator definition: " + left + " and " + right);
        IdOrOpOrAnonymousName name =
	    NodeFactory.makeEnclosing(span, left, right, subscript, opparam.isSome()) ;
        return new FnHeaderFront(name, sparams, params, opparam);
    }

    public static FnHeaderFront makeOpHeaderFront(BufferedWriter writer, Span span,
                                                  Op op, Option<String> big,
                                                  Option<List<StaticParam>> sparams,
                                                  List<Param> params) {
        if (big.isSome())
            op = NodeFactory.makeOpBig(getSpan(op), "BIG " + op.getText());
        else if (op.getText().equals("BIG +") ||
                 op.getText().equals("BIG juxtaposition")) {
        } else if (params.size() == 0) { // nofix
            op = NodeFactory.makeOpNofix(op);
        } else if (isMultifix(params)) { // multifix
            op = NodeFactory.makeOpMultifix(op);
        } else if (params.size() == 1) { // prefix
            op = NodeFactory.makeOpPrefix(op);
        } else if (params.size() == 2) { // infix
            op = NodeFactory.makeOpInfix(op);
        } else { // error
            log(writer, getSpan(op),
                "Operator fixity is invalid in its declaration.");
            op = NodeFactory.makeOpNofix(op);
        }
        if (sparams.isNone())
            return new FnHeaderFront(op, params);
        else
            return new FnHeaderFront(op, sparams.unwrap(), params);
    }

    public static boolean isTraitOrObject(TypeConsIndex t) {
        return (t instanceof TraitIndex);
    }

    public static boolean isTrait(TypeConsIndex t) {
        return (t instanceof ProperTraitIndex);
    }

    public static boolean isObject(TypeConsIndex t) {
        return (t instanceof ObjectTraitIndex);
    }

    public static TraitObjectDecl getDecl(TypeConsIndex t) {
        if ( isTraitOrObject(t) )
            return ((TraitIndex)t).ast();
        else
            return bug("TraitIndex expected, but got " + t);
    }

    public static List<StaticParam> getStaticParameters(TypeConsIndex t) {
        if ( isTraitOrObject(t) )
            return ((TraitIndex)t).staticParameters();
        else
            return bug("TraitIndex expected, but got " + t);
    }

    public static boolean isFunctionalMethod(Function f) {
        return (f instanceof FunctionalMethod);
    }

    public static Id getDeclaringTrait(Function f) {
        if ( isFunctionalMethod(f) )
            return ((FunctionalMethod)f).declaringTrait();
        else
            return bug("Function expected, but got " + f);
    }

    public static FnDecl getDecl(Function f) {
        if ( isFunctionalMethod(f) )
            return ((FunctionalMethod)f).ast();
        else
            return bug("Function expected, but got " + f);
    }

    public static boolean isTraitType(Type t) {
        return (t instanceof TraitType);
    }
    
    /** Returns a String of the entire node AST. */
    public static String toStringAst(Node x) throws IOException {
      int indent = 2;
      OutputStream fout = new ByteArrayOutputStream();
      BufferedWriter utf8out =
          new BufferedWriter(new OutputStreamWriter(fout, Charset.forName("UTF-8")));
      TreeWalker tpw = new FortressSerializationWalker(utf8out, indent);
      x.walk(tpw);
      utf8out.write('\n');
      utf8out.flush();
      return fout.toString();
    }

    public static ArrowType genericArrowFromDecl(FnDecl decl) {
        return NodeFactory.makeArrowType(getSpan(decl), false,
                             domainFromParams(getParams(decl)),
                             // all types have been filled in at this point
                             getReturnType(decl).unwrap(),
                             NodeFactory.makeEffect(getSpan(decl).getEnd(),
                                        getThrowsClause(decl)),
                             getStaticParams(decl),
                             getWhereClause(decl));
    }

    /**
     * Get a domain from a list of params.
     */
    static Type domainFromParams(List<Param> params) {
        List<Type> paramTypes = new ArrayList<Type>();
        List<KeywordType> keywordTypes = new ArrayList<KeywordType>();
        Option<Type> varargsType = Option.<Type>none();

        for (Param param: params) {
            if ( ! isVarargsParam(param) ) {
                Option<Type> maybeType = optTypeOrPatternToType(param.getIdType());

                if (maybeType.isSome()) { // An explicit type is declared.
                    if (param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(NodeFactory.makeKeywordType(param.getName(), maybeType.unwrap()));
                    } else { // We have an ordinary param.
                        paramTypes.add(maybeType.unwrap());
                    }
                } else { // No type is explicitly declared for this parameter.
                    if (param.getDefaultExpr().isSome()) { // We have a keyword param.
                        keywordTypes.add(NodeFactory.makeKeywordType(param.getName(), NodeFactory.make_InferenceVarType(getSpan(param))));
                    } else { // We have an ordinary param.
                        paramTypes.add(NodeFactory.make_InferenceVarType(getSpan(param)));
                    }
                }
            } else { // We have a varargs param.
                varargsType = Option.<Type>some(param.getVarargsType().unwrap());
            }
        }

        return NodeFactory.makeDomain(NodeFactory.makeSpan("TypeEnv_bogus_span_for_empty_list", params), paramTypes, varargsType, keywordTypes);
    }

    public static int selfParameterIndex(List<Param> params) {
        int selfIndex = Naming.NO_SELF;
        int i = 0;
        for (Param p : params) {
            if (p.getName().getText() == "self") {
                selfIndex = i;
                break;
            }
            i++;
        }
        return selfIndex;
    }

    public static boolean hasSelfParameter(List<Param> params) {
        for (Param p : params) {
            if (p.getName().getText() == "self") return true;
        }
        return false;
    }

}
