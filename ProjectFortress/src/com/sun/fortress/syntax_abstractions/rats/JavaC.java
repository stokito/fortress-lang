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

package com.sun.fortress.syntax_abstractions.rats;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.fortress.interpreter.drivers.ProjectProperties;

public class JavaC {

	/**
	 * Line separator.
	 * 
	 * TODO: How do we obtain newline from system? TODO: Is this relevant?
	 * 
	 */
	static final String nl = "\n";
	
	public static int compile(String sourceDir, String destinationDir, String filename) {
		System.err.println("compiling a temporary parser...");
		String classpath = sourceDir+":"+getFortressThirdPartyDependencyJars()+":"+getFortressBuildDir();
		String[] args = {"-cp", classpath , "-d", destinationDir, filename};
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		int compilationResult = com.sun.tools.javac.Main.compile(args, pw);
		String errors = sw.getBuffer().toString();
		System.err.println("done: "+compilationResult);
		if (!errors.equals("")) {
			System.err.println(errors);
		}
		return compilationResult;
	}
	
	private static String getFortressBuildDir() {
		return ProjectProperties.FORTRESS_HOME+File.separatorChar+"ProjectFortress"+File.separatorChar+"build"+File.separatorChar;
	}
	
	private static String getFortressThirdPartyDependencyJars() {
		String sepChar = ""+File.separatorChar;
		String thirdPartyDir = ProjectProperties.FORTRESS_HOME+File.separatorChar+"ProjectFortress"+File.separatorChar+"third_party"+File.separatorChar;
		
		String jars = "";
		jars += thirdPartyDir+"ant"+sepChar+"ant-junit.jar:";
		jars += thirdPartyDir+"ant"+sepChar+"ant.jar:";
		jars += thirdPartyDir+"jsr166y"+sepChar+"jsr166y.jar:";
		jars += thirdPartyDir+"junit"+sepChar+"junit.jar:";
		jars += thirdPartyDir+"junit"+sepChar+"junit_src.jar:";
		jars += thirdPartyDir+"plt"+sepChar+"plt.jar:";
		jars += thirdPartyDir+"xtc"+sepChar+"xtc.jar";
		
		return jars;
	}
	
}
