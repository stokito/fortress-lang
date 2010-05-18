/*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

package com.sun.fortress.compiler

import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.{NodeFactory => NF}
import com.sun.fortress.nodes_util.{NodeUtil => NU}
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.useful.Useful

/**
 * Desugars all patterns.
 */
class PatternMatchingDesugarer extends Walker {

  /** Walk the AST, recursively desugaring any patterns. */
  override def walk(node: Any) = node match {

    // Desugars trait value parameters as abstract fields
    case t @ STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7, params, decls),
                        t3, t4, t5, t6) =>
      val new_decls = params match {
        case Some(ps) => ps.map(paramToDecl) ::: decls
        case _ => decls
      }
      STraitDecl(t1, STraitTypeHeader(h1, h2, h3, h4, h5, h6, h7,
                                      None, new_decls.map(walk(_).asInstanceOf[Decl])), t3, t4, t5, t6)

    case _ => super.walk(node)
  }

  def paramToDecl(param: Param) = toOption(param.getIdType) match {
    case Some(ty) if ty.isInstanceOf[Type] =>
      NF.makeVarDecl(NU.getSpan(param),
                     Useful.list(NF.makeLValue(param.getName, ty, param.getMods)), None)
    case _ =>
      bug("Trait value parameters should be declared with their types.")
  }

}


/*
                LValue(Id name, Modifiers mods,
                       Option<TypeOrPattern> idType, boolean mutable);
                Param(Id name, Modifiers mods,
                      Option<TypeOrPattern> idType, Option<Expr> defaultExpr, Option<Type> varargsType);


                VarDecl(List<LValue> lhs, Option<Expr> init);
                LocalVarDecl(List<LValue> lhs, Option<Expr> rhs);

7                        TraitTypeHeader(List<TraitTypeWhere> extendsClause,
8                                        Option<List<Param>> params,
9                                        List<Decl> decls);

7                        FnHeader(List<Param> params,
8                                 Option<Type> returnType);
 */