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

/*
 * Class given a set of Rats! modules it generates a new Fortress parser extended
 * with the modifications in the given modules.
 *
 */

package com.sun.fortress.syntax_abstractions.rats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.Module;

import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.useful.Debug;

public class RatsParserGenerator {

    public static Class<?> generateParser(Collection<Module> modules) {
        String grammarTempDir = RatsUtil.getTempDir();
        String destinationDir = grammarTempDir + RatsUtil.TEMPLATEPARSER;
        String freshFortressName = FreshName.getFreshName("TemplateParser");

        FortressRatsGrammar fortressGrammar = new FortressRatsGrammar();
        fortressGrammar.initialize(RatsUtil.getParserPath());
        fortressGrammar.initialize(RatsUtil.getTemplateParserPath());
        fortressGrammar.replace(modules);
        fortressGrammar.setName(freshFortressName);
        // fortressGrammar.injectAlternative(new GapAlternative());
        fortressGrammar.clone(grammarTempDir);

        // fortressGrammar.hackClone(grammarTempDir);

        String fortressRats = destinationDir + "TemplateParser" +".rats";
        String[] args = {"-no-exit", "-in", grammarTempDir, "-out", destinationDir, fortressRats};
        xtc.parser.Rats.main(args);

        String fortressJava = RatsUtil.TEMPLATEPARSER + freshFortressName +".java";
        int parserResult = JavaC.compile(grammarTempDir, grammarTempDir, grammarTempDir + fortressJava);
        if (parserResult != 0) {
            throw new RuntimeException("A compiler error occured while compiling a temporary parser");
        }

        ParserLoader parserLoader = new ParserLoader(grammarTempDir);
        try {
            return parserLoader.findClass("com.sun.fortress.parser.templateparser."+freshFortressName);
        } catch (ClassNotFoundException e) {
            throw new WrappedException(e, Debug.isOnMax());
        }
    }

    public static class ParserLoader extends ClassLoader {

        private String basedir;

        public ParserLoader(String basedir) {
            super(ClassLoader.getSystemClassLoader());
            this.basedir = basedir;
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = loadClassData(name);
            if (b != null)
                return defineClass(null, b, 0, b.length);
            throw new ClassNotFoundException(name);
        }

        private byte[] loadClassData(String classname) throws ClassNotFoundException {

            byte[] res = null;
            classname = classname.replaceAll("\\.", ""+File.separatorChar);

            File classfile = new File(basedir + classname+".class");
            if (classfile.exists()) {
                res = new byte[(int) classfile.length()];
                try {
                    new FileInputStream(classfile).read(res);
                } catch (FileNotFoundException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                } catch (IOException e) {
                    throw new ClassNotFoundException(e.getMessage(), e);
                }
            }
            return res;
        }
    }
}
