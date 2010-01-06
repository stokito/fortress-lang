(*******************************************************************************
    Copyright 2010 Sun Microsystems, Inc.,
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

nanoTime(): RR64

trait String
(*)    coerce(n: ZZ32) 
(*)    coerce(n: ZZ64) 
    getter isEmpty(): Boolean
    getter asString(): String
    opr <(self, b:String): Boolean
    opr =(self, b: String): Boolean
    opr |self| : ZZ32
    opr || (self, b:String):String
    opr juxtaposition(self, b:String): String
end

object FlatString extends String
end FlatString

println(s:String):()
println(x:ZZ32):()
println(x:ZZ64):()
(* println(x:RR32):() *)
println(x:RR64):()

strToInt(s:String):ZZ32

trait Number excludes { String }
    abstract getter asString(): String
end

trait ZZ64 extends Number excludes RR64
    coerce(x: IntLiteral)
    coerce(x: ZZ32) 
    getter asZZ32(): ZZ32 
    getter asString(): String 
    opr |self| : ZZ64
    opr -(self): ZZ64
    opr +(self, other:ZZ64): ZZ64
    opr -(self, other:ZZ64): ZZ64 
    opr <(self, other:ZZ64): Boolean 
    opr <=(self, other:ZZ64): Boolean 
    opr >(self, other:ZZ64): Boolean 
    opr >=(self, other:ZZ64): Boolean 
    opr =(self, other:ZZ64): Boolean 
    opr juxtaposition(self, other:ZZ64): ZZ64
    opr DOT(self, other:ZZ64): ZZ64 
    opr DIV(self, other:ZZ64): ZZ64
end

trait ZZ32 extends Number excludes { ZZ64, RR64 }
    coerce(x: IntLiteral)
    getter asZZ32(): ZZ32
    getter asString(): String
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

trait IntLiteral excludes {ZZ32, ZZ64}
    abstract getter asZZ32(): ZZ32
    abstract getter asZZ64(): ZZ64
(*
    abstract getter asNN32(): NN32
    abstract getter asZZ(): ZZ
    abstract getter asRR32(): RR32
*)
    abstract getter asRR64(): RR64
end

trait RR64 extends Number
    getter asString(): String
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
    opr ^(self, other:RR64): RR64
    opr ^(self, other:ZZ32): RR64
end

trait RR32 extends Number 
end

object FloatLiteral extends RR64
end


trait Boolean
end

true : Boolean
false : Boolean

(************************************************************
* Random numbers
************************************************************)

random(i:RR64): RR64
randomZZ32(x:ZZ32): ZZ32

end
