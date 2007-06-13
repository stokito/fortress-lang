/*
 * AtomicFactory.java
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A.  All rights reserved.  
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document.  In particular, and without limitation, these
 * intellectual property rights may include one or more of the
 * U.S. patents listed at http://www.sun.com/patents and one or more
 * additional patents or pending patent applications in the U.S. and
 * in other countries.
 * 
 * U.S. Government Rights - Commercial software.
 * Government users are subject to the Sun Microsystems, Inc. standard
 * license agreement and applicable provisions of the FAR and its
 * supplements.  Use is subject to license terms.  Sun, Sun
 * Microsystems, the Sun logo and Java are trademarks or registered
 * trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.  
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime
 * end uses or end users, whether direct or indirect, are strictly
 * prohibited.  Export or reexport to countries subject to
 * U.S. embargo or to entities identified on U.S. export exclusion
 * lists, including, but not limited to, the denied persons and
 * specially designated nationals lists is strictly prohibited.
 */

package dstm2.factory;
/*
 * AtomicFactory.java
 *
 * Created on November 16, 2005, 1:25 PM
 *
 * Copyright 2005 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is described in this document.  In particular, and without limitation, these intellectual property rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent applications in the U.S. and in other countries.
 * U.S. Government Rights - Commercial software.  Government users are subject to the Sun Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its supplements.  Use is subject to license terms.
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 */
import dstm2.ContentionManager;
import dstm2.exceptions.AbortedException;
import dstm2.exceptions.PanicException;
import dstm2.Transaction;
import dstm2.Transaction.Status;
import dstm2.factory.BaseFactory;
import dstm2.factory.ClassLoader;
import dstm2.factory.Copyable;
import dstm2.factory.Property;
import dstm2.factory.Releasable;
import dstm2.factory.Snapable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.apache.bcel.Repository;

import static org.apache.bcel.Constants.*;
/**
 * Factory for constructing atomic objects.
 * User provides an Adapter class.
 * @author Maurice Herlihy
 */
public class AtomicFactory<T> extends dstm2.factory.BaseFactory<T> {
  
  Class adapterClass;
  String adapterField = "xyzzy";
  
  public AtomicFactory(Class<T> _class, Class<? extends dstm2.factory.Adapter> adapterClass) {
    super(_class);
    synchronized (lock) {                 // BCEL is not thread-safe!
      interfaceName = _class.getName();
      this.adapterClass = adapterClass;
      className = _class.getName() + "$";
      Class[] interfaces = adapterClass.getInterfaces();
      String[] myInterfaces = new String[interfaces.length];
      myInterfaces[0] = _class.getName();
      int i = 1;
      for (Class c : interfaces) {
        if (!c.equals(dstm2.factory.Adapter.class)) {
          myInterfaces[i++] = c.getName();
        }
      }
      _cg = new ClassGen(className,
          "java.lang.Object",
          "Factory.java",
          ACC_PUBLIC | ACC_SUPER,
          myInterfaces);
      _cp = _cg.getConstantPool();
      _factory = new InstructionFactory(_cg, _cp);
      createFields();
      createCtor();
      for (Property p : properties) {
        createGetMethod(p);
        createSetMethod(p);
      }
      for (Class c : interfaces) {
        if (!c.equals(dstm2.factory.Adapter.class)) {
          JavaClass jc = null;
          try {
            jc = Repository.lookupClass(c);
          } catch (java.lang.ClassNotFoundException ex) {
            throw new PanicException("Class not found: " + _class);
          }
          for (Method m : jc.getMethods()) {
            passThrough(c, m);
          }
        }
      }
      createStatic();
      seal();
    }
  }
  
  public T create() {
    try {
      synchronized(lock) {  // not sure this is needed ...
        return theClass.newInstance();
      }
    } catch (InstantiationException ex) {
      throw new PanicException(ex);
    } catch (IllegalAccessException ex) {
      throw new PanicException(ex);
    }
  }
  
  private String getFieldName(Property p) {
    return p.getMethod.getName() + "$$";
  }
  private String setFieldName(Property p) {
    return p.setMethod.getName() + "$$";
  }
  
