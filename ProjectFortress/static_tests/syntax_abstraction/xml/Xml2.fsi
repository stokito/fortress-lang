
(* Xml grammar for writing inline xml. *)

api Xml2

  import FortressAst.{...}
  import FortressSyntax.{Literal}

  object XmlNode
    getter toString()
  end

  grammar xml extends Literal
    LiteralExpr |Expr:=
      xml x:XExpr <[ XmlNode() ]>

    XExpr :Expr:=
      b:XmlStart c:XmlContent e:XmlEnd
      <[ "<" b ">" c "</" e ">" ]>
    | b:XmlStart e:XmlEnd
      <[ "<" b "></" e ">" ]>
    | x:XmlComplete <[ "<" x "/>" ]>

    XExprs :Expr:=
      x:XExpr y:XExprs <[ x y ]>
    | x:XExpr <[ x ]>

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

    (* There must be a simpler way to do this *)
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
    | x:j <[ x ]>
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
    | x:A <[ x ]>
    | x:B <[ x ]>
    | x:C <[ x ]>
    | x:D <[ x ]>
    | x:E <[ x ]>
    | x:F <[ x ]>
    | x:G <[ x ]>
    | x:H <[ x ]>
    | x:I <[ x ]>
    | x:J <[ x ]>
    | x:K <[ x ]>
    | x:L <[ x ]>
    | x:M <[ x ]>
    | x:N <[ x ]>
    | x:O <[ x ]>
    | x:P <[ x ]>
    | x:Q <[ x ]>
    | x:R <[ x ]>
    | x:S <[ x ]>
    | x:T <[ x ]>
    | x:U <[ x ]>
    | x:V <[ x ]>
    | x:W <[ x ]>
    | x:X <[ x ]>
    | x:Y <[ x ]>
    | x:Z <[ x ]>


    (* Shouldn't need [] around < and > *)
    OpenBracket :Expr:=
      [<] <[ "<" ]>

    CloseBracket :Expr:=
      [>] <[ ">" ]>

  end
end
