(*******************************************************************************
    Copyright 2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api FileSupport

(***********************************************************
 * Types to support file input and output
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

trait ReadStream extends { Closeable, Consumable }
    (** eof returns true if an end-of-file condition has been
        encountered on the stream.
     **)
    getter eof():Boolean

    (** ready returns true if there is currently input from the stream
        available to be consumed.
     **)
    getter ready():Boolean

    (** Returns the next available line from the stream, discarding
        line termination characters.  Returns "" on eof.
     **)
    readLine():String

    (** Returns the next available character from the stream, or %char(0)% on eof.
     **)
    readChar():Char

    (** Returns the next k characters from the stream.  It will block
        until at least one character is available, and will then
        return as many characters as are ready.  Will return "" on end
        of file.  If k<=0 or absent a default value is chosen.
     **)
    read(k:ZZ32):String

    read():String

    uncheckedReadLine():String

    uncheckedReadChar():ZZ32

    uncheckedRead(k:ZZ32):String

    (** All file generators yield file contents in parallel by
        default, with a natural ordering corresponding to the order
        data occurs in the underlying file.  The file is closed if all
        its contents have been read.

        These generators pull in multiple chunks of data (where a
        "chunk" is a line, a character, or a block) before it is
        processed.  The maximum number of chunks to pull in before
        processing is given by the last optional argument in every
        case; if it is <=0 or absent a default value is chosen.

        It is possible to read from a ReadStream before invoking any
        of the following methods.  Previously-read data is ignored,
        and only the remaining data is provided by the generator.

        At the moment it is illegal to read from a ReadStream once any
        of these methods has been called; we do not check for this
        condition.  However, the sequential versions of these
        generators may use label/exit or throw in order to escape from
        a loop, and the ReadStream will remain open and ready for
        reading.

        Once the ReadStream has been completely consumed it is closed.
     **)


    (** lines yields the lines found in the file a la readLine().
     **)
    lines(n:ZZ32):Generator[\String\]
    lines():Generator[\String\]

    (** %characters% yields the characters found in the file a la %readChar()%.
     **)
    characters(n:ZZ32):Generator[\Char\]
    characters():Generator[\String\]

    (** chunks returns chunks of characters found in the file, in the
        sense of read().  The first argument is equivalent to the
        argument k to read, the second (if present) is the number of
        chunks at a time.
     **)
    chunks(n:ZZ32,m:ZZ32):Generator[\String\]
    chunks(n:ZZ32): Generator[\String\]
    chunks(): Generator[\String\]
end

object FileGenerator[\S\](reader:ReadStream, upper: ZZ32,
                          readOne:ReadStream -> S)
        extends { Consumable, Generator[\S\] }
end

end
