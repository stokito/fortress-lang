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

api SyntaxHelloWorldTemplate

  import FortressAst.{...}
  import FortressSyntax.{Expression}

  grammar helloworld extends Expression
    Expr |Expr:=
       h:hello Beautiful World
         do
           StringLiteralExpr(h.val " " Beautiful.val " " World.val)
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
