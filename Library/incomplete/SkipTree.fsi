(*******************************************************************************
    Copyright 2008, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api SkipTree
import List.{...}

trait SkipTree[\Key,Value\]

    getter count():ZZ32

    opr |self| : ZZ32
    
    (** Takes a querykey as input and returns either a Just[\Value\]
      * object if the (key,value) pair lives in this map, or
      * returns Nothing[\Value\] otherwise.
      *)
    find(querykey : Key) : Maybe[\Value\]

    (** Takes a newkey and newvalue as input and returns a 
      * new skip tree with the (key, value) pair inserted.
      * Currently, if newkey already exists in the tree,
      * then this operation does not insert (key, newvalue)
      * into the tree.
      *)
    insert(newkey : Key, newvalue : Value) : SkipTree[\Key,Value\]

    (**
     *  Return a random level >= 1 with a negative binomial distribution.
     *)
    randomLevel():ZZ32

    (**
     * The verify method will throw an error
     * if this structure does not satisfy the properties
     * of a Skip Tree.
     **)    
    verify():()

end (* trait SkipTree[\Key,Value\] *)

object Node[\Key,Value\](keys     : List[\Key\], values   : List[\Value\],
                         children : List[\Maybe[\Node[\Key,Value\]\]\],
                         height   : ZZ32) end
                       

LeafNode[\Key,Value\](key : Key, val : Value, height : ZZ32)

object EmptySkipTree[\Key,Value\] extends { SkipTree[\Key,Value\] } end
object NonEmptySkipTree[\Key,Value\](root : Node[\Key,Value\]) extends { SkipTree[\Key,Value\] } end


(**
  *  Implementation of java.util.Collections.binarySearch
  *  JavaDoc from http://java.sun.com/javase/6/docs/api/java/util/Collections.html
  *
  *  Searches the specified list for the specified object using
  *  the binary search algorithm. The list must be sorted into
  *  ascending order according to the natural ordering of its 
  *  elements prior to making this call. If it is not sorted, the
  *  results are undefined. If the list contains multiple elements
  *  equal to the specified object, there is no guarantee which one will be found.
  *
  *  Parameters:
  *      list - the list to be searched.
  *      key - the key to be searched for. 
  *  Returns:
  *      the index of the search key, if it is contained in the list;
  *      otherwise, (-(insertion point) - 1). The insertion point is defined
  *      as the point at which the key would be inserted into the list: 
  *      the index of the first element greater than the key, or list.size()
  *      if all elements in the list are less than the specified key. 
  *      Note that this guarantees that the return value will be >= 0
  *      if and only if the key is found. 
  **)
binarySearch[\T\](list : List[\T\], key : T) : ZZ32


end (* api SkipTree *)
