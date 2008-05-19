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

api Xml2

  import FortressAst.{...}
  import FortressSyntax.{Literal}
  import List.{...}

  trait Content
    toString():String
  end

  object Element(startTag:String, content:List[\Content\], endTag:String) extends Content 
    getter toString():String
  end 
  Element(startTag:String)
  Element(startTag:String, endTag:String)

  object CData(v:String) extends Content
    getter toString():String
  end

  grammar xml extends Literal
    LiteralExpr |Expr:= (* type: Content *)
      x:XExpr <[ x ]>

    XExpr :Expr:= (* type: Content *)
      b:XmlStart c:XmlContent e:XmlEnd
      <[ Element(b, c, e) asif Content ]>
    | b:XmlStart e:XmlEnd
      <[ Element(b,e) asif Content ]>
    | x:XmlComplete <[ Element(x) asif Content ]>

    XmlStart :Expr:=
      o1:OpenBracket# s:String o2:CloseBracket
      <[ s ]>
    | o1:OpenBracket# s:String a:Attributes o2:CloseBracket
      <[ s " " a ]>

    XmlContent :Expr:= (* type: List[\Content\] *)
      s:Strings <[ <| (CData(s) asif Content) |> ]>
    | x:XExprs+ <[ x ]>

    XExprs :Expr:=
      x:XExpr SPACE <[ x ]>

    XmlEnd :Expr:= (* type: String *)
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      <[ s ]>

    XmlComplete :Expr:=
      OpenBracket# s:String Slash# CloseBracket
      <[ s ]>
    | OpenBracket# s:String a:Attributes Slash# CloseBracket
      <[ s " " a ]>

    Attributes :Expr:=
      a:Attribute r:Attributes <[ a " " r ]>
    | a:Attribute <[ a ]>

    Attribute :Expr:=
      key:String = " val:Strings " <[ key "='" val "'" ]>

    Strings :Expr:= (* type: String *)
      s1:String s2:Strings <[ s1 " " s2 ]>
    | s1:String <[ s1 ]>

    Slash :StringLiteralExpr:=
      / <[ "/" ]>

    String :Expr:= (* type: String *)
      x:AnyChar# y:String <[ x y ]>
    | x:AnyChar <[ x "" ]>

    AnyChar :StringLiteralExpr:=
      x:[A:Za:z] <[ x ]>

    OpenBracket :Expr:=
      < <[ "<" ]>

    CloseBracket :Expr:=
      > <[ ">" ]>

  end
end
