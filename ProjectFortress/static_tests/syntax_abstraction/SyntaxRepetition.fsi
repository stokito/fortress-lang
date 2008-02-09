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

api SyntaxRepetition

  import FortressAst.{...}
  import FortressSyntax.{...}

  grammar Helloworld extends { A, Literal }
      LiteralExpr |Expr=
         Hello* world do 
           StringLiteralExpr( (BIG STRING [x<-sequential(Hello)] x.val " ") world.val) end
      end
  end
 
  grammar A
      Hello :Expr:=
         hello SPACE do StringLiteralExpr("hello") end
      end
  end

end
