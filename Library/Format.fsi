(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Format

object FormatException(reason:String) extends UncheckedException
    getter asString(): String
    getter asExprString(): String
end

padLeft(s:String,c:Char,z:ZZ32):String
padRight(s:String,c:Char,z:ZZ32):String
radix(base:ZZ64,val:ZZ64):String
scientific(number:RR64):String
(* scientific(number:ZZ32):String *)
digitSeparator(z:String,nth:ZZ32,separator:String):String
digitSeparator(num:String,nth:ZZ32,separator:String,middle:Char):String
format(string:String,args:Any...): String

end
