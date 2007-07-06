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
    public <T> T accept(NodeVisitor<T> v) {
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
    outputHelp(new TabPrintWriter(writer, 2), false);
  }

  protected void outputHelp(TabPrintWriter writer, boolean lossless) {
    writer.print("ArrayComprehensionClause:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Expr> temp_bind = getBind();
    writer.startLine();
    writer.print("bind = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_bind = true;
    for (Expr elt_temp_bind : temp_bind) {
      isempty_temp_bind = false;
      writer.startLine("* ");
      if (elt_temp_bind == null) {
        writer.print("null");
      } else {
        elt_temp_bind.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_bind) writer.print(" }");
    else writer.startLine("}");

    Expr temp_init = getInit();
    writer.startLine();
    writer.print("init = ");
    temp_init.outputHelp(writer, lossless);

    List<Generator> temp_gens = getGens();
    writer.startLine();
    writer.print("gens = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_gens = true;
    for (Generator elt_temp_gens : temp_gens) {
      isempty_temp_gens = false;
      writer.startLine("* ");
      if (elt_temp_gens == null) {
        writer.print("null");
      } else {
        elt_temp_gens.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_gens) writer.print(" }");
    else writer.startLine("}");
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
