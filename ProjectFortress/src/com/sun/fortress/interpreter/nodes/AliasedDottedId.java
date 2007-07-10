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

public class AliasedDottedId extends AbstractNode {
  private final DottedId _id;
  private final Option<DottedId> _alias;

  /**
   * Constructs a AliasedDottedId.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public AliasedDottedId(Span in_span, DottedId in_id, Option<DottedId> in_alias) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the AliasedDottedId constructor was null");
    }
    _id = in_id;

    if (in_alias == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'alias' to the AliasedDottedId constructor was null");
    }
    _alias = in_alias;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        // TODO Auto-generated method stub
        return null;
    }

    public AliasedDottedId(Span span) {
        super(span);
        // TODO Auto-generated constructor stub
        _id = null;
        _alias = null;
    }

  final public DottedId getId() { return _id; }
  final public Option<DottedId> getAlias() { return _alias; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forAliasedDottedId(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forAliasedDottedId(this); }

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
    writer.print("AliasedDottedId:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    DottedId temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    temp_id.outputHelp(writer, lossless);

    Option<DottedId> temp_alias = getAlias();
    writer.startLine();
    writer.print("alias = ");
    if (lossless) {
      writer.printSerialized(temp_alias);
      writer.print(" ");
      writer.printEscaped(temp_alias);
    } else { writer.print(temp_alias); }
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
      AliasedDottedId casted = (AliasedDottedId) obj;
      DottedId temp_id = getId();
      DottedId casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      Option<DottedId> temp_alias = getAlias();
      Option<DottedId> casted_alias = casted.getAlias();
      if (!(temp_alias == casted_alias || temp_alias.equals(casted_alias))) return false;
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
    DottedId temp_id = getId();
    code ^= temp_id.hashCode();
    Option<DottedId> temp_alias = getAlias();
    code ^= temp_alias.hashCode();
    return code;
  }
}
