(*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
                           pr(z) =
                               if z = () then println "()" else println "Other" end
                           if x <- fs
                           then println x
                                1 + (foo bs**)
                           else (foo bs**)
                           end
                       end
                     ]>
            end
end

end
