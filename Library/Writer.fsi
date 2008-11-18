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

 api Writer
import JavaString.JavaString
import Streams.{...}

stdOut: Writer
stdErr: Writer

object Writer(fileName: String) extends WriteStream
    getter asString(): String
    getter fileName(): String

    write(s: JavaString): ()
    write(c: Char): ()
    flush(): ()
    close(): ()
end

object BufferedWriter(under: Writer) extends WriteStream
    getter asString(): String

    write(s: String): ()
    write(s: JavaString): ()
    write(c: Char): ()
    flush(): ()
    close(): ()
end

end
