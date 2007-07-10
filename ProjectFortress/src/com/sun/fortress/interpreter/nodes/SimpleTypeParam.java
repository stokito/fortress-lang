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

public class SimpleTypeParam extends StaticParam {
  private final Id _id;
  private final Option<List<TypeRef>> _extendsClause;
  private final boolean _absorbs;

  /**
   * Constructs a SimpleTypeParam.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public SimpleTypeParam(Span in_span, Id in_id, Option<List<TypeRef>> in_extendsClause, boolean in_absorbs) {
    super(in_span);

    if (in_id == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'id' to the SimpleTypeParam constructor was null");
    }
    _id = in_id;

    if (in_extendsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'extendsClause' to the SimpleTypeParam constructor was null");
    }
    _extendsClause = in_extendsClause;
    _absorbs = in_absorbs;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forSimpleTypeParam(this);
    }

    SimpleTypeParam(Span span) {
        super(span);
        _id = null;
        _extendsClause = null;
        _absorbs = false;
    }

  final public Id getId() { return _id; }
  final public Option<List<TypeRef>> getExtendsClause() { return _extendsClause; }
  final public boolean isAbsorbs() { return _absorbs; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forSimpleTypeParam(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forSimpleTypeParam(this); }

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
    writer.print("SimpleTypeParam:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    Id temp_id = getId();
    writer.startLine();
    writer.print("id = ");
    temp_id.outputHelp(writer, lossless);

    Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
    writer.startLine();
    writer.print("extendsClause = ");
    if (lossless) {
      writer.printSerialized(temp_extendsClause);
      writer.print(" ");
      writer.printEscaped(temp_extendsClause);
    } else { writer.print(temp_extendsClause); }

    boolean temp_absorbs = isAbsorbs();
    writer.startLine();
    writer.print("absorbs = ");
    writer.print(temp_absorbs);
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
      SimpleTypeParam casted = (SimpleTypeParam) obj;
      Id temp_id = getId();
      Id casted_id = casted.getId();
      if (!(temp_id == casted_id || temp_id.equals(casted_id))) return false;
      Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
      Option<List<TypeRef>> casted_extendsClause = casted.getExtendsClause();
      if (!(temp_extendsClause == casted_extendsClause || temp_extendsClause.equals(casted_extendsClause))) return false;
      boolean temp_absorbs = isAbsorbs();
      boolean casted_absorbs = casted.isAbsorbs();
      if (!(temp_absorbs == casted_absorbs)) return false;
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
    Id temp_id = getId();
    code ^= temp_id.hashCode();
    Option<List<TypeRef>> temp_extendsClause = getExtendsClause();
    code ^= temp_extendsClause.hashCode();
    boolean temp_absorbs = isAbsorbs();
    code ^= temp_absorbs ? 1231 : 1237;
    return code;
  }
}
