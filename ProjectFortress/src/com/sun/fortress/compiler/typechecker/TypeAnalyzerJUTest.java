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

package com.sun.fortress.compiler.typechecker;

import junit.framework.TestCase;
import java.util.*;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;
import edu.rice.cs.plt.text.TextUtil;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static com.sun.fortress.compiler.typechecker.ConstraintFormula.TRUE;
import static com.sun.fortress.compiler.typechecker.ConstraintFormula.FALSE;
import static com.sun.fortress.compiler.Types.*;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

public class TypeAnalyzerJUTest extends TestCase {

    public static void main(String... args) {
      junit.textui.TestRunner.run(TypeAnalyzerJUTest.class);
    }

    private static ConstraintFormula sub(TypeAnalyzer ta, String s, String t) {
        return ta.subtype(parseType(s), parseType(t));
    }

    private static ConstraintFormula sub(TypeAnalyzer ta, String s, Type t) {
        return ta.subtype(parseType(s), t);
    }

    private static ConstraintFormula sub(TypeAnalyzer ta, Type s, String t) {
        return ta.subtype(s, parseType(t));
    }

    private static ConstraintFormula sub(TypeAnalyzer ta, Type s, Type t) {
        return ta.subtype(s, t);
    }
    
    private static Type norm(TypeAnalyzer ta, String t) {
        return ta.normalize(parseType(t));
    }
    
    public void testNormalize() {
        debug.logStart(); try {
        
        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "B"),
                                      trait("E"),
                                      trait("F"),
                                      trait("G"));
        
        assertEquals(type("A"), norm(t, "A"));
        assertEquals(type("Any"), norm(t, "Any"));
        assertEquals(type("Bottom"), norm(t, "Bottom"));
        assertEquals(type("()"), norm(t, "()"));
        
        assertEquals(type("(A,B)"), norm(t, "(A,B)"));
        assertEquals(type("Bottom"), norm(t, "(A,Bottom)"));
        assertEquals(type("(A,Any)"), norm(t, "(A,Any)"));
        assertEquals(type("|{(A,E),(A,F),(A,G)}"), norm(t, "(A, |{E,F,G})"));
        assertEquals(type("|{(C,C,C),(C,C,D),(C,D,C),(C,D,D),(D,C,C),(D,C,D),(D,D,C),(D,D,D)}"),
                     norm(t, "(C|D, C|D, C|D)"));
        assertEquals(type("&{(A,E),(A,F),(A,G)}"), norm(t, "(A, &{E,F,G})"));
        assertEquals(type("&{(C,C,C),(C,C,D),(C,D,C),(C,D,D),(D,C,C),(D,C,D),(D,D,C),(D,D,D)}"),
                     norm(t, "(C&D, C&D, C&D)"));
        assertEquals(type("|{(C,E)&(C,F), (D,E)&(D,F)}"), norm(t, "(C|D,E&F)"));
        
        assertEquals(type("(A,B...)"), norm(t, "(A,B...)"));
        assertEquals(type("A"), norm(t, "(A,Bottom...)"));
        assertEquals(type("(A,Any...)"), norm(t, "(A,Any...)"));
        assertEquals(type("|{(A,E...),(A,F...),(A,G...)}"), norm(t, "(A, |{E,F,G}...)"));
        assertEquals(type("&{(A,E...),(A,F...),(A,G...)}"), norm(t, "(A, &{E,F,G}...)"));
        assertEquals(type("|{(C,E...)&(C,F...), (D,E...)&(D,F...)}"), norm(t, "(C|D,E&F...)"));
        
