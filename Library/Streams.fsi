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

api Streams
import JavaString.JavaString

(***********************************************************
 * Types to support  input and output
 ***********************************************************)

trait Closeable
    close():()
end

trait FileStream extends Closeable
    getter fileName():String
end

trait Consumable
    consume():()
    whenUnconsumed():()
end

trait WriteStream extends { Closeable }

    (** %write(JavaString)% and %write(Char)% are the primitive mechanisms for writing
        characters to the end of a %FileWriteStream%. **)
    write(x:JavaString):()
    write(c:Char):()
    (** %write(Any)% converts its argument to a String using %toString%
        and appends the result to the stream. **)
    write(s:String):()
    write(x:Any):()

    (** %writes% converts each of the generated elements to a string
        using %asString% unless the element is already a %String% in
        which case it is left alone.  The resulting strings are
        appended together to the end of the stream.  To avoid
        interleaving with concurrent output to the same stream, this
        append step is done monolithically. **)
    writes(x:Generator[\Any\]):()

    (** %flush% any output to the stream that is still buffered. **)
    flush():()

    (** %close% the stream. **)
    close():()

end WriteStream

end Streams
