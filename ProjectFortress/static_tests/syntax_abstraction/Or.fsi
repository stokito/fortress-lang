api Or

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar A extends {Expression, Literal}
        Expr |Expr:=
            or {x:Expr ,? SPACE}* =>
            case x of
                Empty => <[ false ]>
                Cons(xa,xb) => <[
                    do
                        println "Hello " xa
                        n = typecase f = xa of
                                Boolean => f
                                else => true
                            end
                        if n then n else or xb** end
                    end
                    ]>
            end
    end

end
