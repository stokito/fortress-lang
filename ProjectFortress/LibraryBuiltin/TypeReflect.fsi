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

api TypeReflect

(** Reify a type parameter as a parametrically-typed object.
 *  Compute joins of reified type parameters, allowing
 *  us to simulate covariance.  Permit comparison with the usual partial
 *  ordering on types.
 *)
trait ReflectedType extends StandardPartialOrder[\ReflectedType\]
  (* comprises { Reflect[\T\] } where [\T\] *)
  getter toString(): String
  opr CMP(self, other:ReflectedType): Comparison
  opr =(self, other:ReflectedType): Boolean
end

value object Reflect[\T\]() extends ReflectedType
  getter toString(): String
  opr =(self, other:Reflect[\T\]): Boolean
end

joinReflected(ty:ReflectedType, tys:ReflectedType...):()

reflectType[\T\](_:T):Reflect[\T\]

end
