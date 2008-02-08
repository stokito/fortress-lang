api SyntaxProductionExtends

  import * from FortressAst

  grammar A extends { B }
      production Foo : Literal extends Bar end
      production Bar : Literal end
  end

  grammar B 
      production Bar:Literal end
  end

end
