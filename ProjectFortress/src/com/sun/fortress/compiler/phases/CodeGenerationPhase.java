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

package com.sun.fortress.compiler.phases;

import java.util.List;

import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.codegen.*;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.typechecker.TraitTable;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.useful.Debug;

public class CodeGenerationPhase extends Phase {

    public static Symbols symbolTable = new Symbols();
    
    public CodeGenerationPhase(Phase parentPhase) {
        super(parentPhase);
    }


    @Override
        public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase CodeGeneration");
        AnalyzeResult previous = parentPhase.getResult();
        FortressRepository repository = getRepository();
        
        Debug.debug(Debug.Type.CODEGEN, 1,
                    "CodeGenerationPhase: components " + previous.components() + 
                    " apis = " + previous.apis().keySet());

        for ( APIName api : previous.apis().keySet() )  
            symbolTable.addApi(api, previous.apis().get(api)); 
	 	 
        for (Component component : previous.componentIterator()) { 
            APIName api = component.getName(); 
            symbolTable.addComponent(api, previous.components().get(api)); 
        } 
	 	 
        Debug.debug(Debug.Type.CODEGEN, 1,  
                    "SymbolTable=" + symbolTable.toString()); 

        for ( APIName api : previous.apis().keySet() ) {
                if (ForeignJava.only.foreignApiNeedingCompilation(api)) {
                    ApiIndex ai = previous.apis().get(api);
                    TypeAnalyzer ta = new TypeAnalyzer(new TraitTable(ai, getEnv()));

                    Relation<IdOrOpOrAnonymousName, Function>  fns = ai.functions();
                    for (IdOrOpOrAnonymousName name : fns.firstSet()) {
                        PredicateSet<Function> defs = fns.matchFirst(name);
                        if (defs.size() > 1) {
                            // Woo-hoo, an overloaded function.
                            System.err.println("Found an overloaded function " + name);
                            for (Function f : defs) {
                                System.err.println("Overload: " + f);
                                List<Param> parameters = f.parameters();
                                for (Param p : parameters) {
                                    Option<Type> ot = p.getIdType();
                                    Option<Type> ovt = p.getVarargsType();
                                }
                            }
                        }
                    }
                    ForeignJava.only.generateWrappersForApi(api);
                }
        }
        
        for (Component component : previous.componentIterator()) {
            Debug.debug(Debug.Type.CODEGEN, 1,
                        "CodeGenerationPhase: Compile(" + component.getName() + ")");
            CodeGen c = new CodeGen(component.getName().getText(), symbolTable);
            component.accept(c);
        }

        return new AnalyzeResult(previous.apis(), previous.components(),
                                 IterUtil.<StaticError> empty(),
                                 previous.typeCheckerOutput());

    }

}
