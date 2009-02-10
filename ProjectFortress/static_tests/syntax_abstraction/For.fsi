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
api For

import FortressAst.{...}
import FortressSyntax.{...}

object Unreachable extends UncheckedException end

grammar ForLoop extends {Expression, Identifier}
    Expr |:=
        for {i:Id <- e:Expr ,? SPACE}* do block:Expr end =>
        <[ for2 i** ; e** ; do block ; end ]>
      | for2 i:Id* ; e:Expr* ; do block:Expr ; end =>
        case i of
            Empty => <[ block ]>
            Cons(ia, ib) =>
                case e of
                    Empty => <[ throw Unreachable ]>
                    Cons (ea, eb) =>
                    <[ ((ea).loop(fn ia => (for2 ib** ; eb** ; do block ; end))) ]>
                end
        end
end

end
