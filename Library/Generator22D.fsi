(*******************************************************************************
  Generator-of-Generators Library is copyrighted free software by Kento Emoto
  <emoto[at]ipl.t.u-tokyo.ac.jp> developed under the collaborative
  research on Fortress Programming Language between Sun Microsystems,
  Inc. and the University of Tokyo.

  You can redistribute it and/or modify it under the following
  BSD-style license or the Sun Contributor Agreement that Kento Emoto signed.


  Copyright 2009 by Kento Emoto
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

      * Redistributions of source code must retain the above copyright
        notice, this list of conditions and the following disclaimer.
      * Redistributions in binary form must reproduce the above copyright
        notice, this list of conditions and the following disclaimer
        in the documentation and/or other materials provided with the
        distribution.
      * Neither the name of Kento Emoto nor the names of its
        contributors may be used to endorse or promote products derived
        from this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
  THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
  OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
  ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


 ******************************************************************************)

api Generator22D

import List.{...}
import Generator2.{...}

(* Generator2 Rows *)
object Rows[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) extends Generator2[\ E \]
  getter seed() : Generator[\ E \]
  toString() : String

  (* actual generation the nested data structure *)
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R
end

rows[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) : Rows[\E, b0, s0, b1, s1\]

(* Generator2 Cols *)
object Cols[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) extends Generator2[\ E \]
  getter seed() : Generator[\ E \]
  toString() : String

  (* actual generation the nested data structure *)
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R
end

cols[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) : Cols[\E, b0, s0, b1, s1\]

(* Generator2 Rects *)
object Rects[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) extends Generator2[\ E \]
  getter seed() : Generator[\ E \]
  toString() : String

  (* actual generation the nested data structure *)
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R
end

rects[\ E, nat b0, nat s0, nat b1, nat s1 \](g : Array2[\E, b0, s0, b1, s1\]) : Rects[\E, b0, s0, b1, s1\]

trait AbideWith[\T\] end

trait SomeMSS2DTuple[\T\] end

mssBody[\T\](v:T) : SomeMSS2DTuple[\T\]

object MSS2DReductionAbove[\T\](op, ot) extends { AssociativeReduction[\SomeMSS2DTuple[\T\]\], AbideWith[\MSS2DReductionBeside[\T\]\] }
  getter asString(): String
  simpleJoin(x: SomeMSS2DTuple[\T\], y: SomeMSS2DTuple[\T\]): SomeMSS2DTuple[\T\]
end

object MSS2DReductionBeside[\T\](op, ot) extends { AssociativeReduction[\SomeMSS2DTuple[\T\]\], AbideWith[\MSS2DReductionAbove[\T\]\] }
    getter asString(): String
    simpleJoin(x: SomeMSS2DTuple[\T\], y: SomeMSS2DTuple[\T\]): SomeMSS2DTuple[\T\]
end

end
