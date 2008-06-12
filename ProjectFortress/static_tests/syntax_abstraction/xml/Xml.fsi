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

(* Xml grammar for writing inline xml. *)

api Xml

  import FortressAst.{...}
  import FortressSyntax.{Literal}


  trait Content
    toString():String
  end

  object Element(startTag:Expr, content:Expr, endTag:Expr) extends Content 
  end 

  grammar xml extends Literal
    LiteralExpr |Expr:=
      xml x:XExpr <[ x ]>

    XExpr :Expr:=
      b:XmlStart c:XmlContent e:XmlEnd
      <[ Element(b, c, e) ]>
    | b:XmlStart e:XmlEnd
    <[ Element(b, "a" "a", e) ]>
    | x:XmlComplete <[ Element(x, "a" "a", x) ]>

    XExprs :Expr:=
      x:XExpr y:XExprs <[ x y ]>
    | x:XExpr <[ x ]>

    XmlComplete :Expr:=
      OpenBracket# s:String Slash# CloseBracket
      <[ s ]>
    | OpenBracket# s:String a:Attributes Slash# CloseBracket
      <[ s " " a ]>
    | OpenBracket# s:String a:Attributes Slash# CloseBracket
      <[ s " " a ]>

    Attributes :Expr:=
      a:Attribute r:Attributes <[ a " " r ]>
    | a:Attribute <[ a ]>

    Attribute :Expr:=
      key:String = " val:Strings " <[ key "='" val "'" ]>

    XmlStart :Expr:=
      o1:OpenBracket# s:String o2:CloseBracket
      <[ s ]>
    | o1:OpenBracket# s:String a:Attributes o2:CloseBracket
      <[ s " " a ]>

    XmlContent :Expr:=
      s:Strings <[ s ]>
    | x:XExprs <[ x ]>

    Strings :Expr:=
      s1:String s2:Strings <[ s1 " " s2 ]>
    | s1:String <[ s1 ]>

    XmlEnd :Expr:=
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      <[ s ]>

    Slash :StringLiteralExpr:=
      / <[ "/" ]>

    String :Expr:=
      x:AnyChar# y:String <[ x y ]>
    | x:AnyChar <[ x "" ]>

    AnyChar :StringLiteralExpr:=
      x:[a:zA:Z] <[ x ]>

    OpenBracket :Expr:=
      < <[ "<" ]>

    CloseBracket :Expr:=
      > <[ ">" ]>

  end
end
