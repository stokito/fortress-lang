(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Regex

    import FortressAst.{...}
    import FortressSyntax.{Expression,Literal}
    import Set.{...}
    import List.{...}

    trait Element
    end

    object Regexp( elements : List[\Element\] )
        opr CONTAINS(self,s:String):Boolean
        opr IN(s:String,self):Boolean
        opr =(self,r:Regexp):Boolean
        (*
        opr =(self,r:Regexp):Boolean
        *)
    end

    object CharElement(s:String) extends Element
    end

    object RepeatElement(e:Element) extends Element
    end

    object RepeatNonGreedyElement(e:Element) extends Element
    end

    object MaybeElement(e:Element) extends Element
    end

    object RepeatOneElement(e:Element) extends Element
    end

    object RepeatOneNonGreedyElement(e:Element) extends Element
    end

    object RepeatExactlyElement(e:Element, n:ZZ32) extends Element
    end

    object RepeatMinElement(e:Element, n:ZZ32) extends Element
    end

    object RepeatMaxElement(e:Element, n:ZZ32) extends Element
    end

    object RepeatBetweenElement(e:Element, n1:ZZ32, n2:ZZ32) extends Element
    end

    object GroupElement(e:List[\Element\]) extends Element
    end

    object RangeElement(s1:String,s2:String) extends Element
    end

    object ClassElement(e:List[\Element\]) extends Element
    end

    object InverseClassElement(e:List[\Element\]) extends Element
    end

    object StartElement() extends Element
    end

    object EndElement() extends Element
    end

    object AnyElement() extends Element
    end

    object EscapedElement(e:String) extends Element
    end

    object AlternateElement() extends Element
    end

    grammar regex extends {Expression, Symbols, Literal}
        Expr |:= (* type: Content *)
           x:Regex => <[ x ]>

        Regex:Regexp :Expr:=
            s1:Slash# e:Element#* s2:Slash# => <[ Regexp(<|e**|>) ]>

        Element:Element :Expr:=
            i:Item# `*# `? => <[ RepeatNonGreedyElement(i) asif Element ]>
        |   i:Item# `+# `? => <[ RepeatOneNonGreedyElement(i) asif Element ]>
        |   i:Item# `* => <[ RepeatElement(i) asif Element ]>
        |   i:Item# `+ => <[ RepeatOneElement(i) asif Element ]>
        |   i:Item# `? => <[ MaybeElement(i) asif Element ]>
        (*
        |   i:Item# `{ n:IntLiteralExpr `} <[ RepeatExactlyElement(i,n) asif Element ]>
        |   i:Item# `{ n:IntLiteralExpr , `} <[ RepeateMinElement(i,n) asif Element ]>
        |   i:Item# `{ n1:IntLiteralExpr , n2:IntLiteralExpr `}
            <[ RepeatBetweenElement(i,n1,n2) asif Element ]>
        |   i:Item# `{ , n:IntLiteralExpr `}
            <[ RepeatMaxElement(i,n) asif Element ]>
        *)
        |   i:Item# `{ n:LiteralExpr `} => <[ RepeatExactlyElement(i,n) asif Element ]>
        |   i:Item# `{ n:LiteralExpr , `} => <[ RepeatMinElement(i,n) asif Element ]>
        |   i:Item# `{ n1:LiteralExpr , n2:LiteralExpr `}
            => <[ RepeatBetweenElement(i,n1,n2) asif Element ]>
        |   i:Item# `{ , n:LiteralExpr `}
            => <[ RepeatMaxElement(i,n) asif Element ]>
        |   `## `{# e:Expr `} => <[ CharElement(e "") asif Element ]>
        |   i:Item => <[ i ]>

        Item:Element :Expr:=
            ^ => <[ StartElement() asif Element ]>
        |   $ => <[ EndElement() asif Element ]>
        |   `| => <[ AlternateElement() asif Element ]>
        |   . => <[ AnyElement() asif Element ]>
        |   \# any:_ => <[ EscapedElement(any "") asif Element ]>
        |   c:CharacterClass => <[ c ]>
        |   (# e:Element#* ) => <[ GroupElement(<|e**|>) asif Element ]>
        |   s:AnyChar => <[ (CharElement(s) asif Element) ]>

        CharacterClass:Element :Expr:=
            `[# ^# r:RangeItem#* `] => <[ InverseClassElement(<|r**|>) asif Element ]>
        |   `[# r:RangeItem#* `] => <[ ClassElement(<|r**|>) asif Element ]>

        RangeItem:Element :Expr:=
            s1:AnyChar# - s2:AnyChar => <[ RangeElement(s1,s2) asif Element ]>
        |   s:AnyChar => <[ CharElement(s) asif Element ]>
    end

    grammar Symbols extends Expression
        Slash:String :StringLiteralExpr:=
            / => <[ "/" ]>

        AnyChar:String :Expr:=
            x:[A:Za:z0:9~!@%&] => <[ x ]>
        |   `: => <[ ":" ]>
        |   `# => <[ "#" ]>
        |   < => <[ "<" ]>
        |   > => <[ ">" ]>
        |   `{ => <[ "{" ]>
        |   `} => <[ "}" ]>
    end
end
