(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api List

import Util.{...}
import Maybe.{...}
import GeneratorLibrary.{DefaultGeneratorImplementation}

object OutOfBounds extends UncheckedException end

trait List[\E extends Any\] extends DefaultGeneratorImplementation[\E\]

  abstract getter isEmpty():Boolean    
  abstract extractLeft(): Maybe[\(E,List[\E\])\]
  abstract extractRight(): Maybe[\(List[\E\],E)\]
  opr ||(self, other:List[\E\]): List[\E\]
  abstract getValWithDefault(x: E):E
  abstract getLeftChild():List[\E\]
  abstract getRightChild():List[\E\]
  abstract generate[\R extends Any\](r: Reduction[\R\], body: E->R): R
  abstract addLeft(e:E):List[\E\]
  abstract addRight(e:E):List[\E\]
  split(n:ZZ32): (List[\E\], List[\E\])
  split(): (List[\E\], List[\E\])
  abstract left():Maybe[\E\]
  abstract right():Maybe[\E\]
  abstract opr | self | : ZZ32
  abstract concat(t2:List[\E\]):List[\E\]
  abstract splitIndex(x:ZZ32):(List[\E\],List[\E\])
  abstract concat3(v:E, t2:List[\E\]):List[\E\]
  opr [i: ZZ32, def: E]: E  
  
  (*)zip[\F\](other: List[\F\]): Generator[\(E,F)\]
  (*)filter(p: E -> Boolean): List[\E\]
  (*)getter reverse(): List[\E\]
  (*)concatMap[\G\](f: E->List[\G\]): List[\G\]
  (*)take(n:ZZ32): List[\E\]
  (*)drop(n:ZZ32): List[\E\]

end

emptyList[\E\](): List[\E\]
singleton[\E\](x:E): List[\E\]
opr <|[\T\]|>: List[\T\]

opr <|[\T\] x1: T |>: List[\T\]
opr <|[\T\] x1: T, x2: T |>: List[\T\]

opr BIG <|[\T\]|> : Comprehension[\T,List[\T\],List[\T\],List[\T\]\]

(*)opr BIG ||[\T\]() : BigReduction[\List[\T\],List[\T\]\]

(*

(** Vararg factory for lists; provides aggregate list constants: *)
opr <|[\E\] xs: E... |>: List[\E\]
(** List comprehensions: *)
opr BIG <|[\T\]|>:Comprehension[\T,List[\T\],List[\T\],List[\T\]\]
opr BIG <|[\T\] g:Generator[\T\]|>:List[\T\]

opr BIG CONCAT[\T\](): BigReduction[\List[\T\],List[\T\]\]
opr BIG CONCAT[\T\](g: Generator[\List[\T\]\]):List[\T\]

(** Convert generator into list (simpler type than comprehension above): *)
list[\E\](g:Generator[\E\]):List[\E\]

(** Flatten a list of lists *)
concat[\E\](x:List[\List[\E\]\]):List[\E\]



(** %emptyList[\E\](n)% allocates an empty list that can accept %n%
    %addRight% operations without reallocating the underlying storage. **)
emptyList[\E\](n:ZZ32): List[\E\]

singleton[\E\](e:E): List[\E\]

(** A reduction object for concatenating lists. *)
object Concat[\E\] extends MonoidReduction[\ List[\E\] \]
  empty(): List[\E\]
  join(a:List[\E\], b:List[\E\]): List[\E\]
end

*)

(*)transpose[\E,F\](xs: List[\(E,F)\]): (List[\E\], List[\F\])


end
