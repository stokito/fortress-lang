(*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************)

api SyntaxRepetition

  import FortressAst.{...}
  import FortressSyntax.{...}
  import ArrayList.{...}

  object SelectQuery(val:List[\String\]) 
    toString():String
  end

  grammar Helloworld extends { A, Literal }
      LiteralExpr |Expr:=
        a:SELECT Tuples* world
        do 
          exprs:List[\Expr\] = emptyList[\Expr\](2);
          ids:List[\Id\] = emptyList[\Id\]();
          ids1:List[\Id\] = ids.addRight(Id("SyntaxRepetition"));
          apiName:APIName = APIName(ids1);
          typeName:Id = Id("SelectQuery");
          name:QualifiedIdName = QualifiedIdName(Just[\APIName\](apiName), typeName);
          exprs1:List[\Expr\] = exprs.addRight(FnRef( <| name |> , emptyList[\StaticArg\]()));
          ops:List[\QualifiedOpName\] = <| QualifiedOpName(Nothing[\APIName\], Enclosing(Op("<|", Just[\Fixity\](EnclosingFixity())), Op("|>", Just[\Fixity\](EnclosingFixity())))) |>
          op:OpRef = OpRef(ops, emptyList[\StaticArg\]());
          oprExpr:OprExpr = OprExpr(op, Tuples)
          exprs2:List[\Expr\] = exprs1.addRight(oprExpr);
          TightJuxt(exprs2)
        end
      end
  end
 
  grammar A
    Tuples :Expr:=
      a SPACE do 
        exprs:List[\Expr\] = emptyList[\Expr\](2)
        exprs1:List[\Expr\] = exprs.addRight(StringLiteralExpr(""))
        exprs2:List[\Expr\] = exprs1.addRight(StringLiteralExpr(a))
        LooseJuxt(exprs2) end
    end
  end

end
