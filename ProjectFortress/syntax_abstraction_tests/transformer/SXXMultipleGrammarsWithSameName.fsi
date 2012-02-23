(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SXXMultipleGrammarsWithSameName

  import FortressAst.{...}
  import FortressSyntax.{Expression}

  grammar Wrong extends Expression
    Bar :Expr:=
      bar do StringLiteral("") end
  end

  grammar Wrong extends Expression
    Foo :Expr:=
      foo do StringLiteral("") end
  end

end
