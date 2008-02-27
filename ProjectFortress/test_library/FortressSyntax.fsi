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

  grammar Declaration 
      Decls : List[\Decl\] end
      Decl : List[\Decl\] end
      AbsDecls : List[\AbsDecl\] end
      AbsDecl : AbsDecl end
  end

  grammar Expression 
      Expr : Expr end
      ExprFront : Expr end
      MathPrimary : Expr end
      Primary : List[\Expr\] end (* Should be a pure list *)
  end

  grammar Identifier 
      id : String end
      idstart : String end
      idrest : String end
  end

  grammar Literal 
      LiteralExpr : Expr end
  end
end
