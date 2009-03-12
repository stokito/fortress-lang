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

api System

(**
Access to configuration information specific to a run of a Fortress program.

Examples include command-line arguments, environment variables,
registry parameters, and the like.
**)

(** args is a top-level variable containing any command-line
    arguments.  It is an arbitrary-sized 1-D array.  Unlike C's argv,
    args does *not* include the program name.  Programmers should use
    programName to access this information.
**)
args : Array[\String,ZZ32\]

(** programName is the name by which the Fortress program was invoked. **)
programName : String

end
