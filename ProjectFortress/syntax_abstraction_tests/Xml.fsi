(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(* Xml grammar for writing inline xml. *)

api Xml

  import FortressAst.{...}
  import FortressSyntax.{Literal}
  import List.{...}

  trait Content
    getter hasElements():Boolean
    toString():String
  end

  object Element(info:Header, content_:List[\Content\], endTag:String) extends Content
    getter tag():String
    getter hasElements():Boolean
    getter children():List[\Element\]
    getter content():CData
    getter attributes():List[\Attribute\]
    getter toXml():String
  end
  Element(info:Header): Element
  Element(info:Header, endTag:String): Element

  object Attribute(key:String, val:String) extends Content
    getter getKey():String
    getter getValue():String
  end

  object Header(startTag:String, attributes:List[\Attribute\])
  end

  object CData(v:String) extends Content
  end

  grammar xml extends {Expression, Symbols}
    Expr |:= (* type: Content *)
      x:XExpr => <[ x ]>

    XExpr:Element :Expr:= (* type: Content *)
      b:XmlStart c:XmlContent e:XmlEnd
      => <[ Element(b, c, e) asif Content ]>
    | b:XmlStart e:XmlEnd
      => <[ Element(b,e) asif Content ]>
    | x:XmlComplete => <[ Element(x) asif Content ]>

    XmlComplete:Header :Expr:=
      OpenBracket# s:String Slash# CloseBracket
      => <[ Header(s,emptyList[\Attribute\]()) ]>
    | OpenBracket# s:String {a:Attribute SPACE}+ Slash# CloseBracket
      => <[ Header(s, <|a**|>) ]>

    XmlStart:Header :Expr:=
      o1:OpenBracket# s:String o2:CloseBracket
      => <[ Header(s,emptyList[\Attribute\]()) ]>
    | o1:OpenBracket# s:String {a:Attribute SPACE}+ o2:CloseBracket
      => <[ Header(s, <|a**|>) ]>

    XmlContent:List[\Content\] :Expr:= (* type: List[\Content\] *)
      s:Strings => <[ <| (CData(s) asif Content) |> ]>
    | c:CData+ => <[ <| CData(BIG ||| <| a.toString() | a <- <|c**|> |>) asif Content |> ]>
    | {x:XExpr SPACE}+ => <[ <|x**|> ]>

    CData:Element :Expr:=
        <# !# `[# CDATA# `[# n:Strings `]# `]# > => <[ CData(n) asif Content ]>

    (*
      e:Expr <[ <! CData("" e) asif Content !> ]>
      *)
      (*
    | x:XExprs+ <[ x ]>
    *)

(*
    XExprs:Element :Expr:=
      x:XExpr SPACE <[ x ]>
      *)

    XmlEnd:String :Expr:= (* type: String *)
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      => <[ s ]>

    (*
    Attributes :Expr:=
      a:Attribute r:Attributes <[ a " " r ]>
    | a:Attribute <[ a ]>
    *)
    (*
    Attributes:Attribute :Expr:=
      a:Attribute SPACE <[ a ]>
      *)

    Attribute:Attribute :Expr:=
      key:String = " val:AttributeStrings " => <[ Attribute(key,val) ]>

    AttributeStrings:String :Expr:= (* type: String *)
      s1:AttributeString s2:AttributeStrings => <[ s1 " " s2 ]>
    | s1:AttributeString => <[ s1 ]>

    AttributeString:String :Expr:= (* type: String *)
      x:AttributeChar# y:AttributeString => <[ x y ]>
    | x:AttributeChar => <[ x "" ]>

    AttributeChar:String :StringLiteralExpr:=
      x:AnyChar => <[ x ]>
    | x:['] => <[ x ]> (* ' *)
    | x:Slash => <[ x ]>

    Strings:String :Expr:= (* type: String *)
      s1:String s2:Strings => <[ s1 " " s2 ]>
    | s1:String => <[ s1 ]>

    String:String :Expr:= (* type: String *)
      x:AnyChar# y:String => <[ x y ]>
    | x:AnyChar => <[ x "" ]>

  end

  grammar Symbols extends Expression

    AnyChar:String :StringLiteralExpr:=
      x:[A:Za:z0:9] => <[ "" x ]>

    OpenBracket:String :Expr:=
      < => <[ "<" ]>

    CloseBracket:String :Expr:=
      > => <[ ">" ]>

    Slash:String :StringLiteralExpr:=
      / => <[ "/" ]>
  end
end
