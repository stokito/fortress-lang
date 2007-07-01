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

public class EnsuresClause extends AbstractNode {
  private final Expr _post;
  private final Option<Expr> _pre;

  /**
   * Constructs a EnsuresClause.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public EnsuresClause(Span in_span, Expr in_post, Option<Expr> in_pre) {
    super(in_span);

    if (in_post == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'post' to the EnsuresClause constructor was null. This class may not have null field values.");
    }
    _post = in_post;

    if (in_pre == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'pre' to the EnsuresClause constructor was null. This class may not have null field values.");
    }
    _pre = in_pre;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forEnsuresClause(this);
    }

    EnsuresClause(Span span) {
        super(span);
        _post = null;
        _pre = null;
    }

  final public Expr getPost() { return _post; }
  final public Option<Expr> getPre() { return _pre; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forEnsuresClause(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forEnsuresClause(this); }

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
    writer.print("EnsuresClause" + ":");
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
    writer.print("post = ");
    Expr temp_post = getPost();
    if (temp_post == null) {
      writer.print("null");
    } else {
      temp_post.outputHelp(writer);
    }

    writer.startLine("");
    writer.print("pre = ");
    Option<Expr> temp_pre = getPre();
    if (temp_pre == null) {
      writer.print("null");
    } else {
      writer.print(temp_pre);
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
      EnsuresClause casted = (EnsuresClause) obj;
      if (! (getPost().equals(casted.getPost()))) return false;
      if (! (getPre().equals(casted.getPre()))) return false;
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
    code ^= getPost().hashCode();
    code ^= getPre().hashCode();
    return code;
  }
}
