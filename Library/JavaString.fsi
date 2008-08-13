api JavaString
  object JavaString
        extends { String }
    getter size() : ZZ32 
    getter toString() : String 
    opr |self| : ZZ32 
    opr =(self, other:String): Boolean 
    opr <(self, other:String): Boolean 
    opr <=(self, other:String): Boolean 
    opr >(self, other:String): Boolean 
    opr >=(self, other:String): Boolean
    opr CMP(self, other:String): TotalComparison
    opr CASE_INSENSITIVE_CMP(self, other:String): TotalComparison

    (** get skips bounds checking. **)
    get(i:ZZ32): Char 
    cmp(other:String): ZZ32
    cicmp(other:String): ZZ32
    substr(lo:ZZ32,hi:ZZ32): String

    (** The operator %||% with at least one String argument converts to string and
        appends **)
    opr ||(self, b:String):String 
    opr ||(self, b:Char): String 

  end
end
