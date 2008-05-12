api Xml

  import FortressAst.{...}
  import FortressSyntax.{Literal}

  grammar xml extends Literal
    LiteralExpr |Expr:=
      xml x:XExpr <[ "Xml string " x ]>
    (*
      xml o1:OpenBracket# o2:OpenBracket
        do StringLiteralExpr( o1.val ) end
    | xml o:OpenBracket# s:String# c:CloseBracket
      <[ "Xml string: " s ]>
    | xml s1:String# s2:String
            do StringLiteralExpr( s1.val "" s2.val ) end
    | xml s:String <[ s ]>
    *)

    XExpr :StringLiteralExpr:=
      b:XmlStart c:XmlContent e:XmlEnd
      <[ c ]>
    | x:XmlComplete <[ x ]>

    (*
    XExpr :StringLiteralExpr:=
      b:XmlStart content:XmlContent e:XmlEnd
      <[ content ]>
      (*
      <[ b "" content "" e ]>
      *)
      *)
      
    XmlComplete :StringLiteralExpr:=
      OpenBracket# s:String Slash# CloseBracket
      <[ s ]>
    | OpenBracket# s:String Attributes Slash# CloseBracket
      <[ s ]>

    Attributes :StringLiteralExpr:=
      Attribute Attributes <[ "" ]>
    | Attribute <[ "" ]>

    Attribute :StringLiteralExpr:=
      key:String = " val:Strings " <[ key ]>

    XmlStart :StringLiteralExpr:=
      o1:OpenBracket# s:String o2:CloseBracket
      <[ s ]>
    | o1:OpenBracket# s:String Attributes o2:CloseBracket
      <[ s ]>
      (*
      <[ "--" s "++" ]>
      *)

    XmlContent :StringLiteralExpr:=
      s:Strings <[ s ]>
    | x:XExpr do StringLiteralExpr( "xml: " x.val ) end

    Strings :StringLiteralExpr:=
      s1:String s2:Strings <[ s1 ]>
    | s1:String <[ s1 ]>

    XmlEnd :StringLiteralExpr:=
      o1:OpenBracket# Slash# s:String# o2:CloseBracket
      <[ s ]>
      (*
      <[ "++" s "--" ]>
      *)

    Slash :StringLiteralExpr:=
      / <[ "/" ]>

(*
    String2 :StringLiteralExpr:=
      x:BigA* <[ x ]>

    BigA :StringLiteralExpr:=
      a <[ "a" ]>
      *)
    
    (*
    String :StringLiteralExpr:=
      x:AnyChar# rest:StringRest <[ x ]>
    | x:AnyChar# <[ x ]>
    *)

    String :StringLiteralExpr:=
      x:AnyChar# String <[ x ]>
    | x:AnyChar <[ x ]>

(*
    StringRest :StringLiteralExpr:=
      NOT _  <[ "" ]>
    | x:AnyChar# rest:StringRest <[ x ]>
    *)

    StringRest :StringLiteralExpr:=
      l <[ "" ]>
    | x:AnyChar# rest:StringRest <[ x ]>

    AnyChar :StringLiteralExpr:=
      x:a <[ x ]>
    | x:b <[ x ]>
    | x:c <[ x ]>
    | x:d <[ x ]>
    | x:e <[ x ]>
    | x:f <[ x ]>
    | x:g <[ x ]>
    | x:h <[ x ]>
    | x:i <[ x ]>
    | x:k <[ x ]>
    | x:l <[ x ]>
    | x:m <[ x ]>
    | x:n <[ x ]>
    | x:o <[ x ]>
    | x:p <[ x ]>
    | x:q <[ x ]>
    | x:r <[ x ]>
    | x:s <[ x ]>
    | x:t <[ x ]>
    | x:u <[ x ]>
    | x:v <[ x ]>
    | x:w <[ x ]>
    | x:x <[ x ]>
    | x:y <[ x ]>
    | x:z <[ x ]>

    OpenBracket :StringLiteralExpr:=
      [<] <[ "<" ]>

    CloseBracket :StringLiteralExpr:=
      [>] <[ ">" ]>

  end
end
