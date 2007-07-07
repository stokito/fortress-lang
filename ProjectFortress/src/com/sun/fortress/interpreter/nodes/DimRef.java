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

public class DimRef extends DimType {
  private final StaticArg _val;

  /**
   * Constructs a DimRef.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public DimRef(Span in_span, StaticArg in_val) {
    super(in_span);

    if (in_val == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'val' to the DimRef constructor was null");
    }
    _val = in_val;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forDimRef(this);
    }

    DimRef(Span span) {
        super(span);
        _val = null;
    }

  final public StaticArg getVal() { return _val; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forDimRef(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forDimRef(this); }

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
    writer.print("DimRef:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    StaticArg temp_val = getVal();
    writer.startLine();
    writer.print("val = ");
    temp_val.outputHelp(writer, lossless);
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
      DimRef casted = (DimRef) obj;
      StaticArg temp_val = getVal();
      StaticArg casted_val = casted.getVal();
      if (!(temp_val == casted_val || temp_val.equals(casted_val))) return false;
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
    StaticArg temp_val = getVal();
    code ^= temp_val.hashCode();
    return code;
  }
}
