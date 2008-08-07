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

api Case

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar Case extends { Expression, Declaration }
        Expr |Expr:=
            foo {e:Expr , SPACE}* =>
                case e of
                    Empty => Expr <[ println "none" ]>
                    Cons(e1,e2) => <[ println "some" ]>
                end
    end

end
