api SyntaxGrammarImportsA
 
  import FortressAst.{...}

  grammar A
      Hello:Expr:=
         hello do StringLiteralExpr("Hello") end
      end

      World:Expr:=
         the answer do FortressAst.IntLiteralExpr("42") end
      end

  end

end
