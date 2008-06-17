package com.sun.fortress.compiler.environments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.StaticPhaseResult;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.StaticError;
import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes_util.NodeUtil;

import edu.rice.cs.plt.collect.HashRelation;
import edu.rice.cs.plt.collect.Relation;


public class TopLevelEnvGen {
	
	
	/**
	 * From the Fortress Language Specification Version 1.0, Section 7.2:
	 * 
	 * 	"Fortress supports three namespaces, one for types, one for values,
	 *  and one for labels. (If we consider the Fortress component system, 
	 *  there is another namespace for APIs.) These namespaces are logically 
	 *  disjoint: names in one namespace do not conflict with names in another." 
	 */
	private static final String FTYPE_NAMESPACE = "$FType";
	private static final String FVALUE_NAMESPACE = "$FValue";
	
	private static final String FVALUE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/values/FValue;";
	private static final String FTYPE_DESCRIPTOR = "Lcom/sun/fortress/interpreter/evaluator/types/FType;";

	private static final String STRING_DESCRIPTOR = Type.getType(String.class).getDescriptor();
	private static final String STRING_INTERNALNAME = Type.getType(String.class).getInternalName();
	
	private static final String CLASSNAME_SUFFIX = "Env";

	
    public static class ComponentResult extends StaticPhaseResult {
        private final Map<APIName, byte[]> _components;
        public ComponentResult(Map<APIName, byte[]> components,
                               Iterable<? extends StaticError> errors) {
            super(errors);
            _components = components;
        }
        public Map<APIName, byte[]> components() { return _components; }
    }
	
	
	
	
	/**
	 * http://blogs.sun.com/jrose/entry/symbolic_freedom_in_the_vm
	 * Dangerous characters are the union of all characters forbidden
	 * or otherwise restricted by the JVM specification, plus their mates,
	 * if they are brackets.

	 * @param identifier
	 * @return
	 */
	public static String mangleIdentifier(String identifier) {
		
		// 1. In each accidental escape, replace the backslash with an escape sequence (\-)
		String mangledString = identifier.replaceAll("\\\\", "\\\\-");

		// 2. Replace each dangerous character with an escape sequence (\| for /, etc.)
		mangledString = mangledString.replaceAll("/", "\\\\|");
		mangledString = mangledString.replaceAll("\\.", "\\\\,");		
		mangledString = mangledString.replaceAll(";", "\\\\?");
		mangledString = mangledString.replaceAll("\\$", "\\\\%");
		mangledString = mangledString.replaceAll("<", "\\\\^");
		mangledString = mangledString.replaceAll(">", "\\\\_");
		mangledString = mangledString.replaceAll("\\[", "\\\\{");
		mangledString = mangledString.replaceAll("\\]", "\\\\}");
		mangledString = mangledString.replaceAll(":", "\\\\!");

		// Non-standard name-mangling convention.  Michael Spiegel 6/16/2008
		mangledString = mangledString.replaceAll("\\ ", "\\\\~");
		
		// 3. If the first two steps introduced any change, <em>and</em> if the
		// string does not already begin with a backslash, prepend a null prefix (\=)
		if (!mangledString.equals(identifier) && !(mangledString.charAt(0) == '\\')) {
			mangledString = "\\=" + mangledString;
		}
		return mangledString;
	}
	
	/**
	 * Given a list of components, generate a Java bytecode compiled environment
	 * for each component.
	 */
	public static ComponentResult generate(Map<APIName, ComponentIndex> components,
			                    GlobalEnvironment env) {

		Map<APIName, byte[]> compiledComponents = new HashMap<APIName, byte[]>();
        Iterable<? extends StaticError> errors = new HashSet<StaticError>();		
		
		for(APIName componentName : components.keySet()) {
			String className = NodeUtil.nameString(componentName);
			className = className + CLASSNAME_SUFFIX;
			
			byte[] envClass = generateForComponent(className,
					              components.get(componentName), env);
			
			compiledComponents.put(componentName, envClass);
			
		}
	
        return new ComponentResult(compiledComponents, errors);		
        
	}

