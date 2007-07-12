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

public abstract class FnDefOrDecl extends AbstractNode implements Generic, Applicable, DefOrDecl {
  private final List<Modifier> _mods;
  private final FnName _fnName;
  private final Option<List<StaticParam>> _staticParams;
  private final List<Param> _params;
  private final Option<TypeRef> _returnType;
  private final List<TypeRef> _throwsClause;
  private final List<WhereClause> _where;
  private final Contract _contract;

  /**
   * Constructs a FnDefOrDecl.
   * @throws java.lang.IllegalArgumentException  If any parameter to the constructor is null.
   */
  public FnDefOrDecl(Span in_span, List<Modifier> in_mods, FnName in_fnName, Option<List<StaticParam>> in_staticParams, List<Param> in_params, Option<TypeRef> in_returnType, List<TypeRef> in_throwsClause, List<WhereClause> in_where, Contract in_contract) {
    super(in_span);

    if (in_mods == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'mods' to the FnDefOrDecl constructor was null");
    }
    _mods = in_mods;

    if (in_fnName == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'fnName' to the FnDefOrDecl constructor was null");
    }
    _fnName = in_fnName;

    if (in_staticParams == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'staticParams' to the FnDefOrDecl constructor was null");
    }
    _staticParams = in_staticParams;

    if (in_params == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'params' to the FnDefOrDecl constructor was null");
    }
    _params = in_params;

    if (in_returnType == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'returnType' to the FnDefOrDecl constructor was null");
    }
    _returnType = in_returnType;

    if (in_throwsClause == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'throwsClause' to the FnDefOrDecl constructor was null");
    }
    _throwsClause = in_throwsClause;

    if (in_where == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'where' to the FnDefOrDecl constructor was null");
    }
    _where = in_where;

    if (in_contract == null) {
      throw new java.lang.IllegalArgumentException("Parameter 'contract' to the FnDefOrDecl constructor was null");
    }
    _contract = in_contract;
  }

    public FnDefOrDecl(Span span) {
        super(span);
        _mods = null;
        _fnName = null;
        _staticParams = null;
        _params = null;
        _returnType = null;
        _throwsClause = null;
        _where = null;
        _contract = null;
    }

  public List<Modifier> getMods() { return _mods; }
  public FnName getFnName() { return _fnName; }
  public Option<List<StaticParam>> getStaticParams() { return _staticParams; }
  public List<Param> getParams() { return _params; }
  public Option<TypeRef> getReturnType() { return _returnType; }
  public List<TypeRef> getThrowsClause() { return _throwsClause; }
  public List<WhereClause> getWhere() { return _where; }
  public Contract getContract() { return _contract; }

  public abstract <RetType> RetType visit(NodeVisitor<RetType> visitor);
  public abstract void visit(NodeVisitor_void visitor);
  public abstract void output(java.io.Writer writer);
  public abstract void outputHelp(TabPrintWriter writer, boolean lossless);
  public abstract int generateHashCode();
}
