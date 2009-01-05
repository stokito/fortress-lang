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

api FlatString

  lineSeparator: String

  object FlatString extends { String }
    opr ||(self, b:FlatString): String
    opr ||(self, b:String):String
    opr ||(self, b:Char): String
    opr ||(a:FlatString, self): String
    flatConcat(self, b:FlatString):String
    flatConcat(self, b:Char):String
  end

end
