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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import edu.rice.cs.plt.tuple.Option;

import xtc.parser.ParseError;
import xtc.parser.Result;
import xtc.parser.SemanticValue;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.env.FortressTests;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.Init;
import com.sun.fortress.interpreter.evaluator.RedefinitionError;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObject;
import com.sun.fortress.interpreter.evaluator.types.FTraitOrObjectOrGeneric;
import com.sun.fortress.interpreter.evaluator.types.FType;
import com.sun.fortress.interpreter.evaluator.tasks.BaseTask;
import com.sun.fortress.interpreter.evaluator.tasks.EvaluatorTask;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunner;
import com.sun.fortress.interpreter.evaluator.tasks.FortressTaskRunnerGroup;
import com.sun.fortress.interpreter.evaluator.values.Closure;
import com.sun.fortress.interpreter.evaluator.values.FString;
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.evaluator.values.FVoid;
import com.sun.fortress.interpreter.glue.Glue;
import com.sun.fortress.nodes.AliasedDottedName;
import com.sun.fortress.nodes.AliasedName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.ImportFrom;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.parser.Fortress;
import com.sun.fortress.interpreter.reader.Lex;
import com.sun.fortress.interpreter.rewrite.Desugarer;
import com.sun.fortress.useful.CheckedNullPointerException;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.PureList;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Visitor2;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Printer;
import com.sun.fortress.nodes_util.Unprinter;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

public class Driver {

    private static int numThreads = Runtime.getRuntime().availableProcessors();

    private static boolean _libraryTest = false;

    private static String LIB_DIR = ProjectProperties.TEST_LIB_DIR;
    private static String LIB_NATIVE_DIR = ProjectProperties.TEST_LIB_NATIVE_DIR;

    public final static String COMP_SOURCE_SUFFIX = "fss";
    public final static String COMP_TREE_SUFFIX = "tfs";
    public final static String API_SOURCE_SUFFIX = "fsi";
    public final static String API_TREE_SUFFIX = "tfi";

    private Driver() {};

    public static Option<CompilationUnit> readJavaAst(String fileName)
            throws IOException {
        BufferedReader br = Useful.utf8BufferedFileReader(fileName);
        try { return readJavaAst(fileName, br); }
        finally { br.close(); }
    }

    /**
     * @param reportedFileName
     * @param br
     * @throws IOException
     */
    public static Option<CompilationUnit>
        readJavaAst(String reportedFileName, BufferedReader br)
        throws IOException
    {
        Lex lex = new Lex(br);
        try {
            Unprinter up = new Unprinter(lex);
            lex.name();
            CompilationUnit p = (CompilationUnit) up.readNode(lex.name());
            if (p == null) { return Option.none(); }
            else { return Option.some(p); }
        }
        finally {
            if (!lex.atEOF())
                System.out.println("Parse of " + reportedFileName
                        + " ended EARLY at line = " + lex.line()
                        + ",  column = " + lex.column());
        }
    }

    public static Option<CompilationUnit> parseToJavaAst (
            String reportedFileName, BufferedReader in
        )
        throws IOException
    {
        Fortress p =
            new Fortress(in,
                         reportedFileName,
                         (int) new File(reportedFileName).length());
        Result r = p.pFile(0);

        if (r.hasValue()) {
            SemanticValue v = (SemanticValue) r;
            CompilationUnit n = (CompilationUnit) v.value;
            return Option.some(n);
        }
        else {
            ParseError err = (ParseError) r;
            if (-1 == err.index) {
                System.err.println("  Parse error");
            }
            else {
                System.err.println("  " + p.location(err.index) + ": "
                        + err.msg);
            }
            return Option.none();
        }
    }

