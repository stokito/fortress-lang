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

api SyntaxOption

  import FortressAst.{...}
  import FortressSyntax.{...}

  grammar Helloworld extends { A, Literal }
      LiteralExpr |Expr:=
         Hello? world
         do
           if h <- Hello then
             StringLiteralExpr(h.val " " world.val)
           else
             StringLiteralExpr(world.val)
           end
         end
  end
 
  grammar A
      Hello :Expr:=
         hello       do StringLiteralExpr(hello.val) end
       | skjfjhfdskh do StringLiteralExpr("hello") end
  end

end
