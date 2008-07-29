package com.sun.fortress.compiler.phases;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.repository.FortressRepository;

public abstract class Phase {	
	
    Phase parentPhase;
	FortressRepository repository;
    GlobalEnvironment env;
    long lastModified;	    
    AnalyzeResult result;    
    
	public Phase(Phase parentPhase){
        this.parentPhase = parentPhase;
        if (parentPhase != null) {
        	repository = parentPhase.getRepository();
        	env = parentPhase.getEnv();
        	lastModified = parentPhase.getLastModified();
        }
    }

    public final AnalyzeResult getResult() { return result; }
    
    public final FortressRepository getRepository() {
		return repository;
	}

	public final GlobalEnvironment getEnv() {
		return env;
	}

	public final long getLastModified() {
		return lastModified;
	}
        
    public abstract AnalyzeResult execute() throws StaticError ;

    public final AnalyzeResult run() throws StaticError {
    	if (parentPhase != null)
    		parentPhase.run();        
        result = execute();
        return result;
    }
	
}