    /**
     * Convenience method for calling parseToJavaAst with a default BufferedReader.
     */
    public static Option<CompilationUnit> parseToJavaAst(String reportedFileName) throws IOException {
        BufferedReader r = Useful.utf8BufferedFileReader(reportedFileName);
        try { return parseToJavaAst(reportedFileName, r); }
        finally { r.close(); }
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

    /* This function has no callers.  I'd rather not create new threads unless we
       have a good reason.  I'm commenting this out until someone squeals.
       Christine
    */
//     public static boolean runCommand(String command, final PrintStream output,
//             final IOException[] exceptions, PrintStream errors)
//             throws IOException {
//         Runtime runtime = Runtime.getRuntime();
//         Process p = runtime.exec(command);
//         final BufferedReader input_from_process = new BufferedReader(
//                 new InputStreamReader(p.getInputStream()));
//         final BufferedReader errors_from_process = new BufferedReader(
//                 new InputStreamReader(p.getErrorStream()));

//         boolean errors_encountered = false;
//         Thread th = new Thread() {
//             public void run() {
//                 try {
//                     try {
//                         String line = input_from_process.readLine();
//                         while (line != null) {
//                             output.println(line);
//                             line = input_from_process.readLine();
//                         }
//                     } finally {
//                         output.close();
//                         input_from_process.close();
//                     }
//                 } catch (IOException ex) {
//                     exceptions[0] = ex;
//                 }
//             }
//         };

//         th.start();

//         // Print errors, discarding any leading blank lines.
//         String line = errors_from_process.readLine();
//         boolean first = true;

//         while (line != null) {
//             if (!first || line.trim().length() > 0) {
//                 errors.println(line);
//                 first = false;
//                 errors_encountered = true;
//             }
//             line = errors_from_process.readLine();
//         }

//         try {
//             th.join();
//         } catch (InterruptedException ex) {

//         }
//         return errors_encountered;
//     }

    public static void writeJavaAst(CompilationUnit p, String s)
            throws IOException {
        BufferedWriter fout = Useful.utf8BufferedFileWriter(s);
        try { writeJavaAst(p, fout); }
        finally { fout.close(); }
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

    public static BetterEnv evalComponent(CompilationUnit p) throws IOException {
        return evalComponent(p, false);
    }
    
    public static ArrayList<ComponentWrapper> components;

    public static BetterEnv evalComponent(CompilationUnit p,
                                          boolean woLibrary) throws IOException {

        Init.initializeEverything();

        /*
         * Begin "linker" -- to be replaced with the real thing, when more
         * infrastructure is present.
         */

        HashMap<String, ComponentWrapper> linker = new HashMap<String, ComponentWrapper>();

        Stack<ComponentWrapper> pile = new Stack<ComponentWrapper>();
        // ArrayList<ComponentWrapper>
        components = new ArrayList<ComponentWrapper>();

        ComponentWrapper comp = new ComponentWrapper((Component) p, false);

        /*
         * This "linker" implements a one-to-one, same-name correspondence
         * between APIs and components.
         *
         * phase 0.5: starting with the main component, identify other
         * components that will be needed (the fortress will contain the map
         * from component C's imported APIs to their implementing components).
         * Each component will be paired with its top level environment (TLE).
         *
         * phase 1: for each component, prepare its TLE by creating all name
         * slots (nothing in the slots will is initialized AT ALL -- it's all
         * thunks, empty mutables, and uninitialized types).
         *
         * phase 1.5: for each component C, add to C's TLE all the names that C
         * imports (using the API->component map from the fortress to figure out
         * the values/types associated with each of those names).
         *
         */
        comp.getExports(false);
        linker.put("main", comp);
        pile.push(comp);


        /*
         * This is a patch; eventually, it will all be done explicitly.
         */
        ComponentWrapper lib = new ComponentWrapper(Libraries.theLibrary(), false);
        lib.getEnvironment().installPrimitives();
        lib.getExports(true);
        pile.push(lib);

        /*
         * This performs closure over APIs and components, ensuring that all are
         * initialized through phase one of building environments.
         */
        while (!pile.isEmpty()) {

            ComponentWrapper cw = pile.pop();
            components.add(cw);

            CompilationUnit c = cw.getComponent();
            List<Import> imports = c.getImports();

            ensureImportsImplemented(linker, pile, imports);
        }

        // Desugarer needs to know about trait members.
        for (ComponentWrapper cw : components) {
            cw.preloadTopLevel();
        }

        // Iterate to a fixed point, pushing trait info as far as necessary.
        boolean change = true;
        while (change) {
            change = false;
            for (ComponentWrapper cw : components) {
                change |= injectTraitMembersForDesugaring(linker, cw);
            }

            for (ComponentWrapper cw : components) {
                for (String s : lib.dis.getTopLevelRewriteNames()) {
                    if (!cw.isOwnNonFunctionName(s))
                        change |= cw.dis.injectAtTopLevel(s, s, lib.dis);
                }
            }
        }

        /*
         * After all apis etc have been imported, populate their environments.
         */
        for (ComponentWrapper cw : components) {
            // System.err.println("populating " + cw);
            cw.populateOne();
        }

        /*
         * Inject imported names into environments.
         *
         * TODO traits and objects must have their members filtered in these
         * injections. This is not at all simple in the case of a component
         * exporting multiple APIs that may also have different views of the
         * same trait, when some subset of those APIs is imported in another
         * component. Because this is not simple, because the many-to-many case
         * is not yet handled, it is not yet implemented. Getting this right
         * will probably affect the code that builds object types and deals with
         * overloading there.
         */
        for (ComponentWrapper cw : components) {
            /*
             * Transitional stuff. Import everything from "library" into a
             * Component.
             */

            if (cw != lib && !woLibrary)
                importAllExcept(cw.getEnvironment(), lib.getEnvironment(), lib.getEnvironment(),
                        cw.ownNonFunctionNames, // Collections.<String> emptyList(),
                        "FortressLibrary",
                        NodeUtil.nameString(cw.getComponent().getName()));

            injectExplicitImports(linker, cw);
        }

        for (ComponentWrapper cw : components) {
            cw.initTypes();
        }
        for (ComponentWrapper cw : components) {
            scanAllFunctionalMethods(cw.getEnvironment());
        }
        for (ComponentWrapper cw : components) {
            cw.initFuncs();
        }
        for (ComponentWrapper cw : components) {
            finishAllFunctionalMethods(cw.getEnvironment());
        }
        
        for (ComponentWrapper cw : components) {
            cw.initVars();
        }

        // Libraries.link(be, dis);

        return comp.getEnvironment();
    }

    private static void injectExplicitImports(
            HashMap<String, ComponentWrapper> linker, ComponentWrapper cw) {
        CompilationUnit c = cw.getComponent();
        List<Import> imports = c.getImports();

        final BetterEnv e = cw.getEnvironment();

        for (Import i : imports) {
            if (i instanceof ImportApi) {
                ImportApi ix = (ImportApi) i;
                List<AliasedDottedName> apis = ix.getApis();
                for (AliasedDottedName adi : apis) {
                    DottedName id = adi.getApi();
                    String from_apiname = NodeUtil.nameString(id);

                    Option<DottedName> alias = adi.getAlias();
                    String known_as = NodeUtil.nameString(Option.unwrap(alias, id));

                    ComponentWrapper from_cw = linker.get(from_apiname);

                    /*
                     * Every name N in api A with optional alias B is added
                     * to e, using the value from the component C
                     * implementing A. If B is present, C.N is referenced as
                     * B.N, else it is referenced as A.N.
                     */

                    /*
                     * Not-yet-implemented because of issues with selectors.
                     */
                    bug(adi, errorMsg("Import of dotted names from API ",
                                      adi, "; try import * from insteand."));
                }

            } else if (i instanceof ImportFrom) {
                ImportFrom ix = (ImportFrom) i;
                DottedName source = ix.getApi();
                String from_apiname = NodeUtil.nameString(source);

                ComponentWrapper from_cw = linker.get(from_apiname);
                BetterEnv from_e = from_cw.getEnvironment();
                BetterEnv api_e = from_cw.getExportedCW(from_apiname)
                        .getEnvironment();

                /* Pull in names, UNqualified */

                if (ix instanceof ImportNames) {
                    /* A set of names */
                    List<AliasedName> names = ((ImportNames) ix).getAliasedNames();
                    for (AliasedName an : names) {
                        SimpleName name = an.getName();
                        Option<SimpleName> alias = an.getAlias();
                        /*
                         * If alias exists, associate the binding from
                         * component_wrapper with alias, otherwise associate
                         * it with plain old name.
                         */

                        inject(e, api_e, from_e, name, alias, from_apiname,
                               NodeUtil.nameString(from_cw.getComponent().getName()));
                    }

                } else if (ix instanceof ImportStar) {
                    /* All names BUT excepts, as they are listed.
                     * Include all names defined locally in the "except" list,
                     * because local definitions block import-* imports.
                     */
                    final List<SimpleName> excepts = ((ImportStar) ix)
                            .getExcept();
                    final Collection<String> except_names = Useful.applyToAllInserting(
                            excepts, new Fn<SimpleName, String>() {
                                public String apply(SimpleName n) {
                                    return NodeUtil.nameString(n);
                                }
                            },
                            cw.ownNonFunctionNames.copy());

                    importAllExcept(e, api_e, from_e, except_names,
                                    from_apiname,
                                    NodeUtil.nameString(from_cw.getComponent().getName()));

                }
            } else {

            }
        }
    }

    /**
     * Copies rewriting information (traits, what members they define) from apis into
     * components.  This scaffolding is necessary to get the simplification right
     * for member (field, method) references.
     *
     * @param linker
     * @param cw
     */
    private static boolean injectTraitMembersForDesugaring(
            HashMap<String, ComponentWrapper> linker, ComponentWrapper cw) {
        CompilationUnit c = cw.getComponent();
        List<Import> imports = c.getImports();
        boolean change = false;

        for (Import i : imports) {
            if (i instanceof ImportApi) {

                    /*
                     * Not-yet-implemented because of issues with selectors.
                     */
                    bug(errorMsg("NYI Import of dotted names ; try 'import * from' instead."));

            } else if (i instanceof ImportFrom) {
                ImportFrom ix = (ImportFrom) i;
                DottedName source = ix.getApi();
                String from_apiname = NodeUtil.nameString(source);

                ComponentWrapper from_cw = linker.get(from_apiname);
                ComponentWrapper api_cw = from_cw.getExportedCW(from_apiname);

                /* Pull in names, UNqualified */

                if (ix instanceof ImportNames) {
                    /* A set of names */
                    List<AliasedName> names = ((ImportNames) ix).getAliasedNames();
                    for (AliasedName an : names) {
                        SimpleName name = an.getName();
                        Option<SimpleName> alias = an.getAlias();
                        /*
                         * If alias exists, associate the binding from
                         * component_wrapper with alias, otherwise associate
                         * it with plain old name.
                         */
                        change |= cw.dis.injectAtTopLevel(Option.unwrap(alias, name).stringName(), name.stringName(), api_cw.dis);

                    }

                } else if (ix instanceof ImportStar) {
                    /* All names BUT excepts, as they are listed. */
                    final List<SimpleName> excepts = ((ImportStar) ix)
                            .getExcept();
                    final Set<String> except_names = Useful.applyToAllInserting(
                            excepts,
                            new Fn<SimpleName, String>() {
                                public String apply(SimpleName n) {
                                    return NodeUtil.nameString(n);
                                }
                            },
                            new HashSet<String>());
                    
                    for (String s : api_cw.dis.getTopLevelRewriteNames()) {
                        if (! except_names.contains(s) && !cw.isOwnNonFunctionName(s)) {
                            change |= cw.dis.injectAtTopLevel(s, s, api_cw.dis);
                        }
                    }


                }
            } else {
                bug(errorMsg("NYI Import " + i));
            }
        }
        return change;
    }

    private static void scanAllFunctionalMethods(final BetterEnv e) {
        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                if (o instanceof FTraitOrObjectOrGeneric) {
                    FTraitOrObjectOrGeneric tooog = (FTraitOrObjectOrGeneric) o;
                    tooog.initializeFunctionalMethods(e);
                    if (tooog instanceof FTraitOrObject) {
                        for (FType t : ((FTraitOrObject)tooog).getProperTransitiveExtends())
                            if (t instanceof FTraitOrObjectOrGeneric)
                                ((FTraitOrObject)t).initializeFunctionalMethods(e);
                    } else {
                        
                    }
                }
            }
        };
        e.visit(vt, null, null, null, null);
    }
    
