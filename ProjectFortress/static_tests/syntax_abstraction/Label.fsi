api Label
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
                        label blah
                            exit blah with do 
                                (foobar bs**)
                                fs
                            end
                        end blah
                    end
                ]>
            end
    end
end
