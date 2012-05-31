/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.runtimeSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.sun.fortress.useful.Useful;

public class Instantiater extends ClassAdapter {
    
    final InstantiationMap types;
    final InstantiationMap oprs;
    final String instanceName;
    final InstantiatingClassloader icl;
    int access_flags;
    
    public Instantiater(ClassVisitor cv, Map<String, String> xlation,
            Map<String, String> opr_xlation, String instanceName, InstantiatingClassloader icl) {
        super(cv);
        this.types = new InstantiationMap(xlation);
        this.oprs = new InstantiationMap(opr_xlation);
        this.instanceName = instanceName;
        this.icl = icl;
    }

    public String getInstanceName() {
        return instanceName;
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        this.access_flags = access;  //save access for use in generating methods for flattened tuples
        String[] new_interfaces = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            new_interfaces[i] = types.getTypeName(interfaces[i]);
        }
        String translated_type_name = types.getTypeName(name);
        // Compiled17ddd⚙$\=G☝reduce⟦Compiled17ddd\%G⟦com\|sun\|fortress\|compiler\|runtimeValues\|FZZ32⟧,fortress\|CompilerBuiltin\%String,com\|sun\|fortress\|compiler\|runtimeValues\|FZZ32⟧✉$✚Arrow⟦Tuple⟦☝,Arrow⟦E,R⟧,Arrow⟦R,R,R⟧⟧,R⟧
        super.visit(version, access,
                translated_type_name,
                // instanceName, 
                signature,
                types.getTypeName(superName), new_interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // TODO Auto-generated method stub
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitAttribute(Attribute attr) {
        // TODO Auto-generated method stub
        super.visitAttribute(attr);
    }

    @Override
    public void visitEnd() {
        // TODO Auto-generated method stub
        super.visitEnd();
    }

    @Override
    public FieldVisitor visitField(int access, String orig_name, String orig_desc,
            String signature, Object value) {
        // TODO Auto-generated method stub
        String desc_flat = types.getFieldDesc(orig_desc, true);
        String desc_wrapped = types.getFieldDesc(orig_desc, false);
        String name = types.getName(orig_name);  // ? do we rewrite the name of a field?
        FieldVisitor fv;
        if (name.equals(Naming.CLOSURE_FIELD_NAME) && 
            ! desc_flat.equals(desc_wrapped)) {
            fv =  super.visitField(access, name, desc_wrapped, signature, value);
            if (fv != null)
                fv.visitEnd();
        }
            
        fv =  super.visitField(access, name, desc_flat, signature, value);
        return fv;
    }

    @Override
    public void visitInnerClass(String name, String outerName,
            String innerName, int access) {
        // TODO Auto-generated method stub
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
    	// necessary?
        name = oprs.getMethodName(name);
        //System.out.println("old desc=" + desc);
        //desc = types.getMethodDesc(desc);
        //System.out.println("new desc=" + desc);
        String newDesc = types.getMethodDesc(desc);
        MethodVisitor mv = cv.visitMethod(access, name, newDesc, signature, exceptions);
        
        if (!desc.equals(newDesc)) { // catch flattened tuples
        	String params = desc.substring(desc.indexOf("(") +1,  //TODO: wrong if nested parens
        								   desc.indexOf(")"));
        	String newParams = newDesc.substring(newDesc.indexOf("(") +1,
					   							 newDesc.indexOf(")"));
        	if(params.split(";").length == 1 && //single generic parameter 
        			newParams.startsWith("LTuple")) {  //tuple substituted in
        		//System.out.println(access + " " + name + " " + signature + " " +this.instanceName);
        		if ( (this.access_flags & Opcodes.ACC_INTERFACE) == 0 &&  //not in an interface
        			 (access & Opcodes.ACC_STATIC) == 0) {  //and not a static method, so generate a body  

        			//extract the parameters and create strings for the types 
        			List<String> paramList = 
        					InstantiationMap.extractStringParameters(newParams, 
        															 newParams.indexOf(Naming.LEFT_OXFORD), 
        															 InstantiationMap.templateClosingRightOxford(newParams), 
        															 new ArrayList<String>());			
        		    String rawParams = "";
        		    for (String p : paramList) rawParams = rawParams + Naming.internalToDesc(p);
        		    final String altDesc = newDesc.substring(0,newDesc.indexOf("(")+1) + 
        		    						rawParams + 
        		    						newDesc.substring(newDesc.indexOf(")"), newDesc.length());        		    
        		    String tuple_params = InstantiatingClassloader.stringListToTuple(paramList);
        		    String make_sig = InstantiatingClassloader.toJvmSig(paramList, Naming.javaDescForTaggedFortressType(tuple_params));
        		    
        		    MethodVisitor altMv = cv.visitMethod(access, name, altDesc, signature, exceptions);
        		    
        		    altMv.visitVarInsn(Opcodes.ALOAD, 0); //load this
        		    
        			final int n = paramList.size();     //load the parameters
        		    for (int i = 1; i <= n; i++) {
        		    	altMv.visitVarInsn(Opcodes.ALOAD, i);
        		    }
        		    altMv.visitMethodInsn(Opcodes.INVOKESTATIC, InstantiatingClassloader.CONCRETE_ + tuple_params, "make", make_sig);  //create a tuple from the parameters
        		    
        		    if (name.equals("<init>")) {
                        altMv.visitMethodInsn(Opcodes.INVOKESPECIAL, this.instanceName, name, newDesc);     //call original method
                        altMv.visitInsn(Opcodes.RETURN); //return
        		    } else {
                        altMv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, this.instanceName, name, newDesc);     //call original method
                        altMv.visitInsn(Opcodes.ARETURN); //return
        		    }
                    altMv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        		    altMv.visitEnd();   
        		}
        	} 
        }
        return new MethodInstantiater(mv, types, icl);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        // TODO Auto-generated method stub
        super.visitOuterClass(owner, name, desc);
    }

    @Override
    public void visitSource(String source, String debug) {
        // TODO Auto-generated method stub
        super.visitSource(source, debug);
    }


}
