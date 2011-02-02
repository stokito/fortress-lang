(*******************************************************************************
    Copyright 2008,2009, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************)

api Map
import CovariantCollection.{...}
import Set.{Set}

object KeyOverlap[\Key,Val\](key:Key, val1:Val, val2:Val)
        extends UncheckedException
end

(** Note that the map interface is purely functional; methods return a
    fresh map rather than updating the receiving map in place.
    Methods that operate on a particular key leave the rest of the map
    untouched unless otherwise specified. **)
trait Map[\Key,Val\]
      extends { Generator[\(Key,Val)\], Equality[\Map[\Key,Val\]\] }
      comprises { ... }
    getter isEmpty():Boolean
    getter asDebugString():String
    dom(self):Set[\Key\]
    opr | self | : ZZ32
    opr[k:Key]: Val throws NotFound
    member(x:Key): Maybe[\Val\]
    (** The two-argument version of %member% returns the default value %v%
        if the key is absent from the map. **)
    member(x:Key, v:Val): Val
    (** %minimum% and %maximum% refer to the key. **)
    minimum():Maybe[\(Key,Val)\]
    deleteMinimum():Map[\Key,Val\]
    extractMinimum():Maybe[\(Key,Val, Map[\Key,Val\])\]
    maximum(): Maybe[\(Key,Val)\]
    deleteMaximum():Map[\Key,Val\]
    extractMaximum():Maybe[\(Key,Val, Map[\Key,Val\])\]

    (** If no mapping presently exists, maps %k% to %v%. **)
    add(k:Key, v:Val):Map[\Key,Val\]
    (** Maps %k% to %v%. **)
    update(k:Key, v:Val):Map[\Key,Val\]
    (** Eliminate any mapping for key %k%. **)
    delete(k:Key):Map[\Key,Val\]
    (** Process mapping for key %k% with function %f%:
        \begin{itemize}
        \item If no mapping exists, %f% is passed %Nothing[\Val\]%
        \item If %k% maps to value %v%, %f% is passed %Just[\Val\](v)%
        \end{itemize}
        If %f% returns %Nothing%, any mapping for %k% is deleted; otherwise,
        %k% is mapped to the value contained in the result.
     **)
    updateWith(f:Maybe[\Val\]->Maybe[\Val\], k:Key): Map[\Key,Val\]
    (** %UNION% favors the leftmost value when a key occurs in both maps. **)
    opr UNION(self, other: Map[\Key,Val\]): Map[\Key,Val\]
    (** %UPLUS% (disjoint union) throws the %KeyOverlap% exception when a key
        occurs in both maps. **)
    opr UPLUS(self, other: Map[\Key,Val\]): Map[\Key,Val\]
    (** The %union% method takes a function %f% used to combine the values
        of keys that overlap.  **)
    union(f:(Key,Val,Val)->Val, other: Map[\Key,Val\]): Map[\Key,Val\]
    (** %combine% is the ``swiss army knife'' combinator on pairs of maps.
        We call %f% on keys present in both input maps.
        We call %doThis% on keys present in %self% but not in %that%.
        We call %doThat% on keys present in %that% but not in %self%.
        When any of these functions returns %r=Just[\Result\]%, the key is mapped
            to %r.get()% in the result.
        When any of these functions returns %Nothing[\Result\]%, there is no
            mapping for the key in the result.

        %mapThis% must be equivalent to %mapFilter(doThis)% and
        %mapThat% must be equivalent to %mapFilter(doThat)%; they are
        included because often they can do their jobs without
        traversing their argument (eg. for union and interesection
        operations we can pass through or discard whole submaps
        without traversing them).
     **)
    combine[\That,Result\](f:(Key,Val,That)->Maybe[\Result\],
                           doThis:(Key,Val)->Maybe[\Result\],
                           doThat:(Key,That)->Maybe[\Result\],
                           mapThis:Map[\Key,Val\]->Map[\Key,Result\],
                           mapThat:Map[\Key,That\]->Map[\Key,Result\],
                           that: Map[\Key,That\]): Map[\Key,Result\]
    (** %self.mapFilter(f)% is equivalent to:
          %{ k |-> v'  |  (k,v) <- self, v' <- f(k,v) }
        It fuses generation, mapping, and filtering.
     **)
    mapFilter[\Result\](f:(Key,Val)->Maybe[\Result\]): Map[\Key,Result\]
end

singleton[\Key,Val\](k:Key,v:Val): Map[\Key,Val\]

mapping[\Key,Val\](g: Generator[\(Key,Val)\]): Map[\Key,Val\]

opr {|->[\Key,Val\] xs:(Key,Val)... }: Map[\Key,Val\]

opr {[\Key,Val\] }: Map[\Key,Val\]

opr BIG {|->[\Key,Val\] } : Comprehension[\(Key,Val),Map[\Key,Val\],AnyCovColl,AnyCovColl\]

opr BIG {|->[\Key,Val\] g:Generator[\(Key,Val)\]}: Map[\Key,Val\]

opr BIG UNION[\Key,Val\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Any,Any\]

opr BIG UNION[\Key,Val\](g: Generator[\Map[\Key,Val\]\]) : Map[\Key,Val\]

opr BIG UPLUS[\Key,Val\]() : Comprehension[\Map[\Key,Val\],Map[\Key,Val\],Any,Any\]

opr BIG UPLUS[\Key,Val\](g: Generator[\Map[\Key,Val\]\]) : Map[\Key,Val\]

end
