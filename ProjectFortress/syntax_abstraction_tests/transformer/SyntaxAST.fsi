(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxAST

  import FortressAst.{...}
  import FortressSyntax.{...}
  import List.{...}

  object SelectQuery(val:List[\String\])
    toString():String
  end

  grammar Helloworld extends { Expression }
      Expr |Expr:=
        world
        do
          exprs:List[\Expr\] = emptyList[\Expr\](2);
          ids:List[\Id\] = emptyList[\Id\]();
          ids1:List[\Id\] = ids.addRight(Id("SyntaxAST"));
          apiName:APIName = APIName(ids1);
          typeName:Id = Id("SelectQuery");
          name:QualifiedIdName = QualifiedIdName(Just[\APIName\](apiName), typeName);
          exprs1:List[\Expr\] = exprs.addRight(FnRef( <| name |> , emptyList[\StaticArg\]()));
          ops:List[\QualifiedOpName\] = <| QualifiedOpName(Nothing[\APIName\], Enclosing(Op("<|", Just[\Fixity\](EnclosingFixity())), Op("|>", Just[\Fixity\](EnclosingFixity())))) |>
          arg:List[\Expr\] = emptyList[\Expr\](1)
          tuples:List[\Expr\] = emptyList[\Expr\](2)
          tuples11:List[\Expr\] = tuples.addRight(StringLiteralExpr(""))
          tuples12:List[\Expr\] = tuples11.addRight(StringLiteralExpr("a"))
          tuples2:List[\Expr\] = tuples12.addRight(StringLiteralExpr("b"))
          arg1:List[\Expr\] = arg.addRight(LooseJuxt(tuples2))
          arg2:List[\Expr\] = arg1.addRight(StringLiteralExpr("b"))
          op:OpRef = OpRef(ops, emptyList[\StaticArg\]());
          oprExpr:OprExpr = OprExpr(op, arg2)
          exprs2:List[\Expr\] = exprs1.addRight(oprExpr);
          TightJuxt(exprs2)
        end
  end

end
