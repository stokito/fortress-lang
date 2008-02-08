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

api SyntaxGrammarImports

  import FortressAst.{...}
  import FortressSyntax.{...}
  import SyntaxGrammarImportsA.{...}

  grammar Helloworld extends { A, B, Literal }
      LiteralExpr |Expr=
         Hello a:Beautiful world do StringLiteralExpr(Hello.val a.val "world") end
      end
  end
 
  grammar B extends Literal
      Foo :Expr:=
         when do StringLiteralExpr("in 84") end
      end

      Beautiful :Expr:=
         beautiful do StringLiteralExpr(" beautiful ") end
      end
  end

end
