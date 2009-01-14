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

api CompilerBuiltin

(** The %builtinPrimitive% function is actually recognized as a special piece
    of built-in magic by the Fortress interpreter.  The %javaClass%
    argument names a Java Class which is a subclass of
    \texttt{com.sun.fortress.interpreter.glue.NativeApp}, which provides code
    for the closure which is used in place of the call to
    %builtinPrimitive%.  Meanwhile all the necessary type information,
    argument names, etc. must be declared here in Fortress-land.  For
    examples, see the end of this file.

    In practice, if you are extending the interpreter you will probably
    want to extend \texttt{com.sun.fortress.interpreter.glue.NativeFn0,1,2,3,4}
    or one of their subclasses defined in
    \texttt{com.sun.fortress.interpreter.glue.primitive}.  These types are
    generally easier to work with, and the boilerplate packing and
    unpacking of values is done for you.
**)
builtinPrimitive[\T\](javaClass:String):T

trait Object extends Any
end Object

trait String
end 

object FlatString extends String
end FlatString

end
