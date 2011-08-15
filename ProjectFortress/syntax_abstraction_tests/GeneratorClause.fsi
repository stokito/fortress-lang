(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api GeneratorClause
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar L extends {Expression, Identifier}
        Expr |:=
            a:foobar {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ println 0 ]>
                Cons(fs,bs) =>
                    <[
                    do
                        for x <- 0#(fs asif ZZ32) do
                            println x
                            (foobar bs**)
                        end
                    end
                ]>
            end
       |  a:goobar {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ println "Empty" ]>
                Cons(fs,bs) =>
                    <[
                    do
                        var t: Boolean = fs
                        while x <- t do
                            t := false
                            println "Cons"
                            (goobar bs**)
                        end
                    end
                ]>
            end
        |  a:moobar {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ 0 ]>
                Cons(fs,bs) =>
                    <[
                    do
                        if x <- fs
                        then 1 + (moobar bs**)
                        else (moobar bs**)
                        end
                    end
                ]>
            end
        |  a:noobar {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ 0 ]>
                Cons(fs,bs) =>
                    <[ (SUM [v <- 0:(fs asif ZZ32)] v) + (noobar bs**) ]>
            end

    end
end
