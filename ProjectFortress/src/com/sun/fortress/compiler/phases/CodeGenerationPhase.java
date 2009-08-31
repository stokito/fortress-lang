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

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.codegen.CodeGen;
import com.sun.fortress.compiler.codegen.ParallelismAnalyzer;
import com.sun.fortress.compiler.codegen.FreeVariables;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.scala_src.typechecker.TraitTable;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.MultiMap;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.iter.IterUtil;

import java.util.Map;
import java.util.Set;

public class CodeGenerationPhase extends Phase {

    public static final boolean debugOverloading = false;

    public CodeGenerationPhase(Phase parentPhase) {
        super(parentPhase);
    }


    @Override
    public AnalyzeResult execute() throws StaticError {
        Debug.debug(Debug.Type.FORTRESS, 1, "Start phase CodeGeneration");
        AnalyzeResult previous = parentPhase.getResult();

        Debug.debug(Debug.Type.CODEGEN,
                    1,
                    "CodeGenerationPhase: components " + previous.components() + " apis = " + previous.apis().keySet());

        GlobalEnvironment apiEnv = new GlobalEnvironment.FromMap(CollectUtil.union(env.apis(), previous.apis()));
        
        
        for (APIName api : previous.apis().keySet()) {
            if (ForeignJava.only.foreignApiNeedingCompilation(api)) {
                ApiIndex ai = previous.apis().get(api);
                TypeAnalyzer ta = new TypeAnalyzer(new TraitTable(ai, apiEnv));

                Relation<IdOrOpOrAnonymousName, Function> fns = ai.functions();
                Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>>
                size_partitioned_overloads = CodeGen.sizePartitionedOverloads(fns);
                Set<OverloadSet> overloads = new BASet<OverloadSet>(DefaultComparator.<OverloadSet>normal());

                for (IdOrOpOrAnonymousName name : fns.firstSet()) {
                    PredicateSet<Function> defs = fns.matchFirst(name);
                    if (defs.size() > 1) {
                        foundAnOverLoadedForeignFunction(ai, ta, name, defs, overloads);
                    }
                }
                ForeignJava.only.generateWrappersForApi(api, overloads, size_partitioned_overloads, ta);
                // Need to generate overloaded functions -- where?
            }
        }

        for (Component component : previous.componentIterator()) {
            Debug.debug(Debug.Type.CODEGEN, 1, "CodeGenerationPhase: Compile(" + component.getName() + ")");
            ComponentIndex ci = previous.components().get(component.getName());
            Relation<IdOrOpOrAnonymousName, Function> fns = ci.functions();
            TypeAnalyzer ta = new TypeAnalyzer(new TraitTable(ci, apiEnv));

            // Temporary code
            ParallelismAnalyzer pa = new ParallelismAnalyzer();
            component.accept(pa);
            pa.printTable();

            // Compute locally bound variables, necessary for closure conversion and
            // task creation.
            FreeVariables lbv = new FreeVariables();
            component.accept(lbv);

            CodeGen c = new CodeGen(component, ta, pa, lbv, ci, apiEnv);
            component.accept(c);
        }

        return new AnalyzeResult(previous.apis(),
                                 previous.components(),
                                 IterUtil.<StaticError>empty(),
                                 previous.typeCheckerOutput());

    }


    /**
     * @param ai
     * @param name
     * @param defs
     * @param overloads
     */
    private void foundAnOverLoadedForeignFunction(ApiIndex ai,
                                                  TypeAnalyzer ta,
                                                  IdOrOpOrAnonymousName name,
                                                  PredicateSet<Function> defs,
                                                  Set<OverloadSet> overloads) {
        // Woo-hoo, an overloaded function.
        if (debugOverloading) System.err.println("Found an overloaded function " + name);

        MultiMap<Integer, Function> partitionedByArgCount = new MultiMap<Integer, Function>();

        for (Function d : defs) {
            partitionedByArgCount.putItem(d.parameters().size(), d);
        }

        for (Map.Entry<Integer, Set<Function>> entry : partitionedByArgCount.entrySet()) {
            int i = entry.getKey();
            Set<Function> fs = entry.getValue();
            if (fs.size() > 1) {
                OverloadSet os = new OverloadSet.Local(ai.ast().getName(), name, ta, fs, i);

                os.split(true);
                String s = os.toString();
                overloads.add(os);
            }
        }

    }

}
