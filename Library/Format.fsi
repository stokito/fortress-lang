api Format

padLeft(s:String,c:Char,z:ZZ32):String
padRight(s:String,c:Char,z:ZZ32):String
radix(base:ZZ64,val:ZZ64):String
scientific(number:RR64):String
(* scientific(number:ZZ32):String *)
digitSeparator(z:String,nth:ZZ32,separator:String):String
digitSeparator(num:String,nth:ZZ32,separator:String,middle:Char):String
format(string:String,args:Any...)

end
