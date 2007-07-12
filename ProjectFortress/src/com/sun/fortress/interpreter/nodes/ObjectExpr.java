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
import com.sun.fortress.interpreter.evaluator.values.FValue;
import com.sun.fortress.interpreter.nodes_util.*;
import com.sun.fortress.interpreter.useful.*;

public class ObjectExpr extends AbsObjectExpr {

  /**
   * Constructs a ObjectExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public ObjectExpr(Span in_span, Option<List<TypeRef>> in_traits, List<? extends DefOrDecl> in_defOrDecls) {
    super(in_span, in_traits, in_defOrDecls);
  }

    public ObjectExpr(Span span) {
        super(span);
    }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forObjectExpr(this);
    }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forObjectExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forObjectExpr(this); }

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
    writer.print("ObjectExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Option<List<TypeRef>> temp_traits = getTraits();
    writer.startLine();
    writer.print("traits = ");
    if (lossless) {
      writer.printSerialized(temp_traits);
      writer.print(" ");
      writer.printEscaped(temp_traits);
    } else { writer.print(temp_traits); }

    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    writer.startLine();
    writer.print("defOrDecls = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_defOrDecls = true;
    for (DefOrDecl elt_temp_defOrDecls : temp_defOrDecls) {
      isempty_temp_defOrDecls = false;
      writer.startLine("* ");
      if (elt_temp_defOrDecls == null) {
        writer.print("null");
      } else {
        elt_temp_defOrDecls.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_defOrDecls) writer.print(" }");
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
      ObjectExpr casted = (ObjectExpr) obj;
      Option<List<TypeRef>> temp_traits = getTraits();
      Option<List<TypeRef>> casted_traits = casted.getTraits();
      if (!(temp_traits == casted_traits || temp_traits.equals(casted_traits))) return false;
      List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
      List<? extends DefOrDecl> casted_defOrDecls = casted.getDefOrDecls();
      if (!(temp_defOrDecls == casted_defOrDecls || temp_defOrDecls.equals(casted_defOrDecls))) return false;
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
    Option<List<TypeRef>> temp_traits = getTraits();
    code ^= temp_traits.hashCode();
    List<? extends DefOrDecl> temp_defOrDecls = getDefOrDecls();
    code ^= temp_defOrDecls.hashCode();
    return code;
  }
}
