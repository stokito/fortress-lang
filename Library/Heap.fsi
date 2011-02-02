(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Heap

(************************************************************
 * Mergeable, pure priority queues (heaps).

** At the moment, we are baking off several potential priority queue
  implementations, based on part on some advice from ``Purely
  Functional Data Structures'' by Okasaki \cite{functionalDS}.
\begin{itemize}
  \item Pairing heaps, the current default:
    %O(1)% merge; %O(lg n)% amortized %extractMinimum%, with %O(n)% worst case.  The worst
    case is proportional to the number of deferred merge operations
    performed with the min; if you merge %n% entries tree-fashion rather
    than one at a time you should obtain %O(lg n)% worst case performance
    as well.  The %heap(gen)% function will follow the merge structure
    of the underlying generator; for a well-written generator this
    will be sufficient to give good performance.

  \item Lazy-esque pairing heaps (supposedly slower but actually easier in
  some ways to implement than pairing heaps, and avoiding a potential
  stack overflow in the latter).  These do not seem to be quite as
  whizzy in this setting as ordinary Pairing heaps: %O(lg n)% merge,
    %O(lg n)% worst-case %extractMinimum%.

  \item Splay heaps (noted as ``fastest in practice'', borne out by other
    experiments).  Problem: %O(n)% merge operation, vs %O(lg n)% for
    everything else.  If we build heaps by performing reductions over
    a generator, with each iteration generating a few elements, this
    will be a problem.  This is not yet implemented.
\end{itemize}
   Minimum complete implementation of %Heap[\K,V\]%:
   %  empty
   %  singleton
   %  isEmpty
   %  extractMinimum
   %  merge(Heap[\K,V\])
 **)
trait Heap[\K,V\] extends Generator[\(K,V)\]
    getter isEmpty(): Boolean
    (** Given an instance of %Heap[\K,V\]%, get the empty %Heap[\K,V\]%. **)
    getter empty(): Heap[\K,V\]
    (** Get the (key,value) pair with minimal associated key. **)
    getter minimum(): Maybe[\(K,V)\]
    (** Given an instance of %Heap[\K,V\]%, generate a singleton %Heap[\K,V\]%. **)
    singleton(k:K, v:V): Heap[\K,V\]
    (** Return a heap that contains the key-value pairings in both of
        the heaps passed in. **)
    merge(h:Heap[\K,V\]): Heap[\K,V\]
    (** Return a heap that contains the additional key-value pairs. **)
    insert(k:K, v:V): Heap[\K,V\]
    (** Extract the (key,value) pair with minimal associated key,
     along with a heap with that key and value pair removed. **)
    extractMinimum(): Maybe[\(K,V,Heap[\K,V\])\]
    (** Delete the minimum (key,value) pair (if any) from the heap,
        and return the resulting heap. *)
    deleteMinimum(): Heap[\K,V\]
end

object HeapMerge[\K,V\](boiler: Heap[\K,V\])
        extends CommutativeMonoidReduction[\Heap[\K,V\]\]
end

trait Pairing[\K,V\] extends Heap[\K,V\]
        comprises { ... }
end

emptyPairing[\K,V\](): Pairing[\K,V\]
singletonPairing[\K,V\](k:K, v:V): Pairing[\K,V\]

pairing[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

(************************************************************
 * Not actually lazy pairing heaps; these are actually more
 * eager in that they merge siblings incrementally on insertion.
 *)
trait LazyPairing[\K,V\] extends Heap[\K,V\]
        comprises { ... }
end

emptyLazy[\K,V\](): LazyPairing[\K,V\]
singletonLazy[\K,V\](k:K, v:V): LazyPairing[\K,V\]

lazy[\K,V\](g:Generator[\(K,V)\]): Heap[\K,V\]

(************************************************************
 * Use these default factories unless you are experimenting.
 * Right now, they yield non-lazy pairing heaps.
 ************************************************************)
emptyHeap[\K,V\](): Pairing[\K,V\]
singletonHeap[\K,V\](k:K, v:V): Pairing[\K,V\]
heap[\K,V\](g:Generator[\(K,V)\]): Pairing[\K,V\]

end
