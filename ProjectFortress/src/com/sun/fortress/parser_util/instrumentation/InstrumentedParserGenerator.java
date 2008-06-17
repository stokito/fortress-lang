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

package com.sun.fortress.parser_util.instrumentation;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xtc.parser.Module;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.parser.Element;
import xtc.parser.Action;
import xtc.parser.Name;
import xtc.parser.SemanticPredicate;
import xtc.parser.ParserAction;
import xtc.tree.Attribute;

/* 
 * Command-line tool for instrumenting the Fortress grammar.
 * 
 * This tool is specific to the Fortress grammar: It knows package names and 
 * the name of the main grammar file. It's output file is hard-coded.
 * It would be nice to change these eventually.
 *
 * Assumptions:
 *  - the input grammar does not use the global state options
 *    (the tool assumes it has complete control over the state)
 * 
 */

public class InstrumentedParserGenerator {

    private static final String PACKAGE =
        "com.sun.fortress.parser_util.instrumentation";
    private static final String INFO_NAME = "moduleInfos";

    private static final String PARSER_PACKAGE = "com.sun.fortress.parser";
    private static final String MAIN_MODULE = PARSER_PACKAGE + ".Fortress";
    private static final String INSTR_PARSER = PARSER_PACKAGE + ".FortressInstrumented";

    private static final String MODULE_INFO_CLASS = PACKAGE + ".Info.ModuleInfo";
    private static final String PRODUCTION_INFO_CLASS = PACKAGE + ".Info.ProductionInfo";
    private static final String SEQUENCE_INFO_CLASS = PACKAGE + ".Info.SequenceInfo";

    private static final String STATE_CLASS = PACKAGE + ".State";

    private static final String MAIN_FILE = "com/sun/fortress/parser/Fortress.rats";

    private File srcDir;
    private File destinationDir;

    private InstrumentedParserGenerator(String srcDir, 
                                        String destinationDir) {
        this.srcDir = new File(srcDir);
        this.destinationDir = new File(destinationDir);
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            new InstrumentedParserGenerator(args[0], args[1]).go();
        } else {
            System.err.println("usage: java InstrumentedParserGenerator src-dir dest-dir");
        }
    }

    private void go() {
        File grammarTempDir = Util.getTempDir();

        Collection<Module> modules = readInModules(srcDir);
        instrument(modules);
        writeOutModules(modules, grammarTempDir);

        File mainFile = new File(grammarTempDir, MAIN_FILE);

        String[] args = {"-no-exit",
                         "-in", grammarTempDir.getPath(), 
                         "-out", destinationDir.getPath(), 
                         mainFile.getPath()};
        xtc.parser.Rats.main(args);
    }

    private Collection<Module> readInModules(File srcDir) {
        Collection<Module> modules = new LinkedList<Module>();
        FileFilter filter = new FileFilter() {
                public boolean accept(File file) {
                    return file.getName().endsWith(".rats")
                        && file.isFile();
                }
            };
        for(File srcFile: srcDir.listFiles(filter)) {
            modules.add(Util.getRatsModule(srcFile));
        }
        return modules;
    }

    private void writeOutModules(Collection<Module> modules, File targetDir) {
        for (Module m: modules) {
            Util.writeRatsModule(m, targetDir);
        }
    }

    private void instrument(Collection<Module> modules) {
        String allModulesInfo = getFreshName("allModulesInfo");
        List<String> infoCode = new ArrayList<String>();
        infoCode.add(declaration("java.util.LinkedList<"+MODULE_INFO_CLASS+">", allModulesInfo));

        for (Module module : modules) {
            instrument(module, allModulesInfo, infoCode);
        }
        for (Module module : modules) {
            if (module.name.name.equals(MAIN_MODULE)) {
                updateGrammarAttributes(module);
                instrumentMainModule(module, allModulesInfo, infoCode);
            }
        }
    }
    private void instrument(Module module, String ami, List<String> infoCode) {
        String moduleInfo = getFreshName("moduleInfo");
        infoCode.add(declaration(MODULE_INFO_CLASS, moduleInfo, ami, quote(module.name.name)));

        for (Production p : module.productions) {
            instrument(p, moduleInfo, infoCode);
        }
        module.attributes = module.attributes == null 
            ? new LinkedList<Attribute>() 
            : new LinkedList<Attribute>(module.attributes);
        module.attributes.add(new Attribute("stateful", STATE_CLASS));
    }

    private void instrument(Production p, String mi, List<String> infoCode) {
        String productionInfo = getFreshName("productionInfo");
        infoCode.add(declaration(PRODUCTION_INFO_CLASS, productionInfo, mi, quote(p.name.name)));

        if (p.isFull()) {
            List<Attribute> p_attrs = new LinkedList<Attribute>();
            p_attrs.add(new Attribute("stateful"));
            if (p.attributes != null) { p_attrs.addAll(p.attributes); }
            p.attributes = p_attrs;
        }

        int index = 1;
        if (p.choice == null) return;
        for (Sequence seq : p.choice.alternatives) {
            instrument(seq, index, productionInfo, infoCode);
            index++;
        }
    }
    private void instrument(Sequence seq, int index, String pi, List<String> infoCode) {
        String sequenceInfo = getFreshName("sequenceInfo");
        infoCode.add(declaration(SEQUENCE_INFO_CLASS, sequenceInfo, pi, quote(seq.name == null ? null : seq.name.name), index));

        List<Element> els = new LinkedList<Element>();
        els.add(makeSequenceAction(sequenceInfo));
        els.addAll(seq.elements);
        seq.elements = els;
    }

    private Element makeSequenceAction(String si) {
        List<String> code = new LinkedList<String>();
        List<Integer> indents = new LinkedList<Integer>();

        indents.add(0);
        code.add(String.format("%s.registerOccurrence(yyState);", si));
        return new Action(code, indents);
    }

    /* After instrumentation */
    private void updateGrammarAttributes(Module module) {
        List<Attribute> attrs = new LinkedList<Attribute>();
        for (Attribute attribute: module.attributes) {
            if (attribute.getName().equals("parser")) {
                attrs.add(new Attribute("parser", INSTR_PARSER));
            } else {
                attrs.add(attribute);
            }
        }
        module.attributes = attrs;
    }
    private void instrumentMainModule(Module module, String ami, List<String> infoCode) {
        List<String> code = module.body.code;
        List<Integer> indents = module.body.indent;

        for (String line : infoCode) {
            code.add(line);
            indents.add(0);
        }

        indents.add(0);
        code.add(String.format("public static java.util.Collection<%s> %s() {",
                               MODULE_INFO_CLASS, INFO_NAME));
        indents.add(2);
        code.add(String.format("return %s;", ami));
        indents.add(0);
        code.add("}");
    }

    /* Utilities */

    private String declaration(String className, String varName, Object... args) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (Object arg : args) {
            if (!first) { buffer.append(", "); }
            buffer.append(arg);
            first = false;
        }
        return String.format("private static final %s %s = new %s(%s);",
                             className, varName, className, buffer.toString());
    }
    private String quote(String in) {
        if (in == null) {
            return "null";
        } else {
            return "\"" + in + "\"";
        }
    }

    private static int freshid = 0;
    public static String getFreshName(String s) {
        return s+(++freshid);
    }
}
