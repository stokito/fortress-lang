package com.sun.fortress.compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.typechecker.TypeCheckerResult;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.iter.IterUtil;

public class TopLevelEnvGenerator {
	private static final String FVALUE_SUFFIX = "$FValue";
	private static final String FTYPE_SUFFIX = "$FType";
	private static final String FVALUE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/values/FValue;";
	private static final String FTYPE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/types/FType;";
	
	public static void generate(Map<APIName, ComponentIndex> components,
			                    GlobalEnvironment env) {
		for(APIName componentName : components.keySet()) {
			String className = NodeUtil.nameString(componentName);
			className = className + "Environment";
			
			byte[] envClass = generateForComponent(className,
					              components.get(componentName), env);
		    outputClassFile(envClass, className + ".class");	
		}
	}

	private static byte[] generateForComponent(String name,
			                                   ComponentIndex componentIndex,
			                                   GlobalEnvironment env) {
		ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, name, null, "java/lang/Object", null);
        // cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "$foo.bar", "I", null, new Integer(7)).visitEnd();
        // MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, 
        //		                                     "main", "([Ljava/lang/String;)V", null, null);
        
        for(Id id : componentIndex.variables().keySet()) {
        	String idString = NodeUtil.nameString(id) + FVALUE_SUFFIX;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }
        
        for(IdOrOpOrAnonymousName id : componentIndex.functions().firstSet()) {
        	String idString = NodeUtil.nameString(id) + FVALUE_SUFFIX;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }
        
        for(Id id : componentIndex.typeConses().keySet()) {
        	String idString = NodeUtil.nameString(id) + FTYPE_SUFFIX;
            cw.visitField(Opcodes.ACC_PUBLIC, idString, FTYPE_DESCRIPTOR, null, null).visitEnd();	
        }
        
        
        cw.visitEnd();
        
        return(cw.toByteArray());
	}
	
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
