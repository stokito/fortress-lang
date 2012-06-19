/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.instrumentation;

import static com.sun.fortress.parser_util.instrumentation.ParserGenerator.RenameParserVisitor;
import static com.sun.fortress.parser_util.instrumentation.ParserGenerator.Visitor;
import static com.sun.fortress.parser_util.instrumentation.Util.getFreshName;
import xtc.parser.*;
import xtc.tree.Attribute;

import java.util.*;

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

    static final String PACKAGE = "com.sun.fortress.parser_util.instrumentation";
    static final String INFO_NAME = "moduleInfos";

    static final String PARSER_PACKAGE = "com.sun.fortress.parser";
    static final String MAIN_MODULE = PARSER_PACKAGE + ".Fortress";
    static final String INSTR_PARSER = PARSER_PACKAGE + ".FortressInstrumented";

    static final String MODULE_INFO_CLASS = PACKAGE + ".Info.ModuleInfo";
    static final String PRODUCTION_INFO_CLASS = PACKAGE + ".Info.ProductionInfo";
    static final String SEQUENCE_INFO_CLASS = PACKAGE + ".Info.SequenceInfo";

    static final String STATE_CLASS = PACKAGE + ".State";

    static final String MAIN_FILE = "com/sun/fortress/parser/Fortress.rats";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("usage: java InstrumentedParserGenerator src-dir dest-dir");
            return;
        }

        String srcDir = args[0];
        String destDir = args[1];

        Visitor renamer = new RenameParserVisitor(MAIN_MODULE, INSTR_PARSER);
        Visitor instrumentor = new CoverageVisitor();

        ParserGenerator.go(MAIN_FILE, srcDir, destDir, renamer, instrumentor);
    }

    static class CoverageVisitor extends Visitor {
        String allModulesInfo;
        List<String> infoCode;
        Map<Module, String> moduleInfoMap;
        Map<Production, String> productionInfoMap;

        CoverageVisitor() {
            this.infoCode = new ArrayList<String>();
            this.allModulesInfo = getFreshName("allModulesInfo");
            this.moduleInfoMap = new HashMap<Module, String>();
            this.productionInfoMap = new HashMap<Production, String>();
        }

        public void instrumentModules(Collection<Module> modules) {
            String allModulesInfoType = "java.util.LinkedList<" + MODULE_INFO_CLASS + ">";
            this.infoCode.add(declaration(allModulesInfoType, allModulesInfo));
            super.instrumentModules(modules);
        }

        public boolean isMainModule(Module m) {
            return m.name.name.equals(MAIN_MODULE);
        }

        public void instrumentMainModule(Module main) {
            List<String> code = main.body.code;
            List<Integer> indents = main.body.indent;

            for (String line : infoCode) {
                code.add(line);
                indents.add(0);
            }

            indents.add(0);
            code.add(String.format("public static java.util.Collection<%s> %s() {", MODULE_INFO_CLASS, INFO_NAME));
            indents.add(2);
            code.add(String.format("return %s;", allModulesInfo));
            indents.add(0);
            code.add("}");
        }

        public void instrumentModule(Module module) {
            String moduleInfo = getFreshName("moduleInfo");
            moduleInfoMap.put(module, moduleInfo);
            infoCode.add(declaration(MODULE_INFO_CLASS, moduleInfo, allModulesInfo, quote(module.name.name)));
            super.instrumentModule(module);
            module.attributes = module.attributes == null ? new LinkedList<Attribute>() : new LinkedList<Attribute>(
                    module.attributes);
            module.attributes.add(new Attribute("stateful", STATE_CLASS));
        }

        public void instrumentProduction(Production p, Module m) {
            String productionInfo = getFreshName("productionInfo");
            String moduleInfo = moduleInfoMap.get(m);
            productionInfoMap.put(p, productionInfo);
            infoCode.add(declaration(PRODUCTION_INFO_CLASS, productionInfo, moduleInfo, quote(p.name.name)));
            super.instrumentProduction(p, m);
        }

        public List<Attribute> adjustProductionAttributes(Production p, List<Attribute> old_attrs) {
            List<Attribute> attrs = new LinkedList<Attribute>();
            if (old_attrs != null) attrs.addAll(old_attrs);
            attrs.add(new Attribute("stateful"));
            return attrs;
        }

        public void instrumentSequence(Sequence seq, int index, Production p, Module m) {
            String sequenceInfo = getFreshName("sequenceInfo");
            String pi = productionInfoMap.get(p);
            infoCode.add(declaration(SEQUENCE_INFO_CLASS, sequenceInfo, pi, quote(
                    seq.name == null ? null : seq.name.name), index));
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

    }

    /* Utilities */

    private static String declaration(String className, String varName, Object... args) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (Object arg : args) {
            if (!first) {
                buffer.append(", ");
            }
            buffer.append(arg);
            first = false;
        }
        return String.format("private static final %s %s = new %s(%s);",
                             className,
                             varName,
                             className,
                             buffer.toString());
    }

    private static String quote(String in) {
        if (in == null) {
            return "null";
        } else {
            return "\"" + in + "\"";
        }
    }
}
