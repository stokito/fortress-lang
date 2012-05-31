/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Class given a set of Rats! modules it generates a new Fortress parser extended
 * with the modifications in the given modules.
 */
package com.sun.fortress.syntax_abstractions.rats;

import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.useful.Debug;
import xtc.parser.Module;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;

public class RatsParserGenerator {

    public static Class<?> generateParser(Collection<Module> modules) {
        String grammarTempDir = RatsUtil.getTempDir();
        String destinationDir = grammarTempDir + RatsUtil.TEMPLATEPARSER;
        String freshFortressName = RatsUtil.getFreshName("TemplateParser");

        FortressRatsGrammar fortressGrammar = new FortressRatsGrammar();
        fortressGrammar.initialize(RatsUtil.getParserPath());
        fortressGrammar.initialize(RatsUtil.getTemplateParserPath());
        fortressGrammar.replace(modules);
        fortressGrammar.setName(freshFortressName);
        fortressGrammar.clone(grammarTempDir);

        String fortressRats = destinationDir + "TemplateParser" + ".rats";
        String[] args = {
                "-no-exit", "-in", grammarTempDir, "-out", destinationDir, "-Ochunks",
                /* leave this out so that unused productions
                * are still accessible via reflection
                */
                // "-Ogrammar",
                "-Oterminals", "-Ocost", "-Otransient", "-Onontransient", "-Orepeated", "-Oleft2", "-Ooptional",
                "-Ochoices1", "-Ochoices2", "-Oerrors1", "-Oselect", "-Ovalues", "-Omatches", "-Oprefixes", "-Ognodes",
                "-Olocation", fortressRats
        };
        xtc.parser.Rats.main(args);

        String fortressJava = RatsUtil.TEMPLATEPARSER + freshFortressName + ".java";
        int parserResult = JavaC.compile(grammarTempDir, grammarTempDir, grammarTempDir + fortressJava);
        if (parserResult != 0) {
            throw new RuntimeException("A compiler error occured while compiling a temporary parser.");
        }

        ParserLoader parserLoader = new ParserLoader(grammarTempDir);
        try {
            return parserLoader.findClass("com.sun.fortress.parser.templateparser." + freshFortressName);
        }
        catch (ClassNotFoundException e) {
            throw new WrappedException(e, Debug.isOnFor(1, Debug.Type.STACKTRACE) );
        }
    }

    private static class ParserLoader extends ClassLoader {
        private String basedir;

        public ParserLoader(String basedir) {
            super(ClassLoader.getSystemClassLoader());
            this.basedir = basedir;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = loadClassData(name);
            if (b != null) return defineClass(null, b, 0, b.length);
            throw new ClassNotFoundException(name);
        }

        private byte[] loadClassData(String classname) throws ClassNotFoundException {
            byte[] res = null;
            classname = classname.replace('.', File.separatorChar);

            File classfile = new File(basedir + classname + ".class");
            if (classfile.exists()) {
                res = new byte[(int) classfile.length()];
                try {
                    new FileInputStream(classfile).read(res);
                }
                catch (FileNotFoundException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }
                catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }
            }
            return res;
        }
    }
}
