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

api TypeProxy

(** Reflection of static type parameters for overloading purposes.
    Works around shortcomings in the story on parametric overloading
    (all overloadings must have the same parameters with the same
    bounds).  Allows us to overload a function based on the parametric
    type of an output. *)

object __Proxy[\T extends (* U *) Any\]
    (* extends { __Proxy[\U\], Object } where { U extends Any } *)
    getter toString(): String
end

end