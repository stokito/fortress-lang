(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxOption

  import FortressAst.{...}
  import FortressSyntax.{Expression}

  grammar Helloworld extends { A, Expression }
      Expr |Expr:=
        Hello? world
        do
        if h <- Hello then
            StringLiteralExpr(h.in_text " " world.in_text ", ")
          else
            StringLiteralExpr(world.in_text ", ")
          end
        end
      | Foo? bar <[
          if h <- Foo then
            "h bar"
          else
            "bar"
          end
        ]>

  end

  grammar A
      Hello :StringLiteralExpr:=
         hello       do StringLiteralExpr(hello.in_text) end
       | skjfjhfdskh do StringLiteralExpr("hello") end

      Foo :StringLiteralExpr:=
         foo <[ foo ]>
  end

end
