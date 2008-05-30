api SyntaxNodes
  
    import FortressAst.{...}
    import FortressSyntax.Literal

    grammar G extends Literal
        LiteralExpr |Expr:=
          b-a-d x:Thing <[ x ]>
       
        Thing :Expr:=
          a <[ 1 ]>
        | b <[ 1 + 1 ]>
        | c <[ "hello" ]>
        | d <[ if 2 > 1 then
                1
                else 
                2
                end ]>
        | e x:Decl <[ println x ]>
        | g {Thing} <[ 1 ]>
        | f {x:Thing y:Thing} <[ 1 ]>
        | n {x:Thing q {y:Thing z:Thing}} <[ 1 ]>
        (*
        | f (x:Thing y:Thing) <[ 1 ]>
        *)
    end

end
