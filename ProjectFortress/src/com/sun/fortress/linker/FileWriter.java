/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.linker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.repository.ProjectProperties;


public class FileWriter {
		
	public static void main(String[] args) {
		
		RepoState st = RepoState.getRepoState();

		if (args.length == 1 && args[0].equals("print")) {
			st.print();
			System.exit(0);
		}
		
		if (args.length == 1 && args[0].equals("reset")) {
			st.reset();
			System.exit(0);
		}
		
		if (args.length != 2) {
			System.out.println("Expecting two arguments");
			System.exit(-1);
		}
		
		String one = args[0];
		String two = args[1];

		st.defaultMap.put(one, two);
		st.writeState();
		
	}
	
}