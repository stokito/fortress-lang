(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

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
