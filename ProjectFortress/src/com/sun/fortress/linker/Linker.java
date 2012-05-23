/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.linker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.ByteCodeWriter;
import com.sun.fortress.useful.Pair;

/**
 * Linker is a singleton object that serves as an interface between the graph 
 * repository that manages compilation and the repository state that contains 
 * the linking information. From an implementation point of view, the Linker object
 * has the only methods that are accessible form outside the linker package. 
 * @author tristan
 */
public final class Linker {
	

	private static Linker ref;
	static private Map<APIName,List<Pair<APIName,APIName>>> rewrites;
	
	private Linker() {
		
		rewrites = new HashMap<APIName,List<Pair<APIName,APIName>>>();
		
	}
		
	
	/**
	 * Use this function to query the linker to discover what components implements a given api for a given component.
	 * This function should be called while computing the graph of dependencies, for each api in each component. 
	 * @param component The component for which you want to find the implementer of an api
	 * @param api The api for which you want to find an implementation
	 * @return The component that implements api for component
	 */
	public static APIName whoIsImplementingMyAPI(APIName component,APIName api) {
		
		if (ref == null)
			ref = new Linker();
				
        RepoState st = RepoState.getRepoState();
                
        String s = null; 
        
        s = st.getSpecMap().get(new Pair<String,String>(component.getText(),api.getText()));
        if (s == null) s = st.getDefaultMap().get(api.getText());;
        if (s == null) {
            //st.recordLink(component.getText(), api.getText(), api.getText());
            //st.writeState();
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
    
        //st.recordLink(component.getText(), api.getText(), implementer.getText());
        //st.writeState();
        
        return implementer;
        
	}
	
	private static void substituteJarFile(String src, String dst, List<Pair<String,String>> substitutions, boolean isAlias) throws IOException {
		
		String srcPath = ProjectProperties.fileName(ProjectProperties.BYTECODE_CACHE_DIR, src, "jar");
		JarFile jarIn = new JarFile(srcPath);
		
		String dstPath = ProjectProperties.fileName(ProjectProperties.BYTECODE_CACHE_DIR, dst, "jar");		
		FileOutputStream out = new FileOutputStream(dstPath);
		JarOutputStream jos = new JarOutputStream(out);
		
		Enumeration contentList = jarIn.entries();
		
		while (contentList.hasMoreElements()) {
			
			ZipEntry ze = (ZipEntry) contentList.nextElement();
			InputStream in = jarIn.getInputStream(ze);
			
			byte[] toWrite = new byte[in.available()];
			in.read(toWrite,0,in.available());
			
			for (Pair<String,String> subs: substitutions) 
				toWrite = ClassRewriter.rewrite(toWrite,subs.first(),subs.second());;
			
			// Chopping off the ".class" from the name
			String fname = ze.getName();
			String name = fname.substring(0,fname.length() - 6);

			// In the case of an alias, we want to change the name of the .class itself
			if (isAlias) 
				if (name.equals(src))
					name = dst;
					
			ByteCodeWriter.writeJarredClass(jos, name, toWrite);
			
		}
		
		jos.close();
		jarIn.close();
		
	}
	
	/**
	 * Use this function to generate the jar files for all of the aliases of a given component.
	 * @param component The component for which you want to generate the aliases.
	 */
	public static void generateAliases(APIName component) {
		
		if (ref == null)
			ref = new Linker();
		
        RepoState st = RepoState.getRepoState();
        
        
        String cmp = component.getText();
        if (cmp.equals("CompilerBuiltin") || cmp.equals("AnyType") || cmp.equals("CompilerLibrary")) 
        	return;
        
        
        List<String> aliases = st.alias.get(component.getText());
    	
    	if (aliases == null)
    		return;
        
    		for (String alias: aliases) {
    			
    			File aliasOut = new File(ProjectProperties.fileName(ProjectProperties.BYTECODE_CACHE_DIR, alias, "jar"));
    			try {
    				aliasOut.createNewFile();
    				List<Pair<String,String>> l = new ArrayList<Pair<String,String>>();
    				l.add(new Pair<String,String>(cmp,alias)); 
    				substituteJarFile(cmp,alias,l,true);
    			} catch (IOException msg) {
    				throw new Error("Failed to copy file " + cmp + " from " + alias );
    			}
    			    			
    		}


		
	}
	
	/**
	 * Use this function to figure out what is the name of the file containing the implementation
	 * of a component. The name of a component and of it's source file may not always match because 
	 * of aliases. 
	 * @param name The name of the component for which you are searching the source implementation.
	 * @return
	 */
	public static APIName whatToSearchFor(APIName name) {
		
		if (ref == null)
			ref = new Linker();
		
        RepoState st = RepoState.getRepoState();
        
        // Let's see if this name is an alias
       
        for (String k: st.getAlias().keySet()) {
        	
        	for (String a: st.getAlias().get(k)) {
        		if (a.equals(name.getText())) {
        			// It is in fact an alias
        			return NodeFactory.makeAPIName(NodeFactory.repoSpan,k);
        			
        		}
        	}
        	
        }
        	
        return name;
		
	}
	
	/**
	 * Use this function to apply the linking rewrites. This function will update the linking for 
	 * all the components in the repository.
	 */
	public static void linkAll() {
		
		Set<APIName> cmps = rewrites.keySet();
		
		for (APIName cmp: cmps)
			linkMyComponent(cmp);
		
	}
	
	private static void linkMyComponent(APIName component) {
		
		if (ref == null)
			ref = new Linker();
		
        RepoState st = RepoState.getRepoState();
		
        List<Pair<APIName,APIName>> l = rewrites.get(component);
        if (l == null) return;
        
        List<Pair<String,String>> substs = new ArrayList<Pair<String,String>>(); 
        
        for (Pair<APIName,APIName> x: l) {
			// Use bookkeeping to find out the current name for x.first()
			String toReplace = x.first().getText();
			
			List<Pair<String,String>> history = st.getRewrites().get(component.getText());
			if (history != null) {
				for (Pair<String,String> p: history) 
					if (p.first().equals(toReplace)) {
						toReplace = p.second();
						break;
					}
			}
					
			substs.add(new Pair<String,String>(toReplace,x.second().getText()));
	        st.recordLink(component.getText(), toReplace, x.second().getText());
		}
        st.writeState();

        try {
        substituteJarFile(component.getText(),component.getText() + "HACKHACK",substs,false);
        } catch (IOException msg) {
        	throw new Error("Jar file substitution failed while linking");
        }
        
        String dir = ProjectProperties.BYTECODE_CACHE_DIR;
        
        File oldfile = new File(ProjectProperties.fileName(dir, component.getText(), "jar"));
        File newfile = new File(ProjectProperties.fileName(dir, component.getText() + "HACKHACK", "jar"));
        
        oldfile.delete();
 		newfile.renameTo(oldfile);
        
	}
		
}        

