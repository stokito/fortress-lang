/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.interpreter.nodes;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public class VectorType extends TraitType {
  private final TypeRef _element;
  private final ExtentRange _dim;

  /**
   * Constructs a VectorType.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public VectorType(Span in_span, TypeRef in_element, ExtentRange in_dim) {
    super(in_span);

    if (in_element == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'element' to the VectorType constructor was null");
    }
    _element = in_element;

    if (in_dim == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'dim' to the VectorType constructor was null");
    }
    _dim = in_dim;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forVectorType(this);
    }

    VectorType(Span span) {
        super(span);
        _element = null;
        _dim = null;
    }

  final public TypeRef getElement() { return _element; }
  final public ExtentRange getDim() { return _dim; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forVectorType(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forVectorType(this); }

  /**
   * Implementation of toString that uses
   * {@link #output} to generate a nicely tabbed tree.
   */
  public java.lang.String toString() {
    java.io.StringWriter w = new java.io.StringWriter();
    output(w);
    return w.toString();
  }

  /**
   * Prints this object out as a nicely tabbed tree.
   */
  public void output(java.io.Writer writer) {
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("VectorType:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    TypeRef temp_element = getElement();
    writer.startLine();
    writer.print("element = ");
    temp_element.outputHelp(writer, lossless);

    ExtentRange temp_dim = getDim();
    writer.startLine();
    writer.print("dim = ");
    if (lossless) {
      writer.printSerialized(temp_dim);
      writer.print(" ");
      writer.printEscaped(temp_dim);
    } else { writer.print(temp_dim); }
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values of the fields of the
   * object. Thus, two objects created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      VectorType casted = (VectorType) obj;
      TypeRef temp_element = getElement();
      TypeRef casted_element = casted.getElement();
      if (!(temp_element == casted_element || temp_element.equals(casted_element))) return false;
      ExtentRange temp_dim = getDim();
      ExtentRange casted_dim = casted.getDim();
      if (!(temp_dim == casted_dim || temp_dim.equals(casted_dim))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    TypeRef temp_element = getElement();
    code ^= temp_element.hashCode();
    ExtentRange temp_dim = getDim();
    code ^= temp_dim.hashCode();
    return code;
  }
}
