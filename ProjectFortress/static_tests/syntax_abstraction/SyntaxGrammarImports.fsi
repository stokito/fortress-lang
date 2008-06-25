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

  grammar Helloworld extends { A, B, Expression }
      Expr |Expr:=
         Hello a:Beautiful world <[ Hello " " a " world" ]>
  end

  grammar B 
      Foo :Expr:=
         when <[ "in 84" ]>

      Beautiful :Expr:=
         beautiful <[ beautiful ]>
  end

end
