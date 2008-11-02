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

api Format

padLeft(s:String,c:Char,z:ZZ32):String
padRight(s:String,c:Char,z:ZZ32):String
radix(base:ZZ64,val:ZZ64):String
scientific(number:RR64):String
(* scientific(number:ZZ32):String *)
digitSeparator(z:String,nth:ZZ32,separator:String):String
digitSeparator(num:String,nth:ZZ32,separator:String,middle:Char):String
format(string:String,args:Any...): String

end
