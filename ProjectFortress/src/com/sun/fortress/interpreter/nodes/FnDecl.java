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

public class FnDecl extends FnDefOrDecl implements Decl, Applicable {
  private final String _selfName;
  private final Expr _body;

  /**
   * Constructs a FnDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public FnDecl(Span in_span, List<Modifier> in_mods, FnName in_fnName, Option<List<StaticParam>> in_staticParams, List<Param> in_params, Option<TypeRef> in_returnType, List<TypeRef> in_throwsClause, List<WhereClause> in_where, Contract in_contract, String in_selfName, Expr in_body) {
    super(in_span, in_mods, in_fnName, in_staticParams, in_params, in_returnType, in_throwsClause, in_where, in_contract);

    if (in_selfName == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'selfName' to the FnDecl constructor was null");
    }
    _selfName = in_selfName.intern();

    if (in_body == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'body' to the FnDecl constructor was null");
    }
    _body = in_body;
  }

    @Override
    public <T> T accept(NodeVisitor<T> v) {
        return v.forFnDecl(this);
    }

    FnDecl(Span span) {
        super(span);
        _selfName = null;
        _body = null;
    }

  final public String getSelfName() { return _selfName; }
  final public Expr getBody() { return _body; }

  public <RetType> RetType visit(NodeVisitor<RetType> visitor) { return visitor.forFnDecl(this); }
  public void visit(NodeVisitor_void visitor) { visitor.forFnDecl(this); }

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
    writer.print("FnDecl:");
    writer.indent();

    Span temp_span = getSpan();
    writer.startLine();
    writer.print("span = ");
    if (lossless) {
      writer.printSerialized(temp_span);
      writer.print(" ");
      writer.printEscaped(temp_span);
    } else { writer.print(temp_span); }

    List<Modifier> temp_mods = getMods();
    writer.startLine();
    writer.print("mods = ");
    writer.print("{");
    writer.indent();
    boolean isempty_temp_mods = true;
    for (Modifier elt_temp_mods : temp_mods) {
      isempty_temp_mods = false;
      writer.startLine("* ");
      if (elt_temp_mods == null) {
        writer.print("null");
      } else {
        elt_temp_mods.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_mods) writer.print(" }");
    else writer.startLine("}");

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
        elt_temp_where.outputHelp(writer, lossless);
      }
    }
    writer.unindent();
    if (isempty_temp_where) writer.print(" }");
    else writer.startLine("}");

    Contract temp_contract = getContract();
    writer.startLine();
    writer.print("contract = ");
    temp_contract.outputHelp(writer, lossless);

    String temp_selfName = getSelfName();
    writer.startLine();
    writer.print("selfName = ");
    if (lossless) {
      writer.print("\"");
      writer.printEscaped(temp_selfName);
      writer.print("\"");
    } else { writer.print(temp_selfName); }

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
      FnDecl casted = (FnDecl) obj;
      List<Modifier> temp_mods = getMods();
      List<Modifier> casted_mods = casted.getMods();
      if (!(temp_mods == casted_mods || temp_mods.equals(casted_mods))) return false;
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
      List<TypeRef> temp_throwsClause = getThrowsClause();
      List<TypeRef> casted_throwsClause = casted.getThrowsClause();
      if (!(temp_throwsClause == casted_throwsClause || temp_throwsClause.equals(casted_throwsClause))) return false;
      List<WhereClause> temp_where = getWhere();
      List<WhereClause> casted_where = casted.getWhere();
      if (!(temp_where == casted_where || temp_where.equals(casted_where))) return false;
      Contract temp_contract = getContract();
      Contract casted_contract = casted.getContract();
      if (!(temp_contract == casted_contract || temp_contract.equals(casted_contract))) return false;
      String temp_selfName = getSelfName();
      String casted_selfName = casted.getSelfName();
      if (!(temp_selfName == casted_selfName)) return false;
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
    List<Modifier> temp_mods = getMods();
    code ^= temp_mods.hashCode();
    FnName temp_fnName = getFnName();
    code ^= temp_fnName.hashCode();
    Option<List<StaticParam>> temp_staticParams = getStaticParams();
    code ^= temp_staticParams.hashCode();
    List<Param> temp_params = getParams();
    code ^= temp_params.hashCode();
    Option<TypeRef> temp_returnType = getReturnType();
    code ^= temp_returnType.hashCode();
    List<TypeRef> temp_throwsClause = getThrowsClause();
    code ^= temp_throwsClause.hashCode();
    List<WhereClause> temp_where = getWhere();
    code ^= temp_where.hashCode();
    Contract temp_contract = getContract();
    code ^= temp_contract.hashCode();
    String temp_selfName = getSelfName();
    code ^= temp_selfName.hashCode();
    Expr temp_body = getBody();
    code ^= temp_body.hashCode();
    return code;
  }
}
