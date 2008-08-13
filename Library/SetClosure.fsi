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

api SetClosure
import Set.{...}

(* closures of sets over generating operations *)

closure[\T\](s: Set[\T\], f: T->Set[\T\]): Set[\T\]
closure2[\T\](s: Set[\T\], f: (T,T)->Set[\T\]): Set[\T\]

(* closure predicates *)

isClosed[\T\](s: Set[\T\], f: T->Set[\T\]): Boolean
isClosed2[\T\](s: Set[\T\], f: (T,T)->Set[\T\]): Boolean

end
