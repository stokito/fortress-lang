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

public class SumStaticArg extends CompoundStaticArg {
  private final List<StaticArg> _values;

  /**
   * Constructs a SumStaticArg.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public SumStaticArg(Span in_span, List<StaticArg> in_values) {
    super(in_span);

    if (in_values == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'values' to the SumStaticArg constructor was null");
    }
    _values = in_values;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSumStaticArg(this);
    }

    SumStaticArg(Span span) {
        super(span);
        _values = null;
    }

  final public List<StaticArg> getValues() { return _values; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forSumStaticArg(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forSumStaticArg(this); }

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
    writer.print("SumStaticArg:");
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
      SumStaticArg casted = (SumStaticArg) obj;
      List<StaticArg> temp_values = getValues();
      List<StaticArg> casted_values = casted.getValues();
      if (!(temp_values == casted_values || temp_values.equals(casted_values))) return false;
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
    List<StaticArg> temp_values = getValues();
    code ^= temp_values.hashCode();
    return code;
  }
}
