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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Pair;

final class RepoState {
	
	Map<String,String> defaultMap;
	Map<Pair<String,String>,String> specMap;
	Map<String,List<Pair<String,String>>> links;
	
	private static RepoState ref;
	
	private RepoState() {
		
		initRepoState();
		defaultMap = new HashMap<String,String>();
		specMap = new HashMap<Pair<String,String>,String>();
		links = new HashMap<String,List<Pair<String,String>>>();
		readState();
		
	}
	
	static RepoState getRepoState() {
		
		if (ref == null) 
			ref = new RepoState();
		
		return ref;
		
	}
	
	Map<String,String> getDefaultMap() {
		
		return defaultMap;
		
	}
	
	Map<Pair<String,String>,String> getSpecMap() {
		
		return specMap;
		
	}
	
	Map<String,List<Pair<String,String>>> getRewrites() {
		
		return links;
		
	}
	
	
    // Important invariant: no duplicates
	void recordLink(String cmp1, String api, String cmp2) {
		
		Pair<String,String> p = new Pair<String,String>(api,cmp2);
		List<Pair<String,String>> l = links.get(cmp1);
		if (l == null) {
			l = new ArrayList<Pair<String,String>>();
			links.put(cmp1,l);
		}
		if (!l.contains(p))
			l.add(p);
		
	}
	
	private int readByte(FileInputStream fi) {
		
		int i = 0;
		
		try {
			i = fi.read();
		}
		catch (IOException msg) {
			throw new Error("Reading of repository state failed. Missing length indicator.");
		}
		
		return i;
		
	}
	
	private void writeByte(FileOutputStream fo, int i) {
		
		try {
			fo.write(i);
		}
		catch (IOException msg) {
			throw new Error("Writing of repository state failed. Cannot write length indicator.");
		}
		
	}
	
	private String readString(FileInputStream fi) {
		
		int length = readByte(fi);
		
		byte[] b = new byte[length];
		
		try {
			fi.read(b,0,length);
		}
		catch (IOException msg) {
			throw new Error("Reading of repository state failed. Missing entry.");
		}
		
		String str = new String(b);
		
		return str;
		
	}
	
	private void writeString(FileOutputStream fo, String s) {
		
		byte[] b = s.getBytes();
		int length = b.length;
		
		writeByte(fo,length);
		
		try {
			fo.write(b);
		}
		catch (IOException msg) {
			throw new Error("Writing of repository state failed. Cannot write string: " + s);
		}
		
		
	}
			
	private void readState() {
		
		File f = new File(ProjectProperties.CACHES + "/global.map");
		FileInputStream fi;
		
		try {
			fi = new FileInputStream(f);
		}
		catch (IOException msg) {
			throw new Error("Cannot find global.map");
		}

		// Reading default API linkage map
		int defaultEntries = readByte(fi);
		
		for (int i = 0; i < defaultEntries ; ++i) 				
			defaultMap.put(readString(fi), readString(fi));

		// Reading specific linkage map
		int specEntries = readByte(fi);
		
		for (int i = 0; i < specEntries; ++i)
			specMap.put(new Pair<String,String>(readString(fi),readString(fi)), readString(fi));

		// Readings links bookkeeping
		int linksEntries = readByte(fi);
		
		for (int i = 0; i < linksEntries; ++i) {
			String key = readString(fi);
			int listLength = readByte(fi);
			List<Pair<String,String>> l = new ArrayList<Pair<String,String>>();
			links.put(key,l);
			for (int j = 0; j < listLength; ++j) 
				l.add(new Pair<String,String>(readString(fi),readString(fi)));			
		}
			
		
		try {
			fi.close();
		}
		catch (IOException msg) {
			throw new Error("Could not close the global map file");
		}
		
	}
		
	void writeState() {
		
		File f = new File(ProjectProperties.CACHES + "/global.map");
				
		FileOutputStream fo;
		
		try {
			fo = new FileOutputStream(f);
		}
		catch (IOException msg) {
			throw new Error("Could not create an output stream to the global map");
		}
		
		// Writing default linkage map
		writeByte(fo,defaultMap.size());
				
		Set<String> keysDefault = defaultMap.keySet();		
		
		for (String k: keysDefault) {
						
			writeString(fo,k);
			writeString(fo,defaultMap.get(k));
						
		}
		
		// Writing specific linkage map
		writeByte(fo,specMap.size());
		
		Set<Pair<String,String>> keysSpec = specMap.keySet();
		
		for (Pair<String,String> k: keysSpec) {
			
			writeString(fo,k.first());
			writeString(fo,k.second());
			writeString(fo,specMap.get(k));
			
		}
 		
		// Writing links bookkeeping
		
		writeByte(fo,links.size());
		
		Set<String> keysLinks = links.keySet();
		
		for (String k: keysLinks) {
			
			List<Pair<String,String>> l = links.get(k);
			writeString(fo,k);
			writeByte(fo,l.size());
			for (Pair<String,String> p: l) {
				
				writeString(fo,p.first());
				writeString(fo,p.second());
				
			}
			
		}
		
		try {
			fo.flush();
		} catch (IOException msg) {
			throw new Error("Could not flush the output stream");
		}
		
		try {
			fo.close();
		}
		catch (IOException msg) {
			throw new Error("Could not close the output stream");
		}
		
	}
	
	void reset() {
		
		defaultMap = new HashMap<String,String>();
		specMap = new HashMap<Pair<String,String>,String>();
		writeState();
		
	}
	
	void print() {
		
		Set<String> defaultKeys = defaultMap.keySet();
		Set<Pair<String,String>> specKeys = specMap.keySet();
		Set<String> linksKeys = links.keySet();
		
		System.out.println("\nAPI map\n");
		
		for (String k: defaultKeys)
			System.out.println("  . " + k + " -> " + defaultMap.get(k));

		System.out.println("\nSpecific linkages map\n");
		
		for (Pair<String,String> k: specKeys)
			System.out.println("  . " + k.first() + " -> " + k.second() + " -> " + specMap.get(k));
		
		System.out.println("\nLinkage bookkeeping\n");
		
		for (String k: linksKeys) {
			System.out.println("  . Links for component " + k + ":");
			for (Pair<String,String> v: links.get(k))
				System.out.println("       API " + v.first() + " -> CMP " + v.second());
		}
		
	}
	
    private void initRepoState() throws Error {
    	
    	File f = new File(ProjectProperties.CACHES + "/global.map");
		if (!f.exists()) {
			try {
				f.createNewFile();
				FileOutputStream out = new FileOutputStream(f);
				out.write(0);
				out.write(0);
				out.write(0);
				out.flush();
				out.close();
    		}
			catch (IOException msg) {
				throw new Error("Failed to create the linker state");
			}
    	}
    	
    }
	
//    void updateLinkage(String cmp1, String api, String cmp2) {
//		
//    	List<Pair<String,String>> l = links.get(cmp1);
//    	for (Pair<String,String> p: l) {
//    		if (p.first().equals(api)) {
//    			p = new Pair<String,String>(api,cmp2);
//    			// cmp1 must be rewritten
//    			Linker.linkMyComponent(cmp1);
//    		}
//    	}
//    	
//	}
//	
//	void updateLinkage(String api, String cmp) {
//		
//	} 
    
}