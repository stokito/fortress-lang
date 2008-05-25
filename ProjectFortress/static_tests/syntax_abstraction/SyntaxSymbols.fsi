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

api SyntaxSymbols

  import FortressAst.{...}
  import FortressSyntax.{...}

  grammar A extends Literal
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
      hello ( world }   <[ "funny thing test   ok" ]>

    Operatortest :Expr:=
      a:hello?  gnu abe* c:world# d:hello+ <[ "operator test    ok" ]>

    Hashtest :Expr:=
      a:foo# SPACE# b:hello?# c:hello# d:hello*# world+# NOT foo# NOT hello# NOT hello+# AND bar# f:bar <[ "hash test   ok" ]>

    Nottest :Expr:=
      NOT hello NOT hello+ NOT hello# NOT hello+ NOT bar oof  <[ "not test ok" ]>

    Andtest :Expr:=
      AND bar AND bar* AND bar# AND bar+ bar   <[ "and test ok" ]>

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
      boz a:_ _# bar _? baz <[ "Any char test " a "   ok"]>
(*
    Formfeedtest : Expr:=
      FORMFEED h:ff FORMFEEDh:ff Form_feed:FORMFEED FORMFEEDFORMFEED a:sFORMFEED <[ "form feed test   ok" ]>

    Backspacetest : Expr:=
      BACKSPACE h:hello BACKSPACEh:hello Backspace:hello BACKSPACEBACKSPACE a:sBACKSPACE <[ "backspace test   ok" ]>

*)
  end


end
