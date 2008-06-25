(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************)

api FortressSyntax

  import FortressAst.{...}
  import List.{...}

  grammar Declaration 
      Decls : List[\Decl\]
      Decl : List[\Decl\]
      AbsDecls : List[\AbsDecl\]
      AbsDecl : AbsDecl
  end

  grammar Expression 
      Expr : Expr
      ExprFront : Expr
      MathPrimary : Expr
      Primary : List[\Expr\] (* Should be a pure list *)
  end

  grammar Identifier 
      id : String
      idstart : String
      idrest : String
      Id : Id
      BindId : Id
      BindIdOrBindIdTuple : List[\Id\]
  end

  grammar Literal 
      LiteralExpr : Expr
  end
end
