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

public class BaseNatRef extends StaticArg {
  private final int _value;

  /**
   * Constructs a BaseNatRef.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public BaseNatRef(Span in_span, int in_value) {
    super(in_span);
    _value = in_value;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBaseNatRef(this);
    }

    BaseNatRef(Span span) {
        super(span);
        _value = 0;
    }

  final public int getValue() { return _value; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forBaseNatRef(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forBaseNatRef(this); }

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
    writer.print("BaseNatRef:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    int temp_value = getValue();
    writer.startLine();
    writer.print("value = ");
    writer.print(temp_value);
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
      BaseNatRef casted = (BaseNatRef) obj;
      int temp_value = getValue();
      int casted_value = casted.getValue();
      if (!(temp_value == casted_value)) return false;
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
    int temp_value = getValue();
    code ^= temp_value;
    return code;
  }
}
