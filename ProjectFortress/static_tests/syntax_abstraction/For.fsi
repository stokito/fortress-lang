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

api For
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar M extends {Expression, Identifier}
        Expr |Expr:=
            a:for {i:Id <- e:Expr ,? SPACE}* ; b:do block:Expr ; c:end =>
            <[ for2 i** ; e** ; do block ; end ]>
        | a:for2 i:Id* ; e:Expr* ; b:do block:Expr ; c:end =>
            case i of
                Empty => <[ block ]>
                Cons(ia, ib) =>
                    case e of
                        Cons (ea, eb) => 
                          <[ ((ea).loop(fn ia => (for2 ib** ; eb** ; do block ; end)))]>
                    end
            end
    end
end
