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
    /* Rewrites overloaded functional declarations.
     * More details in com.sun.fortress.compiler.OverloadRewriter
     */
    OVERLOADREWRITE("Overloading Rewriting"),
    /* Generates a Java bytecode compiled environment.
     * More details in com.sun.fortress.compiler.environments.TopLevelEnvGen
     */
    CODEGEN("Code generation");

    private String phaseName;

    PhaseOrder(String phaseName) {
        this.phaseName = phaseName;
    }

    public Phase makePhase(FortressRepository repository,
            GlobalEnvironment env, Iterable<Api> apis,
            Iterable<Component> components, long lastModified)
            throws StaticError {
        Phase empty = new EmptyPhase(repository, env, apis, components,
                lastModified);
        switch (this) {
        case EMPTY:
            return empty;
        default:
            return makePhaseHelper(empty);
        }
    }

    private Phase makePhaseHelper(Phase phase) {
        switch (this) {
        case EMPTY:
            return phase;
        case PREDISAMBIGUATEDESUGAR:
        	return new PreDisambiguationDesugarPhase(EMPTY.makePhaseHelper(phase));
        case DISAMBIGUATE:
            return new DisambiguatePhase(PREDISAMBIGUATEDESUGAR.makePhaseHelper(phase));
        case GRAMMAR:
            return new GrammarPhase(DISAMBIGUATE.makePhaseHelper(phase));
        case PRETYPECHECKDESUGAR:
        	return new PreTypeCheckDesugarPhase(GRAMMAR.makePhaseHelper(phase));
        case TYPECHECK:
            return new TypeCheckPhase(PRETYPECHECKDESUGAR.makePhaseHelper(phase));
        case DESUGAR:
            return new DesugarPhase(TYPECHECK.makePhaseHelper(phase));
        case OVERLOADREWRITE:
            return new OverloadRewritingPhase(DESUGAR.makePhaseHelper(phase));
        case CODEGEN:
            return new CodeGenerationPhase(OVERLOADREWRITE.makePhaseHelper(phase));
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
