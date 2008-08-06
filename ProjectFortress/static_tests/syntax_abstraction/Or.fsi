api Or

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar A extends Expression
        Expr |Expr:=
            or x:Expr* =>
            case x of
                Empty => <[ true ]>
                Cons(x1,x2) => <[ if x1 then x1 else or x2** end ]>
            end
    end

end
