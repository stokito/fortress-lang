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
package com.sun.fortress.macro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import xtc.parser.Module;
import xtc.parser.PrettyPrinter;
import xtc.tree.Printer;

public class RatsParserGenerator {

	public void generateParser(List<Module> modules) {
		for (Module m: modules) {
			writeModuleToRatsFile(m);
		}
		FortressGrammar fortressGrammar = new FortressGrammar();
		fortressGrammar.writeGrammarToDir(modules, getTempDir());
		
		String filename = getTempDir()+File.separatorChar+"Fortress.rats";
		String[] args = {"-in", getTempDir(), filename};
		xtc.parser.Rats.main(args);
	}
	

	/**
	 * @param m
	 */
	private void writeModuleToRatsFile(Module m) {
		FileOutputStream fo;
		try {
			fo = new FileOutputStream(getTempDir()+File.separatorChar+m.name+".rats");
			PrettyPrinter pp = new PrettyPrinter(new Printer(fo));
			pp.visit(m);
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

	private String getTempDir() {
		return System.getProperty("java.io.tmpdir");
	}
	
}
