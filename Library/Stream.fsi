(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Stream
import FlatString.FlatString

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

    (** %write(FlatString)% and %write(Char)% are the primitive mechanisms
        for writing characters to the end of a %WriteStream%.
        write(FlatString) need not be part of this api, since it is covered by
        write(String) **)

    write(c:Char):()
    write(s:String):()

    (** %write(Any)% converts its argument to a String using %toString%
        and appends the result to the stream. **)
    write(x:Any):()

    (** %writes% converts each of the generated elements to a string
        using %asString% unless the element is already a %String% in
        which case it is left alone.  The resulting strings are
        appended together to the end of the stream.  To avoid
        interleaving with concurrent output to the same stream, this
        append step is done monolithically. **)
    writes(x:Generator[\Any\]):()

    (** %print% and %println% convert each of their arguments to a
        string using %asString% unless the element is already a
        %String% in which case it is left alone.  The resulting
        strings are appended together monolithically to the end of the
        stream.  The %println% method also appends a newline.  Both
        then flush the stream. *)
    print(args:Any...): ()
    println(args:Any...): ()

    (** %flush% any output to the stream that is still buffered. **)
    flush():()

    (** %close% the stream. **)
    close():()

end WriteStream

end Stream
