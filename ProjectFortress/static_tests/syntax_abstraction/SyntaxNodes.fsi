(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

api SyntaxNodes

    import FortressAst.{...}
    import FortressSyntax.Literal

    grammar G extends Literal
        LiteralExpr |Expr:=
          b-a-d x:Thing <[ x ]>

        Thing :Expr:=
          a <[ 1 ]>
        | b <[ 1 + 1 ]>
        | c <[ "hello" ]>
        | d <[ if 2 > 1 then
                1
                else
                2
                end ]>
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

        (*
        | f (x:Thing y:Thing) <[ 1 ]>
        *)
    end

end
