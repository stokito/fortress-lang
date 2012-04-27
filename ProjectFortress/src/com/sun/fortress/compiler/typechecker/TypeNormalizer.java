/*******************************************************************************
    Copyright 2009,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/
package com.sun.fortress.compiler.typechecker;

import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.scala_src.useful.SNodeUtil;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

/**
 * This class is responsible for simplifying types:
 *  - ArrayType
 *   = 1-dimensional ArrayType is desugared to FortressLibrary.Array1.
 *   = 2-dimensional ArrayType is desugared to FortressLibrary.Array2.
 *   = 3-dimensional ArrayType is desugared to FortressLibrary.Array3.
 *   = Other ArrayType throws an exception.
 *  - MatrixType
 *   = 2-dimensional MatrixType is desugared to FortressLibrary.Matrix.
 *   = Other MatrixType throws an exception.
 *  - IntersectionType of a single type is desugared to the single type.
 *  - UnionType of a single type is desugared to the single type.
 */
public class TypeNormalizer extends NodeUpdateVisitor {
	public static Type normalize(Type ty) {
		return (Type)ty.accept(new TypeNormalizer());
	}

    /**
     *    ArrayType ::= Type [ ExtentRange(, ExtentRange)* ]
     *    ArrayType(Type element, Indicies indices)
     *    Indices(List<ExtentRange> extents)
     *    ExtentRange(Option<StaticArg> base, Option<StaticArg> size, Option<Op> op)
     *    trait Array1[\T, nat b0, nat s0\]
     *    trait Array2[\T, nat b0, nat s0, nat b1, nat s1\]
     *    trait Array3[\T, nat b0, nat s0, nat b1, nat s1, nat b2, nat s2\]
     */
    public Node forArrayTypeOnly(ArrayType that,
				 TypeInfo info_result,
				 Type elemType_result,
				 Indices indices_result) {
	Span span = NodeUtil.getSpan(that);
	TypeArg elem = NodeFactory.makeTypeArg(elemType_result);
	IntArg zero = NodeFactory.makeIntArgVal(NodeUtil.getSpan(that),"0");
	List<ExtentRange> dims = indices_result.getExtents();
	try {
	    if (dims.size() == 1) {
		ExtentRange first = dims.get(0);
		Id name = NodeFactory.makeId(span, WellKnownNames.fortressLibrary(), "Array1");
		StaticArg base;
		if (first.getBase().isSome())
		    base = first.getBase().unwrap();
		else base = zero;
		if (first.getSize().isSome())
		    return NodeFactory.makeTraitType(span, false, name,
						     elem, base,
						     first.getSize().unwrap()).accept(this);
		else return bug(that, "Missing size.");
	    } else if (dims.size() == 2) {
		ExtentRange first  = dims.get(0);
		ExtentRange second = dims.get(1);
		Id name = NodeFactory.makeId(span, WellKnownNames.fortressLibrary(), "Array2");
		StaticArg base1;
		StaticArg base2;
		if (first.getBase().isSome())
		    base1 = first.getBase().unwrap();
		else base1 = zero;
		if (second.getBase().isSome())
		    base2 = second.getBase().unwrap();
		else base2 = zero;
		if (first.getSize().isSome()) {
		    if (second.getSize().isSome()) {
			return NodeFactory.makeTraitType(span, false, name,
							 elem, base1,
							 first.getSize().unwrap(),
							 base2,
							 second.getSize().unwrap()).accept(this);
		    } else return bug(second, "Missing size.");
		} else return bug(first, "Missing size.");
	    } else if (dims.size() == 3) {
		ExtentRange first  = dims.get(0);
		ExtentRange second = dims.get(1);
		ExtentRange third  = dims.get(2);
		Id name = NodeFactory.makeId(span, WellKnownNames.fortressLibrary(), "Array3");
		StaticArg base1;
		StaticArg base2;
		StaticArg base3;
		if (first.getBase().isSome())
		    base1 = first.getBase().unwrap();
		else base1 = zero;
		if (second.getBase().isSome())
		    base2 = second.getBase().unwrap();
		else base2 = zero;
		if (third.getBase().isSome())
		    base3 = third.getBase().unwrap();
		else base3 = zero;
		if (first.getSize().isSome()) {
		    if (second.getSize().isSome()) {
			if (third.getSize().isSome()) {
			    return NodeFactory.makeTraitType(span, false, name,
							     elem, base1,
							     first.getSize().unwrap(),
							     base2,
							     second.getSize().unwrap(),
							     base3,
							     third.getSize().unwrap()).accept(this);
			} else return bug(third, "Missing size.");
		    } else return bug(second, "Missing size.");
		} else return bug(first, "Missing size.");
	    }
	    return error("Desugaring " + that + " to TraitType is not " +
			 "yet supported.");
	} catch (Exception x) {
	    return error("Desugaring " + that + " to TraitType is not " +
			 "yet supported.");
	}
    }

    /**
     *    MatrixType ::= Type ^ IntExpr
     *                 | Type ^ ( ExtentRange (BY ExtentRange)* )
     *    MatrixType(Type element, List<ExtentRange> dimensions)
     *    trait Matrix[\T extends Number, nat s0, nat s1\]
     *
     *    TraitType(Id name, List<StaticArg> args)
     *
     */
    public Node forMatrixTypeOnly(MatrixType that,
				  TypeInfo info_result,
				  Type elemType_result,
				  List<ExtentRange> dims) {
	if (dims.size() == 2) {
	    ExtentRange first  = dims.get(0);
	    ExtentRange second = dims.get(1);
	    // Or first.getBase() == second.getBase() == 0
	    if (first.getBase().isNone() && second.getBase().isNone() &&
		first.getSize().isSome() && second.getSize().isSome()) {
		Span span = NodeUtil.getSpan(that);
		Id name = NodeFactory.makeId(span, WellKnownNames.fortressLibrary(), "Matrix");
		return NodeFactory.makeTraitType(span, false, name,
						 NodeFactory.makeTypeArg(elemType_result),
						 first.getSize().unwrap(),
						 second.getSize().unwrap()).accept(this);
	    }
	}
	return error("Desugaring " + that + " to TraitType is not yet " +
		     "supported.");
    }

    public Node forIntersectionTypeOnly(IntersectionType that,
					TypeInfo info_result,
					List<Type> elements_result) {
	if (elements_result.size() == 1)
	    return elements_result.get(0);
	else if (elements_result.size() == 2) {
		Type a = elements_result.get(0);
		Type b = elements_result.get(1);
		if (a instanceof AnyType) return b;
		if (b instanceof AnyType) return a;
		return that;
	}
	else
	    return super.forIntersectionTypeOnly(that, info_result,
						 elements_result);
    }

    public Node forUnionTypeOnly(UnionType that, TypeInfo info_result,
				 List<Type> elements_result) {
	if (elements_result.size() == 1)
	    return elements_result.get(0);
	else
	    return super.forUnionTypeOnly(that, info_result, elements_result);
    }
}
