/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.scala_src.types

import com.sun.fortress.compiler.index._
import com.sun.fortress.compiler.Types.ANY
import com.sun.fortress.compiler.Types.BOTTOM
import com.sun.fortress.compiler.Types.OBJECT
import com.sun.fortress.exceptions.InterpreterBug.bug
import com.sun.fortress.nodes._
import com.sun.fortress.nodes_util.NodeFactory.typeSpan
import com.sun.fortress.scala_src.nodes._
import com.sun.fortress.scala_src.useful.Lists._
import com.sun.fortress.scala_src.useful.Maps._
import com.sun.fortress.scala_src.useful.Options._
import com.sun.fortress.scala_src.useful.Sets._
import com.sun.fortress.scala_src.useful.STypesUtil._
import com.sun.fortress.useful.NI

/*
 * Everything in here should be merged into STypeUtil eventually
 */

object TypeAnalyzerUtil {
  //Type utilites
  def substitute(args: List[StaticArg], params: List[StaticParam], typ: Type): Type = {

    def getVal(x: StaticArg): Node = x match {
      case STypeArg(_, _, v) => v
      case SIntArg(_, _, v) => v
      case SBoolArg(_, _, v) => v
      case SDimArg(_, _, v) => v
      case SUnitArg(_, _, v) => v
      case SOpArg(_, _, v) => v
    }
    val subst = Map((params, args).zipped.map
                    ((p, a) => (p.getName, a)):_*)
    object replacer extends Walker {
      override def walk(node: Any) = node match {
        case n:VarType => subst.get(n.getName).map(getVal).getOrElse(n)
        // OpArgs that are not inference variables have names
        case n:OpArg => subst.get(n.getId).getOrElse(n)
        case n:IntRef => subst.get(n.getName).map(getVal).getOrElse(n)
        case n:BoolRef => subst.get(n.getName).map(getVal).getOrElse(n)
        case n:DimRef => subst.get(n.getName).map(getVal).getOrElse(n)
        case n:UnitRef => subst.get(n.getName).map(getVal).getOrElse(n)
        case _ => super.walk(node)
      }
    }
    replacer(typ).asInstanceOf[Type]
  }

  /**
   * A tuple with var args is equivalent to an infinite union of tuples.
   * (A,B ...) = BOTTOM UNION A UNION (A,B) UNION (A,B,B) ...
   * This method gets the ith disjunct
   */
  def disjunctFromTuple(tuple: TupleType, size: Int): Type = tuple match {
    case STupleType(i, e, Some(v), k) if (size >= e.size) =>
      makeTupleType(i, e ++ List.fill(size-e.size){v}, k)
    case STupleType(_, e, None, _) if (size == e.size)=> tuple
    case _ => BOTTOM
  }

  def makeTupleType(info: TypeInfo, types: List[Type]): Type = makeTupleType(info, types, Nil)

  def makeTupleType(info: TypeInfo, types: List[Type], keys: List[KeywordType]): Type = types match {
    case t::Nil if (keys.isEmpty) => t
    case _ => STupleType(info, types, None, keys)
  }

  def makeIntersectionType(types: Iterable[Type]) = types.toList match {
    case Nil => ANY
    case t::Nil => t
    case ts@_ => SIntersectionType(makeInfo(ts), ts)
  }

  def makeUnionType(types: Iterable[Type]) = types.toList match {
    case Nil => BOTTOM
    case t::Nil => t
    case ts@_ => SUnionType(makeInfo(ts), ts)
  }

  //ToDo: Make a better span
  def makeInfo(types: Iterable[Type]): TypeInfo = {
    typeInfo
  }
  
  def typeInfo = STypeInfo(typeSpan, false ,Nil, None)
  
}
