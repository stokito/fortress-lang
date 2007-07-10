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

public class Name extends FnName {
  private final Option<Id> _id;
  private final Option<Op> _op;

  /**
   * Constructs a Name.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public Name(Span in_span, Option<Id> in_id, Option<Op> in_op) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the Name constructor was null");
    }
    _id = in_id;

    if (in_op == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'op' to the Name constructor was null");
    }
    _op = in_op;
  }

    Name(Span span) {
        super(span);
        _id = null;
        _op= null;
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forName(this);
    }

  final public Option<Id> getId() { return _id; }
  final public Option<Op> getOp() { return _op; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forName(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forName(this); }

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
    writer.print("Name:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<Id> temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    if (lossless) {
      writer.printSerialized(temp_id);
      writer.print(" ");
      writer.printEscaped(temp_id);
    } else { writer.print(temp_id); }

    Option<Op> temp_op = getOp();
    writer.startLine();
    writer.print("op = ");
    if (lossless) {
      writer.printSerialized(temp_op);
      writer.print(" ");
      writer.printEscaped(temp_op);
    } else { writer.print(temp_op); }
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
      Name casted = (Name) obj;
      Option<Id> temp_id = getId();
      Option<Id> casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      Option<Op> temp_op = getOp();
      Option<Op> casted_op = casted.getOp();
      if (!(temp_op == casted_op || temp_op.equals(casted_op))) return false;
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
    Option<Id> temp_id = getId();
    code ^= temp_id.hashCode();
    Option<Op> temp_op = getOp();
    code ^= temp_op.hashCode();
    return code;
  }
}
