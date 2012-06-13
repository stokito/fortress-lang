(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api System

(*
   This provides lookup equivalent to the internal ProjectProperties lookup.
   Java properties, environment variables, and configuration files are consulted,
   and variable substitution is performed on the retrieved or default values.
 *)
getProperty(what:String, ifMissing:String):String

end

