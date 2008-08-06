api Case

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar Case extends { Expression, Declaration }
        Expr |Expr:=
            foo {e:Expr , SPACE}* =>
                case e of
                    Empty => Expr <[ println "none" ]>
                    Cons(e1,e2) => <[ println "some" ]>
                end
    end

end
