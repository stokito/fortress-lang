(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Catch
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar L extends {Expression, Identifier}
        Expr |:=
            a:foobar {e:Expr ,? SPACE}* =>
            case e of
                Empty => <[ 1 ]>
                Cons(fs,bs) =>
                    <[
                    do
                        try
                            fs
                        catch z
                            FailCalled => (foobar bs**)
                        end
                    end
                ]>
            end
    end
end
