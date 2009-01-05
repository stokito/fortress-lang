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

api Reader
import FlatString.FlatString
import Stream.{...}
stdIn: Reader

object Reader(fileName: String) extends ReadStream
    getter asString(): String
    getter fileName(): String
    getter eof(): Boolean
    getter isReady(): Boolean

    whenUnconsumed(): ()
    consume(): ()
    uncheckedReadLine(): String
    uncheckedReadChar(): ZZ32
    uncheckedRead(k: ZZ32): String
    close(): ()
end

end