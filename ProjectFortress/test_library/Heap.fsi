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

api Heap

(************************************************************
 * Mergeable, pure priority queues (heaps).                 *)

(** At the moment we're baking off several potential priority queue
  implementations, based on part on some advice from "Purely
  Functional Data Structures" by Okasaki.

  * Pairing Heaps, the current default:
    O(1) merge
    O(lg n) amortized extractMin, with O(n) worst case.  The worst
    case is proportional to the number of deferred merge operations
    performed with the min; if you merge n entries tree-fashion rather
    than 1 at a time you should obtain O(lg n) worst case performance
    as well.  The heap(gen) function will follow the merge structure
    of the underlying generator; for a well-written generator this
    will be sufficient to give good performance.

  * Lazy-esque pairing heaps (supposedly slower but actually easier in
  some ways to implement than pairing heaps, and avoiding a potential
  stack overflow in the latter).  These don't seem to be quite as
  whizzy in this setting as ordinary Pairing Heaps.
    O(lg n) merge
    O(lg n) worst-case extractMin

  * Splay heaps (noted as "fastest in practice", borne out by other
    experiments).  Problem: O(n) merge operation, vs O(lg n) for
    everything else.  If we build heaps by performing reductions over
    a generator, with each iteration generating a few elements, this
    will be a problem.  This is not yet implemented.

   Minimum complete implementation of Heap[\K,V\]:
     empty
     singleton
     isEmpty
     extractMin
     merge(Heap[\K,V\])
 **)
trait Heap[\K,V\] extends Generator[\(K,V)\]
    getter isEmpty(): Boolean
    (** Given an instance of Heap[\K,V\], get the empty Heap[\K,V\] **)
    getter empty(): Heap[\K,V\]
    (** Get the (key,value) pair with minimal associated key **)
    getter minimum(): (K,V) throws NotFound
    (** Given an instance of Heap[\K,V\], generate a singleton Heap[\K,V\] **)
    singleton(k:K, v:V): Heap[\K,V\]
    (** Return a heap that contains the key-value pairings in both of
        the heaps passed in. **)
    merge(h:Heap[\K,V\]): Heap[\K,V\]
    (** Return a heap that contains the additional key-value pairs **)
    insert(k:K, v:V): Heap[\K,V\]
    (** Extract the (key,value) pair with minimal associated key,
     along with a heap with that key and value pair removed. **)
    extractMin(): (K,V,Heap[\K,V\]) throws NotFound
end

object HeapMerge[\K,V\](boiler: Heap[\K,V\]) extends Reduction[\Heap[\K,V\]\]
end

trait Pairing[\K,V\] extends Heap[\K,V\]
        comprises { ... }
    dump(): String
end

emptyPairing[\K,V\](): Pairing[\K,V\]
singletonPairing[\K,V\](k:K, v:V): Pairing[\K,V\]

pairing[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

(************************************************************
 * Not actually lazy pairing heaps; these are actuallly more
 * eager in that they merge siblings incrementally on insertion.
 *)

trait LazyPairing[\K,V\] extends Heap[\K,V\]
        comprises { ... }
    dump(): String
end

emptyLazy[\K,V\](): LazyPairing[\K,V\]
singletonLazy[\K,V\](k:K, v:V): LazyPairing[\K,V\]

lazy[\K,V\](g:Generator[\(K,V)\]): Heap[\K,V\]

(************************************************************
 * And the winner is...
 * Non-lazy pairing heaps!
 ************************************************************)

emptyHeap[\K,V\](): Pairing[\K,V\]
singletonHeap[\K,V\](k:K, v:V): Pairing[\K,V\]
heap[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

end
