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

public class DimensionStaticArg extends CompoundStaticArg {
  private final DimUnitOp _op;

  /**
   * Constructs a DimensionStaticArg.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public DimensionStaticArg(Span in_span, List<StaticArg> in_values, DimUnitOp in_op) {
    super(in_span, in_values);

    if (in_op == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'op' to the DimensionStaticArg constructor was null");
    }
    _op = in_op;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDimensionStaticArg(this);
    }

    DimensionStaticArg(Span span) {
        super(span);
        _op = null;
    }

  final public DimUnitOp getOp() { return _op; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forDimensionStaticArg(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forDimensionStaticArg(this); }

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
    writer.print("DimensionStaticArg:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<StaticArg> temp_values = getValues();
    writer.startLine();
    writer.print("values = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_values = true;
    for (StaticArg elt_temp_values : temp_values) {
      isempty_temp_values = false;
      writer.startLine("* ");
      if (elt_temp_values == null) {
        writer.print("null");
      } else {
        elt_temp_values.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_values) writer.print(" }");
    else writer.startLine("}");

    DimUnitOp temp_op = getOp();
    writer.startLine();
    writer.print("op = ");
    temp_op.outputHelp(writer, lossless);
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
      DimensionStaticArg casted = (DimensionStaticArg) obj;
      List<StaticArg> temp_values = getValues();
      List<StaticArg> casted_values = casted.getValues();
      if (!(temp_values == casted_values || temp_values.equals(casted_values))) return false;
      DimUnitOp temp_op = getOp();
      DimUnitOp casted_op = casted.getOp();
      if (!(temp_op == casted_op || temp_op.equals(casted_op))) return false;
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
    List<StaticArg> temp_values = getValues();
    code ^= temp_values.hashCode();
    DimUnitOp temp_op = getOp();
    code ^= temp_op.hashCode();
    return code;
  }
}
