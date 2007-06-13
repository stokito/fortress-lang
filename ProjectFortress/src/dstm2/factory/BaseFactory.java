/*
 * BaseFactory.java
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

import dstm2.atomic;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import dstm2.Transaction;
import dstm2.exceptions.PanicException;

import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Method;

/**
 * Provides utility methods for factories.
 * @author Maurice Herlihy
 */
public abstract class BaseFactory<T> implements Factory<T> {
  /**
   * BCEL is not thread-safe
   **/
  protected static Object lock = new Object();
  /**
   * BCEL instruction factory.
   */
  protected InstructionFactory _factory;
  /**
   * Constant pool for created class.
   */
  protected ConstantPoolGen    _cp;
  /**
   * Used to generate the class.
   */
  protected ClassGen           _cg;
  /**
   * The actual class we are building.
   */
  protected Class<T> theClass;
  /**
   * The name of the class we are building.
   */
  protected String className;
  /**
   * Interface exported by created class.
   */
  protected String interfaceName;
  
  /**
   * Set of properties.
   **/
  protected Set<Property> properties = new HashSet<Property>();
  /**
   * Set of other methods.
   **/
  protected Set<Method> methods = new HashSet<Method>();
  
  /**
   * set of interfaces satisfied by this class
   */
  protected Set<Class> interfaces = new HashSet<Class>();
  
  /**
   * This constructor separates methods into properties and miscellaneous
   * methods. For properties, it deduces type and name of properties, and
   * stores them in the <CODE>properties</CODE> set. Other methods go into
   * the <CODE>methods</CODE> field.
   * @param interfaces additional interfaces satisfied by this class and understood by this factory.
   * @param _class interface class
   */
  public BaseFactory(Class<T> _class) {
    if (!_class.isInterface()) {
      throw new PanicException("%s is not an interface", _class);
    }
    parseProperties(_class);
  }
  
