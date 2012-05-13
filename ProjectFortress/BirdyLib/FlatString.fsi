(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api FlatString

  lineSeparator: String

  object FlatString extends { String }
    opr ||(self, b:FlatString): String
    opr ||(self, b:String):String
    opr ||(self, b:Character): String
    opr ||(a:FlatString, self): String
    flatConcat(self, b:FlatString):String
    flatConcat(self, b:Character):String
  end

end
