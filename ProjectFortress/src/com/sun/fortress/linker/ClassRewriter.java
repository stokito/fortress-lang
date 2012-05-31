/*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/

package com.sun.fortress.linker;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Arrays;
import java.lang.Character;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.tree.*;

import com.sun.fortress.runtimeSystem.ByteCodeWriter;

final class ClassRewriter {
		
	private ClassRewriter() {}
	
    /**
     * Read-in the class file at componenetPath and returns its associated ASM ClassNode
     */	
	private static byte[] readIn(String componentPath) throws IOException {
		
		// Read the bytecode into memory
		FileInputStream source = new FileInputStream(componentPath);        
        InputStream stream = new java.io.BufferedInputStream(source);
        byte[] bytecode = new byte[stream.available()];
        stream.read(bytecode);
        
        return bytecode;
        
	}
	
	private static int getByteAsInt(byte b) {
		return ((int) b) & 0x000000FF;
	}
	
	private static int analyzeByte(byte b) {		
		int x = getByteAsInt(b);
		if ((x & 0x80) == 0) return 1;
		if ((x & 0xE0) == 0xC0) return 2;
		if ((x & 0xF0) == 0xE0) return 3;
		throw new FortressLinkerError("Failed to analyze a UTF-8 byte");		
	}
		
	private static String convertToString(byte[] preString) {
		
			String result = new String();
			char x;
			int i =0;
			
			while (i < preString.length) {
				switch (analyzeByte(preString[i])) {
				case 1: 	x = (char) getByteAsInt(preString[i]);
								i += 1;
								break;
				case 2:  	x = (char) (((getByteAsInt(preString[i]) & 0x1F) << 6) + (getByteAsInt(preString[i+1]) & 0x3f));
								i += 2;
								break;
				case 3:	x = (char) ( ((getByteAsInt(preString[i]) & 0xf) << 12) + ((getByteAsInt(preString[i+1]) & 0x3f) << 6) + (getByteAsInt(preString[i+2]) & 0x3f) );
								i += 3;
								break;
				default:   throw new FortressLinkerError("Failure of convertToString");				
				}
				result = result.concat((new Character(x)).toString());
				}
			
			return result;
			
	}
	
	private static byte[] UTF16toMUTF8(char c) {
		
		if ('\u0001' <= c && c <= '\u007f') {
			byte[] enc = {(byte) c};
			return enc;
		}
		if ('\u0080' <= c && c <= '\u07ff') { 
			byte b1 = (byte) (((c & 0x70c) >>> 6) | 0xc0);
			byte b2 = (byte) ((c & 0x3f) | 0x80);
			byte[] enc = {b1 , b2};
			return enc;
		}
		if ('\u0800' <= c && c <= '\uffff') { 
			byte b1 = (byte) (((c & 0xf000) >>> 12)| 0xe0);
			byte b2 = (byte) (((c & 0xfc0) >>> 6) | 0x80);
			byte b3 = (byte) ((c & 0x3f) | 0x80);
			byte[] enc = { b1 , b2 , b3 };
			return enc;
		}
		if ('\u0000' == c) { 
			byte b1 = (byte) (0 | 0xc0);
			byte b2 = (byte) (0 | 0x80);
			byte[] enc = {b1 , b2};
			return enc;
		}
		throw new FortressLinkerError("Cannot encode character as a JVM UTF8");
		
	}
	
	
	
	private static byte[] convertFromString(String s) {
		
		char[] ca = s.toCharArray();
		ArrayList<byte[]> str = new ArrayList<byte[]>();
		
		for (int i = 0 ; i < ca.length ; i++) {
			str.add(UTF16toMUTF8(ca[i]));
		}
		
		return merge(str);
		
	}
	
	private static byte[] append(byte[] a, byte[] b) {
		
		byte[] result = new byte[a.length + b.length];
		for (int i = 0; i < a.length; i++) result[i] = a[i];
		for (int i = 0; i < b.length; i++) result[a.length + i] = b[i];
		
		return result;
		
	}
		
	private static byte[] merge(ArrayList<byte[]> x) {
		byte[] newBytecode = {};
		Iterator<byte[]> itr = x.iterator();
		while (itr.hasNext()) {
			newBytecode = append(newBytecode, itr.next());
		}
		
		return newBytecode;
	}
	
	// A.fss imports C.fsi & D.fsi
	// C.fss exports D.fsi
	// Name clash
	
	static byte[] rewrite(byte[] bytecode, String searched, String replacement) {
		
		//System.out.println("Searching for " + searched);
		
		// newBC is the class file resulting from the rewriting
		ArrayList<byte[]> newBC = new ArrayList<byte[]>();
		newBC.add(Arrays.copyOfRange(bytecode, 0, 10));
		
		int i = 10;		
		int constantPoolSize = (getByteAsInt(bytecode[8]) << 8) + getByteAsInt(bytecode[9]);
		int cp_count = 0;
		//System.out.println("Constant pool size: " + constantPoolSize);
		// Iterate over the bytecode, replacing String searched by String replacement
		while(cp_count < constantPoolSize - 1) { 
			//System.out.println("Decoding a constant pool info");
			// Invariant: i points to a constant pool info tag, which we decode
			switch (getByteAsInt(bytecode[i])) {
			case 7:   	
			case 8:	newBC.add(Arrays.copyOfRange(bytecode, i, i + 3));
							i += 3;
				         	break;
			case 3:
			case 4:	
			case 9: 	
			case 10: 	
			case 11: 	
			case 12: 	newBC.add(Arrays.copyOfRange(bytecode, i, i + 5));
							i += 5;
	                       	break;
			case 5: 	
			case 6: 	newBC.add(Arrays.copyOfRange(bytecode, i, i + 9));
							i += 9;
	                     	break;
	        // The following constant is a utf8 string            	
			case 1:   	int length =  (getByteAsInt(bytecode[i+1]) << 8) + getByteAsInt(bytecode[i+2]);
				            String s = convertToString(Arrays.copyOfRange(bytecode,i + 3, i + 3 + length));
			            	//System.out.println("Found " + s);
				            if (searched.equals(s)) {
				            	//System.out.println(" ... Match!");
				            	byte[] foo = convertFromString(replacement); 
				            	byte[] bar = {bytecode[i] , (byte) ((foo.length & 0x0000FF00) >>> 8) , (byte) (foo.length & 0x000000FF)};
				            	newBC.add(bar);
				            	newBC.add(foo);
				            	i += length + 3;
				            }
				            else { 
				            	//System.out.println(" ... Not a match");
				            	newBC.add(Arrays.copyOfRange(bytecode, i, i + length + 3)); 
				            	i += length + 3;
				            }
				            break;
			default: throw new FortressLinkerError("Failed to properly decode a constant pool tag");	            
			}
			cp_count++;
		}
		
		newBC.add(Arrays.copyOfRange(bytecode,i,bytecode.length));
		
		// Convert newBC to an array of byte
		byte[] newBytecode = merge(newBC);
		return newBytecode;
		
	}
	
    /**
     * Wrote-out the given classNode dotclass into a class file at componentPAth
     */		
	private static void writeOut(byte[] bytecode, String componentPath) throws IOException {
		        
        // Write the bytecode into the class file
		FileOutputStream source = new FileOutputStream(componentPath);        
        OutputStream stream = new java.io.BufferedOutputStream(source);
        stream.write(bytecode);
        stream.flush();
        source.close();
        
	}
	
	static void rewrite(String classToRewrite, String apiToRewrite, String componentTargeted) {
		
        try {
        	byte[] bytecode = readIn(classToRewrite);
        	byte[] newBytecode = rewrite(bytecode,apiToRewrite,componentTargeted);
        	writeOut(newBytecode,classToRewrite);
           } catch (IOException e) {
        		System.out.println("Cannot find component: " + classToRewrite);
        		return;
        	}
	   
    }
	
    static void main(String args[]) {
        //System.out.println("Linking ");
        try {
        	byte[] bytecode = readIn(args[0]);
        	byte[] newBytecode = rewrite(bytecode,args[2],args[3]);
        	writeOut(newBytecode,args[1]);
           } catch (IOException e) {
        		System.out.println("Cannot find component: " + args[0]);
        		return;
        	}
	   
    }
    
}
