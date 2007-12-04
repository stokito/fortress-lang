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

/*
 * Class given a set of Rats! modules it generates a new Fortress parser extended
 * with the modifications in the given modules.
 * 
 */

package com.sun.fortress.syntax_abstractions.rats;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.sun.fortress.compiler.StaticError;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.GrammarEnvironment;
import com.sun.fortress.syntax_abstractions.rats.util.FreshName;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleEnum;
import com.sun.fortress.syntax_abstractions.rats.util.ModuleInfo;
import com.sun.fortress.syntax_abstractions.rules.RuleTranslator;

import xtc.parser.Module;
import xtc.parser.ModuleDependency;
import xtc.parser.ModuleName;
import xtc.parser.Production;
import xtc.tree.Comment;

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

	
	public static Result generateParser(Collection<Module> modules, Set<String> keywords) {
		List<ParserGeneratorError> errors = new LinkedList<ParserGeneratorError>();
		String temporaryParserName = FreshName.getFreshName("FortressTemporaryParser");
		String tempDir = RatsUtil.getTempDir();
		Class<?> parser = null;

		RatsUtil.copyFortressGrammar();
		
		for (Module m: modules) {
			RatsUtil.writeRatsModule(m, tempDir);
		}	

		FortressModule fortressModule = new FortressModule(temporaryParserName);
		fortressModule.addModuleNames(modules, tempDir);
		KeywordModule keywordModule = new KeywordModule();
		keywordModule.addKeywords(keywords, tempDir);

		String fortressRats = tempDir + temporaryParserName+".rats";
		String fortressJava = temporaryParserName+".java";

		String[] args = {"-no-exit", "-in", tempDir, "-out", tempDir, fortressRats};
		xtc.parser.Rats.main(args);

		int parserResult = JavaC.compile(tempDir, fortressJava);

		if (parserResult != 0) {
			throw new RuntimeException("A compiler error occured while compiling a temporary parser");
		}
		
		ParserLoader parserLoader = new ParserLoader(tempDir);
		
		try {
			parser = parserLoader.findClass("com.sun.fortress.parser."+temporaryParserName);
		} catch (ClassNotFoundException e) {
			errors.add(new RatsParserGenerator().new ParserGeneratorError(e.getMessage()));
			e.printStackTrace();
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
			//System.err.println("Looking for: "+name+" in basedir: "+basedir);
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
				File classfile = new File(basedir + File.separatorChar + classname+".class");
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
