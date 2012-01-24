/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.repository.FortressRepository;

public enum PhaseOrder {
    EMPTY("Empty Phase"),
    /* Run desugaring phases that must occur before disambiguation.
     * 1) Remove conditional operators, replacing their operands with thunks.
     * 2) Rewrite trait, object, and object expressions to explicitly extend Object.
     * More details in com.sun.fortress.compiler.PreDisambiguationDesugarer
     */
    PREDISAMBIGUATEDESUGAR("Pre-Disambiguation Desugaring"),
    /* Eliminates ambiguities in an AST that can be resolved solely by knowing what
     * kind of entity a name refers to.  For example, all names referring to APIs
     * are made fully qualified.
     * More details in com.sun.fortress.compiler.Disambiguator
     */
    DISAMBIGUATE("Disambiguation"),
    /* Rewrites grammar definitions.
     * For example, disambiguates an item symbol and rewrites it to either
     * a nonterminal, a keyword, or a token symbol.
     * More details in com.sun.fortress.syntax_abstractions.phases.GrammarRewriter
     */
    GRAMMAR("Grammar Rewriting"),
    /* Performs all remaining desugarings that can be run before type checking.
     */
    PRETYPECHECKDESUGAR("Pre-Type-Checking Desugaring"),
    /* Checks types of expressions.
     * More details in com.sun.fortress.compiler.typechecker.TypeChecker
     */
    TYPECHECK("Typechecking"),
    /* Performs desugaring of Fortress programs.
     * 1) getter/setter desugaring
     * 2) object expression desugaring
     * More details in com.sun.fortress.compiler.Desugarer
     */
    DESUGAR("Desugaring"),
    /*
     * Folds integer literal expressions until they are atomic
     */
    INTEGERLITERALFOLDING("Folding of integer literals"),
    /* Rewrites overloaded functional declarations.
     * More details in com.sun.fortress.compiler.OverloadRewriter
     */
    OVERLOADREWRITE("Overloading Rewriting"),
    
    OVERLOADREWRITE_FOR_INTERPRETER("Overloading Rewriting For Interpreter"),
    /* Generate top level environments
     * Generates a Java bytecode compiled environment.
     * More details in com.sun.fortress.compiler.environments.TopLevelEnvGen
     */
    ENVGEN("Environment Generation"),
    /* Code generation
     * Parallelizability analysis happens in here somewhere, too
     */
    CODEGEN("Code generation"),
    /*
     * Runs a Junit test file.
     */
    JUNIT("Run junit .test file");

    private String phaseName;

    PhaseOrder(String phaseName) {
        this.phaseName = phaseName;
    }

//    public Phase makePhase(FortressRepository repository,
//                           GlobalEnvironment env, Iterable<Api> apis,
//                           Iterable<Component> components, long lastModified)
//            throws StaticError {
//        Phase empty = new EmptyPhase(env, apis, components, lastModified);
//        switch (this) {
//            case EMPTY:
//                return empty;
//            default:
//                return makePhaseHelper(empty);
//        }
//    }

    static public final PhaseOrder[] disambiguatePhaseOrder = {
        PREDISAMBIGUATEDESUGAR,
        DISAMBIGUATE
    };
    
    static public final PhaseOrder[] grammarPhaseOrder = {
        PREDISAMBIGUATEDESUGAR,
        DISAMBIGUATE,
        GRAMMAR
    };

     static public final PhaseOrder[] typecheckPhaseOrder = {
        PREDISAMBIGUATEDESUGAR,
        DISAMBIGUATE,
        GRAMMAR,
        PRETYPECHECKDESUGAR,
        TYPECHECK
    };
     
     static public final PhaseOrder[] desugarPhaseOrder = {
         PREDISAMBIGUATEDESUGAR,
         DISAMBIGUATE,
         GRAMMAR,
         PRETYPECHECKDESUGAR,
         TYPECHECK,
         DESUGAR,
     };
 
    static public final PhaseOrder[] interpreterPhaseOrder = {
            PREDISAMBIGUATEDESUGAR,
            DISAMBIGUATE,
            GRAMMAR,
            PRETYPECHECKDESUGAR,
            TYPECHECK,
            DESUGAR,
            OVERLOADREWRITE_FOR_INTERPRETER
            // Disabled because we are not using them.
            //,ENVGEN
    };
    
   static public final PhaseOrder[] compilerPhaseOrder = {
        PREDISAMBIGUATEDESUGAR,
        DISAMBIGUATE,
        GRAMMAR,
        PRETYPECHECKDESUGAR,
        INTEGERLITERALFOLDING,
        TYPECHECK,
        DESUGAR,
        OVERLOADREWRITE,
        CODEGEN
    };
    

  
 

    public static Phase makePhaseOrder(PhaseOrder[] order,
            FortressRepository repository,
            GlobalEnvironment env,
            Iterable<Api> apis,
            Iterable<Component> components,
            long lastModified) {
        Phase phase = new EmptyPhase(env, apis, components, lastModified);
        
        for (int i = 0; i < order.length; i++) {
            phase = order[i].makePhaseOrderHelper(phase);
        }
        
        return phase;
    }

    public static Phase makePhaseOrder(PhaseOrder[] order,
            GlobalEnvironment env,
            Iterable<Api> apis,
            Iterable<Component> components,
            long lastModified) {
        Phase phase = new EmptyPhase(env, apis, components, lastModified);
        
        for (int i = 0; i < order.length; i++) {
            phase = order[i].makePhaseOrderHelper(phase);
        }
        
        return phase;
    }    
    
    private Phase makePhaseOrderHelper(Phase phase) {
        switch (this) {
            case EMPTY:
                return phase;
            case PREDISAMBIGUATEDESUGAR:
                return new PreDisambiguationDesugarPhase(phase);
            case DISAMBIGUATE:
                return new DisambiguatePhase(phase);
            case GRAMMAR:
                return new GrammarPhase(phase);
            case PRETYPECHECKDESUGAR:
                return new PreTypeCheckDesugarPhase(phase);
            case TYPECHECK:
                return new TypeCheckPhase(phase);
            case DESUGAR:
                return new DesugarPhase(phase);
            case INTEGERLITERALFOLDING:
            	return new IntegerLiteralFoldingPhase(phase);
            case OVERLOADREWRITE:
                return new OverloadRewritingPhase(phase);                
            case OVERLOADREWRITE_FOR_INTERPRETER:
                return new OverloadRewritingForInterpreterPhase(phase);
            case ENVGEN:
                return new EnvGenerationPhase(phase);
            case CODEGEN:
                return new CodeGenerationPhase(phase);
            default:
                return InterpreterBug.bug("Unknown static analysis phase: "
                        + phaseName);
        }
    }
    


    @Override
    public String toString() {
        return phaseName;
    }

}
