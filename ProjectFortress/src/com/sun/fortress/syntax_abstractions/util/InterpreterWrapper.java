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

package com.sun.fortress.syntax_abstractions.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.interpreter.drivers.ASTIO;
import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.fortress.interpreter.evaluator.FortressError;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Export;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes.VarargsType;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.tuple.Option;

/**
 * Class which is a wrapper around the Fortress interpreter.
 * You can feed the wrapper an expression and then it returns a 
 * Fortress value ({@see com.sun.fortres.interpreter.values.FValue}).
 */
public class InterpreterWrapper {

    public class Result extends StaticPhaseResult {

        private FValue value;

        Result(FValue value, Iterable<? extends StaticError> errors) {
            super(errors);
            this.value = value;
        }

        public FValue value() { return value; }
    }

    public static final String FUNCTIONNAME = "transformation";
    private static final boolean test = false;
    private static final boolean libraryTest = false;
    private static final boolean woLibrary = false;
    private List<String> listArgs;
    private static int numThreads = Runtime.getRuntime().availableProcessors();
    static FortressTaskRunnerGroup group;

    public Result evalComponent(String productionName, String component) {
        Collection<StaticError> errors = new LinkedList<StaticError>();
        Option<CompilationUnit> cu = Option.none();
        try {
            cu = readAST(productionName, component);
        } catch (IOException e1) {
            errors.add(StaticError.make("Could not read transformation expression from file: "+productionName+" "+e1.getMessage(), productionName));
            return new Result(null, errors);
        }
        CompilationUnit compilationUnit = null;
        if (cu.isNone()) {
            errors.add(StaticError.make("Could not read transformation expression from file: "+productionName, productionName));
            return new Result(null, errors);
        }

        compilationUnit = Option.unwrap(cu);

        try {
        	System.err.println("Running interpreter...");
            return new Result(runFunction(compilationUnit), errors);
        }	catch (FortressError e) {
            System.err.println("\n--------Fortress error appears below--------\n");
            e.printInterpreterStackTrace(System.err);
            System.err.println();
            System.err.println(e.getMessage());
            System.err.println("Turn on -debug for Java-level error dump.");
            System.exit(1);
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private FValue runFunction(CompilationUnit compilationUnit) throws Throwable {
        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);

        if (group == null)
            group = new FortressTaskRunnerGroup(numThreads);

        if (listArgs == null) {
            listArgs = new LinkedList<String>();
        }

        EvaluatorTask evTask = new EvaluatorTask(Driver.DEFAULT_INTERPRETER_REPOSITORY, compilationUnit, test, woLibrary, FUNCTIONNAME, listArgs);
        try {
            group.invoke(evTask);
        }
        finally {
//          group.interruptAll();
        }
        if (evTask.causedException()) {
            throw evTask.taskException();
        }
        System.err.println("EvTask: "+evTask.result());
        return evTask.result();
    }

    private Option<CompilationUnit> readAST(String filename, String component) throws IOException {
        StringReader sr = new StringReader(component);
        BufferedReader br = new BufferedReader(sr);
        return ASTIO.readJavaAst(filename, br);
    }

}
