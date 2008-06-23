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
import java.util.*;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.*;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.interpreter.drivers.*;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.shell.ConvenientStrings.*;

public final class Shell {
    // public static boolean debug;
    static boolean test;

    public static String fortressLocation() {
        return ProjectProperties.ANALYZED_CACHE_DIR;
    }

    /* Helper method to print usage message.*/
    private static void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-debug [#]]somefile.fs{s,i}");
        System.err.println(" [run] [-test] [-debug [#]] somefile.fss arg...");
        System.err.println(" help");
    }

    private static void printHelpMessage() {
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
    public static void main(String[] tokens) throws InterruptedException, Throwable {
        if (tokens.length == 0) {
            printUsageMessage();
            System.exit(-1);
        }

        // Now match the assembled string.
        try {
            String what = tokens[0];
            if (what.equals("run")) {
                run(Arrays.asList(tokens).subList(1, tokens.length));
            } else if (what.equals("compile")) {
                compile(false, Arrays.asList(tokens).subList(1, tokens.length));
            } else if (what.contains(".fss") || (what.startsWith("-") && tokens.length > 1)) {
                // no "run" command.
                run(Arrays.asList(tokens));
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

    /**
     * This compiler uses a different method for determining what to compile,
     * and how to compile it.
     *
     * @param doLink
     * @param s
     * @throws UserError
     * @throws InterruptedException
     * @throws IOException
     */
    static void compile(boolean doLink, String s) throws UserError, InterruptedException, IOException {
        try {
            //FortressRepository fileBasedRepository = new FileBasedRepository(shell.getPwd());
            Fortress fortress = new Fortress(new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR));

            Path path = ProjectProperties.SOURCE_PATH;

            if (s.contains("/")) {
                String head = s.substring(0, s.lastIndexOf("/"));
                s = s.substring(s.lastIndexOf("/")+1, s.length());
                path = path.prepend(head);
            }

            Iterable<? extends StaticError> errors = fortress.compile(path, s);

            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors, all components will have been written to disk by the FileBasedRepository.
        }
        catch (RepositoryError error) {
            System.err.println(error);
        }
    }

    static void compile(boolean doLink, List<String> args) throws UserError, InterruptedException, IOException {
        if (args.size() == 0) {
            throw new UserError("Need a file to compile");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                Debug.setDebug( 99 );
                if ( ! rest.isEmpty() && isInteger( rest.get( 0 ) ) ){
                    Debug.setDebug( Integer.valueOf( rest.get( 0 ) ) );
                    rest = rest.subList( 1, rest.size() );
                } else {
                    ProjectProperties.debug = true;
                }
            }
            if (s.equals("-test")) test = true;
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            compile( doLink, rest);
        } else {
            compile( doLink, s );
        }
    }

    private static boolean isInteger( String s ){
        try{
            int i = Integer.valueOf(s);
            return i == i;
        } catch ( NumberFormatException n ){
            return false;
        }
    }

    static void run(List<String> args) throws UserError, IOException, Throwable {
        if (args.size() == 0) {
            throw new UserError("Need a file to run");
        }
        String s = args.get(0);
        List<String> rest = args.subList(1, args.size());

        if (s.startsWith("-")) {
            if (s.equals("-debug")){
                Debug.setDebug( 99 );
                if ( ! rest.isEmpty() && isInteger( rest.get( 0 ) ) ){
                    Debug.setDebug( Integer.valueOf( rest.get( 0 ) ) );
                    rest = rest.subList( 1, rest.size() );
                } else {
                    ProjectProperties.debug = true;
                }
            }
            if (s.equals("-test")) test= true;
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            run(rest);
        } else {
            run(s, rest);
        }
    }
    static void run(String fileName, List<String> args) throws UserError, IOException, Throwable {
        try {
            //FortressRepository fileBasedRepository = new FileBasedRepository(shell.getPwd());
            Fortress fortress = new Fortress(new CacheBasedRepository(ProjectProperties.ANALYZED_CACHE_DIR));

            Path path = ProjectProperties.SOURCE_PATH;

            if (fileName.contains("/")) {
                String head = fileName.substring(0, fileName.lastIndexOf("/"));
                fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
                path = path.prepend(head);
            }

            /*
             * Strip suffix to get a bare component name.
             */
            if (fileName.endsWith("." + ProjectProperties.COMP_SOURCE_SUFFIX)) {
                fileName = fileName.substring(0, fileName.length() - ProjectProperties.COMP_SOURCE_SUFFIX.length());
            }

            Iterable<? extends StaticError> errors = fortress.run(path, fileName, test, args);

            /* FIXME: this is a hack to get around some null pointer exceptions
             * if a WrappedException is printed as-is.
             */
            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors, all components will have been written to disk by the FileBasedRepository.
        }
        catch (RepositoryError e) {
            System.err.println(e.getMessage());
        }
        catch (FortressException e) {
            System.err.println(e.getMessage());
            e.printInterpreterStackTrace(System.err);
            if (ProjectProperties.debug) {
                e.printStackTrace();
            } else {
                System.err.println("Turn on -debug for Java-level error dump.");
            }
            System.exit(1);
        }
    }
}
