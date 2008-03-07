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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import xtc.parser.Module;
import xtc.tree.Attribute;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;

public class RatsParserGenerator {

	public class ParserGeneratorError extends StaticError {

		private String description;

		public ParserGeneratorError(String description) {
			super();
			this.description = description;
		}

		@Override
		public String at() {
			return "loading temporary parser";
		}

		@Override
		public String description() {
			return description;
		}

	}

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
		List<ParserGeneratorError> errors = new LinkedList<ParserGeneratorError>();
		Class<?> parser = null;

		String baseDir = RatsUtil.getTempDir();
		String destinationDir = baseDir + RatsUtil.COMSUNFORTRESSPARSER;
		String fortressName = "Fortress";
		String freshFortressName = FreshName.getFreshName(fortressName);
	
		copyGrammar(modules, fortressName, freshFortressName, baseDir);

		String fortressRats = destinationDir + "Fortress" +".rats";
		String[] args = {"-no-exit", "-in", baseDir, "-out", destinationDir, fortressRats};
		xtc.parser.Rats.main(args);

		String fortressJava = RatsUtil.COMSUNFORTRESSPARSER + freshFortressName +".java";
		int parserResult = JavaC.compile(baseDir, baseDir, baseDir + fortressJava);
		if (parserResult != 0) {
			throw new RuntimeException("A compiler error occured while compiling a temporary parser");
		}

		ParserLoader parserLoader = new ParserLoader(baseDir);
		try {
			parser = parserLoader.findClass("com.sun.fortress.parser."+freshFortressName);
		} catch (ClassNotFoundException e) {
			errors.add(new RatsParserGenerator().new ParserGeneratorError(e.getMessage()));
			e.printStackTrace();
		}
		
		return new RatsParserGenerator().new Result(parser, errors);
	}

	public static void copyGrammar(Collection<Module> modules, 
							String fortressName, String freshFortressName,
							String baseDir) {

		RatsUtil.copyFortressGrammar();

		for (Module m: modules) {
			/* Rename the name of the generated java file */
			if (m.name.name.equals("com.sun.fortress.parser."+fortressName)) {
				List<Attribute> attrs = new LinkedList<Attribute>();
				for (Attribute attribute: m.attributes) {
					if (attribute.getName().equals("parser")) {
						attrs.add(new Attribute("parser", "com.sun.fortress.parser."+freshFortressName));
					}
					else {
						attrs.add(attribute);
					}
				}
				m.attributes = attrs;
			}
			RatsUtil.writeRatsModule(m, baseDir);
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

		private byte[] loadClassData(String classname) {
			byte[] res = null;
			try {
				classname = classname.replaceAll("\\.", ""+File.separatorChar);
				//System.err.println(classname);
				File classfile = new File(basedir + classname+".class");
				if (classfile.exists()) {
					res = new byte[(int) classfile.length()];
					new FileInputStream(classfile).read(res);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return res;
		}
	}
}
