(*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api CompilerSystem

(** Right now this is a massive hack, to make the native wrapper
    use array-like *notation* without actually supporting native-side
    arrays per se.  This should be remedied as soon as is practical. **)
object args
    getter size(): ZZ32
    opr |self|: ZZ32
    opr [n:ZZ32]: String
end

end
