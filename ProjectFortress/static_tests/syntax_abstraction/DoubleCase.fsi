api DoubleCase

    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar Case extends Expression
        Expr |Expr:=
            foo {e:Expr , SPACE}* =>
                case e of
                    Empty => <[ println "none" ]>
                    Cons(e1,e2) =>
                    case e2 of 
                        Empty => <[ println "some 1" ]>
                        Cons(e3,e4) => <[ println "some more" ]>
                    end
                end
    end

end