        assertEquals(type("A->Any"), norm(t, "A->Any"));
        assertEquals(type("Any->A"), norm(t, "Any->A"));
        assertEquals(type("A->Bottom"), norm(t, "A->Bottom"));
        assertEquals(type("Any"), norm(t, "Bottom->A"));
        assertEquals(type("A->B"), norm(t, "A->(B|C)"));
        assertEquals(type("A->(C|D)"), norm(t, "A->(C|D)"));
        assertEquals(type("A->C"), norm(t, "A->(B&C)"));
        assertEquals(type("(A->C)&(A->D)"), norm(t, "A->(C&D)"));
        assertEquals(type("B->A"), norm(t, "(B|C)->A"));
        assertEquals(type("(C->A)&(D->A)"), norm(t, "(C|D)->A"));
        assertEquals(type("C->A"), norm(t, "(B&C)->A"));
        assertEquals(type("(C&D)->A"), norm(t, "(C&D)->A"));
        assertEquals(type("&{C->C,C->D,D->C,D->D}"), norm(t, "(C|D)->(C&D)"));
        assertEquals(type("&{A->(C|E),A->(C|F),A->(D|E),A->(D|F)}"), norm(t, "A->(C&D)|(E&F)"));
        assertEquals(type("A->C throws E io"), norm(t, "A->C throws E io"));
        assertEquals(type("Any"), norm(t, "Bottom->C throws E io"));
        assertEquals(type("&{(A,C...)->E,(A,D...)->E}"), norm(t, "(A, (C|D)...)->E"));
        assertEquals(type("((A,C...)&(A,D...))->E"), norm(t, "(A, (C&D)...)->E"));
        assertEquals(type("(C&D, foo=E&F, bar=G)->A"), norm(t, "(C&D, foo=E&F, bar=G)->A"));
        assertEquals(type("&{(C,foo=E,bar=G)->A,(C,foo=F,bar=G)->A,(D,foo=E,bar=G)->A,(D,foo=F,bar=G)->A}"),
                          norm(t, "(C|D, foo=E|F, bar=G)->A"));
        
        assertEquals(type("Any"), norm(t, "&{}"));
        assertEquals(type("()"), norm(t, "&{()}"));
        assertEquals(type("B"), norm(t, "A&B"));
        assertEquals(type("&{C,D}"), norm(t, "&{A,B,C,D}"));
        assertEquals(type("&{E,C,D}"), norm(t, "(A&E)&(C&(D&E))"));
        assertEquals(type("|{C&E,C&F,C&G,D&E,D&F,D&G}"), norm(t, "(C|D)&(E|F|G)"));
        
