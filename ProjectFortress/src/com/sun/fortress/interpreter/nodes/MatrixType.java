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

public class MatrixType extends TraitType {
  private final TypeRef _element;
  private final List<ExtentRange> _dimensions;

  /**
   * Constructs a MatrixType.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public MatrixType(Span in_span, TypeRef in_element, List<ExtentRange> in_dimensions) {
    super(in_span);

    if (in_element == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'element' to the MatrixType constructor was null");
    }
    _element = in_element;

    if (in_dimensions == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'dimensions' to the MatrixType constructor was null");
    }
    _dimensions = in_dimensions;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forMatrixType(this);
    }

    MatrixType(Span span) {
        super(span);
        _element = null;
        _dimensions = null;
    }

  final public TypeRef getElement() { return _element; }
  final public List<ExtentRange> getDimensions() { return _dimensions; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forMatrixType(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forMatrixType(this); }

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

  public void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("MatrixType:");
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

    List<ExtentRange> temp_dimensions = getDimensions();
    writer.startLine();
    writer.print("dimensions = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_dimensions = true;
    for (ExtentRange elt_temp_dimensions : temp_dimensions) {
      isempty_temp_dimensions = false;
      writer.startLine("* ");
      if (elt_temp_dimensions == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_dimensions);
          writer.print(" ");
          writer.printEscaped(elt_temp_dimensions);
        } else { writer.print(elt_temp_dimensions); }
      }
    }
    writer.unindent();
    if (isempty_temp_dimensions) writer.print(" }");
    else writer.startLine("}");
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
      MatrixType casted = (MatrixType) obj;
      TypeRef temp_element = getElement();
      TypeRef casted_element = casted.getElement();
      if (!(temp_element == casted_element || temp_element.equals(casted_element))) return false;
      List<ExtentRange> temp_dimensions = getDimensions();
      List<ExtentRange> casted_dimensions = casted.getDimensions();
      if (!(temp_dimensions == casted_dimensions || temp_dimensions.equals(casted_dimensions))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with equals.  The value of
   * the hashCode is formed by XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  public int generateHashCode() {
    int code = getClass().hashCode();
    TypeRef temp_element = getElement();
    code ^= temp_element.hashCode();
    List<ExtentRange> temp_dimensions = getDimensions();
    code ^= temp_dimensions.hashCode();
    return code;
  }
}
