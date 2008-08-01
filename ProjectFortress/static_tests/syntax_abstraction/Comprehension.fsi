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

api Comprehension

  import FortressAst.{...}
  import FortressSyntax.{...}
  import List.{...}

  grammar Comprehension extends { Expression, Identifier }

    Expr |Expr:=
      foo DComprehension bar
      <[ DComprehension ]>

    DComprehension :Expr:=
      a e:Expr d DGeneratorClauseList b 
      <[ DGeneratorClauseList(e) ]>

    Big :StringLiteralExpr:= BIG <[ "BIG" ]>

(*    (Big w)? LeftEncloser StaticArgs? w e:Expr wr | wr GeneratorClauseList w RightEncloser *)
    DGeneratorClauseList(e:Expr) :Expr:= 
      x:Id <- gen:Expr , GeneratorClauseList <[ "A" ]>
    | filter:Expr , GeneratorClauseList <[ "B" ]>
    | x:Id <- gen:Expr <[ __generate(gen, SumReduction, fn x => e) ]>
    | filter:Expr <[ "D" ]>      

(*
__nest(gen, fn y => DGeneratorClauseList(e))
__nest(filter, fn () => DGeneratorClauseList(e))
DGeneratorClauseList(e)
__nest(filter, fn () => e)

x:BindIdOrBindIdTuple
    LeftEncloser :Expr:=
      <| <[ "LeftEncloser" ]>

    RightEncloser :Expr:=
      |> <[ "RightEncloser" ]>
*)

  end 

end

