(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)
api LetCC

import FortressAst.{...}
import FortressSyntax.{...}

grammar LetCC extends {Expression, Identifier}
    Expr |:=
        letcc k:Id e:Expr =>
        <[ label L
               k(v) = exit L with v
               e
           end L
        ]>
end

end
