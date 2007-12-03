package com.sun.fortress.syntaxabstractions.rats;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;

import com.sun.fortress.interpreter.drivers.Driver;
import com.sun.tools.javac.*;

public class JavaC {

	/**
	 * Line separator.
	 * 
	 * TODO: How do we obtain newline from system? TODO: Is this relevant?
	 * 
	 */
	static final String nl = "\n";
	
	public static int compile(String dir, String filename) {
		System.err.println("compiling...");
		String classpath = dir+":"+getFortressThirdPartyDependencyJars()+":"+getFortressBuildDir();
		String[] args = {"-cp", classpath , "-d", dir, dir+filename};
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		int compilationResult = Main.compile(args, pw);
		String errors = sw.getBuffer().toString();
		System.err.println("done: "+compilationResult);
		if (!errors.equals("")) {
			System.err.println(errors);
		}
		return compilationResult;
	}
	
	private static String getFortressBuildDir() {
		return System.getenv("FORTRESS_HOME")+File.separatorChar+"ProjectFortress"+File.separatorChar+"build"+File.separatorChar;
	}
	
	private static String getFortressThirdPartyDependencyJars() {
		String sepChar = ""+File.separatorChar;
		String thirdPartyDir = System.getenv("FORTRESS_HOME")+File.separatorChar+"ProjectFortress"+File.separatorChar+"third_party"+File.separatorChar;
		
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
