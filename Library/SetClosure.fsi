(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SetClosure
import Set.{...}

(* closures of sets over generating operations *)

closure[\T\](s: Set[\T\], f: T->Set[\T\]): Set[\T\]
closure2[\T\](s: Set[\T\], f: (T,T)->Set[\T\]): Set[\T\]

(* closure predicates *)

isClosed[\T\](s: Set[\T\], f: T->Set[\T\]): Boolean
isClosed2[\T\](s: Set[\T\], f: (T,T)->Set[\T\]): Boolean

end
