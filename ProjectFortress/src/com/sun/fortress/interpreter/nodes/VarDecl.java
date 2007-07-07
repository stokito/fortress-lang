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

public class VarDecl extends VarDefOrDecl implements Decl {
  private final Expr _init;

  /**
   * Constructs a VarDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public VarDecl(Span in_span, List<LValue> in_lhs, Expr in_init) {
    super(in_span, in_lhs);

    if (in_init == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'init' to the VarDecl constructor was null");
    }
    _init = in_init;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forVarDecl(this);
    }

    VarDecl(Span span) {
        super(span);
        _init = null;
    }

  final public Expr getInit() { return _init; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forVarDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forVarDecl(this); }

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
    writer.print("VarDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<LValue> temp_lhs = getLhs();
    writer.startLine();
    writer.print("lhs = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_lhs = true;
    for (LValue elt_temp_lhs : temp_lhs) {
      isempty_temp_lhs = false;
      writer.startLine("* ");
      if (elt_temp_lhs == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_lhs);
          writer.print(" ");
          writer.printEscaped(elt_temp_lhs);
        } else { writer.print(elt_temp_lhs); }
      }
    }
    writer.unindent();
    if (isempty_temp_lhs) writer.print(" }");
    else writer.startLine("}");

    Expr temp_init = getInit();
    writer.startLine();
    writer.print("init = ");
    temp_init.outputHelp(writer, lossless);
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
      VarDecl casted = (VarDecl) obj;
      List<LValue> temp_lhs = getLhs();
      List<LValue> casted_lhs = casted.getLhs();
      if (!(temp_lhs == casted_lhs || temp_lhs.equals(casted_lhs))) return false;
      Expr temp_init = getInit();
      Expr casted_init = casted.getInit();
      if (!(temp_init == casted_init || temp_init.equals(casted_init))) return false;
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
    List<LValue> temp_lhs = getLhs();
    code ^= temp_lhs.hashCode();
    Expr temp_init = getInit();
    code ^= temp_init.hashCode();
    return code;
  }
}
