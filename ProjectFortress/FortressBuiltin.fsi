(*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

trait Any extends { Any } end

object Boolean extends { Any } end

object String extends { Any } 
    (* excludes { IntLiteral, FloatLiteral, Boolean } *)
end

object Char extends { Any }
end

trait  Number extends { Any } 
    (* excludes { String, Boolean } *)
end

trait  Integral extends { Number } 
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

trait BufferedReader extends { Any } end
trait BufferedWriter extends { Any } end

object ZZ32 extends { Integral } 
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

object ZZ64 extends { Integral } 
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

object RR64 extends { Number } 
    (* excludes { String, Boolean } *)
end

object IntLiteral extends { ZZ32, ZZ64, RR64 } end

object FloatLiteral extends { RR64 } end

true: Boolean
false: Boolean

end