	/**
	 * Given one component, generate a Java bytecode compiled environment
	 * for that component.
	 */
	private static byte[] generateForComponent(String className,
			                                   ComponentIndex componentIndex,
			                                   GlobalEnvironment env) {
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		
		cw.visit(Opcodes.V1_6, 
        		Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, 
        		className, null, "com/sun/fortress/interpreter/env/WorseEnv", null);

		// Implementing "static reflection"
		// Please remove these data structures and all associated
		// lookups once the Fortress compiler is implemented.
    	Relation<String, Integer> fValueHashCode = new HashRelation<String,Integer>();        
    	Relation<String, Integer> fTypeHashCode = new HashRelation<String,Integer>();        
    	
        writeFields(componentIndex, cw, fValueHashCode, fTypeHashCode);

        writeMethodInit(cw, className);
        
        writeMethodGetValueRaw(cw, className, fValueHashCode);        
        
        writeMethodPutValueUnconditionally(cw, className, fValueHashCode);

        cw.visitEnd();        
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
            cv.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), FVALUE_DESCRIPTOR, null, null).visitEnd();
        }
        
        // Create all functions as fields in the environment
        for(IdOrOpOrAnonymousName id : componentIndex.functions().firstSet()) {
        	String idString = NodeUtil.nameString(id);
            fValueHashCode.add(idString, idString.hashCode());        	
        	idString = idString + FVALUE_NAMESPACE;
            cv.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), FVALUE_DESCRIPTOR, null, null).visitEnd();	
        }

        // Create all types as fields in the environment
        for(Id id : componentIndex.typeConses().keySet()) {
        	String idString = NodeUtil.nameString(id);
            fTypeHashCode.add(idString, idString.hashCode());        	
        	idString = idString + FTYPE_NAMESPACE;        	
            cv.visitField(Opcodes.ACC_PUBLIC, mangleIdentifier(idString), FTYPE_DESCRIPTOR, null, null).visitEnd();	
        }
	}
	
	/**
	 * Generate the default constructor for this class.
	 */
	private static void writeMethodInit(ClassVisitor cv, String className) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		Label l0 = new Label();
		mv.visitLabel(l0);
		mv.visitVarInsn(Opcodes.ALOAD, 0);
		mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
				"com/sun/fortress/interpreter/env/WorseEnv", "<init>",
				"()V");
		mv.visitInsn(Opcodes.RETURN);
		Label l1 = new Label();
		mv.visitLabel(l1);
		mv.visitLocalVariable("this", "L" + className + ";", null, l0, l1, 0);
		mv.visitMaxs(1, 1);
		mv.visitEnd();	
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
	 * Implementing "static reflection" for the method getValueRaw so the
	 * interpreter has O(log n) lookups based on the hash values of String names
	 * in this namespace.
	 */
	private static void writeMethodGetValueRaw(ClassVisitor cv, String className,
			Relation<String, Integer> valueHashCode) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC,
        		"getValueRaw", 
        		"(Ljava/lang/String;)" + 
        		FVALUE_DESCRIPTOR, 
        		null, null); 
        mv.visitCode();

        
        Label defQueryHashCode = new Label();
        mv.visitLabel(defQueryHashCode);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 2);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        for(Integer testHashCode : valueHashCode.secondSet()) {
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitLdcInsn(testHashCode);
            Label beforeInnerLoop = new Label();            
            Label afterInnerLoop = new Label();
            mv.visitJumpInsn(Opcodes.IF_ICMPNE, afterInnerLoop);
            mv.visitLabel(beforeInnerLoop);

            for(String testString : valueHashCode.getFirsts(testHashCode)) {
            	mv.visitVarInsn(Opcodes.ALOAD, 1);
            	mv.visitLdcInsn(testString);
            	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
                Label beforeReturn = new Label();            
                Label afterReturn = new Label();            	
                mv.visitJumpInsn(Opcodes.IFEQ, afterReturn);
                mv.visitLabel(beforeReturn);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                String idString = testString + FVALUE_NAMESPACE;
                mv.visitFieldInsn(Opcodes.GETFIELD, className, 
                        mangleIdentifier(idString),
                		FVALUE_DESCRIPTOR);
                mv.visitInsn(Opcodes.ARETURN);
                mv.visitLabel(afterReturn);
            }
            mv.visitLabel(afterInnerLoop);
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
	 * Implementing "static reflection" for the method putValueUnconditionally so the
	 * interpreter has O(log n) lookups based on the hash values of String names
	 * in this namespace.
	 */
	private static void writeMethodPutValueUnconditionally(ClassVisitor cv, String className,
			Relation<String, Integer> valueHashCode) {
		MethodVisitor mv = cv.visitMethod(Opcodes.ACC_PUBLIC,
        		"putValueUnconditionally", 
        		"(" + STRING_DESCRIPTOR + FVALUE_DESCRIPTOR + ")V", 
        		null, null); 
        mv.visitCode();

        
        Label defQueryHashCode = new Label();
        mv.visitLabel(defQueryHashCode);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, STRING_INTERNALNAME, "hashCode", "()I");
        mv.visitVarInsn(Opcodes.ISTORE, 3);
        Label beginLoop = new Label();
        mv.visitLabel(beginLoop);

        Label endComparisons = new Label();
        Iterator<Integer> iterator = valueHashCode.secondSet().iterator();        
        while (iterator.hasNext()) {
        	Integer testHashCode = iterator.next();
            mv.visitVarInsn(Opcodes.ILOAD, 3);
            mv.visitLdcInsn(testHashCode);
            Label beforeInnerLoop = new Label();            
            Label afterInnerLoop = new Label();
            if (iterator.hasNext()) {
            	mv.visitJumpInsn(Opcodes.IF_ICMPNE, afterInnerLoop);
            } else {
            	mv.visitJumpInsn(Opcodes.IF_ICMPNE, endComparisons);            	
            }
            mv.visitLabel(beforeInnerLoop);

            for(String testString : valueHashCode.getFirsts(testHashCode)) {
            	mv.visitVarInsn(Opcodes.ALOAD, 1);
            	mv.visitLdcInsn(testString);
            	mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z");
                Label beforeSetValue = new Label();            
                Label afterSetValue = new Label();            	
                mv.visitJumpInsn(Opcodes.IFEQ, afterSetValue);
                mv.visitLabel(beforeSetValue);
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                String idString = testString + FVALUE_NAMESPACE;                
                mv.visitFieldInsn(Opcodes.PUTFIELD, className, 
                		mangleIdentifier(idString), FVALUE_DESCRIPTOR);                                
                mv.visitJumpInsn(Opcodes.GOTO, endComparisons);
                mv.visitLabel(afterSetValue);
            }                                    
            if (iterator.hasNext()) {
                mv.visitJumpInsn(Opcodes.GOTO, endComparisons);
            	mv.visitLabel(afterInnerLoop);
            } else {
            	mv.visitLabel(endComparisons);
            }
        }
        mv.visitInsn(Opcodes.RETURN);        
        Label endFunction = new Label();
        mv.visitLabel(endFunction);
        mv.visitLocalVariable("this", "L" + className + ";", null, 
        		defQueryHashCode, endFunction, 0);
        mv.visitLocalVariable("queryString", STRING_DESCRIPTOR, null, 
        		defQueryHashCode, endFunction, 1);
        mv.visitLocalVariable("value", FVALUE_DESCRIPTOR, null, defQueryHashCode, endFunction, 2);
        mv.visitLocalVariable("queryHashCode", "I", null, beginLoop, endFunction, 3);
        mv.visitMaxs(2, 4);
        mv.visitEnd();
	}	

	public static void outputClassFiles(ComponentResult componentResult) {
		for(APIName componentName : componentResult.components().keySet()) {
			String className = NodeUtil.nameString(componentName);
			className = className + CLASSNAME_SUFFIX + ".class";
			String fileName = ProjectProperties.BYTECODE_CACHE_DIR + File.separator + className;
			outputClassFile(componentResult.components().get(componentName), fileName);
		}
	}
	
	/**
	 * Given a Java bytecode class stored in a byte array, save that
	 * class into a file on disk.
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
	
	public static void main(String args[]) {
		String input = "/.;$<>[]:\\"; 		//  "/.;$<>[]:\\" --> "\|\,\?\%\^\_\{\}\!\-"
		
		System.out.println(mangleIdentifier(input));
		System.out.println(mangleIdentifier("hello" + input));		
	}
}
