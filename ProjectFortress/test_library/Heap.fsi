(*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

api Heap

(************************************************************
 * Mergeable, pure priority queues (heaps).                 *)

(** At the moment we're baking off several potential priority queue
  implementations, based on part on some advice from "Purely
  Functional Data Structures" by Okasaki.

  * Pairing Heaps.

  * Lazy-esque pairing heaps (supposedly slower but actually easier in
  some ways to implement than pairing heaps, and avoiding a potential
  stack overflow in the latter).

  * Splay heaps (noted as "fastest in practice", borne out by other
    experiments).  Problem: O(n) merge operation, vs O(lg n) for
    everything else.  If we build heaps by performing reductions over
    a generator, with each iteration generating a few elements, this
    will be a problem.

   Minimum complete implementation of Heap[\K,V\]:
     empty
     singleton
     isEmpty
     extractMin
     merge(H)
 **)
trait Heap[\H extends Heap[\H,K,V\],K,V\] extends Generator[\(K,V)\]
    getter isEmpty(): Boolean
    (** Given an instance of H, get the empty H **)
    getter empty(): H
    (** Get the (key,value) pair with minimal associated key **)
    getter minimum(): (K,V) throws QueueEmpty
    (** Given an instance of H, generate a singleton H **)
    singleton[\K,V\](k:K, v:V): H
    (** Return a heap that contains the key-value pairings in both of
        the heaps passed in. **)
    merge(h:H): H
    (** Return a heap that contains the additional key-value pairs **)
    insert(k:K, v:V): H
    (** Extract the (key,value) pair with minimal associated key,
     along with a heap with that key and value pair removed. **)
    extractMin(): (K,V,H) throws QueueEmpty
end

object QueueEmpty extends CheckedException
end

object HeapMerge[\H extends Heap[\K,V\],K,V\](boiler: H) extends Reduction[\H\]
end

hm[\H extends Heap[\K,V\],K,V\](boiler: H)

trait Pairing[\K,V\] extends Heap[\Pairing[\K,V\],K,V\]
        comprises { EmptyP[\K,V\], NodeP[\K,V\] }
    dump(): String
end

object EmptyP[\K,V\]() extends Pairing[\K,V\]
end

object NodeP[\K,V\](k:K, v:V, sibs: Pairing[\K,V\], kids: Pairing[\K,V\])
        extends Pairing[\K,V\]
end

emptyPairing[\K,V\](): EmptyP[\K,V\]
singletonPairing[\K,V\](k:K, v:V): NodeP[\K,V\]

pairing[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

(************************************************************
 * Not actually lazy pairing heaps; these are actuallly more
 * eager in that they merge siblings incrementally on insertion.
 *)

trait LazyPairing[\K,V\] extends Heap[\LazyPairing[\K,V\],K,V\]
        comprises { EmptyLP[\K,V\], NodeLP[\K,V\] }
    dump(): String
end

object EmptyLP[\K,V\]() extends LazyPairing[\K,V\]
end

object NodeLP[\K,V\](k:K, v:V, pending: LazyPairing[\K,V\], kids: LazyPairing[\K,V\])
        extends LazyPairing[\K,V\]
end

emptyLazy[\K,V\](): EmptyLP[\K,V\]
singletonLazy[\K,V\](k:K, v:V): LazyPairing[\K,V\]

lazy[\K,V\](g:Generator[\(K,V)\]): LazyPairing[\K,V\]

(************************************************************
 * And the winner is...
 * Non-lazy pairing heaps!
 ************************************************************)

emptyHeap[\K,V\](): EmptyL[\K,V\]
singletonHeap[\K,V\](k:K, v:V): NodeL[\K,V\]
heap[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

end
