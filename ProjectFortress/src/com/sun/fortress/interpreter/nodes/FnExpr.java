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

public class FnExpr extends Expr implements Decl, Applicable {
  private final FnName _fnName;
  private final Option<List<StaticParam>> _staticParams;
  private final List<Param> _params;
  private final Option<TypeRef> _returnType;
  private final List<WhereClause> _where;
  private final List<TypeRef> _throwsClause;
  private final Expr _body;

  /**
   * Constructs a FnExpr.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public FnExpr(Span in_span, FnName in_fnName, Option<List<StaticParam>> in_staticParams, List<Param> in_params, Option<TypeRef> in_returnType, List<WhereClause> in_where, List<TypeRef> in_throwsClause, Expr in_body) {
    super(in_span);

    if (in_fnName == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'fnName' to the FnExpr constructor was null");
    }
    _fnName = in_fnName;

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the FnExpr constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_params == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'params' to the FnExpr constructor was null");
    }
    _params = in_params;

    if (in_returnType == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'returnType' to the FnExpr constructor was null");
    }
    _returnType = in_returnType;

    if (in_where == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'where' to the FnExpr constructor was null");
    }
    _where = in_where;

    if (in_throwsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'throwsClause' to the FnExpr constructor was null");
    }
    _throwsClause = in_throwsClause;

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the FnExpr constructor was null");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFnExpr(this);
    }

    FnExpr(Span span) {
        super(span);
        _fnName = null;
        _staticParams = null;
        _params = null;
        _returnType = null;
        _where = null;
        _throwsClause = null;
        _body = null;
    }

  final public FnName getFnName() { return _fnName; }
  final public Option<List<StaticParam>> getStaticParams() { return _staticParams; }
  final public List<Param> getParams() { return _params; }
  final public Option<TypeRef> getReturnType() { return _returnType; }
  final public List<WhereClause> getWhere() { return _where; }
  final public List<TypeRef> getThrowsClause() { return _throwsClause; }
  final public Expr getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFnExpr(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFnExpr(this); }

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
    writer.print("FnExpr:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    FnName temp_fnName = getFnName();
    writer.startLine();
    writer.print("fnName = ");
    temp_fnName.outputHelp(writer, lossless);

    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    writer.startLine();
    writer.print("staticParams = ");
    if (lossless) {
      writer.printSerialized(temp_staticParams);
      writer.print(" ");
      writer.printEscaped(temp_staticParams);
    } else { writer.print(temp_staticParams); }

    List<Param> temp_params = getParams();
    writer.startLine();
    writer.print("params = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_params = true;
    for (Param elt_temp_params : temp_params) {
      isempty_temp_params = false;
      writer.startLine("* ");
      if (elt_temp_params == null) {
        writer.print("null");
      } else {
        elt_temp_params.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_params) writer.print(" }");
    else writer.startLine("}");

    Option<TypeRef> temp_returnType = getReturnType();
    writer.startLine();
    writer.print("returnType = ");
    if (lossless) {
      writer.printSerialized(temp_returnType);
      writer.print(" ");
      writer.printEscaped(temp_returnType);
    } else { writer.print(temp_returnType); }

    List<WhereClause> temp_where = getWhere();
    writer.startLine();
    writer.print("where = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_where = true;
    for (WhereClause elt_temp_where : temp_where) {
      isempty_temp_where = false;
      writer.startLine("* ");
      if (elt_temp_where == null) {
        writer.print("null");
      } else {
        if (lossless) {
          writer.printSerialized(elt_temp_where);
          writer.print(" ");
          writer.printEscaped(elt_temp_where);
        } else { writer.print(elt_temp_where); }
      }
    }
    writer.unindent();
    if (isempty_temp_where) writer.print(" }");
    else writer.startLine("}");

    List<TypeRef> temp_throwsClause = getThrowsClause();
    writer.startLine();
    writer.print("throwsClause = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_throwsClause = true;
    for (TypeRef elt_temp_throwsClause : temp_throwsClause) {
      isempty_temp_throwsClause = false;
      writer.startLine("* ");
      if (elt_temp_throwsClause == null) {
        writer.print("null");
      } else {
        elt_temp_throwsClause.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_throwsClause) writer.print(" }");
    else writer.startLine("}");

    Expr temp_body = getBody();
    writer.startLine();
    writer.print("body = ");
    temp_body.outputHelp(writer, lossless);
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
      FnExpr casted = (FnExpr) obj;
      FnName temp_fnName = getFnName();
      FnName casted_fnName = casted.getFnName();
      if (!(temp_fnName == casted_fnName || temp_fnName.equals(casted_fnName))) return false;
      Option<List<StaticParam>> temp_staticParams = getStaticParams();
      Option<List<StaticParam>> casted_staticParams = casted.getStaticParams();
      if (!(temp_staticParams == casted_staticParams || temp_staticParams.equals(casted_staticParams))) return false;
      List<Param> temp_params = getParams();
      List<Param> casted_params = casted.getParams();
      if (!(temp_params == casted_params || temp_params.equals(casted_params))) return false;
      Option<TypeRef> temp_returnType = getReturnType();
      Option<TypeRef> casted_returnType = casted.getReturnType();
      if (!(temp_returnType == casted_returnType || temp_returnType.equals(casted_returnType))) return false;
      List<WhereClause> temp_where = getWhere();
      List<WhereClause> casted_where = casted.getWhere();
      if (!(temp_where == casted_where || temp_where.equals(casted_where))) return false;
      List<TypeRef> temp_throwsClause = getThrowsClause();
      List<TypeRef> casted_throwsClause = casted.getThrowsClause();
      if (!(temp_throwsClause == casted_throwsClause || temp_throwsClause.equals(casted_throwsClause))) return false;
      Expr temp_body = getBody();
      Expr casted_body = casted.getBody();
      if (!(temp_body == casted_body || temp_body.equals(casted_body))) return false;
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
    FnName temp_fnName = getFnName();
    code ^= temp_fnName.hashCode();
    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    List<Param> temp_params = getParams();
    code ^= temp_params.hashCode();
    Option<TypeRef> temp_returnType = getReturnType();
    code ^= temp_returnType.hashCode();
    List<WhereClause> temp_where = getWhere();
    code ^= temp_where.hashCode();
    List<TypeRef> temp_throwsClause = getThrowsClause();
    code ^= temp_throwsClause.hashCode();
    Expr temp_body = getBody();
    code ^= temp_body.hashCode();
    return code;
  }
}
