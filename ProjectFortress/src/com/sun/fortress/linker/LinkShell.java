/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.linker;

import java.io.File;

import com.sun.fortress.Shell;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.MainWrapper;
import com.sun.fortress.useful.Pair;

public class LinkShell {
		
	public static void main(String[] args) throws Throwable, InterruptedException{
		
		RepoState st = RepoState.getRepoState();

		if (args.length < 1) {
			System.out.println("Usage: ");
			System.exit(-1);
		}
		
		String what = args[0];
		
		if (what.equals("print")) {
			st.print();
			System.exit(0);
		}
		
		if (what.equals("reset")) {
			st.reset();
			System.exit(0);
		}
		
		if (what.equals("alink")) {
			if (args.length != 3) {
				System.out.println("Expecting two arguments");
				System.exit(-1);
			}
			st.defaultMap.put(args[1], args[2]);
		}
		
		if (what.equals("clink")) {
			if (args.length != 4) {
				System.out.println("Expecting three arguments");
				System.exit(-1);
			}
			st.specMap.put(new Pair<String,String>(args[1],args[2]),args[3]);
		}
		
		if (what.equals("path")) {
			ProjectProperties.SOURCE_PATH.print();
			System.exit(0);
		}
		
		if (what.equals("build")) {	
			if (args.length != 2) {
				System.out.println("Expecting one argument");
				System.exit(-1);
			}
			File f = ProjectProperties.SOURCE_PATH.findFile(args[1]);
			String[] compileArgs = {"link",f.toString()}; 
			Shell.main(compileArgs);
		}
	
		st.writeState();
		
	}
	
}