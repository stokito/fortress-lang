(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxNodes

    import FortressAst.{...}
    import FortressSyntax.Expression

    grammar G extends Expression
        Expr |:=
          b-a-d x:Thing => <[ x ]>

        B :Expr:=
        b => <[ 17 ]>

        Thing :Expr:=
        n:B => <[ n ]>
        | q:Expr => <[ (b-a-d b) + q ]>
        (*
        b q:Expr <[ (b-a-d e q 4) + 1 ]>
        *)
        (*
          a <[ 1 ]>
        | c <[ "hello" ]>
        | d <[ if 2 > 1 then
                println "Hey, I'm in a macro!"
                1
                else
                2
                end ]>
                *)
                (*
        | e q:Expr z:Expr <[ (b-a-d a q z) ]>
        *)
                (*
        | e x:Decl <[ println x ]>
        | g {Thing} <[ 1 ]>
        | f {x:Thing y:Thing} {hi z:Thing} <[ x + y + z ]>
        | n {x:Thing q {y:Thing z:Thing}} <[ 1 ]>
        | m {x:Thing q {y:Thing z:Thing}}* <[ 1 ]>
        | m1 {x:Thing q {y:Thing z:Thing}}+ <[ 1 ]>
        | m2 {x:Thing q {y:Thing z:Thing}*}* <[ 1 ]>
        | m3 {x:Thing q}? <[ 1 ]>
        | m4 p:`+ <[ 1 ]>
        | m5 p:`+? <[ 1 ]>
        | m6 p:`+* <[ 1 ]>
        | m7 {p:`+} <[ 1 ]>
        | m8 {p:`+*} <[ 1 ]>
        | m9 {p:`+?} <[ 1 ]>
        | m10 {p:`+}? <[ 1 ]>
        | m11 {p:`+}* <[ 1 ]>
        *)

        (*
        | f (x:Thing y:Thing) <[ 1 ]>
        *)
    end

end
