(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SXXTemplateParamsAreNotApplicable
  import FortressAst.{...}

  grammar A
    Foo(e:LooseJuxt) :Expr:= b:[ab] foo a:_ => <[ e(foo) a(foo) b(foo) ]>
  end

end
