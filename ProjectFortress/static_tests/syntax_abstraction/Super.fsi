
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

api Super

    import FortressAst.{...}
    import FortressSyntax.Expression

    grammar A extends Expression
        Expr |Expr:=
          afoo <[ 1 ]>
    end

    grammar B extends {A, Expression}
        Expr |Expr:=
          bfoo <[ 2 ]>
        | Expr from A
    end

    grammar C extends {B, Expression}
        Expr |Expr:=
           cfoo <[ (afoo) + (bfoo) ]>
        |  Expr from B
    end

end
