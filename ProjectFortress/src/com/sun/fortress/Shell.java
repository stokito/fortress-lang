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

package com.sun.fortress;

import com.sun.fortress.repository.CacheBasedRepository;
import com.sun.fortress.repository.FortressRepository;
import com.sun.fortress.repository.ProjectProperties;
import java.io.*;
import java.util.*;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.*;
import com.sun.fortress.exceptions.shell.UserError;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.exceptions.FortressException;
import com.sun.fortress.exceptions.shell.RepositoryError;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ASTIO;
import com.sun.fortress.useful.Path;
import com.sun.fortress.useful.Debug;

import static com.sun.fortress.useful.ConvenientStrings.*;

public final class Shell {
    static boolean test;

    /* Helper method to print usage message.*/
    private static void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-out file] [-debug [#]] somefile.fs{s,i}");
        System.err.println(" [run] [-test] [-debug [#]] somefile.fss arg...");
        System.err.println(" parse [-out file] [-debug [#]] somefile.fs{s,i}...");
        System.err.println(" typecheck [-out file] [-debug [#]] somefile.fs{s,i}...");
        System.err.println(" help");
    }

    private static void printHelpMessage() {
        System.err.println
        ("Invoked as script: fortress args\n"+
         "Invoked by java: java ... com.sun.fortress.Shell args\n"+
         "fortress compile [-out file] [-debug [#]] somefile.fs{s,i}\n"+
         "  Compile somefile. If compilation succeeds no message will be printed.\n"+
         "   -out file : dumps the processed abstract syntax tree to a file.\n" +
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "\n"+
         "fortress [run] [-test] [-debug [#]] somefile.fss arg ...\n"+
         "  Runs somefile.fss through the Fortress interpreter, passing arg ... to the\n"+
         "  run method of somefile.fss.\n"+
         "   -test : first runs test functions associated with the program.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "\n"+
         "fortress parse [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Parses a file. If parsing succeeds the message \"Ok\" will be printed.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "   -out file : dumps the abstract syntax tree to a file.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "\n"+
         "fortress typecheck [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Typechecks a file. If type checking succeeds no message will be printed.\n"+
         "   -out file : dumps the processed abstract syntax tree to a file.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"
        );
    }

    private static void turnOnTypeChecking(){
        com.sun.fortress.compiler.StaticChecker.typecheck = true;
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
            List<String> args = Arrays.asList(tokens).subList(1, tokens.length);
            if (what.equals("compile")) {
                compile(args, Option.<String>none());
            } else if (what.equals("run")) {
                run(args);
            } else if ( what.equals("parse" ) ){
                parse(args, Option.<String>none());
            } else if (what.equals("typecheck")) {
                turnOnTypeChecking();
                compile(args, Option.<String>none());
            } else if (what.contains(ProjectProperties.COMP_SOURCE_SUFFIX)
                       || (what.startsWith("-") && tokens.length > 1)) {
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
     * Parse a file. If the file parses ok it will say "Ok".
     * If you want a dump then give -out somefile.
     */
    private static void parse(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
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
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList( 1, rest.size() );
            }
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;

            parse( rest, out );
        } else {
            parse( s, out );
        }
    }

    private static void parse( String file, Option<String> out){
        try{
            CompilationUnit unit = Parser.parseFile(cuName(file), new File(file));
            System.out.println( "Ok" );
            if ( out.isSome() ){
                try{
                    ASTIO.writeJavaAst(unit, out.unwrap());
                    System.out.println( "Dumped parse tree to " + out.unwrap() );
                } catch ( IOException e ){
                    System.err.println( "Error while writing " + out.unwrap() );
                }
            }
        } catch ( FileNotFoundException f ){
            System.err.println( file + " not found" );
        } catch ( IOException ie ){
            System.err.println( "Error while reading " + file );
        } catch ( StaticError s ){
            System.err.println(s);
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

    public static boolean isApi(String file){
        return file.endsWith(ProjectProperties.API_SOURCE_SUFFIX);
    }

    public static boolean isComponent(String file){
        return file.endsWith(ProjectProperties.COMP_SOURCE_SUFFIX);
    }

    public static APIName cuName( String file ){
        if ( file.endsWith( ProjectProperties.COMP_SOURCE_SUFFIX ) ||
             file.endsWith( ProjectProperties.API_SOURCE_SUFFIX ) ){
            return NodeFactory.makeAPIName(file.substring( 0, file.lastIndexOf(".") ));
        }
        return NodeFactory.makeAPIName(file);
    }

    /**
     * Compile a file.
     * If you want a dump then give -out somefile.
     */
    private static void compile(List<String> args, Option<String> out)
        throws UserError, InterruptedException, IOException {
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
            if (s.equals("-out") && ! rest.isEmpty() ){
                out = Option.<String>some(rest.get(0));
                rest = rest.subList(1, rest.size());
            }
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            compile(rest, out);
        } else {
            compile(s, out );
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
    private static void compile(String s, Option<String> out)
        throws UserError, InterruptedException {
        try {
            Fortress fortress = new Fortress();
            Path path = ProjectProperties.SOURCE_PATH;

            /* Questions 1)
               1) Not for parse
               2) What if there are multiple "/"s
             */
            if (s.contains("/")) {
                String head = s.substring(0, s.lastIndexOf("/"));
                s = s.substring(s.lastIndexOf("/")+1, s.length());
                path = path.prepend(head);
            }

            Iterable<? extends StaticError> errors = fortress.compile(path, s);

            if ( errors.iterator().hasNext() ){
                for (StaticError error: errors) {
                    System.err.println(error);
                }
            // If there are no errors, all components will have been written to disk by the CacheBasedRepository.
            } else if ( out.isSome() ){
                try{
                    if ( isApi(s) ){
                        ASTIO.writeJavaAst(fortress.getRepository().getApi(cuName(s)).ast(), out.unwrap());
                    } else if ( isComponent(s) ){
                        ASTIO.writeJavaAst(fortress.getRepository().getComponent(cuName(s)).ast(), out.unwrap());
                    } else {
                        System.out.println( "Don't know what kind of file " + s + " is. Append .fsi or .fss." );
                    }
                } catch ( IOException e ){
                    System.err.println( "Error while writing " + out.unwrap() );
                }
            }
        } catch (RepositoryError error) {
            System.err.println(error);
        }
    }

    /**
     * Run a file.
     */
    private static void run(List<String> args)
        throws UserError, IOException, Throwable {
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
            if (s.equals("-test")) test = true;
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            run(rest);
        } else {
            run(s, rest);
        }
    }

    private static void run(String fileName, List<String> args)
        throws UserError, Throwable {
        try {
            Fortress fortress = new Fortress();

            Path path = ProjectProperties.SOURCE_PATH;

            if (fileName.contains("/")) {
                String head = fileName.substring(0, fileName.lastIndexOf("/"));
                fileName = fileName.substring(fileName.lastIndexOf("/")+1, fileName.length());
                path = path.prepend(head);
            }

            Iterable<? extends StaticError> errors = fortress.run(path, cuName(fileName), test, args);

            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors, all components will have been written to disk by the CacheBasedRepository.
        } catch ( StaticError e ){
            System.err.println(e);
            if ( ProjectProperties.debug ){
                e.printStackTrace();
            }
        } catch (RepositoryError e) {
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
