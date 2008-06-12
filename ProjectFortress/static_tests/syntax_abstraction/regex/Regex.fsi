api Regex

    (* import FortressAst.{...} *)
    import FortressSyntax.{Expression}
    import Set.{...}
    import List.{...}

    trait Element
    end
    
    object Regexp( elements : List[\Element\] )
    end

    object CharElement(s:String) extends Element
    end

    object RepeatElement(e:Element) extends Element
    end

    object GroupElement(e:Element) extends Element
    end

    object RangeElement(s1:String,s2:String) extends Element
    end

    grammar regex extends {Expression, Symbols}
        Expr:Regexp |Expr:= (* type: Content *)
            x:Regex <[ x ]>

        Regex:Regexp :Expr:=
            s1:Slash# e:Element* s2:Slash# <[ Regexp(e) ]>

        Element:Element :Expr:=
            i:Item `* <[ RepeatElement(i) ]>
        |   i:Item <[ i ]>
        |   ( e:Element ) <[ GroupElement(e) ]>

        Item:Element :Expr:=
            s:AnyChar <[ (CharElement(s) asif Element) ]>
        |   s1:AnyChar# -# s2:AnyChar <[ RangeElement(s1,s2) ]> 
    end

    grammar Symbols 
        Slash:String :StringLiteralExpr:=
            / <[ "/" ]>
    
        AnyChar:String :Expr:=
            x:[A:Za:z] <[ "a" ]>
    end
end
