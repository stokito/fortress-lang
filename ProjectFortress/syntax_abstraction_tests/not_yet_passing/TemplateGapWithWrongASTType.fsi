(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api TemplateGapWithWrongASTType

  import FortressAst.{...}
  import FortressSyntax.{...}

  grammar helloworld extends { Expression, Declaration }
    Expr |Expr:=
      x:Hello a1:World => <[ x " " a1(x) ]>

    World(e:StringLiteralExpr) :Expr:=
      world => <[ e " world" ]>

    Hello :StringLiteralExpr:=
      hello => <[ "7" ]>
  end


end
