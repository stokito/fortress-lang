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

api FortressBuiltin

value object Float extends RR64
end

object FloatLiteral extends RR64
end

value object ZZ32 extends ZZ64
    opr =(self, b:ZZ32):Boolean
    opr <(self, b:ZZ32):Boolean

    opr -(self):ZZ32
    opr +(self,b:ZZ32):ZZ32
    opr -(self,b:ZZ32):ZZ32
    opr DOT(self,b:ZZ32):ZZ32
    opr juxtaposition(self,b:ZZ32):ZZ32
    opr DIV(self,b:ZZ32):ZZ32
    opr REM(self,b:ZZ32):ZZ32
    opr MOD(self,b:ZZ32):ZZ32
    opr GCD(self,b:ZZ32):ZZ32
    opr LCM(self,b:ZZ32):ZZ32
    opr CHOOSE(self,b:ZZ32):ZZ32
    opr BITAND(self,b:ZZ32):ZZ32
    opr BITOR(self,b:ZZ32):ZZ32
    opr BITXOR(self,b:ZZ32):ZZ32
    opr LSHIFT(self,b:Integral):ZZ32
    opr RSHIFT(self,b:Integral):ZZ32
    opr BITNOT(self):ZZ32
    opr ^(self, b:Integral):Number
    widen(self):ZZ64
    partitionL(self):ZZ32
end

value object Long extends ZZ64
end

object IntLiteral extends Integral
    opr =(self, b: IntLiteral):Boolean
    opr <(self, other:String): Boolean
    opr <=(self, other:String): Boolean
    opr >(self, other:String): Boolean
    opr >=(self, other:String): Boolean
    opr CMP(self, other:String): TotalComparison

    opr -(self): IntLiteral
    opr +(self, b: IntLiteral): IntLiteral
    opr -(self, b: IntLiteral): IntLiteral
    opr DOT(self, b: IntLiteral): IntLiteral
    opr juxtaposition(self, b: IntLiteral): IntLiteral
    opr DIV(self, b: IntLiteral): IntLiteral
    opr REM(self, b: IntLiteral): IntLiteral
    opr MOD(self, b: IntLiteral): IntLiteral
    opr GCD(self, b: IntLiteral): IntLiteral
    opr LCM(self, b: IntLiteral): IntLiteral
    opr CHOOSE(self, b: IntLiteral): IntLiteral
    opr BITAND(self, b: IntLiteral): IntLiteral
    opr BITOR(self, b: IntLiteral): IntLiteral
    opr BITXOR(self, b: IntLiteral): IntLiteral
    opr LSHIFT(self, b:Integral): IntLiteral
    opr RSHIFT(self, b:Integral): IntLiteral
    opr BITNOT(self): IntLiteral
    opr ^(self, b:Integral):Number
end

object Boolean
    extends { Condition[\()\], StandardTotalOrder[\Boolean\] }
end

object Char extends { StandardTotalOrder[\Char\] }
    (** %char.ord% converts %char% to the equivalent integer code point.
        It is always the case that %c = char(c.ord())% for %c : Char%. **)
    getter ord(): ZZ32

    (** |c| means the same as %c.chr()%; it's unclear if this is
        actually a good idea, and we solicit feedback on the subject. **)
    opr |self| : ZZ32

    (** Ordering resepects %ord%. **)
    opr =(self, other:Char): Boolean
    opr <(self, other:Char): Boolean
end

object String extends { StandardTotalOrder[\String\] }
    getter size() : ZZ32
    getter toString() : String
    getter indices() : FullRange[\ZZ32\]
    getter generator() : Generator[\Char\]
    opr |self| : ZZ32
    opr =(self, other:String): Boolean
    opr <(self, other:String): Boolean
    opr <=(self, other:String): Boolean
    opr >(self, other:String): Boolean
    opr >=(self, other:String): Boolean
    opr CMP(self, other:String): TotalComparison
    opr CASE_INSENSITIVE_CMP(self, other:String): TotalComparison
    opr [i:ZZ32]: Char
    (** As a convenience, we permit LowerRange indexing to go 1 past the bounds
        of the string, returning the empty string, in order to permit some convenient
        string-trimming idioms. **)
    opr[r0:Range[\ZZ32\]] : String


    (** The operator %||% with at least one String argument converts to string and
        appends **)
    opr ||(self, b:String):String
    opr ||(self, b:Number):String
    opr ||(self, c:Char):String
    opr ||(self, b:()):String
    opr ||(self, b:(Any,Any)):String
    opr ||(self, b:(Any,Any,Any)):String
    opr ||(a:Any, self):String
    opr ||(self, b:Any):String

    (** The operator %|||% with at least one String argument converts to string,
        then appends with a whitespace separator unless one of the two arguments is
        empty.  If there is an empty argument, the other argument is returned. **)
    opr |||(self, b:String): String
    opr |||(self, b:Any): String
    opr |||(a:Any, self): String

    (** Right now for backward compatibility juxtaposition works like %||% **)
    opr juxtaposition(a:Any, self):String
    opr juxtaposition(self, b:String):String
    opr juxtaposition(self, b:Any):String

    (** opr // appends with a single newline separator. **)
    opr //(self) : String
    opr //(self, a:String): String
    opr //(self, a:Any): String
    opr //(a:Any, self): String

    (** opr /// appends with a double newline separator **)
    opr ///(self) : String
    opr ///(self, a:String): String
    opr ///(self, a:Any): String
    opr ///(a:Any, self): String
end

object Thread[\T\](fcn:()->T)
    getter val():T
    getter ready():Boolean
    wait():()
    stop():()
end

abort():()

end
