(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
            v:name# :# column:name <[
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
