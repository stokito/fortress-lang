/*
 * Property.java
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
import dstm2.atomic;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

/**
 * A property is a private or protected field <CODE>prop</CODE> of type
 * T with <CODE>T getProp()</CODE> and <CODE>void setProp(T)</CODE> methods.
 * If T is Boolean, getter can be <CODE>boolean isProp()</CODE>.
 */
public class Property {
  /**
   * Java class of property
   */
  public Class _class;
  /**
   * BCEL type of property
   */
  public Type type;
  /**
   * Method that reads property value.
   */
  public Method getMethod;
  /**
   * Method that sets property value.
   */
  public Method setMethod;
  /**
   * Name of field associated with property.
   */
  public String name;
  /**
   * Create a property
   * @param _type property type
   * @param _name property name
   */
  Property(Class _class, Type _type, String _name) {
    this._class = _class;
    this.type   = _type;
    this.name   = _name;
  }
  
 
  /**
   * for debugging
   * @return property description
   */
  public String toString() {
    return String.format("Property[%s %s]", type, name);
  }
  
}
