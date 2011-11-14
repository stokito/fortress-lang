(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SXXSyntaxGrammarImports

  import FortressAst.{...}
  import FortressSyntax.{...}

  (* Fails because it refers to Expr without importing Expression *)
  grammar Hello extends { Identifier }
      Expr |Expr:=
         hello => <[ "hello" ]>
  end

end