  private void createFields() {
    FieldGen field;
    // static Class _class;
    field = new FieldGen(ACC_STATIC, new ObjectType("java.lang.Class"), "_class", _cp);
    _cg.addField(field.getField());
    // Adapter adapter;
    field = new FieldGen(0, new ObjectType(adapterClass.getName()), adapterField, _cp);
    _cg.addField(field.getField());
    // getter and setter fields for each property
    for (Property p : properties) {
      field = new FieldGen(0, new ObjectType("dstm2.factory.Adapter$Getter"), getFieldName(p), _cp);
      _cg.addField(field.getField());
      
      field = new FieldGen(0, new ObjectType("dstm2.factory.Adapter$Setter"), setFieldName(p), _cp);
      _cg.addField(field.getField());
    }
  }
  
  private void initRegularField(Property p, InstructionList il, MethodGen method) {
    
    InstructionHandle ih_0 = il.append(new PUSH(_cp, p._class.getName()));
    il.append(_factory.createStore(Type.OBJECT, 1));
    il.append(_factory.createLoad(Type.OBJECT, 1));
    il.append(_factory.createInvoke("java.lang.Class", "forName", new ObjectType("java.lang.Class"), new Type[] { Type.STRING }, Constants.INVOKESTATIC));
    il.append(_factory.createStore(Type.OBJECT, 2));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.GETFIELD));
    il.append(new PUSH(_cp, p.getMethod.getName()));
    il.append(_factory.createLoad(Type.OBJECT, 2));
    il.append(_factory.createInvoke(adapterClass.getName(), "makeGetter", new ObjectType("dstm2.factory.Adapter$Getter"), new Type[] { Type.STRING, new ObjectType("java.lang.Class") }, Constants.INVOKEVIRTUAL));
    il.append(_factory.createFieldAccess(className, getFieldName(p), new ObjectType("dstm2.factory.Adapter$Getter"), Constants.PUTFIELD));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.GETFIELD));
    il.append(new PUSH(_cp, p.setMethod.getName()));
    il.append(_factory.createLoad(Type.OBJECT, 2));
    il.append(_factory.createInvoke(adapterClass.getName(), "makeSetter", new ObjectType("dstm2.factory.Adapter$Setter"), new Type[] { Type.STRING, new ObjectType("java.lang.Class") }, Constants.INVOKEVIRTUAL));
    InstructionHandle ih_33 = il.append(_factory.createFieldAccess(className, setFieldName(p), new ObjectType("dstm2.factory.Adapter$Setter"), Constants.PUTFIELD));
    InstructionHandle ih_36;
    BranchInstruction goto_36 = _factory.createBranchInstruction(Constants.GOTO, null);
    ih_36 = il.append(goto_36);
    InstructionHandle ih_39 = il.append(_factory.createStore(Type.OBJECT, 1));
    il.append(_factory.createNew("dstm2.exceptions.PanicException"));
    il.append(InstructionConstants.DUP);
    il.append(_factory.createLoad(Type.OBJECT, 1));
    il.append(_factory.createInvoke("dstm2.exceptions.PanicException", "<init>", Type.VOID, new Type[] { new ObjectType("java.lang.Throwable") }, Constants.INVOKESPECIAL));
    il.append(InstructionConstants.ATHROW);
    InstructionHandle ih_49 = il.append(InstructionConstants.NOP);
    goto_36.setTarget(ih_49);
    method.addExceptionHandler(ih_0, ih_33, ih_39, new ObjectType("java.lang.ClassNotFoundException"));
  }
  
  private void initPrimitiveField(Property p, InstructionList il, MethodGen method) {
    InstructionHandle ih_0 = il.append(new PUSH(_cp, p._class.getName()));
    il.append(_factory.createStore(Type.OBJECT, 1));
    il.append(_factory.createLoad(Type.OBJECT, 1));
    pushPrimitiveClass(il, p._class);
    il.append(_factory.createStore(Type.OBJECT, 2));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.GETFIELD));
    il.append(new PUSH(_cp, p.getMethod.getName()));
    il.append(_factory.createLoad(Type.OBJECT, 2));
    il.append(_factory.createInvoke(adapterClass.getName(), "makeGetter", new ObjectType("dstm2.factory.Adapter$Getter"), new Type[] { Type.STRING, new ObjectType("java.lang.Class") }, Constants.INVOKEVIRTUAL));
    il.append(_factory.createFieldAccess(className, getFieldName(p), new ObjectType("dstm2.factory.Adapter$Getter"), Constants.PUTFIELD));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.GETFIELD));
    il.append(new PUSH(_cp, p.setMethod.getName()));
    il.append(_factory.createLoad(Type.OBJECT, 2));
    il.append(_factory.createInvoke(adapterClass.getName(), "makeSetter", new ObjectType("dstm2.factory.Adapter$Setter"), new Type[] { Type.STRING, new ObjectType("java.lang.Class") }, Constants.INVOKEVIRTUAL));
    InstructionHandle ih_33 = il.append(_factory.createFieldAccess(className, setFieldName(p), new ObjectType("dstm2.factory.Adapter$Setter"), Constants.PUTFIELD));
    InstructionHandle ih_36;
    BranchInstruction goto_36 = _factory.createBranchInstruction(Constants.GOTO, null);
    ih_36 = il.append(goto_36);
    InstructionHandle ih_39 = il.append(_factory.createStore(Type.OBJECT, 1));
    il.append(_factory.createNew("dstm2.exceptions.PanicException"));
    il.append(InstructionConstants.DUP);
    il.append(_factory.createLoad(Type.OBJECT, 1));
    il.append(_factory.createInvoke("dstm2.exceptions.PanicException", "<init>", Type.VOID, new Type[] { new ObjectType("java.lang.Throwable") }, Constants.INVOKESPECIAL));
    il.append(InstructionConstants.ATHROW);
    InstructionHandle ih_49 = il.append(InstructionConstants.NOP);
    goto_36.setTarget(ih_49);
    method.addExceptionHandler(ih_0, ih_33, ih_39, new ObjectType("java.lang.ClassNotFoundException"));
  }
  // Create constructor
  private void createCtor() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<init>", className, il, _cp);
    
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createNew(adapterClass.getName()));
    il.append(InstructionConstants.DUP);
    il.append(_factory.createFieldAccess(className, "_class", new ObjectType("java.lang.Class"), Constants.GETSTATIC));
    il.append(_factory.createInvoke(adapterClass.getName(), "<init>", Type.VOID, new Type[] { new ObjectType("java.lang.Class") }, Constants.INVOKESPECIAL));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.PUTFIELD));
    
    for (Property p : properties) {
      if (p._class.isPrimitive()) {
        initPrimitiveField(p, il, method);
      } else {
        initRegularField(p, il, method);
      }
    }
    il.append(_factory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  private void createGetMethod(Property p) {
    if (p._class.isPrimitive()) {
      createPrimitiveGetMethod(p);
    } else {
      createRegularGetMethod(p);
    }
  }
  private void createSetMethod(Property p) {
    if (p._class.isPrimitive()) {
      createPrimitiveSetMethod(p);
    } else {
      createRegularSetMethod(p);
    }
  }
  
  private void createStatic() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_STATIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<clinit>", className, il, _cp);
    
    InstructionHandle ih_0 = il.append(new PUSH(_cp, interfaceName));
    il.append(_factory.createInvoke("java.lang.Class", "forName", new ObjectType("java.lang.Class"), new Type[] { Type.STRING }, Constants.INVOKESTATIC));
    InstructionHandle ih_5 = il.append(_factory.createFieldAccess(className, "_class", new ObjectType("java.lang.Class"), Constants.PUTSTATIC));
    InstructionHandle ih_8;
    BranchInstruction goto_8 = _factory.createBranchInstruction(Constants.GOTO, null);
    ih_8 = il.append(goto_8);
    InstructionHandle ih_11 = il.append(_factory.createStore(Type.OBJECT, 0));
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createInvoke("java.lang.ClassNotFoundException", "printStackTrace", Type.VOID, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
    InstructionHandle ih_16 = il.append(_factory.createReturn(Type.VOID));
    goto_8.setTarget(ih_16);
    method.addExceptionHandler(ih_0, ih_5, ih_11, new ObjectType("java.lang.ClassNotFoundException"));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  private void pushPrimitiveClass(InstructionList il, Class _class) {
    il.append(_factory.createFieldAccess(unboxedClass(_class), "TYPE", new ObjectType("java.lang.Class"), Constants.GETSTATIC));
  }
  
  private String unboxedClass(Class _class) {
    if (_class.equals(Boolean.TYPE)) {
      return "java.lang.Boolean";
    } else if (_class.equals(Character.TYPE)) {
      return "java.lang.Character";
    } else if (_class.equals(Byte.TYPE)) {
      return "java.lang.Byte";
    } else if (_class.equals(Short.TYPE)) {
      return "java.lang.Short";
    } else if (_class.equals(Integer.TYPE)) {
      return "java.lang.Integer";
    } else if (_class.equals(Long.TYPE)) {
      return "java.lang.Long";
    } else if (_class.equals(Float.TYPE)) {
      return "java.lang.Float";
    } else if (_class.equals(Double.TYPE)) {
      return "java.lang.Double";
    } else {
      throw new PanicException("Unrecognized primitive type: " + _class);
    }
  }
  
  private void createPrimitiveGetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_PUBLIC, p.type, Type.NO_ARGS, new String[] {  }, p.getMethod.getName(), className, il, _cp);
    
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, getFieldName(p), new ObjectType("dstm2.factory.Adapter$Getter"), Constants.GETFIELD));
    il.append(_factory.createInvoke("dstm2.factory.Adapter$Getter", "call", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
    il.append(_factory.createCheckCast(new ObjectType(unboxedClass(p._class))));
    il.append(_factory.createInvoke(unboxedClass(p._class), p._class.getName() + "Value", p.type, Type.NO_ARGS, Constants.INVOKEVIRTUAL));
    il.append(_factory.createReturn(Type.INT));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  private void createPrimitiveSetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, new Type[] { p.type }, new String[] { "arg0" }, p.setMethod.getName(), className, il, _cp);
    
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, setFieldName(p), new ObjectType("dstm2.factory.Adapter$Setter"), Constants.GETFIELD));
    il.append(_factory.createLoad(Type.INT, 1));
    il.append(_factory.createInvoke(unboxedClass(p._class), "valueOf", new ObjectType(unboxedClass(p._class)), new Type[] { p.type }, Constants.INVOKESTATIC));
    il.append(_factory.createInvoke("dstm2.factory.Adapter$Setter", "call", Type.VOID, new Type[] { Type.OBJECT }, Constants.INVOKEINTERFACE));
    il.append(_factory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  private void createRegularGetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_PUBLIC, p.type, Type.NO_ARGS, new String[] {  }, p.getMethod.getName(), className, il, _cp);
    
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, getFieldName(p), new ObjectType("dstm2.factory.Adapter$Getter"), Constants.GETFIELD));
    il.append(_factory.createInvoke("dstm2.factory.Adapter$Getter", "call", Type.OBJECT, Type.NO_ARGS, Constants.INVOKEINTERFACE));
    il.append(_factory.createCheckCast(new ObjectType(p._class.getName())));
    il.append(_factory.createReturn(Type.OBJECT));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
    
  }
  private void createRegularSetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(ACC_PUBLIC, Type.VOID, new Type[] { p.type }, new String[] { "arg0" }, p.setMethod.getName(), className, il, _cp);
    
    il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, setFieldName(p), new ObjectType("dstm2.factory.Adapter$Setter"), Constants.GETFIELD));
    il.append(_factory.createLoad(Type.OBJECT, 1));
    il.append(_factory.createInvoke("dstm2.factory.Adapter$Setter", "call", Type.VOID, new Type[] { Type.OBJECT }, Constants.INVOKEINTERFACE));
    il.append(_factory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  private void passThrough(Class c, Method m) {
    InstructionList il = new InstructionList();
    Type[] argTypes = m.getArgumentTypes();
    String[] argNames = new String[m.getArgumentTypes().length];
    int argCount = argTypes.length;
    for (int i = 0; i < argCount; i++) {
      argNames[i] = "arg" + i;
    }
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC,
        m.getReturnType(),
        argTypes, argNames,
        m.getName(), className, il, _cp);
    
    InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, adapterField, new ObjectType(adapterClass.getName()), Constants.GETFIELD));
    il.append(_factory.createCheckCast(new ObjectType(c.getName())));
    il.append(_factory.createStore(Type.OBJECT, argCount + 1));
    
    il.append(_factory.createLoad(Type.OBJECT, argCount + 1));
    for (int i = 0; i < argCount; i++) {
      il.append(_factory.createLoad(argTypes[i], i + 1));
    }
    il.append(_factory.createInvoke(c.getName(),
        m.getName(),
        m.getReturnType(),
        m.getArgumentTypes(),
        Constants.INVOKEINTERFACE));
    il.append(_factory.createReturn(m.getReturnType()));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
}
