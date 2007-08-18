(* Casting *)
cast[\T extends Any\](x: Any): T

(* Instanceof Testing *)
instanceOf[\T extends Any\](x: Any): Boolean

(* Ignoring Values *)
ignore(x: Any): ()

(* Enforcing Tuples *)
tuple[\T extends Tuple\](x: T): T

(* Identity *)
identity[\T extends Any\](x: T): T

(* Coercion *)
coerce_[\T\](x: T): T

(* Optional Values *)
trait Maybe[\T\] comprises { Nothing, Just[\T\] }
  isNothing: Boolean
end
object Nothing extends Maybe[\T\] excludes Just[\T\] where {T extends Object}
end
object Just[\T\](just: T) extends Maybe[\T\]
end
