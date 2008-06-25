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

api SyntaxTemplateVars

  import FortressAst.{...}
  import FortressSyntax.{Expression,Identifier}

(*  *)
  grammar helloworld extends { Expression, Identifier }
    Expr |Expr:=
      hello a1:Beautiful a2:Beautiful a3:World <[ hello " " a1 " " a2 " " a3(a1) " " a3(hello) ]>

(*
    | a b:BindIdOrBindIdTuple x:Id y:Id Foo <[ a " " Foo(b,x,y) ]> 
*)
    World(e:Expr) :Expr:=
      world <[ e " world" ]>

(*
    Foo(b:BindIdOrBindIdTuple, x:Id, y:Id) :Expr:=
      foo <[ 
        do 
          b = (7,6)
          x * y
        end
      ]>
*)
    Beautiful :Expr:=
      beautiful <[ "beautiful" ]>
    | foobar do StringLiteralExpr("foobar") end

  end


end
