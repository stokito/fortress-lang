(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Maybe

  object NothingInHere extends CheckedException end

  trait Maybe[\T\] comprises { Just[\T\] , Nothing[\T\] } 
    isSome(): Boolean
    isSome(self): Boolean
    extract() : T throws NothingInHere
    extract(self): T throws NothingInHere
  end
  
  object Just[\T\](content: T) extends Maybe[\T\] end
  object Nothing[\T\] extends Maybe[\T\] end

  __cond[\E,R\](c:Maybe[\E\], t:E->R, e:()->R): R 
  __cond[\E\](c:Maybe[\E\], t:E->()): ()

end