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

api Regex

    (* import FortressAst.{...} *)
    import FortressSyntax.{Expression}
    import Set.{...}
    import List.{...}

    trait Element
        getter toString():String
    end

    object Regexp( elements : List[\Element\] )
    end

    object CharElement(s:String) extends Element
    end

    object RepeatElement(e:Element) extends Element
    end
    
    object RepeatOneElement(e:Element) extends Element
    end

    object GroupElement(e:List[\Element\]) extends Element
    end

    object RangeElement(s1:String,s2:String) extends Element
    end

    object ClassElement(e:List[\Element\]) extends Element
    end

    object StartElement() extends Element
    end
    
    object EndElement() extends Element
    end

    grammar regex extends {Expression, Symbols}
        Expr:Regexp |Expr:= (* type: Content *)
            x:Regex <[ x ]>

        Regex:Regexp :Expr:=
            s1:Slash# e:Element#* s2:Slash# <[ Regexp(e) ]>

        Element:Element :Expr:=
            i:Item# `* <[ RepeatElement(i) asif Element ]>
        |   i:Item# `+ <[ RepeatOneElement(i) asif Element ]>
        |   i:Item <[ i ]>

        Item:Element :Expr:=
            s:AnyChar <[ (CharElement(s) asif Element) ]>
        |   ^ <[ StartElement() asif Element ]>
        |   $ <[ EndElement() asif Element ]>
        |   `[# r:RangeItem#* `] <[ ClassElement(r) asif Element ]>
        |   (# e:Element#* ) <[ GroupElement(e) asif Element ]>

        RangeItem:Element :Expr:=
            s1:AnyChar# - s2:AnyChar <[ RangeElement(s1,s2) asif Element ]>
        |   s:AnyChar <[ CharElement(s) asif Element ]>
    end

    grammar Symbols
        Slash:String :StringLiteralExpr:=
            / <[ "/" ]>

        AnyChar:String :Expr:=
            x:[A:Za:z0:9] <[ x ]>
    end 
end
