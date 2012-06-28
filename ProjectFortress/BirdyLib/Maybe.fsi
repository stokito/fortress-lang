(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Maybe

  object NothingInHere extends CheckedException end

  trait Maybe[\T extends Any\] comprises { Just[\T\] , Nothing[\T\] } 
    isSome(): Boolean
    isSome(self): Boolean
    extract() : T throws NothingInHere
    abstract extract(self): T throws NothingInHere
  end
  
  object Just[\T extends Any\](content: T) extends Maybe[\T\] end
  object Nothing[\T extends Any\] extends Maybe[\T\] end

  __cond[\E extends Any,R extends Any\](c:Maybe[\E\], t:E->R, e:()->R): R 
  __cond[\E extends Any\](c:Maybe[\E\], t:E->()): ()

end