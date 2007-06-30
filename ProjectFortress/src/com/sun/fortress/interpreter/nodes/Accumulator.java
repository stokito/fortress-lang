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

public class Accumulator extends FlowExpr {
  private final Op _op;
  private final List<Generator> _gens;
  private final Expr _body;

  /**
   * Constructs a Accumulator.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public Accumulator(Span in_span, Op in_op, List<Generator> in_gens, Expr in_body) {
    super(in_span);

    if (in_op == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'op' to the Accumulator constructor was null. This class may not have null field values.");
    }
    _op = in_op;

    if (in_gens == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'gens' to the Accumulator constructor was null. This class may not have null field values.");
    }
    _gens = in_gens;

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the Accumulator constructor was null. This class may not have null field values.");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forAccumulator(this);
    }

    Accumulator(Span span) {
        super(span);
        _op = null;
        _gens = null;
        _body = null;
    }

  final public Op getOp() { return _op; }
  final public List<Generator> getGens() { return _gens; }
  final public Expr getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forAccumulator(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forAccumulator(this); }

  /**
   * Implementation of toString that uses
   * {@see #output} to generated nicely tabbed tree.
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
    outputHelp(new TabPrintWriter(writer, 2));
  }

  public void outputHelp(TabPrintWriter writer) {
    writer.print("Accumulator" + ":");
    writer.indent();

    writer.startLine("");
    writer.print("span = ");
    Span temp_span = getSpan();
    if (temp_span == null) {
      writer.print("null");
    } else {
      writer.print(temp_span);
    }

    writer.startLine("");
    writer.print("op = ");
    Op temp_op = getOp();
    if (temp_op == null) {
      writer.print("null");
    } else {
      temp_op.outputHelp(writer);
    }

    writer.startLine("");
    writer.print("gens = ");
    List<Generator> temp_gens = getGens();
    if (temp_gens == null) {
      writer.print("null");
    } else {
      writer.print(temp_gens);
    }

    writer.startLine("");
    writer.print("body = ");
    Expr temp_body = getBody();
    if (temp_body == null) {
      writer.print("null");
    } else {
      temp_body.outputHelp(writer);
    }
    writer.unindent();
  }

  /**
   * Implementation of equals that is based on the values
   * of the fields of the object. Thus, two objects
   * created with identical parameters will be equal.
   */
  public boolean equals(java.lang.Object obj) {
    if (obj == null) return false;
    if ((obj.getClass() != this.getClass()) || (obj.hashCode() != this.hashCode())) {
      return false;
    } else {
      Accumulator casted = (Accumulator) obj;
      if (! (getOp().equals(casted.getOp()))) return false;
      if (! (getGens().equals(casted.getGens()))) return false;
      if (! (getBody().equals(casted.getBody()))) return false;
      return true;
    }
  }

  /**
   * Implementation of hashCode that is consistent with
   * equals. The value of the hashCode is formed by
   * XORing the hashcode of the class object with
   * the hashcodes of all the fields of the object.
   */
  protected int generateHashCode() {
    int code = getClass().hashCode();
    code ^= 0;
    code ^= getOp().hashCode();
    code ^= getGens().hashCode();
    code ^= getBody().hashCode();
    return code;
  }
}
