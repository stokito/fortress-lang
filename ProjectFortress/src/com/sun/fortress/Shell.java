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
    // public static boolean debug;
    static boolean test;
    private static String COMPONENT_SUFFIX = ".fss";
    private static String API_SUFFIX = ".fsi";

    public static String fortressLocation() {
        return ProjectProperties.ANALYZED_CACHE_DIR;
    }

    /* Helper method to print usage message.*/
    private static void printUsageMessage() {
        System.err.println("Usage:");
        System.err.println(" compile [-out file] [-debug [#]]somefile.fs{s,i}");
        System.err.println(" [run] [-test] [-debug [#]] somefile.fss arg...");
        System.err.println(" parse [-out file] [-debug [#]] somefile.fs{s,i}...");
        System.err.println(" typecheck [-out file] [-debug [#]] somefile.fs{s,i}...");
        System.err.println(" help");
    }

    private static void printHelpMessage() {
        System.err.println
        ("Invoked as script: fortress args\n"+
         "Invoked by java: java ... com.sun.fortress.shell.Shell args\n"+
         "fortress [run] [-test] [-debug [#]] somefile.fss arg ...\n"+
         "  Runs somefile.fss through the Fortress interpreter, passing arg ... to the\n"+
         "  run method of somefile.fss.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "   -test : first runs test functions associated with the program.\n"+
         "\n"+
         "fortress compile [-debug [#]] [-out file] somefile.fs{s,i}\n"+
         "  Compile somefile. If compilation succeeds no message will be printed.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "   -out file : dumps the processed abstract syntax tree to a file.\n" +
         "\n"+
         "fortress parse [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Parses a file. If the parse succeeds the message \"Ok\" will be printed.\n"+
         "  If -out file is given, a message about the file being written to will be printed.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "   -out file : dumps the abstract syntax tree to a file.\n"+
         "\n"+
         "fortress typecheck [-out file] [-debug [#]] somefile.fs{i,s}\n"+
         "  Typechecks a file. If the typecheck succeeds no message will be printed.\n"+
         "   -debug : enables debugging to the maximum level and prints java stack traces.\n"+
         "   -debug # : sets debugging to the specified level, where # is a number.\n"+
         "   -out file : dumps the processed abstract syntax tree to a file\n"
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
            if (what.equals("run")) {
                run(Arrays.asList(tokens).subList(1, tokens.length));
            } else if (what.equals("compile")) {
                compile(false, Arrays.asList(tokens).subList(1, tokens.length), Option.<String>none());
            } else if (what.equals("typecheck")) {
                turnOnTypeChecking();
                compile(false, Arrays.asList(tokens).subList(1, tokens.length), Option.<String>none());
            } else if (what.contains(COMPONENT_SUFFIX) || (what.startsWith("-") && tokens.length > 1)) {
                // no "run" command.
                run(Arrays.asList(tokens));
            } else if ( what.equals("parse" ) ){
                parse(Arrays.asList(tokens).subList(1, tokens.length), Option.<String>none());
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
     * Parse a file. If the file parses ok it will say "Ok". If you want a dump then give
     * -out somefile.
     */
    private static void parse(List<String> args, Option<String> out){
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
            if (s.equals("-test")) test = true;
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;

            parse( rest, out );
        } else {
            parse( s, out );
        }
    }

    private static void parse( String file, Option<String> out){
        try{
            CompilationUnit unit = Parser.parseFile(apiName(file), new File(file));
            if ( ! out.isSome() ){
                System.out.println( "Ok" );
            } else {
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

    private static boolean isApi(String file){
        return file.endsWith(API_SUFFIX);
    }

    private static boolean isComponent(String file){
        return file.endsWith(COMPONENT_SUFFIX);
    }

    private static APIName apiName( String file ){
        if ( file.endsWith( COMPONENT_SUFFIX ) || file.endsWith( API_SUFFIX ) ){
            return NodeFactory.makeAPIName(file.substring( 0, file.lastIndexOf(".") ));
        }
        return NodeFactory.makeAPIName(file);
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
    static void compile(boolean doLink, String s, Option<String> out) throws UserError, InterruptedException, IOException {
        try {
            //FortressRepository fileBasedRepository = new FileBasedRepository(shell.getPwd());
            FortressRepository repository = new CacheBasedRepository(fortressLocation());
            Fortress fortress = new Fortress(repository);

            Path path = ProjectProperties.SOURCE_PATH;

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
            } else if ( out.isSome() ){
                try{
                    if ( isApi(s) ){
                        ASTIO.writeJavaAst(repository.getApi(apiName(s)).ast(), out.unwrap());
                    } else if ( isComponent(s) ){
                        ASTIO.writeJavaAst(repository.getComponent(apiName(s)).ast(), out.unwrap());
                    } else {
                        System.out.println( "Don't know what kind of file " + s + " is. Append .fsi or .fss." );
                    }
                } catch ( FileNotFoundException e ){
                    System.out.println( "Could not find file " + s );
                } catch ( IOException e ){
                    System.out.println( "Error while writing " + out.unwrap() );
                }
            }
            // If there are no errors, all components will have been written to disk by the FileBasedRepository.
        }
        catch (RepositoryError error) {
            System.err.println(error);
        }
    }

    static void compile(boolean doLink, List<String> args, Option<String> out) throws UserError, InterruptedException, IOException {
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
                out = Option.wrap(rest.get(0));
                rest = rest.subList(1, rest.size());
            }
            if (s.equals("-test")) test = true;
            if (s.equals("-noPreparse")) ProjectProperties.noPreparse = true;
            compile( doLink, rest, out);
        } else {
            compile( doLink, s, out );
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

            for (StaticError error: errors) {
                System.err.println(error);
            }
            // If there are no errors, all components will have been written to disk by the FileBasedRepository.
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