  /**
   * Once methods and fields have been defined, this method turns the
   * byte code into a Java class.
   */
  protected void seal() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      _cg.getJavaClass().dump(out);
    } catch (IOException ex) {
      throw new PanicException(ex);
    }
    byte[] bytes = out.toByteArray();
    ClassLoader<T> classLoader = new ClassLoader<T>(bytes);
    theClass = classLoader.make();
  }
  
  /**
   * Create a new field.
   * @param type BCEL type of field.
   * @param name field name
   */
  protected void createField(Type type, String name) {
    FieldGen field = new FieldGen(0, type, name, _cp);
    _cg.addField(field.getField());
  }
  
  /**
   * Extracts properties from interface definition. Does rudimentary type
   * and sanity checking
   * @param _class class to inspect.
   */
  private void parseProperties(Class _class) {
    // Make sure implied field types are scalar or atomic.
    // Enough to check return type of getField() methods.
    for (java.lang.reflect.Method method : _class.getMethods()) {
      if (method.getName().substring(0,3).equals("get")) {
        if (!isAtomicOrScalar(method.getReturnType())) {
          throw new PanicException("Method %s return type %s not scalar or atomic.",
              method.getName(), method.getReturnType().getName());
        }
      }
    }
    JavaClass jc = null;
    try {
      jc = Repository.lookupClass(_class);
    } catch (java.lang.ClassNotFoundException ex) {
      throw new PanicException("Class not found: " + _class);
    }
    properties = new HashSet<Property>();
    methods = new HashSet<Method>();
    Map<String,Property> fieldMap = new HashMap<String,Property>();
    for (Method m : jc.getMethods()) {
      String methodName = m.getName();
      Class returnClass = null; // need both class & type
      try {
        returnClass = _class.getMethod(methodName).getReturnType();
      } catch (NoSuchMethodException e) {};
      Type[] parameterTypes = m.getArgumentTypes();
      // for get and set methods
      String fieldName = unCapitalize(methodName.substring(3));
      // for boolean is method
      String fieldNameBool = unCapitalize(methodName.substring(2));
      // get method?
      if (methodName.substring(0,3).equals("get")) {
        Property record = fieldMap.get(fieldName);
        if (parameterTypes.length != 0) {
          throw new PanicException("Class %s: method %s\n has bad signature\n",
              jc.getClassName(), methodName);
        }
        Type returnType = m.getReturnType();
        // first time?
        if (record == null) {
          Property newRecord = new Property(returnClass, returnType, fieldName);
          properties.add(newRecord);
          newRecord.getMethod = m;
          fieldMap.put(fieldName, newRecord);
        } else {  // not first time
          if (!record.type.equals(returnType)) {
            throw new PanicException("Class %s: inconsistent definitions for field type %s: %s vs %s\n",
                jc.getClassName(), fieldName, record.type, returnType);
          } else if (record.getMethod != null) {
            throw new PanicException("Class %s: multiple definitions for %s\n",
                jc.getClassName(), methodName);
          }
          record.getMethod = m;
        }
        // is method
      } else if (methodName.substring(0,2).equals("is")) {
        Property record = fieldMap.get(fieldNameBool);
        if (parameterTypes.length != 0) {
          throw new PanicException("Class %s: method %s\n has bad signature\n",
              jc.getClassName(), methodName);
        }
        Type returnType = m.getReturnType();
        if (!returnType.equals(Type.BOOLEAN)) {
          throw new PanicException("Class %s: method %s\n must have Boolean argument\n",
              jc.getClassName(), methodName);
        }
        // first time?
        if (record == null) {
          Property newRecord = new Property(returnClass, returnType, fieldNameBool);
          properties.add(newRecord);
          newRecord.getMethod = m;
          fieldMap.put(fieldNameBool, newRecord);
        } else {  // not first time
          if (!record.type.equals(returnType)) {
            throw new PanicException("Class %s: inconsistent definitions for field type %s: %s vs %s\n",
                jc.getClassName(), fieldNameBool, record.type, returnType);
          } else if (record.getMethod != null) {
            throw new PanicException("Class %s: multiple definitions for %s\n",
                jc.getClassName(), methodName);
          }
          record.getMethod = m;
        }
      } else if (methodName.substring(0,3).equals("set")) {
        Property record = fieldMap.get(fieldName);
        if (parameterTypes.length != 1) {
          throw new PanicException("Class %s: method %s has bad signature\n",
              jc.getClassName(), methodName);
        }
        Type argType = parameterTypes[0];
        // first time?
        if (record == null) {
          Property newRecord = new Property(returnClass, argType, fieldName);
          properties.add(newRecord);
          newRecord.setMethod = m;
          fieldMap.put(fieldName, newRecord);
        } else {  // not first time
          if (!record.type.equals(argType)) {
            throw new PanicException("Class %s: inconsistent definitions for field arg type %s: %s vs %s\n",
                jc.getClassName(), fieldName, record.type, argType);
          } else if (record.setMethod != null) {
            throw new PanicException("Class %s: multiple definitions for %s\n",
                jc.getClassName(), methodName);
          }
          record.setMethod = m;
        }
      } else {
        throw new PanicException("%s: not a get/set method\n", m.getName());
      }
    }
    for (Entry<String, Property> entry : fieldMap.entrySet()) {
      if (entry.getValue().getMethod == null) {
        throw new PanicException("Class %s: method get%s not defined\n",
            jc.getClassName(), entry.getKey());
      } else if (entry.getValue().setMethod == null) {
        throw new PanicException("Class %s: method set%s not defined\n",
            jc.getClassName(), entry.getKey());
      }
    }
  }
  
  /**
   * Did caller declare a <CODE>void release()</CODE> method?
   * @return whether to provide a release() implementation.
   */
  protected boolean releaseDefined() {
    for (Method m : methods) {
      if (m.getName().equals("release") &&
          m.getReturnType() == Type.NULL &&
          m.getArgumentTypes().length == 0) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Checks whether a field type is scalar or declared atomic.
   * @param _class class to check
   * @return whether field is legit type for an atomic object
   */
  public boolean isAtomicOrScalar(Class _class) {
    return _class.isPrimitive() ||
        _class.isEnum() ||
        _class.isAnnotationPresent(atomic.class) ||
        _class.equals(Boolean.class) ||
        _class.equals(Character.class) ||
        _class.equals(Byte.class) ||
        _class.equals(Short.class) ||
        _class.equals(Integer.class) ||
        _class.equals(Long.class) ||
        _class.equals(Float.class) ||
        _class.equals(Double.class) ||
        _class.equals(String.class) ||
	_class.equals(com.sun.fortress.interpreter.evaluator.values.FValue.class);
  }
  
  String unCapitalize(String s) {
    StringBuffer sb = new StringBuffer(s);
    sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
    return sb.toString();
  }
}
