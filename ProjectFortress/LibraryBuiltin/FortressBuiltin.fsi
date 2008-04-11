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
(* import NativeSimpleTypes.{Boolean} *)

trait Any end

trait  Number extends { Any }
    (* excludes { String, Boolean } *)
end

trait  Integral extends { Number }
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

trait ZZ32 extends { Integral }
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

trait ZZ64 extends { Integral }
    (* excludes { String, Boolean, RR64, FloatLiteral } *)
end

trait RR64 extends { Number }
    (* excludes { String, Boolean } *)
end

trait IntLiteral extends { ZZ32, ZZ64, RR64 } end

trait FloatLiteral extends { RR64 } end

end
