api For
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar M extends {Expression, Identifier}
        Expr |Expr:=
            ffor {i:Id <- e:Expr ,? SPACE}* ; Do block:Expr ; End =>
            <[ ffor2 i** ; e** ; fdo block ; fend ]>
            (*
            case i of
                Empty => <[ block ]>
                Cons(ia, ib) =>
                    case e of
                        Cons (ea, eb) => <[ (ea).loop( fn ia => ffor2 ib** ; eb** ; fdo block fend ) ]>
                    end
            end
            *)
        | ffor2 i:Id* ; e:Expr* ; Do block:Expr ; End =>
            case i of
                Empty => <[ block ]>
                Cons(ia, ib) =>
                    case e of
                        Cons (ea, eb) => <[ ( (ea).loop( fn ia => (ffor2 ib** ; eb** ; fdo block ; fend) ) ) ]>
                    end
            end

        Do :Expr:=
            fdo => <[ "" ]>

        End :Expr:=
            fend => <[ "" ]>
    end
end
