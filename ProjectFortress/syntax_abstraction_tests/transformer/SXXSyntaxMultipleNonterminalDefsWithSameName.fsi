(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api XXXSyntaxMultipleNonterminalDefsWithSameName

  import FortressAst.{...}
  import FortressSyntax.{Expression}

  grammar Wrong extends Expression
    Foo :Expr:=
      wrong do StringLiteral("") end

    Foo :Expr:=
      very wrong do StringLiteral("") end

  end

end
