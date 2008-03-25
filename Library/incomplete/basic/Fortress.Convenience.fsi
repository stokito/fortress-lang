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
