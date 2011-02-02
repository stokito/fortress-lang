(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

(**
  \chapter{Convenience Functions and Types}
  \chaplabel{lib:convenience}

  \section{Convenience Functions}
  \seclabel{convenience-functions}
 **)

                                              (** \Method { **)
(* Casting *)
cast[\T extends Any\](x: Any): T
                                              (** } \seclabel{convenience-cast} **)

(**
  The function \VAR{cast} converts the type of its argument to a given type.
  If the static type of the argument is not a subtype of the given type, a
  \TYP{CastException} is thrown.
 **)

                                              (** \Method { **)
(* Instanceof Testing *)
instanceOf[\T extends Any\](x: Any): Boolean
                                              (** } \seclabel{convenience-instanceOf} **)

(**
  The function \VAR{instanceOf} tests whether its argument has a given type
  and returns a boolean value.
 **)

                                              (** \Method { **)
(* Ignoring Values *)
ignore(x: Any): ()
                                              (** } \seclabel{convenience-ignore} **)

(**
  The function \VAR{ignore} discards the value of its argument and
  returns \EXP{()}.
 **)

                                              (** \Method { **)
(* Identity *)
identity[\T extends Any\](x: T): T
                                              (** } \seclabel{convenience-identity} **)

(**
The function \VAR{identity} returns its argument.
 **)
