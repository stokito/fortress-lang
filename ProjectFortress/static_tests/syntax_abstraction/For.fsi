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
    Expr |:= for b:forStart => <[ b ]>

    forStart :Expr:=
        i:Id <- e:Expr d:doFront      => <[ ((e).loop(fn i => d)) ]>
      | e:Expr d:doFront              => <[ if e then d end ]>
      | i:Id <- e:Expr , b:forStart   => <[ ((e).loop(fn i => b)) ]>
      | e:Expr , b:forStart           => <[ if e then b end ]>

    doFront :Expr:=
        a:atomicFront => <[ a ]>
      | at e:Expr a:atomicFront => <[ a ]>

    atomicFront :Expr:=
        do d:doBody => <[ d ]>
      | atomic do d:doBody =>
        <[ label atomicBlock
               while true do
                   try
                       result = tryatomic d
                       exit atomicBlock with result
                   catch e
                       TryAtomicFailure => ()
                   end
               end
               throw Unreachable
           end atomicBlock
        ]>

    doBody :Expr:=
        end => <[ () ]>
      | block:Expr end => <[ block ]>
end

end
