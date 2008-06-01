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

package com.sun.fortress.syntax_abstractions.rats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import xtc.parser.Action;
import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleInstantiation;
import xtc.parser.ModuleList;
import xtc.parser.ModuleName;
import xtc.parser.PParser;
import xtc.parser.ParseError;
import xtc.parser.PrettyPrinter;
import xtc.parser.Production;
import xtc.parser.Result;
import xtc.parser.SemanticValue;
import xtc.tree.Comment;
import xtc.tree.Printer;
import xtc.type.JavaAST;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.io.IOUtil;

public abstract class RatsUtil {

    private static final char SEP = '/'; // File.separatorChar

    public static final String COMSUNFORTRESSPARSER = "com"+ SEP +"sun"+SEP+"fortress"+SEP+"parser"+SEP;

    public static Module getRatsModule(String filename) {
        Option<Module> result = parseRatsModule(filename);
        if (result.isNone()) {
            System.err.println("FAIL: Syntax error(s).");
            System.exit(1);
            return null;
        }
        return result.unwrap();
    }

    public static Option<Module> parseRatsModule(String filename) {
        BufferedReader in;
        try {
            in = Useful.utf8BufferedFileReader(filename);
            xtc.parser.PParser parser = new PParser(in,
                    filename,
                    (int) new File(filename).length());
            Result r = parser.pModule(0);
            if (r.hasValue()) {
                SemanticValue v = (SemanticValue) r;
                Module n = (Module) v.value;
                return Option.some(n);
            }
            ParseError err = (ParseError) r;
            if (-1 == err.index) {
                System.err.println("  Parse error");
            }
            else {
                System.err.println("  " + parser.location(err.index) + ": "
                        + err.msg);
            }
            return Option.none();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return Option.none();
    }

    public static void writeRatsModule(Module module, String tempDir) {
        FileOutputStream fo;
        try {
            makeSureDirectoryExists(tempDir);
            String name = RatsUtil.getModulePath(module.name.name);
            File file = new File(tempDir+name+".rats");
            fo = new FileOutputStream(file);  
            PrettyPrinter pp = new PrettyPrinter(new Printer(fo), new JavaAST(), true);
            pp.visit(module);
            pp.flush();
            fo.flush();
            fo.close();
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException ee) {
            // TODO Auto-generated catch block
            ee.printStackTrace();
        }
    }

    private static void makeSureDirectoryExists(String tempDir) {
        File dir = new File(tempDir+COMSUNFORTRESSPARSER);
        if (!dir.isDirectory()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create directories: "+dir.getAbsolutePath());
            }
        }
    }

    public static Module makeExtendingRatsModule(com.sun.fortress.syntax_abstractions.intermediate.Module module) {
        Module m = new Module();
        m.name = new ModuleName(RatsUtil.getModuleNamePrefix()+module.getName());
        m.productions = new LinkedList<Production>();

        List<ModuleName> parameters = new LinkedList<ModuleName>();
        parameters.addAll(module.getParameters());
        m.parameters = new ModuleList(parameters);

        List<ModuleDependency> dependencies = new LinkedList<ModuleDependency>();
        dependencies.addAll(module.getDependencies());
        m.dependencies = dependencies;

        m.documentation = getComment();
        m.header = createHeader();
        return m;
    }

    /** 
     * In the generated module we use maps to associate variable names to their values
     * @return
     */
    private static Action createHeader() {
        List<String> imports = new LinkedList<String>();
        List<Integer> indents = new LinkedList<Integer>();
        indents.add(3);
        imports.add("import java.util.Map;");
        indents.add(3);
        imports.add("import java.util.HashMap;");
        Action a = new Action(imports, indents);
        return a;
    }

    /**
     * @param m
     */
    public static Comment getComment() {
        List<String> lines = new LinkedList<String>();
        lines.add("Module generated by the Fortress compiler on "+new Date());
        return new Comment(Comment.Kind.SINGLE_LINE, lines);
    }

    public static String getModuleNamePrefix() {
        return ModuleInfo.MODULE_NAME_PREFIX;
    }

    public static String getModulePath(String dottedName) {
        return dottedName.replace('.', SEP);
    }

    public static String getParserPath() {
        return RatsUtil.getFortressSrcDir() + COMSUNFORTRESSPARSER;
    }

    public static String getTempDir() {
        // return System.getProperty("java.io.tmpdir")+SEP;
        try{
            return IOUtil.createAndMarkTempDirectory("fortress","rats").getCanonicalPath() + SEP;
        } catch ( IOException e ){
            return System.getProperty("java.io.tmpdir")+SEP;
        }
    }

    public static String getFortressSrcDir() {
        return ProjectProperties.FORTRESS_AUTOHOME+SEP+"ProjectFortress"+SEP+"src"+SEP;
    }

    public static void addParametersToInstantiation(Module m, String moduleName, Set<ModuleName> parameters) {
        for (ModuleDependency md: m.dependencies) {
            if (md instanceof ModuleInstantiation) {
                if (md.module.name.equals(RatsUtil.getModuleNamePrefix()+moduleName)) {
                    List<ModuleName> deps = new LinkedList<ModuleName>();
                    ModuleInstantiation mi = (ModuleInstantiation) md;
                    deps.addAll(mi.arguments.names);
                    deps.addAll(parameters);
                    mi.arguments = new ModuleList(deps);
                }
            }
        }
    }

    public static void addParameterToInstantiation(Module fortress,
            String moduleName, ModuleName name) {
        for (ModuleDependency md: fortress.dependencies) {
            if (md instanceof ModuleInstantiation) {
                if (md.module.name.equals(moduleName)) {
                    Set<ModuleName> deps = new LinkedHashSet<ModuleName>();
                    ModuleInstantiation mi = (ModuleInstantiation) md;
                    deps.addAll(mi.arguments.names);
                    deps.add(name);
                    List<ModuleName> depsls = new LinkedList<ModuleName>();
                    depsls.addAll(deps);
                    mi.arguments = new ModuleList(depsls);
                }
            }
        }
    }


}
