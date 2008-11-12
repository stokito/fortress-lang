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

api Timing

(** A Timing represents the duration of a computation. **)
object Timing(desc:String, duration: ZZ64)
    getter durationString(): String
    getter secs(): RR64
    getter msecs(): RR64
    getter usecs(): RR64
    getter nsecs(): ZZ64
end

opr PRINTTIME[\R\](desc:String, thunk:()->R): R

opr TIMING[\R\](desc:String, thunk:()->R): (R, Timing)

end
