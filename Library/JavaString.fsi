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
