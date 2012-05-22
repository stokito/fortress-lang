(*******************************************************************************
    Copyright 2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api LocalFn

import FortressAst.{...}
import FortressSyntax.{...}

grammar LocalFn extends { Expression, Declaration }
    Expr |:=
        foo {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ 0 ]>
                Cons(fs,bs) =>
                    <[ do
                           pr(z) = z
                           pr(fs) + (foo bs**)
                       end
                     ]>
            end
end

end
