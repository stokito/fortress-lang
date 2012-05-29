(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
args : ImmutableArray[\String,ZZ32\]

(** programName is the name by which the Fortress program was invoked. **)
programName : String

(** A way to get environment information from inside of fortress **)
getEnvironment(name:String, defaultValue:String):String

(** A way to get fortress-style property information (that can be
    overridden by environment variable settings; uses the same code as
    property settings for the fortress implementation itself). **)
getProperty(name:String, defaultValue:String):String

(** Turn string into properly-terminated directory name
    (on Unix/Windows, add a trailing / if one is missing and string is nonempty) **)
toDirectoryName(s: String): String

end
