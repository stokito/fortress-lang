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

package com.sun.fortress.compiler.environments;

import java.io.IOException;
import java.util.HashMap;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.interpreter.env.BetterEnvLevelZero;
import com.sun.fortress.interpreter.env.CUWrapper;
import com.sun.fortress.interpreter.env.ComponentWrapper;
import com.sun.fortress.interpreter.evaluator.BuildTopLevelEnvironments;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.interpreter.evaluator.types.IntNat;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.repository.ProjectProperties;

public class TopLevelEnvBenchmark {

    public static final int iterations = 100000000;
    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Environment compiled = SimpleClassLoader.loadEnvironment("FortressLibrary", false);
            FortressRepository defaultRepository = new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR);
            ApiIndex library = defaultRepository.getApi(NodeFactory.makeAPIName("FortressLibrary"));
            Environment betterEnv = new BetterEnvLevelZero(library.ast());
            BuildTopLevelEnvironments be = new BuildTopLevelEnvironments(betterEnv, new HashMap<String, ComponentWrapper>());
            be.visit(library.ast());
            //Set verboseDump to true
            //betterEnv.dump(System.out);
            IntNat three = IntNat.make(3);
            compiled.putTypeRaw("LessThan", three);
            betterEnv.putTypeRaw("LessThan", three);
            System.out.println(compiled.getLeafTypeNull("LessThan")); // leaf
            System.out.println(betterEnv.getLeafTypeNull("LessThan")); // leaf
            long start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                compiled.getLeafTypeNull("LessThan"); // leaf
            }
            long difference = System.currentTimeMillis() - start;
            System.out.println("Compiled environments get: " + (difference / (float) iterations) + " msec");
            start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                betterEnv.getLeafTypeNull("LessThan"); // leaf
            }
            difference = System.currentTimeMillis() - start;
            System.out.println("BATree environments get: " + (difference / (float) iterations) + " msec");

            start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                compiled.putTypeRaw("LessThan", three);
            }
            difference = System.currentTimeMillis() - start;
            System.out.println("Compiled environments put: " + (difference / (float) iterations) + " msec");
            start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                betterEnv.putTypeRaw("LessThan", three);
            }
            difference = System.currentTimeMillis() - start;
            System.out.println("BATree environments put: " + (difference / (float) iterations) + " msec");

        } catch (IOException e) {

            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

}
