package com.sun.fortress.compiler.environments;

import java.io.IOException;
import java.util.HashMap;

import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.interpreter.env.BetterEnvLevelZero;
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
            System.out.println(compiled.getTypeNull("LessThan"));
            System.out.println(betterEnv.getTypeNull("LessThan"));
            long start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                compiled.getTypeNull("LessThan");
            }
            long difference = System.currentTimeMillis() - start;
            System.out.println("Compiled environments get: " + (difference / (float) iterations) + " msec");
            start = System.currentTimeMillis();
            for(int i = 0; i < iterations; i++) {
                betterEnv.getTypeNull("LessThan");
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
