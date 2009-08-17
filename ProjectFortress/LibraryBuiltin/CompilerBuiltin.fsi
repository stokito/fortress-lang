(*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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

api CompilerBuiltin
import AnyType.{Any}

trait Object extends Any
end Object

trait String
    asString(): String
opr || (self, b:String):String
    opr juxtaposition(self, b:String): String
end

object FlatString extends String
end FlatString

println(s:String):()
printlnZZ32(x:ZZ32):()

strToInt(s:String):ZZ32

trait Number excludes { String }
end

trait ZZ64 extends Number
end

trait ZZ32 extends ZZ64 comprises { IntLiteral }
    asString(): String
    opr |self| : ZZ32
    opr -(self): ZZ32
    opr +(self, other:ZZ32): ZZ32
    opr -(self, other:ZZ32): ZZ32
    opr <(self, other:ZZ32) : Boolean
    opr <=(self, other:ZZ32): Boolean
    opr >(self, other:ZZ32): Boolean
    opr >=(self, other:ZZ32): Boolean
    opr =(self, other:ZZ32): Boolean
    opr juxtaposition(self, other:ZZ32): ZZ32
    opr DOT(self, other:ZZ32): ZZ32
    opr DIV(self, other:ZZ32): ZZ32
end

object IntLiteral extends ZZ32
end

trait RR64 extends Number
    asString(): String
    opr |self| : RR64
    opr -(self): RR64
    opr +(self, other:RR64): RR64
    opr -(self, other:RR64): RR64
    opr <(self, other:RR64): Boolean
    opr <=(self, other:RR64): Boolean
    opr >(self, other:RR64): Boolean
    opr >=(self, other:RR64): Boolean
    opr =(self, other:RR64): Boolean
    opr juxtaposition(self, other:RR64): RR64
    opr DOT(self, other:RR64): RR64
    opr /(self, other:RR64): RR64
end

trait RR32 extends RR64
end

object FloatLiteral extends RR32
end


trait Boolean
end

end
