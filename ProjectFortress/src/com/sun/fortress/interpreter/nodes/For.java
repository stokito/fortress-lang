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

public class For extends DelimitedExpr {
  private final List<Generator> _gens;
  private final DoFront _body;

  /**
   * Constructs a For.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public For(Span in_span, List<Generator> in_gens, DoFront in_body) {
    super(in_span);

    if (in_gens == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'gens' to the For constructor was null. This class may not have null field values.");
    }
    _gens = in_gens;

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the For constructor was null. This class may not have null field values.");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFor(this);
    }

    For(Span span) {
        super(span);
        _gens = null;
        _body = null;
    }

  final public List<Generator> getGens() { return _gens; }
  final public DoFront getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFor(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFor(this); }

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
    writer.print("For" + ":");
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
    writer.print("gens = ");
    List<Generator> temp_gens = getGens();
    if (temp_gens == null) {
      writer.print("null");
    } else {
      writer.print(temp_gens);
    }

    writer.startLine("");
    writer.print("body = ");
    DoFront temp_body = getBody();
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
      For casted = (For) obj;
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
    code ^= getGens().hashCode();
    code ^= getBody().hashCode();
    return code;
  }
}
