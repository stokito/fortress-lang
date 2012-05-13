(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api File

import FlatString.FlatString
import FileSupport.{...}
import Util.{...}

FileReadStream(filename: String): FileReadStream

object FileReadStream(filename:FlatString) extends { FileStream, ReadStream }
    getter fileName():String
    getter eof():Boolean
    getter ready():Boolean
    close():()
    readLine():String
    readCharacter():Character
    read(k:ZZ32):String
    read():String
    lines(n:ZZ32):Generator[\String\]
    lines():Generator[\String\]
    characters(n:ZZ32):Generator[\Character\]
    characters():Generator[\String\]
    chunks(n:ZZ32,m:ZZ32):Generator[\String\]
    chunks(n:ZZ32): Generator[\String\]
    chunks(): Generator[\String\]
end



end
