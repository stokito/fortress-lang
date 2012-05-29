(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Case

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar Case extends { Expression, Declaration }
        Expr |:=
            foo {e:Expr , SPACE}* =>
                case e of
                    Empty => Expr <[ println "none" ]>
                    Cons(e1,e2) => <[ println "some" ]>
                end
    end

end
