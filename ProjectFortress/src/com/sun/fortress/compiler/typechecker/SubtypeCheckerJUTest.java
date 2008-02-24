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
import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static java.lang.Boolean.TRUE;
import static java.lang.Boolean.FALSE;
import static com.sun.fortress.compiler.typechecker.Types.*;

public class SubtypeCheckerJUTest extends TestCase {

    private static Boolean sub(SubtypeChecker ta, String s, String t) {
        return ta.subtype(parseType(s), parseType(t));
    }

    private static Boolean sub(SubtypeChecker ta, String s, Type t) {
        return ta.subtype(parseType(s), t);
    }

    private static Boolean sub(SubtypeChecker ta, Type s, String t) {
        return ta.subtype(s, parseType(t));
    }

    private static Boolean sub(SubtypeChecker ta, Type s, Type t) {
        return ta.subtype(s, t);
    }

    public void testAnySubtyping() {
        SubtypeChecker t = makeAnalyzer(trait("A"),
                                        trait("B", "A"),
                                        trait("C", "A"));

        assertEquals(TRUE, sub(t, ANY,      ANY));
        assertEquals(TRUE, sub(t, BOTTOM,   ANY));
        assertEquals(TRUE, sub(t, VOID,     ANY));
        assertEquals(TRUE, sub(t, "B",      ANY));
        // tparam
        assertEquals(TRUE, sub(t, "A->B",   ANY));
        assertEquals(TRUE, sub(t, "(A, B)", ANY));
        // tapp
    }

    public void testBasicTraitSubtyping() {
        SubtypeChecker t = makeAnalyzer(trait("A"),
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
    }

    public void testArrowSubtyping() {
        SubtypeChecker t = makeAnalyzer(trait("A"),
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
    }

    public void testVoidSubtyping() {
        SubtypeChecker t = makeAnalyzer(trait("A"),
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
    }


    private static final GlobalEnvironment GLOBAL_ENV;
    static {
        Map<APIName, ApiIndex> apis = new HashMap<APIName, ApiIndex>();

        ApiIndex builtin =
            api("FortressBuiltin",
                absTrait("Any"),
                absTrait("Tuple", "FortressBuiltin.Any"));
        apis.put(builtin.ast().getName(), builtin);

        ApiIndex library =
            api("FortressLibrary",
                absTrait("Object", "FortressBuiltin.Any"));
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
                            CollectUtil.<SimpleName, Function>emptyRelation(),
                            traitMap,
                            CollectUtil.<Id, Dimension>emptyMap(),
                            CollectUtil.<Id, Unit>emptyMap(),
                            Collections.<QualifiedIdName, GrammarIndex>emptyMap(),
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
                                  CollectUtil.<SimpleName, Function>emptyRelation(),
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
            TraitType supT = (TraitType) parseType(sup);
            extendsClause.add(new TraitTypeWhere(supT, new WhereClause()));
        }
        TraitAbsDeclOrDecl ast;
        List<StaticParam> sparams = Collections.<StaticParam>emptyList();
        /*
        if (name.contains("[\\")) {
            int index = name.indexOf("[\\");
            name = name.substring(0, index);
            String s = name.substring(index+3);
            while(s.contains(",")) {
                index = s.indexOf(",");
                sparams.add(parseStaticArg(s.substring(0, index)));
                s = s.substring(index+1);
            }
            sparams.add(parseType(s));
        }
        */
        if (absDecl) {
            ast = new AbsTraitDecl(NodeFactory.makeId(name), sparams,
                                   extendsClause,
                                   Collections.<AbsDecl>emptyList());
        }
        else {
            ast = new TraitDecl(NodeFactory.makeId(name), sparams,
                                extendsClause,
                                Collections.<Decl>emptyList());
        }
        return new ProperTraitIndex(ast,
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Function>emptySet(),
                                    CollectUtil.<SimpleName, Method>emptyRelation(),
                                    CollectUtil.<SimpleName, FunctionalMethod>emptyRelation());
    }

    private static Type parseType(String s) {
        s = s.trim();
        if (s.contains("->")) {
            int arrowIndex = s.indexOf("->");
            Type left = parseType(s.substring(0, arrowIndex));
            Type right = parseType(s.substring(arrowIndex+2));
            List<Type> thrown = Collections.singletonList(BOTTOM);
            return new ArrowType(left, right, Option.some(thrown), false);
        }
        if (s.startsWith("(")) {
            List<Type> types = new ArrayList<Type>();
            s = s.substring(1, s.length()-1);
            while(s.contains(",")) {
                int commaIndex = s.indexOf(",");
                types.add(parseType(s.substring(0, commaIndex)));
                s = s.substring(commaIndex+1);
            }
            types.add(parseType(s));
            return NodeFactory.makeTupleType(types);
        }
        /*
        else if (s.contains("[\\")) {
            int openIndex = s.indexOf("[\\");
            QualifiedIdName name = NodeFactory.makeQualifiedIdName(s.substring(0, openIndex));
            List<StaticArg> sargs = new ArrayList<StaticArg>();
            s = s.substring(openIndex+3, s.length()-3);
            while(s.contains(",")) {
                int commaIndex = s.indexOf(",");
                sargs.add(parseStaticArg(s.substring(0, commadIndex)));
                s = s.substring(commaIndex+1);
            }
            sargs.add(parseStaticArg(s));
            return NodeFactory.makeInstantiatedType(name, sargs);
        }
        */
        else {
            return NodeFactory.makeInstantiatedType(s);
        }
    }

    /*
    private static StaticArg parseStaticArg(String s) {
        s = s.trim();
        if (s.contains("[\\")) {
            int openIndex = s.indexOf("[\\");
            QualifiedIdName name = NodeFactory.makeQualifiedIdName(s.substring(0, openIndex));
            List<StaticArg> sargs = new ArrayList<StaticArg>();
            s = s.substring(openIndex+3, s.length()-3);
            while(s.contains(",")) {
                int commaIndex = s.indexOf(",");
                sargs.add(parseStaticArg(s.substring(0, commadIndex)));
                s = s.substring(commaIndex+1);
            }
            sargs.add(parseStaticArg(s));
            return NodeFactory.makeTypeArg(NodeFactory.makeInstantiatedType(name, sargs));
        }
        else {
            return NodeFactory.makeIdArg(s);
        }
    }
    */

    /** Assumes each TraitIndex wraps a non-abstract declaration (a Decl). */
    private static SubtypeChecker makeAnalyzer(TraitIndex... traits) {
        ComponentIndex c = component("SubtypeCheckerTestComponent", traits);
        return SubtypeChecker.make(new TraitTable(c, GLOBAL_ENV));
    }

}
