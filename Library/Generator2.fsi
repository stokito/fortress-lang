(*******************************************************************************
  Generator-of-Generators Library is copyrighted free software by Kento Emoto 
  <emoto[at]ipl.t.u-tokyo.ac.jp> developed under the collaborative 
  research on Fortress Programming Language between Sun Microsystems, 
  Inc. and the University of Tokyo.  
  
  You can redistribute it and/or modify it under the following 
  BSD-style license or the Sun Contributor Agreement that Kento Emoto signed.
  
  
  Copyright (c) 2008 by Kento Emoto
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

api Generator2

import List.{...}

(*---------------------------------- Misc  -------------------------*)

distributes[\R,L1,L2\](q : ActualReduction[\R,L1\], r : ActualReduction[\R,L2\]) : Boolean

(* checking relational predicates *)
relational[\E\](p : Generator[\E\] -> Condition[\()\]) : Boolean

(* checking commutativity *)
commutative[\R\](r : R) : Boolean 

(* Generators-of-Generators *)

(* Generator2 Inits *)
object Inits[\ E \](g : Generator[\ E \]) extends Generator2[\ E \]
  toString() : String
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R 
  generate2[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], f : E -> R) : R 

end
inits[\E\](g : Generator[\E\]) : Inits[\E\]

(* Generator2 Tails *)
object Tails[\ E \](g : Generator[\ E \]) extends Generator2[\ E \]
  toString() : String
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R 
  generate2[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], f : E -> R) : R 

end

tails[\E\](g : Generator[\E\]) : Tails[\E\]

(* Generator2 Segs *)
object Segs[\ E \](g : Generator[\ E \]) extends Generator2[\ E \]
  toString() : String
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R 
  generate2[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], f : E -> R) : R 

end

segs[\E\](g : Generator[\E\]) : Segs[\E\]

(* Generator2 Subs *)
object Subs[\ E \](g : Generator[\ E \]) extends Generator2[\ E \]
  toString() : String
  generate[\ R \](r : Reduction[\ R \], body : Generator[\ E \] -> R) : R 
  generate2[\ R, L1, L2 \](q : ActualReduction[\ R, L1 \], r : ActualReduction[\ R, L2 \], f : E -> R) : R 

end

subs[\E\](g : Generator[\E\]) : Subs[\E\]

end
