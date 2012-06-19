(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api KeywordNotId

    import FortressAst.{...}
    import FortressSyntax.Expression

    grammar G extends Expression
        Expr |:=
          a:one b:fish c:two d:fish => <[ "red fish blue fish" ]>
    end

end
