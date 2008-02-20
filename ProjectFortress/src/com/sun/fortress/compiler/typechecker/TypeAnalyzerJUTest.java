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

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.*;

import static com.sun.fortress.compiler.typechecker.ConstraintFormula.TRUE;
import static com.sun.fortress.compiler.typechecker.ConstraintFormula.FALSE;
import static com.sun.fortress.compiler.typechecker.Types.*;

import static edu.rice.cs.plt.debug.DebugUtil.debug;

public class TypeAnalyzerJUTest extends TestCase {
    
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
        
    public void testBasicTraitSubtyping() {
        debug.logStart();
        
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
        
        debug.logEnd();
    }
    
    public void testArrowSubtyping() {
        debug.logStart();
        
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
        
        debug.logEnd();
    }
    
    public void xxxtestSimpleUnionSubtyping() {
        debug.logStart();
        
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
        
        debug.logEnd();
    }
    
    public void xxxtestSimpleIntersectionSubtyping() {
        debug.logStart();
        
        TypeAnalyzer t = makeAnalyzer(trait("A"),
                                      trait("B", "A"),
                                      trait("C", "B"),
                                      trait("D", "A"),
                                      trait("E", "D"));
        
        assertEquals(TRUE, sub(t, "B&D", "A"));
        assertEquals(TRUE, sub(t, "B&D", "B"));
        assertEquals(TRUE, sub(t, "B&D", "C"));
        assertEquals(FALSE, sub(t, "B&D", "D"));
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
        
        debug.logEnd();
    }
    
//    public void testInferenceSubtyping() {
//        assertEquals(sub(t, "$1
//        

    public void xxxtestVoidSubtyping() {
        debug.logStart();
        
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
        
        debug.logEnd();
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
        else if (s.contains("|")) {
            int orIndex = s.indexOf("|");
            Type left = parseType(s.substring(0, orIndex));
            Type right = parseType(s.substring(orIndex+1));
            return new OrType(left, right);
        }
        else if (s.contains("&")) {
            int andIndex = s.indexOf("&");
            Type left = parseType(s.substring(0, andIndex));
            Type right = parseType(s.substring(andIndex+1));
            return new AndType(left, right);
        }
        else if (s.startsWith("#")) {
            return new InferenceVarType(s);
        }
        else if (s.length() == 1 && s.charAt(0) >= 'P' && s.charAt(0) <= 'Z') {
            return NodeFactory.makeIdType(s);
        }
        else {
            return NodeFactory.makeInstantiatedType(s);
        }
    }
        
    /** Assumes each TraitIndex wraps a non-abstract declaration (a Decl). */
    private static TypeAnalyzer makeAnalyzer(TraitIndex... traits) {
        ComponentIndex c = component("TypeAnalyzerTestComponent", traits);
        return TypeAnalyzer.make(new TraitTable(c, GLOBAL_ENV));
    }
    
}
