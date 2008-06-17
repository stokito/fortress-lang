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
import xtc.tree.Attribute;

import com.sun.fortress.compiler.Fortress;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.exceptions.WrappedException;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

public class RatsParserGenerator {

    public class Result extends StaticPhaseResult {
        Class<?> parserClass;

        public Result(Class<?> parserClass,
                Iterable<? extends StaticError> errors) {
            super(errors);
            this.parserClass = parserClass;
        }

        public Class<?> parserClass() { return parserClass; }
    }


    public static Result generateParser(Collection<Module> modules) {
        List<StaticError> errors = new LinkedList<StaticError>();
        Class<?> parser = null;

        String grammarTempDir = RatsUtil.getTempDir();
        String destinationDir = grammarTempDir + RatsUtil.COMSUNFORTRESSPARSER;
        String freshFortressName = FreshName.getFreshName("Fortress");

        FortressRatsGrammar fortressGrammar = new FortressRatsGrammar();
        fortressGrammar.initialize(RatsUtil.getParserPath());
        fortressGrammar.replace(modules);
        fortressGrammar.setName(freshFortressName);
        // fortressGrammar.injectAlternative(new GapAlternative());
        fortressGrammar.clone(grammarTempDir);

        String fortressRats = destinationDir + "Fortress" +".rats";
        String[] args = {"-no-exit", "-in", grammarTempDir, "-out", destinationDir, fortressRats};
        xtc.parser.Rats.main(args);

        String fortressJava = RatsUtil.COMSUNFORTRESSPARSER + freshFortressName +".java";
        int parserResult = JavaC.compile(grammarTempDir, grammarTempDir, grammarTempDir + fortressJava);
        if (parserResult != 0) {
            throw new RuntimeException("A compiler error occured while compiling a temporary parser");
        }

        ParserLoader parserLoader = new ParserLoader(grammarTempDir);
        try {
            parser = parserLoader.findClass("com.sun.fortress.parser."+freshFortressName);
        } catch (ClassNotFoundException e) {
            errors.add(new WrappedException(e, ProjectProperties.debug));
        }
        return new RatsParserGenerator().new Result(parser, errors);
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
