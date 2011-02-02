(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