        assertEquals(type("Bottom"), norm(t, "|{}"));
        assertEquals(type("()"), norm(t, "|{()}"));
        assertEquals(type("A"), norm(t, "A|B"));
        assertEquals(type("A"), norm(t, "|{A,B,C,D}"));
        assertEquals(type("|{A,E}"), norm(t, "(A|E)|(C|(D|E))"));
        assertEquals(type("(C&D)|(&{E,F,G})"), norm(t, "(C&D)|(E&F&G)"));
        
        } finally { debug.logEnd(); }
    }

    public void testBasicTraitSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "B"));

        assertEquals(TRUE, sub(t, "A", "A"));
        assertEquals(FALSE, sub(t, "A", "B"));
        assertEquals(FALSE, sub(t, "A", "C"));
        assertEquals(FALSE, sub(t, "A", "D"));

        assertEquals(TRUE, sub(t, "B", "A"));
        assertEquals(TRUE, sub(t, "B", "B"));
        assertEquals(FALSE, sub(t, "B", "C"));
        assertEquals(FALSE, sub(t, "B", "D"));

        assertEquals(TRUE, sub(t, "C", "A"));
        assertEquals(TRUE, sub(t, "C", "B"));
        assertEquals(TRUE, sub(t, "C", "C"));
        assertEquals(FALSE, sub(t, "C", "D"));

        assertEquals(TRUE, sub(t, "D", "A"));
        assertEquals(TRUE, sub(t, "D", "B"));
        assertEquals(FALSE, sub(t, "D", "C"));
        assertEquals(TRUE, sub(t, "D", "D"));

        } finally { debug.logEnd(); }
    }

    public void testArrowSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "B"));

        assertEquals(TRUE, sub(t, "A->A", "A->A"));
        assertEquals(TRUE, sub(t, "A->B", "A->A"));
        assertEquals(TRUE, sub(t, "A->C", "A->A"));
        assertEquals(TRUE, sub(t, "A->D", "A->A"));

        assertEquals(TRUE, sub(t, "A->C", "C->C"));
        assertEquals(TRUE, sub(t, "B->C", "C->C"));
        assertEquals(TRUE, sub(t, "C->C", "C->C"));
        assertEquals(FALSE, sub(t, "D->C", "C->C"));

        assertEquals(FALSE, sub(t, "C->A", "A->C"));
        assertEquals(TRUE, sub(t, "A->C", "C->A"));

        } finally { debug.logEnd(); }
    }

    public void testSimpleUnionSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));

        assertEquals(FALSE, sub(t, "A", "B|D"));
        assertEquals(TRUE, sub(t, "B", "B|D"));
        assertEquals(TRUE, sub(t, "C", "B|D"));
        assertEquals(TRUE, sub(t, "D", "B|D"));
        assertEquals(TRUE, sub(t, "E", "B|D"));

        assertEquals(FALSE, sub(t, "A", "C|E"));
        assertEquals(FALSE, sub(t, "B", "C|E"));
        assertEquals(TRUE, sub(t, "C", "C|E"));
        assertEquals(FALSE, sub(t, "D", "C|E"));
        assertEquals(TRUE, sub(t, "E", "C|E"));

        assertEquals(TRUE, sub(t, "B|D", "A"));
        assertEquals(FALSE, sub(t, "B|D", "B"));
        assertEquals(FALSE, sub(t, "B|D", "C"));
        assertEquals(FALSE, sub(t, "B|D", "D"));
        assertEquals(FALSE, sub(t, "B|D", "E"));
        assertEquals(TRUE, sub(t, "B|D", "B|D"));
        assertEquals(TRUE, sub(t, "B|D", "D|B"));
        assertEquals(FALSE, sub(t, "B|D", "C|E"));
        assertEquals(FALSE, sub(t, "B|D", "E|C"));

        assertEquals(TRUE, sub(t, "C|E", "A"));
        assertEquals(FALSE, sub(t, "C|E", "B"));
        assertEquals(FALSE, sub(t, "C|E", "C"));
        assertEquals(FALSE, sub(t, "C|E", "D"));
        assertEquals(FALSE, sub(t, "C|E", "E"));
        assertEquals(TRUE, sub(t, "C|E", "B|D"));
        assertEquals(TRUE, sub(t, "C|E", "D|B"));
        assertEquals(TRUE, sub(t, "C|E", "C|E"));
        assertEquals(TRUE, sub(t, "C|E", "E|C"));

        } finally { debug.logEnd(); }
    }

    public void testSimpleIntersectionSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));

        assertEquals(TRUE, sub(t, "B&D", "A"));
        assertEquals(TRUE, sub(t, "B&D", "B"));
        assertEquals(FALSE, sub(t, "B&D", "C"));
        assertEquals(TRUE, sub(t, "B&D", "D"));
        assertEquals(FALSE, sub(t, "B&D", "E"));

        assertEquals(TRUE, sub(t, "C&E", "A"));
        assertEquals(TRUE, sub(t, "C&E", "B"));
        assertEquals(TRUE, sub(t, "C&E", "C"));
        assertEquals(TRUE, sub(t, "C&E", "D"));
        assertEquals(TRUE, sub(t, "C&E", "E"));

        assertEquals(FALSE, sub(t, "A", "B&D"));
        assertEquals(FALSE, sub(t, "B", "B&D"));
        assertEquals(FALSE, sub(t, "C", "B&D"));
        assertEquals(FALSE, sub(t, "D", "B&D"));
        assertEquals(FALSE, sub(t, "E", "B&D"));
        assertEquals(TRUE, sub(t, "B&D", "B&D"));
        assertEquals(TRUE, sub(t, "D&B", "B&D"));
        assertEquals(TRUE, sub(t, "C&E", "B&D"));
        assertEquals(TRUE, sub(t, "E&C", "B&D"));

        assertEquals(FALSE, sub(t, "A", "C&E"));
        assertEquals(FALSE, sub(t, "B", "C&E"));
        assertEquals(FALSE, sub(t, "C", "C&E"));
        assertEquals(FALSE, sub(t, "D", "C&E"));
        assertEquals(FALSE, sub(t, "E", "C&E"));
        assertEquals(FALSE, sub(t, "B&D", "C&E"));
        assertEquals(FALSE, sub(t, "D&B", "C&E"));
        assertEquals(TRUE, sub(t, "C&E", "C&E"));
        assertEquals(TRUE, sub(t, "E&C", "C&E"));

        } finally { debug.logEnd(); }
    }

    public void testVoidSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));

        assertEquals(FALSE, sub(t, "A", VOID));
        assertEquals(TRUE, sub(t, VOID, VOID));
        assertEquals(FALSE, sub(t, VOID, "A"));
        assertEquals(TRUE, sub(t, VOID, ANY));
        assertEquals(TRUE, sub(t, BOTTOM, VOID));
        assertEquals(FALSE, sub(t, ANY, VOID));

        } finally { debug.logEnd(); }
    }
    
    public void testTupleSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));

        assertEquals(TRUE, sub(t, "(A, B)", "(A, B)"));
        assertEquals(FALSE, sub(t, "(A, B)", "(B, A)"));
        assertEquals(FALSE, sub(t, "(A, B)", "(A, B, Any)"));
        assertEquals(TRUE, sub(t, "(B, D)", "(A, A)"));
        assertEquals(FALSE, sub(t, "(A, A)", "(A, B)"));

        } finally { debug.logEnd(); }
    }
    
    public void testVarargSubtyping() {
        debug.logStart(); try {

        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));

        assertEquals(TRUE, sub(t, "()", "(A...)"));
        assertEquals(TRUE, sub(t, "A", "(A...)"));
        assertEquals(TRUE, sub(t, "(A, A)", "(A...)"));
        assertEquals(TRUE, sub(t, "(A, A, A)", "(A...)"));
        assertEquals(TRUE, sub(t, "(B, C, E)", "(A...)"));
        assertEquals(FALSE, sub(t, "Any", "(A...)"));
        assertEquals(TRUE, sub(t, "Bottom", "(A...)"));
        assertEquals(TRUE, sub(t, "(A, A...)", "(A...)"));

        assertEquals(FALSE, sub(t, "()", "(A, A...)"));
        assertEquals(TRUE, sub(t, "A", "(A, A...)"));
        assertEquals(TRUE, sub(t, "(A, A)", "(A, A...)"));
        assertEquals(TRUE, sub(t, "(A, A, A)", "(A, A...)"));
        assertEquals(TRUE, sub(t, "(B, C, E)", "(A, A...)"));
        assertEquals(FALSE, sub(t, "Any", "(A, A...)"));
        assertEquals(TRUE, sub(t, "Bottom", "(A, A...)"));
        assertEquals(TRUE, sub(t, "(A, A...)", "(A, A...)"));

        assertEquals(FALSE, sub(t, "()", "(A, A, A...)"));
        assertEquals(FALSE, sub(t, "A", "(A, A, A...)"));
        assertEquals(TRUE, sub(t, "(A, A)", "(A, A, A...)"));
        assertEquals(TRUE, sub(t, "(A, A, A)", "(A, A, A...)"));
        assertEquals(TRUE, sub(t, "(B, C, E)", "(A, A, A...)"));
        assertEquals(FALSE, sub(t, "Any", "(A, A, A...)"));
        assertEquals(TRUE, sub(t, "Bottom", "(A, A, A...)"));
        assertEquals(FALSE, sub(t, "(A, A...)", "(A, A, A...)"));

        assertEquals(TRUE, sub(t, "()", "(Any...)"));
        assertEquals(TRUE, sub(t, "A", "(Any...)"));
        assertEquals(TRUE, sub(t, "(A, A)", "(Any...)"));
        assertEquals(TRUE, sub(t, "(A, A, A)", "(Any...)"));
        assertEquals(TRUE, sub(t, "(B, C, E)", "(Any...)"));
        assertEquals(TRUE, sub(t, "Any", "(Any...)"));
        assertEquals(TRUE, sub(t, "Bottom", "(Any...)"));
        assertEquals(TRUE, sub(t, "(A, A...)", "(Any...)"));
        
        assertEquals(TRUE, sub(t, "(B...)", "(A...)"));
        assertEquals(FALSE, sub(t, "(A...)", "(B...)"));

        } finally { debug.logEnd(); }
    }


    private static final GlobalEnvironment GLOBAL_ENV;
    static {
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();

        ApiIndex builtin = api("AnyType", absTrait("Any"));
        apis.put(builtin.ast().getName(), builtin);

        ApiIndex library = api("FortressLibrary", absTrait("Object"));
        apis.put(library.ast().getName(), library);

        GLOBAL_ENV = new GlobalEnvironment.FromMap(apis);
    }

    /**
     * Make an ApiIndex with the given name and traits.
     * @param name  Assumed to consist of a single part (has no '.' separators).
     * @param traits  Each index is assumed to wrap an AbsDecl (not a Decl).
     */
    private static ApiIndex api(String name, TraitIndex... traits) {
        List<AbsDecl> traitDecls = new ArrayList<AbsDecl>(traits.length);
        Map<Id, TypeConsIndex> traitMap = new HashMap<Id, TypeConsIndex>();
        for (TraitIndex t : traits) {
            traitDecls.add((AbsDecl) t.ast());
            traitMap.put(t.ast().getName(), t);
        }
        Api ast = new Api(NodeFactory.makeAPIName(name),
                          Collections.<Import>emptyList(),
                          traitDecls);
        return new ApiIndex(ast,
                            Collections.<Id, Variable>emptyMap(),
                            CollectUtil.<IdOrOpOrAnonymousName, Function>emptyRelation(),
                            traitMap,
                            CollectUtil.<Id, Dimension>emptyMap(),
                            CollectUtil.<Id, Unit>emptyMap(),
                            Collections.<String, GrammarIndex>emptyMap(),
                            0);
    }

    /**
     * Make a ComponentIndex with the given name and traits.
     * @param name  Assumed to consist of a single part (has no '.' separators).
     * @param traits  Each index is assumed to wrap a Decl (not an AbsDecl).
     */
    private static ComponentIndex component(String name, TraitIndex... traits) {
        List<Decl> traitDecls = new ArrayList<Decl>(traits.length);
        Map<Id, TypeConsIndex> traitMap = new HashMap<Id, TypeConsIndex>();
        for (TraitIndex t : traits) {
            traitDecls.add((Decl) t.ast());
            traitMap.put(t.ast().getName(), t);
        }
        Component ast = new Component(NodeFactory.makeAPIName(name),
                                      Collections.<Import>emptyList(),
                                      Collections.<Export>emptyList(),
                                      traitDecls);
        return new ComponentIndex(ast,
                                  Collections.<Id, Variable>emptyMap(),
                                  Collections.<VarDecl>emptySet(),
                                  CollectUtil.<IdOrOpOrAnonymousName, Function>emptyRelation(),
                                  traitMap,
                                  CollectUtil.<Id, Dimension>emptyMap(),
                                  CollectUtil.<Id, Unit>emptyMap(),
                                  0);
    }

    /**
     * Make a ProperTraitIndex with the given name and supertypes.  Wrapped AST is a Decl.
     * @param name  A simple name.
     * @param supers  Type strings (parsed by parseType()); must parse to TraitTypes.
     */
    private static ProperTraitIndex trait(String name, String... supers) {
        return traitHelper(name, supers, false);
    }

    /**
     * Make a ProperTraitIndex with the given name and supertypes.  Wrapped AST is an AbsDecl.
     * @param name  A simple name.
     * @param supers  Type strings (parsed by parseType()); must parse to TraitTypes.
     */
    private static ProperTraitIndex absTrait(String name, String... supers) {
        return traitHelper(name, supers, true);
    }

    private static ProperTraitIndex traitHelper(String name, String[] supers, boolean absDecl) {
        List<TraitTypeWhere> extendsClause = new ArrayList<TraitTypeWhere>(supers.length);
        for (String sup : supers) {
            BaseType supT = (BaseType) parseType(sup);
            extendsClause.add(new TraitTypeWhere(supT, new WhereClause()));
        }
        TraitAbsDeclOrDecl ast;
        if (absDecl) {
            ast = new AbsTraitDecl(NodeFactory.makeId(name),
                                   Collections.<StaticParam>emptyList(),
                                   extendsClause,
                                   Collections.<AbsDecl>emptyList());
        }
        else {
            ast = new TraitDecl(NodeFactory.makeId(name),
                                Collections.<StaticParam>emptyList(),
                                extendsClause,
                                Collections.<Decl>emptyList());
        }
        return new ProperTraitIndex(ast,
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Function>emptySet(),
                                    CollectUtil.<IdOrOpOrAnonymousName, Method>emptyRelation(),
                                    CollectUtil.<IdOrOpOrAnonymousName, FunctionalMethod>emptyRelation());
    }
    
    /** Shortcut for parseType */
    private static Type type(String s) { return parseType(s); }

    /**
     * Parse the given string as a type.  This is a permissive algorithm: many strings that
     * don't make sense will parse without error, but strings that do make sense are parsed
     * as expected.  (However, there are some simple sanity checks to limit the set of
     * parsed malformed strings.)  The following conventions are followed:
     * <ul>
     * <li>Order of operations: "->" takes precedence over "|", which takes precedence over
     * "&"; all these are handled before other kinds of types.  The leftmost highest-priority
     * operator is always handled first.  Parens can be used freely to change associativity
     * and the order of operations.</li>
     * <li>Arrows are parsed with full support for domains and effects.</li>
     * <li>"|{<comma-list>}" is a arbitrary-arity union; "A|B" is a binary union.</li>
     * <li>"&{<comma-list>}" is a arbitrary-arity intersection; "A&B" is a binary intersection.</li>
     * <li>Empty paren-lists are parsed as void; a singleton is either a varargs tuple or just that
     * type; and longer lists are parsed as tuples (or varargs tuples).  (Note, however, that
     * expressions to the left of arrows are parsed as Domains.)</li>
     * <li>Atomic types starting with "#" are inference variables.</li>
     * <li>"Any" and "Bottom" are literals for those types.</li>
     * <li>Single-letter types in the range "P"..."Z" are type variables.</li>
     * <li>All other atomic types are TraitTypes with no static arguments.</li>
     * </ul>
     */
    private static Type parseType(String s) {
        s = s.trim();
        int opIndex;
        
        opIndex = findAtTop(s, "->");
        if (opIndex >= 0) {
            s = s + " "; // recognize a trailing "io"
            int effectStart = findAtTop(s, " throws ", " io ");
            if (effectStart == -1) { effectStart = s.length(); }
            Domain d = parseDomain(s.substring(0, opIndex));
            Type r = parseType(s.substring(opIndex+2, effectStart));
            Effect e = parseEffect(s.substring(effectStart));
            return new ArrowType(d, r, e);
        }
        
        opIndex = findAtTop(s, "|");
        if (opIndex == 0) {
            return new UnionType(parseTypeList(s, "|{", "}"));
        }
        else if (opIndex > 0) {
            Type left = parseType(s.substring(0, opIndex));
            Type right = parseType(s.substring(opIndex+1));
            return NodeFactory.makeUnionType(left, right);
        }
        
        opIndex = findAtTop(s, "&");
        if (opIndex == 0) {
            return new IntersectionType(parseTypeList(s, "&{", "}"));
        }
        else if (opIndex > 0) {
            Type left = parseType(s.substring(0, opIndex));
            Type right = parseType(s.substring(opIndex+1));
            return NodeFactory.makeIntersectionType(left, right);
        }
        
        if (s.startsWith("(")) {
            boolean varargs = s.endsWith("...)");
            if (varargs) { s = s.substring(0, s.length()-4) + ")"; }
            List<Type> ts = parseTypeList(s, "(", ")");
            if (varargs) { return new VarargTupleType(ts, ts.remove(ts.size()-1)); }
            else if (ts.size() == 0) { return VOID; }
            else if (ts.size() == 1) { return ts.get(0); }
            else { return new TupleType(ts); }
        }
        
        if (s.length() == 0 |
            TextUtil.containsAny(s, '(', ')', '{', '}', ',', '-', '>', '&', '|') |
            s.contains("...")) {
            throw new IllegalArgumentException("Malformed name: \"" + s + "\"");
        }
        
        if (s.equals("Any")) { return ANY; }
        
        if (s.equals("Bottom")) { return BOTTOM; }
        
        if (s.startsWith("#")) {
            return new InferenceVarType(s);
        }
        
        if (s.length() == 1 && s.charAt(0) >= 'P' && s.charAt(0) <= 'Z') {
            return NodeFactory.makeVarType(s);
        }
        
        return NodeFactory.makeTraitType(s);
    }
    
    private static Domain parseDomain(String s) {
        s = s.trim();
        // check whether this is entirely enclosed in parens
        if (s.startsWith("(") && findAtTop(s, "->", "&", "|") == -1) {
            List<Type> args = new LinkedList<Type>();
            Option<Type> varargs = Option.none();
            List<KeywordType> keys = new LinkedList<KeywordType>();
            for (String elt : splitList(s, "(", ")")) {
                if (elt.endsWith("...")) {
                    elt = elt.substring(0, elt.length()-3);
                    varargs = Option.some(parseType(elt));
                }
                else {
                    int eq = findAtTop(elt, "=");
                    if (eq >= 0) {
                        Id k = NodeFactory.makeId(elt.substring(0, eq).trim());
                        Type t = parseType(elt.substring(eq+1));
                        keys.add(new KeywordType(k, t));
                    }
                    else { args.add(parseType(elt)); }
                }
            }
            return new Domain(args, varargs, keys);
        }
        else {
            return new Domain(Collections.singletonList(parseType(s)));
        }
    }
    
    private static Effect parseEffect(String s) {
        if (s.length() == 0) { return new Effect(); }
        
        boolean io = false;
        s = s.trim();
        if (s.equals("io")) { return new Effect(true); }
        else if (s.startsWith("io ")) { io = true; s = s.substring(3).trim(); }
        else if (s.endsWith(" io")) { io = true; s = s.substring(0, s.length()-3).trim(); }
        
        if (!s.startsWith("throws ")) {
            throw new IllegalArgumentException("Unrecognized effect: \"" + s + "\"");
        }
        s = s.substring(7).trim();
        List<String> elts;
        if (s.startsWith("{")) { elts = splitList(s, "{", "}"); }
        else { elts = Collections.singletonList(s); }
        List<BaseType> ts = new LinkedList<BaseType>();
        for (String elt : elts) {
            Type t = parseType(elt);
            if (!(t instanceof BaseType)) {
                throw new IllegalArgumentException("Non-BaseType in throws: " + t);
            }
            ts.add((BaseType) t);
        }
        return new Effect(Option.some(ts), io);
    }
    
    private static List<Type> parseTypeList(String s, String leftDelim, String rightDelim) {
        return parseTypeList(splitList(s, leftDelim, rightDelim));
    }
    
    private static List<Type> parseTypeList(List<String> ss) {
        List<Type> ts = new LinkedList<Type>();
        for (String elt : ss) { ts.add(parseType(elt)); }
        return ts;
    }
    
    /**
     * Get the index of a non-parenthesized instance of the given substring, or -1
     * if it is not found.
     */
    private static int findAtTop(String s, String... subs) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '(': depth++; break;
                case '{': depth++; break;
                case ')': depth--; break;
                case '}': depth--; break;
                default:
                    if (depth == 0) {
                        for (String sub : subs) {
                            if (s.substring(i).startsWith(sub)) { return i; }
                        }
                    }
            }
        }
        return -1;
    }
    
    private static List<String> splitList(String s, String leftDelim, String rightDelim) {
        if (!s.startsWith(leftDelim) || !s.endsWith(rightDelim)) {
            throw new IllegalArgumentException("Bad list: \"" + s + "\"");
        }
        s = s.substring(leftDelim.length(), s.length()-rightDelim.length());
        
        List<String> elts = new LinkedList<String>();
        int depth = 0;
        int eltStart = 0;
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case '(': depth++; break;
                case '{': depth++; break;
                case ')': depth--; break;
                case '}': depth--; break;
                case ',':
                    if (depth == 0) {
                        elts.add(s.substring(eltStart, i));
                        eltStart = i+1;
                    }
            }
        }
        if (eltStart > 0 || s.length() - eltStart > 0) {
            elts.add(s.substring(eltStart));
        }
        return elts;
    }
    

    /** Assumes each TraitIndex wraps a non-abstract declaration (a Decl). */
    private static TypeAnalyzer makeAnalyzer(TraitIndex... traits) {
        ComponentIndex c = component("TypeAnalyzerTestComponent", traits);
        return new TypeAnalyzer(new TraitTable(c, GLOBAL_ENV));
    }

}
