(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
        SELECT tuples:ATuples* world
        do
(*          GrammarHelper().makeObjectInstance(ATuples) *)
          exprs:List[\Expr\] = emptyList[\Expr\](2)
          ids:List[\Id\] = emptyList[\Id\]()
          ids1:List[\Id\] = ids.addRight(Id(Nothing[\APIName\], "SyntaxRepetition"))
          apiName:APIName = APIName(ids1)
          typeName:String = "" "SelectQuery"
          name:Id = Id(Just[\APIName\](apiName), typeName)
          staticArgs:List[\StaticArg\] = emptyList[\StaticArg\]()
          names:List[\Id\] = <| name |>
          exprs1:List[\Expr\] = exprs.addRight(FnRef(1, name, names, staticArgs))

          in_api:Maybe[\APIName\] = Nothing[\APIName\]
          open:Op = Op(Nothing[\APIName\], "<|", Just[\Fixity\](EnclosingFixity))
          close:Op = Op(Nothing[\APIName\],"|>", Just[\Fixity\](EnclosingFixity))
          enclosing:Enclosing = Enclosing(in_api, open, close)
          ops1:List[\OpName\] = emptyList[\OpName\](1)
          ops2:List[\OpName\] = ops1.addRight(enclosing)
          listStaticArgs1:List[\StaticArg\] = emptyList[\StaticArg\]()
          fortressBuiltin1:List[\Id\] = emptyList[\Id\]()
          fortressBuiltin2:List[\Id\] = fortressBuiltin1.addRight(Id(Nothing[\APIName\], "FortressBuiltin"))
          fortressBuiltin:APIName = APIName(fortressBuiltin2)
          stringId:Id = Id(Just[\APIName\](fortressBuiltin), "String")
          stringType:Type = TraitType(stringId, 1, emptyList[\StaticArg\]())
          typeArg:TypeArg = TypeArg(stringType)
          listStaticArgs:List[\StaticArg\] = emptyList[\StaticArg\]()
          listStaticArgs2:List[\StaticArg\] = listStaticArgs.addRight(typeArg);
          op:OpRef = OpRef(1, enclosing, ops2, listStaticArgs)
          opExpr:OpExpr = OpExpr(op, tuples)
          exprs2:List[\Expr\] = exprs1.addRight(opExpr)

          multiFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](MultiFixity))
          multifix1:List[\OpName\] = emptyList[\OpName\]()
          multifix2:List[\OpName\] = multifix1.addRight(multiFix)
          multiFixOpRef:OpRef = OpRef(1, multiFix, multifix2, emptyList[\StaticArg\]())
          inFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](InFixity))
          infix1:List[\OpName\] = emptyList[\OpName\]()
          infix2:List[\OpName\] = infix1.addRight(inFix)
          inFixOpRef:OpRef = OpRef(1, inFix, infix2, emptyList[\StaticArg\]())
          TightJuxt(multiFixOpRef, inFixOpRef, exprs2)

        end
      | SELECT BTuples* world
        do
          StringLiteralExpr("BBBB")
(*          exprs:List[\Expr\] = emptyList[\Expr\](2)
          ids:List[\Id\] = emptyList[\Id\]()
          ids1:List[\Id\] = ids.addRight(Id(Nothing[\APIName\], "SyntaxRepetition"))
          apiName:APIName = APIName(ids1)
          typeName:String = "" "SelectQuery"
          name:Id = Id(Just[\APIName\](apiName), typeName)
          names:List[\Id\] = <| name |>
          staticArgs:List[\StaticArg\] = emptyList[\StaticArg\]()
          exprs1:List[\Expr\] = exprs.addRight(FnRef(1, name, names, staticArgs))


          in_api:Maybe[\APIName\] = Nothing[\APIName\]
          open:Op = Op(Nothing[\APIName\], "<|", Just[\Fixity\](EnclosingFixity))
          close:Op = Op(Nothing[\APIName\],"|>", Just[\Fixity\](EnclosingFixity))
          enclosing:Enclosing = Enclosing(in_api, open, close)
          ops1:List[\OpName\] = emptyList[\OpName\](1)
          ops2:List[\OpName\] = ops1.addRight(enclosing)
          op:OpRef = OpRef(1, enclosing, ops2, emptyList[\StaticArg\]())
          opExpr:OpExpr = OpExpr(op, BTuples)
          exprs2:List[\Expr\] = exprs1.addRight(opExpr)

          multiFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](MultiFixity))
          multifix1:List[\OpName\] = emptyList[\OpName\]()
          multifix2:List[\OpName\] = multifix1.addRight(multiFix)
          multiFixOpRef:OpRef = OpRef(1, multiFix, multifix2, emptyList[\StaticArg\]())
          inFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](InFixity))
          infix1:List[\OpName\] = emptyList[\OpName\]()
          infix2:List[\OpName\] = infix1.addRight(inFix)
          inFixOpRef:OpRef = OpRef(1, inFix, infix2, emptyList[\StaticArg\]())
          TightJuxt(multiFixOpRef, inFixOpRef, exprs2)
*)
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
        exprs2:List[\Expr\] = exprs1.addRight(StringLiteralExpr(a.in_text))
        multiFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](MultiFixity))
        multifix1:List[\OpName\] = emptyList[\OpName\]()
        multifix2:List[\OpName\] = multifix1.addRight(multiFix)
        multiFixOpRef:OpRef = OpRef(1, multiFix, multifix2, emptyList[\StaticArg\]())
        inFix:Op = Op(Nothing[\APIName\], "juxtaposition", Just[\Fixity\](InFixity))
        infix1:List[\OpName\] = emptyList[\OpName\]()
        infix2:List[\OpName\] = infix1.addRight(inFix)
        inFixOpRef:OpRef = OpRef(1, inFix, infix2, emptyList[\StaticArg\]())
        LooseJuxt(multiFixOpRef, inFixOpRef, exprs2)
      end
  end

  grammar B
    BTuples :Expr:=
      b SPACE <[ "" b ]>
  end

end
