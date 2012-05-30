(*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Typecase
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
                        typecase "One" of
                            q:String => q " " fs
                            else => (foobar bs**)
                        end
                    end
                ]>
            end
    end
end
