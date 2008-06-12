package com.sun.fortress.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeUtil;


public class TopLevelEnvGenerator {
	
	
	/**
	 * From the Fortress Language Specification Version 1.0, Section 7.2:
	 * 
	 * 	"Fortress supports three namespaces, one for types, one for values,
	 *  and one for labels. (If we consider the Fortress component system, 
	 *  there is another namespace for APIs.) These namespaces are logically 
	 *  disjoint: names in one namespace do not conﬂict with names in another." 
	 */
	private static final String FTYPE_NAMESPACE = "$FType";
	private static final String FVALUE_NAMESPACE = "$FValue";
	
	private static final String FVALUE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/values/FValue;";
	private static final String FTYPE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/types/FType;";
	
	private static final String CLASSNAME_SUFFIX = "Env";
	
	
	/**
	 * Given a list of components, generate a Java bytecode compiled environment
	 * for each component, and dump that compiled class to the filesystem.
	 * @param components
	 * @param env
	 */
	public static void generate(Map<APIName, ComponentIndex> components,
			                    GlobalEnvironment env) {
		for(APIName componentName : components.keySet()) {
			String className = NodeUtil.nameString(componentName);
			className = className + CLASSNAME_SUFFIX;
			
			byte[] envClass = generateForComponent(className,
					              components.get(componentName), env);
		    outputClassFile(envClass, className + ".class");	
		}
	}

	/**
	 * Given one component, generate a Java bytecode compiled environment
	 * for that component.
	 * @param className
	 * @param componentIndex
	 * @param env
	 * @return
	 */
	private static byte[] generateForComponent(String className,
			                                   ComponentIndex componentIndex,
			                                   GlobalEnvironment env) {
		ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, className, null, "java/lang/Object", null);

        // Create all variables as fields in the environment
        for(Id id : componentIndex.variables().keySet()) {
        	String idString = NodeUtil.nameString(id) + FVALUE_NAMESPACE;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }
        
        // Create all functions as fields in the environment
        for(IdOrOpOrAnonymousName id : componentIndex.functions().firstSet()) {
        	String idString = NodeUtil.nameString(id) + FVALUE_NAMESPACE;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }

        // Create all types as fields in the environment
        for(Id id : componentIndex.typeConses().keySet()) {
        	String idString = NodeUtil.nameString(id) + FTYPE_NAMESPACE;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FTYPE_DESCRIPTOR, null, null).visitEnd();	
        }
               
        cw.visitEnd();
        
        return(cw.toByteArray());
	}

	/**
	 * Given a Java bytecode class stored in a byte arrray, save that
	 * class into a file on disk.
	 * @param bytecode
	 * @param fileName
	 */
	private static void outputClassFile(byte[] bytecode, String fileName) {
		FileOutputStream outStream;
		try {
			outStream = new FileOutputStream(new File(fileName));
			outStream.write(bytecode);
			outStream.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
