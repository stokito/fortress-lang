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
	DISAMBIGUATE("Disambiguation"),	
	GRAMMAR("Grammar Rewriting"),	
	TYPECHECK("Typechecking"),
	DESUGAR("Desugaring"),	
	CODEGEN("Code generation");
	
	private String phaseName;

	PhaseOrder(String phaseName) {
		this.phaseName = phaseName;
	}
	
	public Phase makePhase(FortressRepository repository, GlobalEnvironment env, 
			Iterable<Api> apis, Iterable<Component> components, long lastModified) throws StaticError {
		Phase empty = new EmptyPhase(repository,env,apis,components,lastModified);
		switch(this) {
			case EMPTY: return empty;
			default: return makePhaseHelper(empty);
		}
	}
	
	private Phase makePhaseHelper(Phase emptyPhase) {
		switch(this) {
			case EMPTY: return emptyPhase;
			case DISAMBIGUATE: return new DisambiguatePhase(EMPTY.makePhaseHelper(emptyPhase));
			case GRAMMAR: return new GrammarPhase(DISAMBIGUATE.makePhaseHelper(emptyPhase));
			case TYPECHECK: return new TypeCheckPhase(GRAMMAR.makePhaseHelper(emptyPhase));
			case DESUGAR: return new DesugarPhase(TYPECHECK.makePhaseHelper(emptyPhase));
			case CODEGEN: return new CodeGenerationPhase(DESUGAR.makePhaseHelper(emptyPhase));
			default: return InterpreterBug.bug("Unknown static analysis phase: " + phaseName);
		}
	}
	
	@Override
	public String toString() {
		return phaseName;
	}
			
}