    private static void finishAllFunctionalMethods(final BetterEnv e) {
        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                if (o instanceof FTraitOrObjectOrGeneric) {
                    FTraitOrObjectOrGeneric tooog = (FTraitOrObjectOrGeneric) o;
                    tooog.finishFunctionalMethods(e);
                    if (tooog instanceof FTraitOrObject) {
                        for (FType t : ((FTraitOrObject)tooog).getProperTransitiveExtends())
                            if (t instanceof FTraitOrObjectOrGeneric)
                                ((FTraitOrObject)t).finishFunctionalMethods(e);
                    }
                }
            }
        };
        e.visit(vt, null, null, null, null);
    }

    
    private static void notImport(String s, Object o, String api, String component) {
//        System.err.println("Not importing from " + api + " into " + component + " name " + s + ", value " + o);
    }
    
    /**
     * @param into_e
     * @param from_e
     * @param except_names
     *
     * The current import process can attempt to import some entities
     * twice into the same component.  This happesn in the case of
     * certain kinds of recursive api import, and also (more
     * importantly) when there are imports in FortressLibrary.fss.
     * Eventually fixes elsewhere in the system should eliminate this
     * problem; in the mean time we simply catch duplicate insertions
     * here and silently ignore them.  This is a stopgap measure that
     * should go away.
     */
    private static boolean importAllExcept(final BetterEnv into_e,
            BetterEnv api_e, final BetterEnv from_e,
            final Collection<String> except_names, final String a, final String c) {
        
        final boolean[] flag = new boolean[1];

        Visitor2<String, FType> vt = new Visitor2<String, FType>() {
            public void visit(String s, FType o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putType(s, NI.cnnf(from_e.getTypeNull(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        /*
         * Potential problem here, we have to make overloading work when we put
         * a function.
         */
        Visitor2<String, FValue> vv = new Visitor2<String, FValue>() {
            public void visit(String s, FValue o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putValue(s, NI.cnnf(from_e.getValueRaw(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                    }
                }
            }
        };
        Visitor2<String, Number> vi = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putInt(s, NI.cnnf(from_e.getIntNull(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        Visitor2<String, Number> vn = new Visitor2<String, Number>() {
            public void visit(String s, Number o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putNat(s, NI.cnnf(from_e.getNat(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                }
            }
            }
        };
        Visitor2<String, Boolean> vb = new Visitor2<String, Boolean>() {
            public void visit(String s, Boolean o) {
                try {
                    if (!except_names.contains(s)) {
                        into_e.putBool(s, NI.cnnf(from_e.getBool(s)));
                    } else {
                        notImport(s, o, a, c);
                    }
                } catch (CheckedNullPointerException ex) {
                    error("Import of " + s + " from api " + a
                          + " not found in implementing component " + c);
                } catch (RedefinitionError re) {
                    if (re.existingValue == null ||
                        re.existingValue != re.attemptedReplacementValue) {
                        /* Completely new or bogus definition. */
                        throw re;
                    } else {
                        /* Redefining entity as itself; silently ignore. */
                    }
                }
            }
        };

        api_e.visit(vt, vn, vi, vv, vb);
        return flag[0];
    }

    private static void inject(BetterEnv e, BetterEnv api_e, BetterEnv from_e,
            SimpleName name, Option<SimpleName> alias, String a, String c) {
        String s = NodeUtil.nameString(name);
        String add_as = s;
        if (alias.isSome()) {
            add_as = NodeUtil.nameString(Option.unwrap(alias));
        }
        try {
            if (api_e.getNatNull(s) != null) {
                e.putNat(add_as, NI.cnnf(from_e.getNatNull(s)));
            }
            if (api_e.getIntNull(s) != null) {
                e.putInt(add_as, NI.cnnf(from_e.getIntNull(s)));
            }
            if (api_e.getBoolNull(s) != null) {
                e.putBool(add_as, NI.cnnf(from_e.getBoolNull(s)));
            }
            if (api_e.getTypeNull(s) != null) {
                e.putType(add_as, NI.cnnf(from_e.getTypeNull(s)));
            }
            if (api_e.getValueRaw(s) != null) {
                e.putValue(add_as, NI.cnnf(from_e.getValueRaw(s)));
            }
        } catch (CheckedNullPointerException ex) {
            error(errorMsg("Import of ", name, " from api ", a,
                           " not found in implementing component ", c));
        }

    }

    /**
     * For each imported API, checks to see if it is already "linked". If not,
     * "finds" it (looks for apiname.fss/apiname.tfs) and reads it in and sticks
     * it in the "pile".
     *
     * For each component, also reads in the corresponding APIs so that we know
     * what to export.
     *
     * @param linker
     * @param pile
     * @param imports
     */
    private static void ensureImportsImplemented (
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile,
            List<Import> imports
        )
        throws IOException
    {

        for (Import i : imports) {
            if (i instanceof ImportApi) {
                ImportApi ix = (ImportApi) i;
                List<AliasedDottedName> apis = ix.getApis();
                for (AliasedDottedName adi : apis) {
                    DottedName id = adi.getApi();
                    ensureApiImplemented(linker, pile, id);
                }
            }
            else if (i instanceof ImportFrom) {
                ImportFrom ix = (ImportFrom) i;
                DottedName source = ix.getApi();
                ensureApiImplemented(linker, pile, source);
            }
            else {
                bug(i, "Unrecognized import");
            }
        }
    }

    /**
     * @param linker
     * @param pile
     * @param id
     */
    private static void ensureApiImplemented(
            HashMap<String, ComponentWrapper> linker,
            Stack<ComponentWrapper> pile, DottedName name) throws IOException {
        String apiname = NodeUtil.nameString(name);
        if (linker.get(apiname) == null) {
            /*
             * Here, the linker prototype takes the extreme shortcut of assuming
             * that Api Foo is implemented by Component Foo, dots and all.
             *
             * These few lines are what needs to be replaced by a real linker.
             */
            Api newapi = readTreeOrSourceApi(apiname, LIB_DIR + apiname);
            Component newcomp;
            boolean is_native = false;
            try {
                newcomp = readTreeOrSourceComponent(apiname, LIB_DIR + apiname);
            } catch (Exception ex) {
                try {
                    newcomp = readTreeOrSourceComponent(apiname, LIB_NATIVE_DIR + apiname);
                    is_native = true;
                } catch (Exception ex1) {
                    newcomp = error(errorMsg(ex, " AND ", ex1));
                }
            }
            ComponentWrapper apicw = new ComponentWrapper(newapi, false);
            ComponentWrapper newwrapper = new ComponentWrapper(newcomp, apicw, is_native);
            newwrapper.getExports(true);
            linker.put(apiname, newwrapper);
            pile.push(newwrapper);

            List<Import> imports = newcomp.getImports();
            // This is what pile.push is for:
            // ensureImportsImplemented(linker, pile, imports);

            imports = newapi.getImports();
            ensureImportsImplemented(linker, pile, imports);
        }
    }

    // This runs the program from inside a task.
    public static void
        runProgramTask(CompilationUnit p, boolean runTests, boolean woLibrary,
                       List<String> args)
        throws IOException
    {

        FortressTests.reset();
        BetterEnv e = evalComponent(p, woLibrary);

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
            error("run method returned non-void value");
        System.out.println("finish runProgram");
    }

    static FortressTaskRunnerGroup group;

    // This creates the parallel context
    public static void runProgram(CompilationUnit p,
                                  boolean runTests,
                                  boolean libraryTest,
                                  boolean woLibrary,
                                  List<String> args)
        throws Throwable
    {
        _libraryTest = libraryTest;
        String numThreadsString = System.getenv("FORTRESS_THREADS");
        if (numThreadsString != null)
            numThreads = Integer.parseInt(numThreadsString);

        if (group == null)

           group = new FortressTaskRunnerGroup(numThreads);

        EvaluatorTask evTask = new EvaluatorTask(p, runTests, woLibrary, args);
        try {
            group.invoke(evTask);
        }
        finally {
            // group.interruptAll();
        }
        if (evTask.causedException()) {
            throw evTask.taskException();
        }
    }

    public static void runProgram(CompilationUnit p, boolean runTests,
            List<String> args) throws Throwable {
        runProgram(p, runTests, false, false, args);
    }

    private static class Toplevel implements HasAt {
        public String at() {
            return "toplevel";
        }

        public String stringName() {
            return "driver";
        }
    }

    public static Component readTreeOrSourceComponent(String key, String basename) throws IOException {
        return (Component) readTreeOrSource(key + "." + COMP_SOURCE_SUFFIX, basename + "." + COMP_SOURCE_SUFFIX, basename
                + "." + COMP_TREE_SUFFIX);
    }

    public static Api readTreeOrSourceApi(String key, String basename) throws IOException {
        return (Api) readTreeOrSource(key + "." + API_SOURCE_SUFFIX, basename + "." + API_SOURCE_SUFFIX, basename + "." + API_TREE_SUFFIX);
    }

    static Hashtable<String, CompilationUnit> libraryCache = new Hashtable<String, CompilationUnit>();

    /**
     * Attempts to read in preparsed program. Reparses if parsed form is missing
     * or newer than preparsed form.
     *
     * @param librarySource
     * @param libraryTree
     */
    public static CompilationUnit
        readTreeOrSource(String key, String librarySource, String libraryTree) throws IOException
    {
        if (false && libraryCache.containsKey(key))
            return libraryCache.get(key);

        if (Useful.olderThanOrMissing(libraryTree, librarySource)) {

            System.err.println("Missing or stale preparsed AST "
                               + libraryTree + ", rebuilding from source "
                               + librarySource );

            long begin = System.currentTimeMillis();

            BufferedReader r = Useful.utf8BufferedFileReader(librarySource);
            try {
                // Because of the check above, we can retrieve the value of
                // the Option immediately.
                CompilationUnit c = Option.unwrap(parseToJavaAst(librarySource, r));

                System.err.println
                    ("Parsed " + librarySource + ": "
                         + (System.currentTimeMillis() - begin)
                         + " milliseconds");
                writeJavaAst(c, libraryTree);
                libraryCache.put(key, c);
                return c;
            }
            finally { r.close(); }
        }
        else {
            long begin = System.currentTimeMillis();
            Option<CompilationUnit> c = readJavaAst(libraryTree);

            System.err.println
                ("Read " + libraryTree + ": "
                     + (System.currentTimeMillis() - begin)
                     + " milliseconds");

            if (c.isSome()) {
                libraryCache.put(key, Option.unwrap(c));
                return Option.unwrap(c);
            }
            else {
                return error("Could not read " + librarySource + " or " + libraryTree);
            }
        }
    }

}
