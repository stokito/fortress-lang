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
