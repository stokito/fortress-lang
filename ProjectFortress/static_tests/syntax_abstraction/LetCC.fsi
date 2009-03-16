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
