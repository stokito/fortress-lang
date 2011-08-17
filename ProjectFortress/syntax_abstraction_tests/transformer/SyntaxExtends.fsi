(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxExtends

  import FortressAst.{...}
  import FortressSyntax.{...}
  import List.{...}

  grammar A extends { Declaration, D }
    Decl |List[\Decl\]:=
      some Thing
        do
          fnDef : FnDecl = FnDecl(emptyList[\Modifier\](),
                     Id(Nothing[\APIName\], "some" Thing.in_text),
                     emptyList[\StaticParam\](),
                     emptyList[\Param\](),
                     Nothing[\Type\],
                     Nothing[\List[\BaseType\]\],
                     WhereClause(emptyList[\WhereBinding\](), emptyList[\WhereConstraint\]()),
                     Contract(Nothing[\List[\Expr\]\], Nothing[\List[\EnsuresClause\]\], Nothing[\List[\Expr\]\]),
                     "foo",
                     StringLiteralExpr("some " Thing.in_text))
          ls : List[\FnDecl\] = emptyList[\FnDecl\](1)
          ls.addRight(fnDef)
        end
  end

  grammar B extends { C1, C2 }
    Thing |Expr:=
      thingB1 <[ thingB1 ]>
  end

  grammar C
    Thing :Expr:=
      thingC0 <[ thingC0 ]>
  end

  grammar D extends { Expression, C }
    Expr |Expr:=
      Thing do Thing.in_text end

    Bar :Expr:=
      Expr do Expr end
  end

  grammar C1 extends C
    Thing |Expr:=
      thingC1 <[ thingC1 ]>

    Gnu :Expr:=
      Thing do Thing.in_text end

  end

  grammar C2 extends C
    Thing |Expr:=
      thingC2 <[ thingC2 ]>
  end

  grammar E
    Thing :Expr:=
      Foo <[ Foo ]>
  end

  grammar F extends C
    Thing |Expr:=
      thingF <[ thingF ]>
  end

end
