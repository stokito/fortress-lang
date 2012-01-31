/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.linker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import com.sun.fortress.exceptions.ProgramError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.Useful;

public final class Linker {
	

	private static Linker ref;
	static private Map<APIName,List<Pair<APIName,APIName>>> rewrites;
	
	private Linker() {
		
		rewrites = new HashMap<APIName,List<Pair<APIName,APIName>>>();
		
	}
		
	public static APIName whoIsImplementingMyAPI(APIName component,APIName api) {
		
		if (ref == null)
			ref = new Linker();
				
        RepoState st = RepoState.getRepoState();
                
        String s = null; 
        
        s = st.getSpecMap().get(new Pair<String,String>(component.getText(),api.getText()));
        if (s == null) s = st.getDefaultMap().get(api.getText());;
        if (s == null) { 
        	return api;
        }
                
        List<Pair<APIName,APIName>> l = rewrites.get(component);
        APIName implementer = NodeFactory.makeAPIName(NodeFactory.repoSpan,s);
        Pair<APIName,APIName> rewrite_rule = new Pair<APIName,APIName>(api,implementer);
        
        
        if (l == null) {
        	l = new ArrayList<Pair<APIName,APIName>>();  
        	rewrites.put(component, l);
        }
        l.add(rewrite_rule);

        return implementer;
        
	}
	
	public static void linkMyComponent(APIName component) {
		
		if (ref == null)
			ref = new Linker();
		
        List<Pair<APIName,APIName>> l = rewrites.get(component);
        if (l != null) {
        	
        	String jarFile = ProjectProperties.fileName(ProjectProperties.BYTECODE_CACHE_DIR, component.getText(), "jar");
        	String jarFileTMP = ProjectProperties.fileName(ProjectProperties.CACHES, component.getText() + "HACKHACK", "jar");
        	JarFile jarHandle;
        	try {
        		File original = new File(jarFile);
        		jarHandle = new JarFile(original);
        		InputStream in = jarHandle.getInputStream(jarHandle.getEntry(component+".class"));
        		byte[] toWrite = new byte[in.available()];
        		in.read(toWrite,0,in.available());
        		
        		        		
        		for (Pair<APIName,APIName> x: l) {
        			toWrite = ClassRewriter.rewrite(toWrite,x.first().getText(),x.second().getText());;
        			
        		}
        	
        		File newversion = new File(jarFileTMP);
        		FileOutputStream f = new FileOutputStream(newversion);
        		JarOutputStream jos = new JarOutputStream(f);
        		ByteCodeWriter.writeJarredClass(jos, component.getText(), toWrite);
        		jos.flush();
        		jos.close();
        		
        		original.delete();
        		newversion.renameTo(original);
        		
        	} catch(IOException msg) {
        		throw new ProgramError();
        	}
        	
        }
        
	}
	
}