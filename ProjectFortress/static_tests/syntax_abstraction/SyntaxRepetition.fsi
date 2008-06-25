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
  import List.{...}

  object SelectQuery(val:List[\String\]) 
    toString():String
  end
(*
  object GrammarHelper() 
    makeObjectInstance(ls:List[\Expr\]):TightJuxt
  end
*)
  grammar Select1 extends { A, B, Expression }
      Expr |Expr:=
        SELECT ATuples* world
        do 
(*          GrammarHelper().makeObjectInstance(ATuples) *)
          exprs:List[\Expr\] = emptyList[\Expr\](2);
          ids:List[\Id\] = emptyList[\Id\]();
          ids1:List[\Id\] = ids.addRight(Id(Nothing[\APIName\], "SyntaxRepetition"));
          apiName:APIName = APIName(ids1);
          typeName:String = "" "SelectQuery";
          name:Id = Id(Just[\APIName\](apiName), typeName);
          exprs1:List[\Expr\] = exprs.addRight(FnRef( <| name |> , emptyList[\StaticArg\]()));
          ops1:List[\OpName\] = emptyList[\OpName\](1);
          ops2:List[\OpName\] = ops1.addRight(Enclosing(Op(Nothing[\APIName\], "<|", Just[\Fixity\](EnclosingFixity())), Op(Nothing[\APIName\],"|>", Just[\Fixity\](EnclosingFixity()))));
          op:OpRef = OpRef(ops2, emptyList[\StaticArg\]());
          opExpr:OpExpr = OpExpr(op, ATuples)
          exprs2:List[\Expr\] = exprs1.addRight(opExpr);
          TightJuxt(exprs2)
        end
      | SELECT BTuples* world
        do 
          exprs:List[\Expr\] = emptyList[\Expr\](2);
          ids:List[\Id\] = emptyList[\Id\]();
          ids1:List[\Id\] = ids.addRight(Id(Nothing[\APIName\], "SyntaxRepetition"));
          apiName:APIName = APIName(ids1);
          typeName:String = "" "SelectQuery";
          name:Id = Id(Just[\APIName\](apiName), typeName);
          exprs1:List[\Expr\] = exprs.addRight(FnRef( <| name |> , emptyList[\StaticArg\]()));
          ops1:List[\OpName\] = emptyList[\OpName\](1);
          ops2:List[\OpName\] = ops1.addRight(Enclosing(Op(Nothing[\APIName\], "<|", Just[\Fixity\](EnclosingFixity())), Op(Nothing[\APIName\],"|>", Just[\Fixity\](EnclosingFixity()))));
          op:OpRef = OpRef(ops2, emptyList[\StaticArg\]());
          opExpr:OpExpr = OpExpr(op, BTuples)
          exprs2:List[\Expr\] = exprs1.addRight(opExpr);
          TightJuxt(exprs2)
        end
  end

  grammar Select2 extends { A, B, Expression }
      Expr |Expr:=
        SELECT ATuples* from
        <[ SelectQuery( ATuples ) ]>
      | SELECT BTuples* from
        <[ SelectQuery( BTuples ) ]>
  end
 
  grammar A
    ATuples :Expr:=
      a SPACE do 
        exprs:List[\Expr\] = emptyList[\Expr\](2)
        exprs1:List[\Expr\] = exprs.addRight(StringLiteralExpr(""))
        exprs2:List[\Expr\] = exprs1.addRight(StringLiteralExpr(a.val))
        LooseJuxt(exprs2) end
  end

  grammar B
    BTuples :Expr:=
      b SPACE <[ "" b ]>
  end

end
