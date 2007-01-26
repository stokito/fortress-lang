/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

package com.sun.fortress.interpreter.drivers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;
import EDU.oswego.cs.dl.util.concurrent.FJTaskRunnerGroup;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Printer;
import com.sun.fortress.interpreter.nodes.Unprinter;
import com.sun.fortress.interpreter.parser.Fortress;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.interpreter.rewrite.Disambiguate;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.useful.Useful;

public class Driver {

    private static int numThreads = 8;
    private static boolean _libraryTest = false;

    private Driver() {
    };

    public static CompilationUnit readJavaAst(String fileName) throws IOException {
        BufferedReader br = Useful.utf8BufferedFileReader(fileName);
        return readJavaAst(fileName, br);

    }

    /**
     * @param reportedFileName
     * @param br
     * @return
     * @throws IOException
     */
    public static CompilationUnit readJavaAst(String reportedFileName, BufferedReader br)
            throws IOException {
        Lex lex = new Lex(br);
        try {
            Unprinter up = new Unprinter(lex);
            lex.name();
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            return p;
        } finally {
            if (!lex.atEOF())
                System.out.println("Parse of " + reportedFileName
                        + " ended EARLY at line = " + lex.line()
                        + ",  column = " + lex.column());
        }
    }

    public static CompilationUnit parseToJavaAst(String reportedFileName, BufferedReader in)
    throws IOException {
        Fortress p  = new Fortress(in, reportedFileName, (int)new File(reportedFileName).length());
        Result   r  = p.pFile(0);

        if (r.hasValue()) {
            SemanticValue v = (SemanticValue)r;
            CompilationUnit n = (CompilationUnit) v.value;
            return n;
        } else {
          ParseError err = (ParseError)r;
          if (-1 == err.index) {
            System.err.println("  Parse error");
          } else {
            System.err.println("  " + p.location(err.index) + ": " + err.msg);
          }
          return null;
        }
    }

    /**
     * Runs a command and captures its output and errors streams.
     *
     * @param command
     *            The command to run
     * @param output
     *            Output from the command is written here.
     * @param errors
     *            Errors from the command are written here.
     * @param exceptions
     *            If the execution of the command throws an exception, it is
     *            stored here.
     * @return true iff any errors were written.
     * @throws IOException
     */
    public static boolean runCommand(String command, final PrintStream output,
            final IOException[] exceptions, PrintStream errors)
            throws IOException {
        Runtime runtime = Runtime.getRuntime();
        Process p = runtime.exec(command);
        final BufferedReader input_from_process = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
        final BufferedReader errors_from_process = new BufferedReader(
                new InputStreamReader(p.getErrorStream()));

        boolean errors_encountered = false;
        Thread th = new Thread() {
            public void run() {
                try {
                    try {
                        String line = input_from_process.readLine();
                        while (line != null) {
                            output.println(line);
                            line = input_from_process.readLine();
                        }
                    } finally {
                        output.close();
                        input_from_process.close();
                    }
                } catch (IOException ex) {
                    exceptions[0] = ex;
                }
            }
        };

        th.start();

        // Print errors, discarding any leading blank lines.
        String line = errors_from_process.readLine();
        boolean first = true;

        while (line != null) {
            if (!first || line.trim().length() > 0) {
                errors.println(line);
                first = false;
                errors_encountered = true;
            }
            line = errors_from_process.readLine();
        }

        try {
            th.join();
        } catch (InterruptedException ex) {

        }
        return errors_encountered;
    }


    public static void writeJavaAst(CompilationUnit p, String s) throws IOException {
        BufferedWriter fout = Useful.utf8BufferedFileWriter(s);
        writeJavaAst(p, fout);
        fout.close();
    }

    /**
     * @param p
     * @param fout
     * @throws IOException
     */
    public static void writeJavaAst(CompilationUnit p, BufferedWriter fout)
            throws IOException {
        (new Printer()).dump(p, fout, 0);
    }

    static public void runTests() {
    }

    public static BetterEnv evalComponent(CompilationUnit p) {
        Init.initializeEverything();
        BetterEnv e = BetterEnv.empty();
        e.installPrimitives();
        BuildEnvironments be = new BuildEnvironments(e);
        Disambiguate dis = new Disambiguate();
	if (_libraryTest) {
            /*
             * It's not quite clear what's going on here,
             * and this is probably busted after the
             * 4-pass upgrade to BuildEnvironments.
             */ 
	    Glue.installHooks(be.getEnvironment());
            be.secondPass();
            be.resetPass();
	} else {
	    Libraries.link(be, dis);
	}
        p = (CompilationUnit) dis.visit(p); // note that p is unused after the next
                                    // line.
        p.accept(be);
        dis.registerObjectExprs(be.getEnvironment());
        return be.getEnvironment();
    }

    // This runs the program from inside a task.
    public static void runProgramTask(CompilationUnit p, boolean runTests,
            List<String> args) {
        FortressTests.reset();
        BetterEnv e = evalComponent(p);
        Closure run_fn = e.getRunMethod();
        Toplevel toplevel = new Toplevel();
        if (runTests) {
            List<Closure> testClosures = FortressTests.get();
            for (Iterator<Closure> i = testClosures.iterator(); i.hasNext();) {
                Closure testCl = i.next();
                List<FValue> fvalue_args = new ArrayList<FValue>();

                testCl.apply(fvalue_args, toplevel, e);
            }
        }
        ArrayList<FValue> fvalueArgs = new ArrayList<FValue>();
        for (String s : args) {
            fvalueArgs.add(FString.make(s));
        }
        FValue ret = run_fn.apply(fvalueArgs, toplevel, e);
        // try {
        // e.dump(System.out);
        // } catch (IOException ioe) {
        // System.out.println("io exception" + ioe);
        // }
        if (!(ret instanceof FVoid))
            throw new ProgramError("run method returned non-void value");
        System.out.println("finish runProgram");
    }


    static FJTaskRunnerGroup group;

    // This creates the parallel context
    public static void runProgram(CompilationUnit p, boolean runTests,
				  boolean libraryTest,
				  List<String> args) throws Throwable {
	_libraryTest = libraryTest;
        String numThreadsString = System.getenv("NumFortressThreads");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);

        if (group == null)
           group = new FJTaskRunnerGroup(numThreads);

        EvaluatorTask evTask = new EvaluatorTask(p, runTests, args);
        try {
            group.invoke(evTask);
        } catch (InterruptedException ex) {
        } finally {
            // group.interruptAll();
        }
        if (evTask.causedException()) {
            Throwable th =  evTask.getException();
            throw th;
        }
    }

    public static void runProgram(CompilationUnit p, boolean runTests,
				  List<String> args) throws Throwable {
	runProgram(p, runTests, false, args);
    }

    private static class Toplevel implements HasAt {
        public String at() {
            return "toplevel";
        }

        public String stringName() {
            return "driver";
        }
    }

}
