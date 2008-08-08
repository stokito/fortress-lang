api For
    import FortressAst.{...}
    import FortressSyntax.{...}

    grammar M extends {Expression, Identifier}
        Expr |Expr:=
            for {i:Id <- e:Expr ,? SPACE}* ; do block:Expr ; end =>
            <[ for2 i** ; e** ; do block ; end ]>
            (*
            case i of
                Empty => <[ block ]>
                Cons(ia, ib) =>
                    case e of
                        Cons (ea, eb) => <[ (ea).loop( fn ia => ffor2 ib** ; eb** ; fdo block fend ) ]>
                    end
            end
            *)
        | for2 i:Id* ; e:Expr* ; do block:Expr ; end =>
            case i of
                Empty => <[ block ]>
                Cons(ia, ib) =>
                    case e of
                        Cons (ea, eb) => <[ ( (ea).loop( fn ia => (for2 ib** ; eb** ; do block ; end) ) ) ]>
                    end
            end

        (*
        Do :Expr:=
            fdo => <[ "" ]>

        End :Expr:=
            fend => <[ "" ]>
            *)
    end
end
