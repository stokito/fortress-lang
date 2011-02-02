(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxHelloWorldTemplate

  import FortressAst.{...}
  import FortressSyntax.{Expression}

  grammar helloworld extends Expression
    Expr |Expr:=
       h:hello Beautiful World
         do
           StringLiteralExpr(h.in_text " " Beautiful.in_text " " World.in_text)
         end
     | hello a1:Beautiful a2:Beautiful World <[ hello " " a1 " " a2 " " World ]>

    World :Expr:=
      world
        do
          StringLiteralExpr("world")
        end

    Beautiful :Expr:=
      beautiful <[ "beautiful" ]>
  end

end
