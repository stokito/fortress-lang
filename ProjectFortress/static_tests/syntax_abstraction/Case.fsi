api Case

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar Case extends Expression
        Expr |Expr:=
            foo {e:Expr , SPACE}* =>
                case e of
                    Empty => <[ println "none" ]>
                    Cons(e1,e2) => <[ println "some" ]>
                end
    end

end
