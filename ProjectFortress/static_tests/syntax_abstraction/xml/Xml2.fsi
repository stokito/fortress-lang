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

  object Element(info:Header, content:List[\Content\], endTag:String) extends Content 
    getter toString():String
  end 
  Element(startTag:String)
  Element(startTag:String, endTag:String)

  object Attribute(key:String, val:String) extends Content
    getter getKey():String
    getter getValue():String
  end
  
  object Header(startTag:String, attributes:List[\Attribute\])
    getter getTag():String
    getter attributes():String
  end

  object CData(v:String) extends Content
    getter toString():String
  end

  grammar xml extends {Literal, Symbols}
    LiteralExpr |Expr:= (* type: Content *)
      x:XExpr <[ x ]>

    XExpr :Expr:= (* type: Content *)
      b:XmlStart c:XmlContent e:XmlEnd
      <[ Element(b, c, e) asif Content ]>
    | b:XmlStart e:XmlEnd
      <[ Element(b,e) asif Content ]>
    | x:XmlComplete <[ Element(x) asif Content ]>

    XmlComplete :Expr:=
      OpenBracket# s:String Slash# CloseBracket
      <[ Header(s,emptyList[\Attribute\]) ]>
    | OpenBracket# s:String a:Attributes+ Slash# CloseBracket
      <[ Header(s,a) ]>

    XmlStart :Expr:=
      o1:OpenBracket# s:String o2:CloseBracket
      <[ Header(s,emptyList[\Attribute\]) ]>
    | o1:OpenBracket# s:String a:Attributes+ o2:CloseBracket
      <[ Header(s, a) ]>

    XmlContent :Expr:= (* type: List[\Content\] *)
      s:Strings <[ <| (CData(s) asif Content) |> ]>
    | x:XExprs+ <[ x ]>

    XExprs :Expr:=
      x:XExpr SPACE <[ x ]>

    XmlEnd :Expr:= (* type: String *)
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      <[ s ]>

    (*
    Attributes :Expr:=
      a:Attribute r:Attributes <[ a " " r ]>
    | a:Attribute <[ a ]>
    *)
    Attributes :Expr:=
      a:Attribute SPACE <[ a ]>

    Attribute :Expr:=
      key:String = " val:AttributeStrings " <[ Attribute(key,val) ]>

    AttributeStrings :Expr:= (* type: String *)
      s1:AttributeString s2:AttributeStrings <[ s1 " " s2 ]>
    | s1:AttributeString <[ s1 ]>

    AttributeString :Expr:= (* type: String *)
      x:AttributeChar# y:String <[ x y ]>
    | x:AttributeChar <[ x "" ]>

    AttributeChar :StringLiteralExpr:=
      x:AnyChar <[ x ]>
    | x:['] <[ x ]>

    Strings :Expr:= (* type: String *)
      s1:String s2:Strings <[ s1 " " s2 ]>
    | s1:String <[ s1 ]>

    String :Expr:= (* type: String *)
      x:AnyChar# y:String <[ x y ]>
    | x:AnyChar <[ x "" ]>

  end

  grammar Symbols 

    AnyChar :StringLiteralExpr:=
      x:[A:Za:z] <[ x ]>

    OpenBracket :Expr:=
      < <[ "<" ]>

    CloseBracket :Expr:=
      > <[ ">" ]>

    Slash :StringLiteralExpr:=
      / <[ "/" ]>
  end
end
