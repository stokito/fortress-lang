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

public class ArrayComprehensionClause extends AbstractNode {
  private final List<Expr> _bind;
  private final Expr _init;
  private final List<Generator> _gens;

  /**
   * Constructs a ArrayComprehensionClause.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public ArrayComprehensionClause(Span in_span, List<Expr> in_bind, Expr in_init, List<Generator> in_gens) {
    super(in_span);

    if (in_bind == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'bind' to the ArrayComprehensionClause constructor was null. This class may not have null field values.");
    }
    _bind = in_bind;

    if (in_init == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'init' to the ArrayComprehensionClause constructor was null. This class may not have null field values.");
    }
    _init = in_init;

    if (in_gens == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'gens' to the ArrayComprehensionClause constructor was null. This class may not have null field values.");
    }
    _gens = in_gens;
  }

    @Override
    public <T> T acceptInner(NodeVisitor<T> v) {
        return v.forArrayComprehensionClause(this);
    }

    ArrayComprehensionClause(Span span) {
        super(span);
        _bind = null;
        _init = null;
        _gens = null;
    }

  final public List<Expr> getBind() { return _bind; }
  final public Expr getInit() { return _init; }
  final public List<Generator> getGens() { return _gens; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forArrayComprehensionClause(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forArrayComprehensionClause(this); }

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
    writer.print("ArrayComprehensionClause" + ":");
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
    writer.print("bind = ");
    List<Expr> temp_bind = getBind();
    if (temp_bind == null) {
      writer.print("null");
    } else {
      writer.print(temp_bind);
    }

    writer.startLine("");
    writer.print("init = ");
    Expr temp_init = getInit();
    if (temp_init == null) {
      writer.print("null");
    } else {
      temp_init.outputHelp(writer);
    }

    writer.startLine("");
    writer.print("gens = ");
    List<Generator> temp_gens = getGens();
    if (temp_gens == null) {
      writer.print("null");
    } else {
      writer.print(temp_gens);
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
      ArrayComprehensionClause casted = (ArrayComprehensionClause) obj;
      if (! (getBind().equals(casted.getBind()))) return false;
      if (! (getInit().equals(casted.getInit()))) return false;
      if (! (getGens().equals(casted.getGens()))) return false;
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
    code ^= getBind().hashCode();
    code ^= getInit().hashCode();
    code ^= getGens().hashCode();
    return code;
  }
}
