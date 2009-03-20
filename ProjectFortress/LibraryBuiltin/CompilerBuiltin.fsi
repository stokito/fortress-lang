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

trait Object extends Any
end Object

trait String
(* opr || (self, b:String): String  *)
end

object FlatString extends String
end FlatString

println(s:String):()

trait Number
end

object ZZ32 extends Number
end

object ZZ64 extends Number
end

object RR32 extends Number
end

object RR64 extends Number
end

end
