api SyntaxHelloWorld

(*  import * from FortressAst *)

  grammar helloworld
(*       import Literal from Fortress.Syntax.Literal *)

      (*public*) production helloworld : Literal extends Literal
         hello world --> StringLiteral("Hello world")
      end
  end

end
