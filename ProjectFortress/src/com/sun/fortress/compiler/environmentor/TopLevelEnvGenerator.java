package com.sun.fortress.compiler.environmentor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;


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

	// http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
	public static String mangleIdentifier(String identifier) {
		return null;
	}
	
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
		ClassVisitor cv = new CheckClassAdapter(cw);		
		
		cv.visit(Opcodes.V1_5, 
        		Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, 
        		className, null, "com/sun/fortress/interpreter/evaluator/BaseEnv", null);

		// Implementing "static reflection"
		// Please remove these data structures and all associated
		// lookups once the Fortress compiler is implemented.
    	Relation<String, Integer> fValueHashCode = new HashRelation<String,Integer>();        
    	Relation<String, Integer> fTypeHashCode = new HashRelation<String,Integer>();        
    	
        writeFields(componentIndex, cv, fValueHashCode, fTypeHashCode);

        writeMethodGetValueRaw(cv, className, fValueHashCode);        

        cv.visitEnd();        
        return(cw.toByteArray());
	}

	private static void writeFields(ComponentIndex componentIndex,
			ClassVisitor cv, Relation<String, Integer> fValueHashCode,
			Relation<String, Integer> fTypeHashCode) {
		
		// Create all variables as fields in the environment
        for(Id id : componentIndex.variables().keySet()) {
        	String idString = NodeUtil.nameString(id);
            fValueHashCode.add(idString, idString.hashCode());        	
        	idString = idString + FVALUE_NAMESPACE;
            cv.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();
        }
        
        // Create all functions as fields in the environment
        for(IdOrOpOrAnonymousName id : componentIndex.functions().firstSet()) {
        	String idString = NodeUtil.nameString(id);
            fValueHashCode.add(idString, idString.hashCode());        	
        	idString = idString + FVALUE_NAMESPACE;
        	System.err.println("idString: " + idString);
            cv.visitField(Opcodes.ACC_PUBLIC, idString, FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }

        // Create all types as fields in the environment
        for(Id id : componentIndex.typeConses().keySet()) {
        	String idString = NodeUtil.nameString(id);
            fTypeHashCode.add(idString, idString.hashCode());        	
        	idString = idString + FTYPE_NAMESPACE;        	
            cv.visitField(Opcodes.ACC_PUBLIC, idString, FTYPE_DESCRIPTOR, null, null).visitEnd();	
        }
	}

	/*
	 * 	public FValue getValueRaw(String queryString) {
	 *	   int queryHashCode = queryString.hashCode();
	 *	   if (queryHashCode == 1) {
	 *		   return className.field1$FValue;
	 *	   } else if (queryHashCode == 2) {
	 * 		   return className.field2$FValue;
	 * 	   } else if (queryHashCode == 3) {
	 *		   return className.field3$FValue;
	 *	   } else if (queryHashCode == 4) {
	 *		   return className.field4$FValue;
	 *	   }
	 *	   return null;
	 *  }
	 */	
	
	/**
	 * Implementing "static reflection" for the method getValueRaw
	 * so the interpreter has O(log n) lookups based on the hash values
	 * of String names in this namespace.        
	 * @param cv
	 * @param className 
	 * @param valueHashCode 
	 */
	private static void writeMethodGetValueRaw(ClassVisitor cv, String className,
			Relation<String, Integer> valueHashCode) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC,
        		"getValueRaw", 
        		"(Ljava/lang/String;)" + 
        		"Lcom/sun/fortress/interpreter/evaluator/values/FValue;", 
        		null, null); 
        mv.visitCode();

        
        Label defQueryHashCode = new Label();
        mv.visitLabel(defQueryHashCode);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 2);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        boolean first = true;
        for(Integer testHashCode : valueHashCode.secondSet()) {
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitLdcInsn(testHashCode);
            Label beforeReturn = new Label();            
            Label afterReturn = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, afterReturn);
            mv.visitLabel(beforeReturn);
            mv.visitVarInsn(Opcodes.ALOAD, 0);

            // This is wrong.  Should be using string equals() to test hash collisions
            mv.visitFieldInsn(Opcodes.GETFIELD, className, 
            		valueHashCode.getFirsts(testHashCode).iterator().next() + FVALUE_NAMESPACE,
            		"Lcom/sun/fortress/interpreter/evaluator/values/FValue;");
            // Previous instruction is wrong
            
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitLabel(afterReturn);
            if (first) {
                mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {Opcodes.INTEGER}, 0, null);
                first = false;
            } else {
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);            	
            }
        }
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this", "L" + className + ";", null, defQueryHashCode, endFunction, 0);
        mv.visitLocalVariable("queryString", "Ljava/lang/String;", null, defQueryHashCode, endFunction, 1);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 2);
        mv.visitMaxs(2, 3);
        mv.visitEnd();
	}

	/**
	 * Given a Java bytecode class stored in a byte array, save that
	 * class into a file on disk.
	 * @param bytecode
	 * @param fileName
	 */
	private static void outputClassFile(byte[] bytecode, String fileName) {
		FileOutputStream outStream;
		try {
			outStream = new FileOutputStream(new File("classes" + File.separator + fileName));
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
