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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public class _WrappedFValue extends DelimitedExpr {
  private final FValue _fValue;

  /**
   * Constructs a _WrappedFValue.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public _WrappedFValue(Span in_span, FValue in_fValue) {
    super(in_span);

    if (in_fValue == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'fValue' to the _WrappedFValue constructor was null");
    }
    _fValue = in_fValue;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.for_WrappedFValue(this);
    }

    _WrappedFValue(Span span) {
        super(span);
        _fValue = null;
    }

  final public FValue getFValue() { return _fValue; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.for_WrappedFValue(this); }
  public void visit(NodeVisitor_void visitor) { visitor.for_WrappedFValue(this); }

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
    writer.print("_WrappedFValue:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    FValue temp_fValue = getFValue();
    writer.startLine();
    writer.print("fValue = ");
    if (lossless) {
      writer.printSerialized(temp_fValue);
      writer.print(" ");
      writer.printEscaped(temp_fValue);
    } else { writer.print(temp_fValue); }
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
      _WrappedFValue casted = (_WrappedFValue) obj;
      FValue temp_fValue = getFValue();
      FValue casted_fValue = casted.getFValue();
      if (!(temp_fValue == casted_fValue || temp_fValue.equals(casted_fValue))) return false;
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
    FValue temp_fValue = getFValue();
    code ^= temp_fValue.hashCode();
    return code;
  }
}
