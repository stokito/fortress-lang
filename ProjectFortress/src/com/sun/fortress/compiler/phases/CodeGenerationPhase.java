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

import java.util.Comparator;
import java.util.Map;
import java.util.Set;

import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.codegen.*;
import com.sun.fortress.compiler.environments.TopLevelEnvGen;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.MultipleStaticError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.scala_src.typechecker.TraitTable;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Useful;

public class CodeGenerationPhase extends Phase {

    public static Symbols symbolTable = new Symbols();
    public static final boolean debugOverloading = false;

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

//        for ( APIName api : previous.apis().keySet() )
//            symbolTable.addApi(api, previous.apis().get(api));

        for (Map.Entry<APIName, ApiIndex> entry : repository.apis().entrySet()) {
            symbolTable.addApi(entry.getKey(), entry.getValue());
        }

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
                   
                   Set<OverloadSet> overloads =
                       new BASet<OverloadSet>(DefaultComparator.<OverloadSet>normal());

                    for (IdOrOpOrAnonymousName name : fns.firstSet()) {
                        PredicateSet<Function> defs = fns.matchFirst(name);
                        if (defs.size() > 1) {
                            foundAnOverLoadedForeignFunction(ai, ta, name, defs, overloads);
                        }
                    }
                    ForeignJava.only.generateWrappersForApi(api, overloads);
                    // Need to generate overloaded functions -- where?
                }
        }

        for (Component component : previous.componentIterator()) {
            Debug.debug(Debug.Type.CODEGEN, 1,
                        "CodeGenerationPhase: Compile(" + component.getName() + ")");
            ComponentIndex ci = previous.components().get(component.getName());
            Relation<IdOrOpOrAnonymousName, Function>  fns = ci.functions();
            TypeAnalyzer ta = new TypeAnalyzer(new TraitTable(ci, getEnv()));

            CodeGen c = new CodeGen(component, symbolTable, ta, ci);
            component.accept(c);
        }

        return new AnalyzeResult(previous.apis(), previous.components(),
                                 IterUtil.<StaticError> empty(),
                                 previous.typeCheckerOutput());

    }


    /**
     * @param ai
     * @param name
     * @param defs
     * @param overloads 
     */
    private void foundAnOverLoadedForeignFunction(ApiIndex ai, TypeAnalyzer ta,
            IdOrOpOrAnonymousName name, PredicateSet<Function> defs, Set<OverloadSet> overloads) {
        // Woo-hoo, an overloaded function.
        if (debugOverloading)
            System.err.println("Found an overloaded function " + name);
        
        MultiMap<Integer, Function> partitionedByArgCount = new MultiMap<Integer, Function> ();
        
        for (Function d : defs) {
            partitionedByArgCount.putItem(d.parameters().size(), d);
        }
        
        for(Map.Entry<Integer, Set<Function>> entry : partitionedByArgCount.entrySet()) {
            int i = entry.getKey();
            Set<Function> fs = entry.getValue();
            if (fs.size() > 1) {
                OverloadSet os = new OverloadSet.Local(ai.ast().getName(), name, ta, fs,
                        i);
                
                os.split(true);
                String s = os.toString();
                overloads.add(os);
            }
        }
     
     }

}
