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

object FileReadStream(filename: JavaString) extends ReadStream 
    getter fileName():String
    getter eof():Boolean
    getter ready():Boolean
    close():()
    readLine():String
    readCharacter():Character
    read(k:ZZ32):String
    read():String
end



end
