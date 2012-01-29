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
		
	public static Map<String,String> readMap() {
		
		File f = new File(ProjectProperties.CACHES + "/global.map");
		FileInputStream fi;
		int entries = 0;
		
		Map<String,String> m = new HashMap<String,String>();


		
		try {
			fi = new FileInputStream(f);
		}
		catch (IOException msg) {
			throw new Error("Cannot find global.map");
		}
		
		try {
			entries = fi.read();
		}
		catch (IOException msg) {
			throw new Error("Cannot extract number of entries in the global map");
		}
		
		for (int i = 0; i < entries ; ++i) {
			int s1, s2 = 0;
			try {
				s1 = fi.read();
			}
			catch (IOException msg) {
				throw new Error("Could not read first size for entry " + i);
			}
			try {
				s2 = fi.read();
			}
			catch (IOException msg) {
				throw new Error("Could not read second size for entry " + i);
			}
			
			byte[] b1 = new byte[s1];
			byte[] b2 = new byte[s2];
			
			try {
				fi.read(b1,0,s1);
			}
			catch (IOException msg) {
				throw new Error("Could not read api name for entry " + i);
			}
			
			try {
				fi.read(b2,0,s2);
			}
			catch (IOException msg) {
				throw new Error("Could not read component for entry" + i);
			}
			
			String str1 = new String(b1);
			String str2 = new String(b2);
			
			m.put(str1, str2);
			
		}
		
		try {
			fi.close();
		}
		catch (IOException msg) {
			throw new Error("Could not close the global map file");
		}
				
		return m;
	}
	
	public static void writeMap(Map<String,String> m) {
		
		File f = new File(ProjectProperties.CACHES + "/global.map");
				
		FileOutputStream fo;
		
		try {
			fo = new FileOutputStream(f);
		}
		catch (IOException msg) {
			throw new Error("Could not create an output stream to the global map");
		}
		
		try {
			fo.write(m.size());
		}
		catch (IOException msg) {
			throw new Error("Could not write the number of entries");
		}
		
		Set<String> keys = m.keySet();		
		
		for (String k: keys) {
			
			String v = m.get(k);
			
			byte[] b1 = k.getBytes();
			byte[] b2 = v.getBytes();
			
			int s1 = b1.length;
			int s2 = b2.length;
			
			try {
				fo.write(s1);
			}
			catch (IOException msg) {
				throw new Error("Could not write size for api " + k);
			}

			try {
				fo.write(s2);
			}
			catch (IOException msg) {
				throw new Error("Could not write size for component " + v);
			}
			
			try {
				fo.write(b1);
			}
			catch (IOException msg) {
				throw new Error("Could not write api name " + k);
			}
			
			try {
				fo.write(b2);
			}
			catch (IOException msg) {
				throw new Error("Could not write component name " + v);
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
	
	public static void main(String[] args) {
		
		String one = args[0];
		String two = args[1];
		
		Map<String,String> m = readMap();
		m.put(one, two);
		writeMap(m);
		
	}
	
}