(*******************************************************************************
    Copyright 2012, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Map

import Comparison.{...}
import Maybe.{...}
import Set.{Set}
import Util.{...}

object KeyOverlap[\Key extends Object, Val extends { Object, Equality[\Val\] }\](key: Key, val1: Val, val2: Val)
    extends UncheckedException
    getter asString(): String
end

(** Note that the map interface is purely functional; methods return a
    fresh map rather than updating the receiving map in place.
    Methods that operate on a particular key leave the rest of the map
    untouched unless otherwise specified. **)
trait Map[\Key extends { StandardTotalOrder[\Key\], Object }, Val extends { Object, Equality[\Val\] }\]
    extends SequentialGenerator[\(Key,Val)\]
    
    abstract getter isEmpty(): Boolean
    abstract getter asDebugString(): String

    abstract seqgen[\R\](r: Reduction[\R\], body: (Key,Val)->R): R
    abstract generate[\R\](r: Reduction[\R\], body: (Key,Val)->R): R 
    abstract ivgen[\R\](i0: ZZ32, r: Reduction[\R\], body: (ZZ32,(Key,Val))->R): R
    abstract dom(self): Set[\Key\]
    abstract opr | self |: ZZ32
    abstract getPair(): (Key, Val) throws NotFound
    abstract getKey(): Key throws NotFound
    abstract getVal(): Val throws NotFound
    abstract getLeftChild(): Map[\Key, Val\]
    abstract getRightChild(): Map[\Key, Val\]
    opr[k:Key]: Val throws NotFound
    mem(k: Key): Map[\Key, Val\]
    member(x: Key): Maybe[\Val\]
    member(x: Key, v: Val): Val
    abstract minimum(): Maybe[\(Key, Val)\]
    deleteMinimum(): Map[\Key, Val\]
    abstract extractMinimum(): Maybe[\(Key, Val, Map[\Key, Val\])\]
    abstract maximum(): Maybe[\(Key, Val)\]
    deleteMaximum(): Map[\Key, Val\]
    abstract extractMaximum(): Maybe[\(Key, Val, Map[\Key, Val\])\]
    abstract add(k: Key, v: Val): Map[\Key, Val\]
    abstract update(k: Key, v: Val): Map[\Key, Val\]
    abstract delete(k: Key): Map[\Key, Val\]
    abstract updateWith(f: Maybe[\Val\]->Maybe[\Val\], k: Key): Map[\Key, Val\]
    abstract opr CUP(self, other: Map[\Key, Val\]): Map[\Key, Val\]
    abstract opr UPLUS(self, other: Map[\Key, Val\]): Map[\Key, Val\]
    abstract union(f: (Key, Val, Val)->Val, other: Map[\Key, Val\]): Map[\Key, Val\]
    abstract splitAt(k: Key): (Map[\Key, Val\], Maybe[\Val\], Map[\Key, Val\])
    abstract balancedDelete(r: Map[\Key, Val\]): Map[\Key, Val\]
    abstract balancedAdd(k: Key, v: Val, left: Map[\Key, Val\], right: Map[\Key, Val\]): Map[\Key, Val\]
    abstract concat(t2: Map[\Key, Val\]): Map[\Key, Val\]
    abstract concat3(k: Key, v: Val, t2: Map[\Key, Val\]): Map[\Key, Val\]
    abstract combine[\That extends { Object, Equality[\That\] }, Result extends { Object, Equality[\Result\] }\](f: (Key, Val, That)->Maybe[\Result\], doThis: (Key, Val)->Maybe[\Result\],
            doThat: (Key, That)->Maybe[\Result\],
            mapThis: Map[\Key, Val\]->Map[\Key, Result\],
            mapThat: Map[\Key, That\]->Map[\Key, Result\],
            that: Map[\Key, That\]): Map[\Key, Result\]
    abstract mapFilter[\Result extends { Object, Equality[\Result\] }\](f: (Key, Val)->Maybe[\Result\]): Map[\Key, Result\]
    abstract opr =(self, other: Map[\Key, Val\]): Boolean
end    
    
singleton[\Key extends { StandardTotalOrder[\Key\], Object },
        Val extends { Object, Equality[\Val\] }\](k: Key, v: Val): Map[\Key, Val\]    
  
opr BIG UPLUS[\Key extends {StandardTotalOrder[\Key\], Object},Val extends { Object , Equality[\Val\] }\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Map[\Key,Val\],Map[\Key,Val\]\] 
  
opr BIG UNION[\Key extends {StandardTotalOrder[\Key\], Object},Val extends { Object , Equality[\Val\] }\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Map[\Key,Val\],Map[\Key,Val\]\] 
    
mapping[\Key extends {StandardTotalOrder[\Key\], Object},Val extends { Object , Equality[\Val\] }\](g: Generator[\(Key,Val)\]): Map[\Key,Val\]  
    
(*


opr {|->[\Key,Val\] xs:(Key,Val)... }: Map[\Key,Val\]

opr {[\Key,Val\] }: Map[\Key,Val\]

opr BIG {|->[\Key,Val\] } : Comprehension[\(Key,Val),Map[\Key,Val\],AnyCovColl,AnyCovColl\]

opr BIG {|->[\Key,Val\] g:Generator[\(Key,Val)\]}: Map[\Key,Val\]

opr BIG UNION[\Key,Val\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Any,Any\]

opr BIG UNION[\Key,Val\](g: Generator[\Map[\Key,Val\]\]) : Map[\Key,Val\]

opr BIG UPLUS[\Key,Val\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Any,Any\]

opr BIG UPLUS[\Key,Val\](g: Generator[\Map[\Key,Val\]\]) : Map[\Key,Val\]

*)

end
