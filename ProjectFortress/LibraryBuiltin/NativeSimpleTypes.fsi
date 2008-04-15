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

api NativeSimpleTypes

object Boolean
    extends { SequentialGenerator[\()\], StandardTotalOrder[\Boolean\] }
end

object Char extends { StandardTotalOrder[\Char\] }
    (** %char.ord% converts %char% to the equivalent integer code point.
        It is always the case that %c = char(c.ord())% for %c : Char%. **)
    getter ord(): ZZ32

    (** %|c|% means the same as %c.chr()%; it's unclear if this is
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
    opr [i:ZZ32]: Char
    (** As a convenience, we permit LowerRange indexing to go 1 past the bounds
        of the string, returning the empty string, in order to permit some convenient
        string-trimming idioms. **)
    opr[r0:Range[\ZZ32\]] : String
end

object Thread[\T\](fcn:()->T)
    getter val():T
    getter ready():Boolean
    wait():()
    stop():()
end

abort():()

end
