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

public class CharLiteral extends Literal {
  private final int _val;

  /**
   * Constructs a CharLiteral.
   * @throw java.lang.IllegalArgumentException if any parameter to the constructor is null.
   */
  public CharLiteral(Span in_span, String in_text, int in_val) {
    super(in_span, in_text);
    _val = in_val;
  }

  public CharLiteral(Span in_span) {
    super(in_span);
    _val = 0;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forCharLiteral(this);
    }

  final public int getVal() { return _val; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forCharLiteral(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forCharLiteral(this); }

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
    writer.print("CharLiteral" + ":");
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
    writer.print("text = ");
    String temp_text = getText();
    if (temp_text == null) {
      writer.print("null");
    } else {
      writer.print(temp_text);
    }

    writer.startLine("");
    writer.print("val = ");
    int temp_val = getVal();
    writer.print(temp_val);
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
      CharLiteral casted = (CharLiteral) obj;
      if (! (getText() == casted.getText())) return false;
      if (! (getVal() == casted.getVal())) return false;
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
    code ^= getText().hashCode();
    code ^= getVal();
    return code;
  }
}
