/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.instrumentation;

import xtc.parser.Module;
import xtc.parser.Production;
import xtc.parser.Sequence;
import xtc.tree.Attribute;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/*
 * Class for doing transformations on grammars.
 */

public class ParserGenerator {

    public static void go(String mainFileName, String srcDirName, String destDirName, Visitor... visitors) {
        File srcDir = new File(srcDirName);
        File destDir = new File(destDirName);
        File grammarTempDir = Util.getTempDir();
        Collection<Module> modules = readInModules(srcDir);

        for (Visitor visitor : visitors) {
            visitor.instrumentModules(modules);
        }
        writeOutModules(modules, grammarTempDir);
        File mainFile = new File(grammarTempDir, mainFileName);
        String[] args = {
                "-no-exit", "-in", grammarTempDir.getPath(), "-out", destDir.getPath(), mainFile.getPath()
        };
        xtc.parser.Rats.main(args);
    }

    private static Collection<Module> readInModules(File srcDir) {
        Collection<Module> modules = new LinkedList<Module>();
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                return file.getName().endsWith(".rats") && file.isFile();
            }
        };
        for (File srcFile : srcDir.listFiles(filter)) {
            modules.add(Util.getRatsModule(srcFile));
        }
        return modules;
    }

    private static void writeOutModules(Collection<Module> modules, File targetDir) {
        for (Module m : modules) {
            Util.writeRatsModule(m, targetDir);
        }
    }

    public static class Visitor {
        public void instrumentModules(Collection<Module> ms) {
            Module main = null;
            for (Module m : ms) {
                if (isMainModule(m)) main = m;
                instrumentModule(m);
            }
            if (main != null) instrumentMainModule(main);
        }

        public void instrumentModule(Module m) {
            for (Production p : m.productions) {
                instrumentProduction(p, m);
            }
        }

        public boolean isMainModule(Module m) {
            return false;
        }

        public void instrumentMainModule(Module m) {
            return;
        }

        public void instrumentProduction(Production p, Module m) {
            if (p.isFull()) {
                p.attributes = adjustProductionAttributes(p, p.attributes);
            }
            if (p.choice != null) {
                int index = 1;
                for (Sequence seq : p.choice.alternatives) {
                    instrumentSequence(seq, index, p, m);
                    index++;
                }
            }
        }

        public List<Attribute> adjustProductionAttributes(Production p, List<Attribute> old_attrs) {
            return old_attrs;
        }

        public void instrumentSequence(Sequence s, int i, Production p, Module m) {
            ;
        }
    }


    public static class RenameParserVisitor extends Visitor {
        String mainModuleName;
        String parserName;

        RenameParserVisitor(String mainModuleName, String parserName) {
            this.mainModuleName = mainModuleName;
            this.parserName = parserName;
        }

        public boolean isMainModule(Module m) {
            return m.name.name.equals(mainModuleName);
        }

        public void instrumentMainModule(Module module) {
            List<Attribute> attrs = new LinkedList<Attribute>();
            for (Attribute attribute : module.attributes) {
                if (attribute.getName().equals("parser")) {
                    attrs.add(new Attribute("parser", parserName));
                } else {
                    attrs.add(attribute);
                }
            }
            module.attributes = attrs;
        }
    }
}
