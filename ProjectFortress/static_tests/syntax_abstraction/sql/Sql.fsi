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

api Sql

    (* import FortressAst.{...} *)
    import FortressSyntax.{Expression,Literal}
    import Set.{...}
    import List.{...}

    trait SqlThing
    end

    object SqlObject extends SqlThing
    end

    grammar sql extends {Expression, Symbols, Literal}
        Expr:Sql |Expr:=
            select things from table <[ SqlObject ]>

        things:Sql :Expr:=
            thing , things <[ SqlObject() ]>
        |   thing <[ SqlObject() ]>

        thing:Sql :Expr:=
            name <[ SqlObject() ]>

        table:Sql :Expr:=
            name <[ SqlObject() ]>

        name:String :Expr:=
            m:AnyChar* <[ BIG || m ]>
    end

    grammar Symbols
        Slash:String :StringLiteralExpr:=
            / <[ "/" ]>

        AnyChar:String :Expr:=
            x:[A:Za:z0:9~!@%&] <[ x ]>
        |   `: <[ ":" ]>
        |   `# <[ "#" ]>
        |   < <[ "<" ]>
        |   > <[ ">" ]>
    end 
end
