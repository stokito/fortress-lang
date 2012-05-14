(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Array

  import Util.{...}

  trait Array[\T\] extends Generator[\T\] 
    init(i: ZZ32, v: T): ()
    opr [x: GeneratorZZ32]: Array[\T\] (*) Should be Range but need to change opr # in CompilerLibrary first
    loop(f:T->()): () 
  end
  
  array[\T\](s: ZZ32): Array[\T\] 

end