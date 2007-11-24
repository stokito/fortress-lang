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

api SkipList

(* A SkipList type consists of a root node and pInverse = 1/p, where the fraction
   p is used in the negative binomial distribution to select random levels for insertion.
*)
object SkipList[\Key,Val,nat pInverse\](root:Node[\Key,Val\]) end

(* Construct an empty skip list *)
NewList[\Key,Val,nat pInverse\]():SkipList[\Key,Val,pInverse\]


(* A Node is the basic type for the Skip List data structure.
   There are four types of Nodes:
    i)   Empty Nodes - represents an empty tree.
    ii)  Leaf Nodes - stores a (key,val) pair. Lives at the bottom of the tree.
    iii) Internal Nodes - stores N keys and N+1 children for N > 0.
    iv)  White Nodes - stores exactly zero keys and one child.
*)
trait Node[\Key,Val\] comprises {EmptyNode[\Key,Val\], LeafNode[\Key,Val\],
  WhiteNode[\Key,Val\], InternalNode[\Key,Val\]}

  (* The height of the current node.  Height = 0 must be a leaf *)
  getter height():ZZ32

  (* The number of values stores in this subtree.  The number of 
     values is greater than or equal to the number of keys,
     as duplicates are allowed in this implementation. *)  
  getter size():ZZ32

  (* given a search key, try to return a value that matches that key *)
  search(k:Key):Maybe[\Val\]

  toString():String
  (* toGraphViz():String   wait until hashCode() is implemented*)

  (* the add method will grow the tree if level > root height *)
  add(leaf:LeafNode[\Key,Val\], level:ZZ32):Node[\Key,Val\]

  (* the add_helper method is only invoked if level <= root height *)
  (* returns the new node and whether a new key was inserted *)
  add_helper(leaf:LeafNode[\Key,Val\], level:ZZ32):(Node[\Key,Val\], Boolean)

  (* perform a search operation, and remove a value if it is found *)
  remove(k:Key):(Node[\Key,Val\],Maybe[\Val\])

  (* merge must always be invoked with at least one element in the merge list *)
  merge(nodes:List[\Node[\Key,Val\]\]):Node[\Key,Val\]
  
  (* return the list of leaves that are under the current subtree *)
  getLeaves():List[\LeafNode[\Key,Val\]\]

end

(* There are two types of white nodes:
    a) WhiteLevel1 are white nodes that live at level 1 of the tree.  Their children are leaves.
    b) WhiteLevelN are white nodes that live at level > 1 of the tree. Their children are non-leaves. *)
trait WhiteNode[\Key,Val\] extends Node[\Key,Val\] excludes InternalNode[\Key,Val\] comprises {WhiteLevel1[\Key,Val\], WhiteLevelN[\Key,Val\]} end


(* There are two types of internal nodes:
    a) InternalLevel1 are internal nodes that live at level 1 of the tree.  Their children are leaves.
    b) InternalLevelN are internal nodes that live at level > 1 of the tree. Their children are non-leaves. *)    
trait InternalNode[\Key,Val\] extends Node[\Key,Val\] excludes WhiteNode[\Key,Val\] comprises {InternalLevel1[\Key,Val\], InternalLevelN[\Key, Val\]} end

(* There are four types of NonLeafNodes and they are the union of the WhiteNode types and InternalNode types *)
trait NonLeafNode[\Key,Val\] extends Node[\Key,Val\] comprises {WhiteLevel1[\Key,Val\], WhiteLevelN[\Key,Val\], InternalLevel1[\Key,Val\], InternalLevelN[\Key, Val\]} end

(* WhiteNodeHelper is the trait that is used to distinguish between WhiteLevel1 and WhiteLevelN *)
trait WhiteNodeHelper[\Key,Val,ChildType extends Node[\Key,Val\]\] extends Node[\Key,Val\] 
  comprises {WhiteLevel1[\Key,Val\], WhiteLevelN[\Key,Val\]}

  getter child():ChildType
  
end

(* InternalNodeHelper is the trait that is used to distinguish between InternalLevel1 and InternalLevelN *)
trait InternalNodeHelper[\SelfType extends InternalNodeHelper[\SelfType,Key,Val,ChildType\],Key,Val,ChildType extends Node\]
  extends Node[\Key,Val\] comprises {InternalLevel1[\Key,Val\], InternalLevelN[\Key,Val\]}

  getter keys():Array[\Key,ZZ32\]
  getter children():Array[\ChildType,ZZ32\]

  (* given an instance of SelfType, generate a singleton SelfType *)
  singleton(keys':Array[\Key,ZZ32\], children':Array[\ChildType,ZZ32\]) : SelfType
  
  (* given a key k, return the largest offset with a value less than or equal to k *)
  find_index(k:Key):ZZ32

  (* break this internal node in half.  Returns the two halves along with the key used for breaking *)
  break():(NonLeafNode[\Key,Val\],NonLeafNode[\Key,Val\],Key)
  
  (* return a new internal node that has children[index] and keys[index - 1] missing *)
  index_remove(index:ZZ32): SelfType

end

(* Given a length > 0 and a node, generate that many white nodes that 
   connect "as a tail" to the node. *)
generate_tail[\Key,Val\](node:LeafNode[\Key,Val\], length:ZZ32):Node[\Key,Val\]
generate_tail[\Key,Val\](node:NonLeafNode[\Key,Val\], length:ZZ32):Node[\Key,Val\]

(* Represents an empty tree *)
object EmptyNode[\Key,Val\]() extends Node[\Key,Val\] end

(* A leaf node.  For a given unique key, stores an array of values *)
object LeafNode[\Key,Val\](key: Key, values: Array[\Val,ZZ32\]) extends Node[\Key,Val\] end

(* A white node with one leaf child *)
object WhiteLevel1[\Key,Val\](child:LeafNode[\Key,Val\]) extends {WhiteNode[\Key,Val\], 
  WhiteNodeHelper[\Key,Val,LeafNode[\Key,Val\]\], NonLeafNode[\Key,Val\]} end

(* A white node with one nonleaf child *)
object WhiteLevelN[\Key,Val\](child:NonLeafNode[\Key,Val\]) extends {WhiteNode[\Key,Val\], 
  WhiteNodeHelper[\Key,Val,NonLeafNode[\Key,Val\]\], NonLeafNode[\Key,Val\]} end

(* An internal node with k keys and k + 1 leaf children *)
object InternalLevel1[\Key,Val\](keys:Array[\Key,ZZ32\],
  children:Array[\LeafNode[\Key,Val\],ZZ32\]) extends {InternalNode[\Key,Val\], 
  InternalNodeHelper[\InternalLevel1[\Key,Val\],Key,Val,LeafNode[\Key,Val\]\], NonLeafNode[\Key,Val\]} end

(* An internal node with k keys and k + 1 nonleaf children *)
object InternalLevelN[\Key,Val\](keys:Array[\Key,ZZ32\],
  children:Array[\NonLeafNode[\Key,Val\],ZZ32\]) extends {InternalNode[\Key,Val\], 
  InternalNodeHelper[\InternalLevelN[\Key,Val\],Key,Val,NonLeafNode[\Key,Val\]\], NonLeafNode[\Key,Val\]} end

end
