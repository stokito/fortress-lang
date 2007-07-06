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

public class BaseBoolRef extends BoolRef {
  private final boolean _bool;

  /**
   * Constructs a BaseBoolRef.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public BaseBoolRef(Span in_span, boolean in_bool) {
    super(in_span);
    _bool = in_bool;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forBaseBoolRef(this);
    }

    BaseBoolRef(Span span) {
        super(span);
        _bool = false;
    }

  final public boolean isBool() { return _bool; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forBaseBoolRef(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forBaseBoolRef(this); }

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
    writer.print("BaseBoolRef:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    boolean temp_bool = isBool();
    writer.startLine();
    writer.print("bool = ");
    writer.print(temp_bool);
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
      BaseBoolRef casted = (BaseBoolRef) obj;
      boolean temp_bool = isBool();
      boolean casted_bool = casted.isBool();
      if (!(temp_bool == casted_bool)) return false;
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
    boolean temp_bool = isBool();
    code ^= temp_bool ? 1231 : 1237;
    return code;
  }
}
