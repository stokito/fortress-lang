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

package com.sun.fortress.shell;

import java.io.*;
import java.util.regex.Pattern;
import java.util.Arrays;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;

import static com.sun.fortress.shell.ConvenientStrings.*;

public final class Shell {
    /* Patterns for parsing shell messages.*/
//    private static final String NAME =                   "[\\S]+";
//    private static final String COMPILE_PATTERN =        "compile " + NAME + "((.fss)|(.fsi))";
//    private static final String SELF_UPGRADE_PATTERN =   "selfupgrade " + NAME + ".tar";
//    private static final String SCRIPT_PATTERN =         "script " + NAME + ".fsx";
//    private static final String RUN_PATTERN =            "run " + NAME;
//    private static final String API_PATTERN =            "api " + NAME + ".fss";
//    private static final String LINK_PATTERN =           "link " + NAME + " from " + NAME + " with " + NAME;
//    private static final String UPGRADE_PATTERN =        "upgrade " + NAME + " from " + NAME + " with " + NAME;
//    private static final String EXISTS_PATTERN =         "exists " + NAME;

    private String pwd;
    /* Relative location of the resident fortress to the jar file this class is packaged into. */
    private String components;

    private CommandInterpreter interpreter;

    public Shell(String _pwd) {
        pwd = _pwd;
        // For now, assume compiled components and APIs are in pwd.
        components = pwd;
        interpreter = new CommandInterpreter(this);
    }

    String getPwd() { return pwd; }
    String getComponents() { return components; }
    CommandInterpreter getInterpreter() { return interpreter; }

    public static String fortressLocation() {
        return ProjectProperties.ANALYZED_CACHE_DIR;
    }

    /* Helper method to print usage message.*/
    private void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-debug [#]]somefile.fs{s,i}");
        //System.err.println("  " + SELF_UPGRADE_PATTERN);
        //System.err.println("  " + SCRIPT_PATTERN);
        System.err.println(" [run] [-test] [-debug [#]] somefile.fss arg...");
        //System.err.println("  " + API_PATTERN);
        //System.err.println("  " + LINK_PATTERN);
        //System.err.println("  " + UPGRADE_PATTERN);
        System.err.println(" help");
    }

    private void printHelpMessage() {
        System.err.println
        ("Invoked as script: fortress args\n"+
         "Invoked by java: java ... com.sun.fortress.shell.Shell args\n"+
         "args: compile somefile.fs{s,i}\n"+
         "  ensures that component or interface 'somefile' is up-to-date,\n"+
         "  checked and present in the cache\n"+
         "args: [run] [-test] [-debug] somefile.fss more_args\n"+
         "  compile somefile.fss and link/compile all APIs and components\n"+
         "  necessary to run it, and then run it, passing more_args to the\n"+
         "  fortress program.  A runnable fortress program exports Executable\n"+
         "  and supplies a run(args:String...) function.\n"+
         "  -test  first runs test functions associated with the program.\n"+
         "  -debug includes Java stack traces with any errors, and enables\n"+
         "         additional output."
        );
    }

    /* Main entry point for the fortress shell.*/
    public void execute(String[] tokens) throws InterruptedException, Throwable {

        if (tokens.length == 0) {
            printUsageMessage();
            System.exit(-1);
        }

        // Now match the assembled string.
        try {
            String what = tokens[0];
            if (what.equals("run")) {
                interpreter.run(Arrays.asList(tokens).subList(1, tokens.length));
            } else if (what.equals("compile")) {
                interpreter.compile(false, Arrays.asList(tokens).subList(1, tokens.length));
            } else if (what.contains(".fss") || (what.startsWith("-") && tokens.length > 1)) {
                // no "run" command.
                interpreter.run(Arrays.asList(tokens));
            } else if (what.equals("help")) {
                printHelpMessage();

            } else { printUsageMessage(); }
        }
        catch (UserError error) {
            System.err.println(error.getMessage());
        }
        catch (IOException error) {
            System.err.println(error.getMessage());
        }
    }

    public static void main(String[] args) throws InterruptedException, Throwable {
        new Shell("").execute(args);
    }

}
