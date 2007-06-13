/*
 * SequentialFactory.java
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
import dstm2.exceptions.PanicException;
import dstm2.Transaction;
import dstm2.factory.BaseFactory;
import dstm2.factory.ClassLoader;
import dstm2.factory.Property;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

/**
 * Simple factory that implements properties declared in an interface.
 * Ignores all non-property methods.
 * @author Maurice Herlihy
 */
public class SequentialFactory<T> extends BaseFactory<T> {
  
  /**
   * Creates a new instance of SequentialFactory
   * @param _class Run-time class of interface being implemented.
   */
  public SequentialFactory(Class<T> _class) {
    super(_class);
    synchronized(lock) {
      className = _class.getName() + "$";
      int constants = Constants.ACC_PUBLIC | Constants.ACC_SUPER;
      String[] interfaces = new String[] {_class.getName()};
      _cg = new ClassGen(className, "java.lang.Object", null, constants, interfaces);
      _cp = _cg.getConstantPool();
      _factory = new InstructionFactory(_cg, _cp);
      createCtor();
      for (Property p : properties) {
        createField(p.type, p.name);
        createGetMethod(p);
        createSetMethod(p);
      }
      seal();
    }
  }
  
  /**
   * Create an object.
   * @return the object.
   */
  public T create() {
    try {
      synchronized (lock) {
        return theClass.newInstance();
      }
    } catch (Exception ex) {
      throw new PanicException(ex);
    }
  }
  
  /**
   * Create constructor for new class.
   */
  private void createCtor() {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, Type.NO_ARGS, new String[] {  }, "<init>", className, il, _cp);
    
    InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createInvoke("java.lang.Object", "<init>", Type.VOID, Type.NO_ARGS, Constants.INVOKESPECIAL));
    InstructionHandle ih_4 = il.append(_factory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  /**
   * Create getter for property.
   * @param p Property to implement.
   */
  private void createGetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC, p.type, Type.NO_ARGS, new String[] {  }, p.getMethod.getName(), className, il, _cp);
    
    InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createFieldAccess(className, p.name, p.type, Constants.GETFIELD));
    InstructionHandle ih_4 = il.append(_factory.createReturn(p.type));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
  /**
   * create setter for new class
   * @param p Property to implement
   */
  private void createSetMethod(Property p) {
    InstructionList il = new InstructionList();
    MethodGen method = new MethodGen(Constants.ACC_PUBLIC, Type.VOID, new Type[] { p.type }, new String[] { "value" }, p.setMethod.getName(), className, il, _cp);
    
    InstructionHandle ih_0 = il.append(_factory.createLoad(Type.OBJECT, 0));
    il.append(_factory.createLoad(p.type, 1));
    il.append(_factory.createFieldAccess(className, p.name, p.type, Constants.PUTFIELD));
    InstructionHandle ih_5 = il.append(_factory.createReturn(Type.VOID));
    method.setMaxStack();
    method.setMaxLocals();
    _cg.addMethod(method.getMethod());
    il.dispose();
  }
  
}
