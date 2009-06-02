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

api Or
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar M extends {Expression, Identifier}
        Expr |:=
            a:or {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ false ]>
                Cons(fs,ds) => <[
                    do
                        n = fs
                        if n then
                            n
                        else
                            (or ds**)
                        end
                    end
                ]>
            end
    end
end
