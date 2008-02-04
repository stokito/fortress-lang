api SyntaxGrammarImports

  import FortressAst.{...}
  import FortressSyntax.{...}
  import SyntaxGrammarImportsA.{...}

  grammar Helloworld extends { A, B, Literal }
      LiteralExpr |Expr=
         Hello a:Beautiful world do StringLiteralExpr(Hello.val a.val "world") end
      end
  end
 
  grammar B extends Literal
      Foo :Expr:=
         when do StringLiteralExpr("in 84") end
      end

      Beautiful :Expr:=
         beautiful do StringLiteralExpr(" beautiful ") end
      end
  end

end
