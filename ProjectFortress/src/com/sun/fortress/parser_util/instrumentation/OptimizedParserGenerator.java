/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.instrumentation;

import static com.sun.fortress.parser_util.instrumentation.ParserGenerator.Visitor;
import static com.sun.fortress.useful.Useful.utf8BufferedFileReader;
import xtc.parser.Production;
import xtc.tree.Attribute;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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

public class OptimizedParserGenerator {

    private static final String PARSER_PACKAGE = "com.sun.fortress.parser";
    private static final String MAIN_MODULE = PARSER_PACKAGE + ".Fortress";

    private static final String MAIN_FILE = "com/sun/fortress/parser/Fortress.rats";

    private static final boolean FORCE_ALL_TRANSIENT = false;

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: java OptimizedParserGenerator src-dir dest-dir transient-list-file");
            return;
        }

        String srcDir = args[0];
        String destDir = args[1];
        String transientListFile = args[2];

        Collection<String> transientList = readTransientList(new File(transientListFile));

        Visitor transientVisitor = new TransientVisitor(transientList);
        ParserGenerator.go(MAIN_FILE, srcDir, destDir, transientVisitor);
    }

    static class TransientVisitor extends Visitor {
        Collection<String> transientList;

        TransientVisitor(Collection<String> transientList) {
            this.transientList = transientList;
        }

        public List<Attribute> adjustProductionAttributes(Production p, List<Attribute> old_attrs) {
            boolean forceTransient = FORCE_ALL_TRANSIENT || transientList.contains(p.name.toString());
            if (forceTransient) {
                System.err.println("Making '" + p.name.toString() + "' transient");
            }
            List<Attribute> attrs = new LinkedList<Attribute>();
            boolean saw_inline_or_transient = false;
            if (old_attrs != null) {
                for (Attribute old_attr : old_attrs) {
                    String name = old_attr.getName();
                    if (name.equals("transient")) {
                        attrs.add(old_attr);
                        saw_inline_or_transient = true;
                    } else if (name.equals("memoized")) {
                        if (!forceTransient) attrs.add(old_attr);
                    } else if (name.equals("inline")) {
                        attrs.add(old_attr);
                        saw_inline_or_transient = true;
                    } else if (name.equals("noinline")) {
                        attrs.add(old_attr);
                    } else {
                        attrs.add(old_attr);
                    }
                }
            }
            if (forceTransient && !saw_inline_or_transient) {
                attrs.add(new Attribute("transient"));
            }
            return attrs;
        }
    }

    private static List<String> readTransientList(File file) {
        BufferedReader in = null;
        try {
            List<String> list = new ArrayList<String>();
            in = utf8BufferedFileReader(file);
            // Read until EOF or until Transient List starts
            while (true) {
                String line = in.readLine();
                if (line == null || line.startsWith("** Transient")) break;
            }
            // Read into list until EOF or blank line
            while (true) {
                String line = in.readLine();
                if (line == null || line.length() == 0) break;
                list.add(line);
            }
            return list;
        }
        catch (FileNotFoundException fnfe) {
            return new LinkedList<String>();
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        finally {
            if (in != null) try {
                in.close();
            }
            catch (IOException ioe) {
            }
        }
    }
}
