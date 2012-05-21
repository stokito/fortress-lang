(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SyntaxSymbols

  import FortressAst.{...}
  import FortressSyntax.{Literal, Expression}

  grammar A extends { Literal, Expression }
(* mangler ??*)

    LiteralExpr |Expr:=
         `FORMFEED [a`TAB] [b``c`TAB:J] [K:`TAB]   world  <[ "escape test   ok" ]>
       | e:FunnyThingstest <[ e ]>
       | e:Operatortest <[ e ]>
       | e:Hashtest <[ e ]>
       | e:Nottest <[ e ]>
       | e:Andtest <[ e ]>
       | e:Spacetest <[ e ]>
       | e:Tabtest <[ e ]>
       | e:Returntest <[ e ]>
       | e:Newlinetest <[ e ]>
       | e:SPACEtest <[ e ]>
       | e:AnyCharTest <[ e ]>

(*
       | e:Formfeedtest <[ e ]>
       | e:Backspacetest <[ e ]>
*)

(*
    EscapePlustest :Expr:=
       `++ he`+llo h:hel`+lo NOT he`+llo <[ "escape plus test   ok" ]>
*)


    FunnyThingstest :Expr:=
      hello ( world )   <[ "funny thing test   ok" ]>

    Operatortest :Expr:=
      a:hello  gnu abe* c:world# d:hello+ <[ "operator test    ok" ]>

    Hashtest :Expr:=
      a:foo# SPACE# b:hello#? c:hello# d:hello#* world#+ NOT foo# NOT hello# NOT hello#+ AND bar# f:bar <[ "hash test   ok" ]>

    Nottest :Expr:=
      NOT hello NOT hello+ NOT hello# NOT hello+ NOT bar oof  <[ "not test ok" ]>

    Andtest :Expr:=
      AND b1:bar AND b2:bar* AND b3:bar# AND b4:bar+ bar   <[ "and test ok" ]>

    Spacetest :Expr:=
      h:fnug    g:fnug fnug <[ "space test    ok" ]>

    Tabtest :Expr:=
      h:blob# TAB TABh:blob# Tab:TAB s:blob TABTAB  a:sTAB  <[ "tab test   ok" ]>

    Returntest : Expr:=
      h:rr# NEWLINE# RETURNh:rr Return:rr RETURNRETURN a:sRETURN <[ "return test    ok" ]>

    Newlinetest : Expr:=
      h:nl# NEWLINE#  NEWLINEh:nl Newline:nl NEWLINENEWLINE a:sNEWLINE <[ "newline test   ok" ]>

    SPACEtest : Expr:=
      h:sp# SPACE#  SPACEh:sp Space:sp SPACE SPACE SPACESPACE a:sSPACE <[ "SPACE test     ok" ]>

    AnyCharTest : Expr:=
      boz a:_ _# bar _? baz b:AnyCharTest2 <[ "Any char test " a b(a, bar) "   ok"]>

    AnyCharTest2(x:CharLiteralExpr, y:Expr) : Expr:=
      boz a:_ <[ "boz " a " " x " " y ]>

    AnyChar :CharLiteralExpr:= a:_ <[ a ]>

(*
    Formfeedtest : Expr:=
      FORMFEED h:ff FORMFEEDh:ff Form_feed:FORMFEED FORMFEEDFORMFEED a:sFORMFEED <[ "form feed test   ok" ]>

    Backspacetest : Expr:=
      BACKSPACE h:hello BACKSPACEh:hello Backspace:hello BACKSPACEBACKSPACE a:sBACKSPACE <[ "backspace test   ok" ]>

*)
  end


end
