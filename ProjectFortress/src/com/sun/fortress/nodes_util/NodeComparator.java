/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.AnyListComparer;
import com.sun.fortress.useful.ListComparer;
import com.sun.fortress.exceptions.InterpreterBug;

public class NodeComparator {
    /* option comparers **************************************************/
    public static int compareOptionalType(Option<Type> a,
					  Option<Type> b) {
	if (a.isSome() != b.isSome()) {
	    return a.isSome() ? 1 : -1;
	}
	if (a.isSome()) {
	    return compare(a.unwrap(), b.unwrap());
	}
	return 0;
    }

    public static int compareOptionalStaticArg(Option<StaticArg> a,
					       Option<StaticArg> b) {
	if (a.isSome() != b.isSome()) {
	    return a.isSome() ? 1 : -1;
	}
	if (a.isSome()) {
	    return compare(a.unwrap(), b.unwrap());
	}
	return 0;
    }

    public static int compareOptionalOp(Option<Op> a,
					Option<Op> b) {
	if (a.isSome() != b.isSome()) {
	    return a.isSome() ? 1 : -1;
	}
	if (a.isSome()) {
	    return a.unwrap().getText().compareTo(b.unwrap().getText());
	}
	return 0;
    }
    /* list comparers ****************************************************/
    static class ExtentRangeComparer implements Comparator<ExtentRange>, Serializable {
	public int compare(ExtentRange left, ExtentRange right) {
	    return NodeComparator.compare(left, right);
	}
    }
    final static ExtentRangeComparer extentRangeComparer = new ExtentRangeComparer();
    public final static AnyListComparer<ExtentRange> extentRangeListComparer =
	new AnyListComparer<ExtentRange>(extentRangeComparer);

    static class KeywordTypeComparer implements Comparator<KeywordType>, Serializable {
	public int compare(KeywordType left, KeywordType right) {
	    return NodeComparator.compare(left, right);
	}
    }
    final static KeywordTypeComparer keywordTypeComparer = new KeywordTypeComparer();
    public final static AnyListComparer<KeywordType> keywordTypeListComparer =
	new AnyListComparer<KeywordType>(keywordTypeComparer);

    static class ParamComparer implements Comparator<Param>, Serializable {
	public int compare(Param left, Param right) {
	    return NodeComparator.compare(left, right);
	}
    }
    final static ParamComparer paramComparer = new ParamComparer();
    public final static AnyListComparer<Param> paramListComparer =
	new AnyListComparer<Param>(paramComparer);

    static class StaticParamComparer implements Comparator<StaticParam>, Serializable {
	public int compare(StaticParam left, StaticParam right) {
	    Class<? extends StaticParam> tclass = left.getClass();
	    Class<? extends StaticParam> oclass = right.getClass();
	    if (oclass != tclass) {
		return tclass.getName().compareTo(oclass.getName());
	    }
	    return NodeComparator.compare(left, right);
	}
    }
    public final static StaticParamComparer staticParamComparer =
	new StaticParamComparer();
    public static final AnyListComparer<StaticParam> staticParamListComparer =
	new AnyListComparer<StaticParam>(staticParamComparer);

    static class StaticArgComparer implements Comparator<StaticArg>, Serializable {
	public int compare(StaticArg left, StaticArg right) {
	    return NodeComparator.compare(left, right);
	}
    }
    final static StaticArgComparer staticArgComparer = new StaticArgComparer();
    public final static AnyListComparer<StaticArg> staticArgListComparer =
	new AnyListComparer<StaticArg>(staticArgComparer);

    static class BaseTypeComparer implements Comparator<BaseType>, Serializable {
	public int compare(BaseType left, BaseType right) {
	    return NodeComparator.compare(left, right);
	}
    }
    final static BaseTypeComparer traitTypeComparer = new BaseTypeComparer();
    public final static AnyListComparer<BaseType> traitTypeListComparer =
	new AnyListComparer<BaseType>(traitTypeComparer);

    public static class TypeComparer implements Comparator<Type>, Serializable {
	public int compare(Type left, Type right) {
	    return NodeComparator.compare(left, right);
	}
    }
    public final static TypeComparer typeComparer = new TypeComparer();
    public final static AnyListComparer<Type> typeListComparer =
	new AnyListComparer<Type>(typeComparer);

    public static class IdOrOpComparer implements Comparator<IdOrOp>, Serializable {
	public int compare(IdOrOp left, IdOrOp right) {
	    return NodeComparator.compare(left, right);
	}
    }
    public final static IdOrOpComparer idOrOpComparer = new IdOrOpComparer();

    /* comparing lists ***************************************************/
    public static int compare(List<StaticParam> left, List<StaticParam> right) {
	return staticParamListComparer.compare(left, right);
    }

    public static final Comparator<APIName> apiNameComparer = new Comparator<APIName>() {

	public int compare(APIName o1, APIName o2) {
	    return NodeComparator.compare(o1, o2);
	}

    };

    /* compare methods ***************************************************/
    public static int compare(APIName left, APIName right) {
	return ListComparer.stringListComparer.compare(NodeUtil.toStrings(left),
						       NodeUtil.toStrings(right));
    }

    public static int compare(Id left, Id right) {
	return NodeUtil.nameString(left).compareTo(NodeUtil.nameString(right));
    }

    public static int compare(ExtentRange left, ExtentRange right) {
	// TODO Optional parameters on extent ranges are tricky things; perhaps
	// they need not both be present.
	int x = compareOptionalStaticArg(left.getBase(), right.getBase());
	if (x != 0) return x;
	x = compareOptionalStaticArg(left.getSize(), right.getSize());
	if (x != 0) return x;
	x = compareOptionalOp(left.getOp(), right.getOp());
	return x;
    }

    public static int compare(FnDecl left, FnDecl right) {
	IdOrOpOrAnonymousName fn0 = NodeUtil.getName(left);
	IdOrOpOrAnonymousName fn1 = NodeUtil.getName(right);
	int x = NodeComparator.compare(fn0, fn1);
	if (x != 0)  return x;
	x = compare(NodeUtil.getStaticParams(left), NodeUtil.getStaticParams(right));
	if (x != 0)  return x;
	x = paramListComparer.compare(NodeUtil.getParams(left), NodeUtil.getParams(right));
	return x;
    }

    static class FnDeclComparer implements Comparator<FnDecl>, Serializable {
	public int compare(FnDecl left, FnDecl right) {
	    return NodeComparator.compare(left, right);
	}
    }

    public final static FnDeclComparer fnAbsDeclOrDeclComparer = new FnDeclComparer();

    public static int compare(IdOrOpOrAnonymousName left, IdOrOpOrAnonymousName right) {
	Class<? extends IdOrOpOrAnonymousName> leftClass = left.getClass();
	Class<? extends IdOrOpOrAnonymousName> rightClass = right.getClass();

	if (leftClass != rightClass) {
	    return leftClass.getName().compareTo(rightClass.getName());
	}
	else {
	    return left.stringName().compareTo(right.stringName());
	}
    }
    public final static Comparator<IdOrOpOrAnonymousName> IoooanComparer =
        new Comparator<IdOrOpOrAnonymousName>() {
            @Override
            public int compare(IdOrOpOrAnonymousName arg0,
                    IdOrOpOrAnonymousName arg1) {
                return NodeComparator.compare(arg0, arg1);
            }
        
    };

    public static int compare(KeywordType left, KeywordType right) {
	return compare(left.getName(), right.getName(),
		       left.getKeywordType(), right.getKeywordType());
    }

    public static int compare(Param left, Param right) {
	int x = NodeUtil.nameString(left.getName())
			.compareTo(NodeUtil.nameString(right.getName()));
	if (x != 0) return x;
	if ( ! NodeUtil.isVarargsParam(left) && ! NodeUtil.isVarargsParam(right)) {
	    x = compareOptionalType(NodeUtil.optTypeOrPatternToType(left.getIdType()),
                                    NodeUtil.optTypeOrPatternToType(right.getIdType()));
	}
	if ( NodeUtil.isVarargsParam(left) && NodeUtil.isVarargsParam(right)) {
	    x = compareOptionalType(left.getVarargsType(), right.getVarargsType());
	}
	if (x != 0) return x;
	// TODO default expr, mods, must enter into comparison also.
	return x;
    }

    public static int compare(StaticArg left, StaticArg right) {
	Class<? extends StaticArg> leftClass = left.getClass();
	Class<? extends StaticArg> rightClass = right.getClass();

	if (leftClass != rightClass) {
	    return leftClass.getName().compareTo(rightClass.getName());
	}
	else {
	    return subtypeCompareTo(left, right);
	}
    }

    public static int compare(Type left, Type right) {
	Class<? extends Type> leftClass = left.getClass();
	Class<? extends Type> rightClass = right.getClass();

	if (leftClass != rightClass) {
	    return leftClass.getName().compareTo(rightClass.getName());
	}
	else {
	    return subtypeCompareTo(left, right);
	}
    }

    /* comparing tuples **************************************************/
    public static int compare(Id a, Id b, Type c, Type d) {
	int x = compare(a, b);
	if (x != 0) return x;
	return compare(c, d);
    }

    public static int compare(Type a, Type b, ExtentRange c, ExtentRange d) {
	int x = compare(a, b);
	if (x != 0) return x;
	//        return compare(c, d);
	return compare(c, d);
    }

    public static int compare(Type a, Type b, Indices c, Indices d) {
	int x = compare(a, b);
	if (x != 0) return x;
	//        return compare(c, d);
	return compare(c, d);
    }

    public static int compare(Type a, Type b, Type c, Type d) {
	int x = compare(a, b);
	if (x != 0) return x;
	return compare(c, d);
    }

    /* subtypeCompareTo **************************************************/
    static int compare(ArrayType left, ArrayType right) {
	return compare(left.getElemType(), right.getElemType(),
		       left.getIndices(), right.getIndices());
    }

    static int compare(ArrowType left, ArrowType right) {
	int x = compare(left.getRange(), right.getRange());
	if (x != 0) return x;
	x = compare(left.getDomain(), right.getDomain());
	if (x != 0) return x;
	return compare(left.getEffect(), right.getEffect());
    }

    static int compare(IntersectionType left, IntersectionType right) {
	int x = typeListComparer.compare(left.getElements(), right.getElements());
	return x;
    }

    static int compare(UnionType left, UnionType right) {
	int x = typeListComparer.compare(left.getElements(), right.getElements());
	return x;
    }

    static int compare(TupleType left, TupleType right) {
	int x = typeListComparer.compare(left.getElements(), right.getElements());
	if (x != 0) return x;
	x = compareOptionalType(left.getVarargs(), right.getVarargs());
	if (x != 0) return x;
	return keywordTypeListComparer.compare(left.getKeywords(), right.getKeywords());
    }

    static int compare(Effect left, Effect right) {
	if (left.isIoEffect() != right.isIoEffect())
	    return left.isIoEffect() ? 1 : -1;
	if (left.getThrowsClause().isSome() != right.getThrowsClause().isSome())
	    return left.getThrowsClause().isSome() ? 1 : -1;
	if (left.getThrowsClause().isSome()) {
	    return typeListComparer.compare(left.getThrowsClause().unwrap(),
					    right.getThrowsClause().unwrap());
	}
	else return 0;
    }

    static int compare(IntArg left, IntArg right) {
	return subtypeCompareTo(left.getIntVal(), right.getIntVal());
    }

    static int subtypeCompareTo(IntExpr left, IntExpr right) {
	if (left instanceof IntBase && right instanceof IntBase)
	    return ((IntBase)left).getIntVal().getIntVal().intValue() -
		   ((IntBase)right).getIntVal().getIntVal().intValue();
	/* nat types -- difference will not overflow */
	else return 0;
    }

    public static int compare(Indices left, Indices right) {
	return extentRangeListComparer
	    .compare(left.getExtents(), right.getExtents());
    }

    static int compare(VarType left, VarType right) {
	return compare(left.getName(), right.getName());
    }

    static int subtypeCompareTo(Indices left, Indices right) {
	throw new InterpreterBug(left,
				 "subtypeCompareTo(" + left.getClass() + " " +
				 right.getClass() + ") is not implemented!");
    }

    static int compare(MatrixType left, MatrixType right) {
	int y = compare(left.getElemType(), right.getElemType());
	if (y != 0) return y;
	return extentRangeListComparer.compare(left.getDimensions(),
					       right.getDimensions());
    }

    static int compare(OpArg left, OpArg right) {
	return compare(left.getId(), right.getId());
    }

    static int compare(TraitSelfType left, TraitSelfType right) {
	int c = compare(left.getNamed(), right.getNamed());
	if (c != 0) return c;
        return typeListComparer.compare(left.getComprised(),
                                        right.getComprised());
    }

    static int compare(ObjectExprType left, ObjectExprType right) {
        return typeListComparer.compare(left.getExtended(),
                                        right.getExtended());
    }

    static int compare(TraitType left, TraitType right) {
	int c = compare(left.getName(), right.getName());
	if (c != 0) return c;
	return staticArgListComparer.compare(left.getArgs(),
					     right.getArgs());
    }

    static int compare(TaggedDimType left, TaggedDimType right) {
	throw new InterpreterBug(left,
				 "subtypeCompareTo(" + left.getClass() + " " +
				 right.getClass() + ") is not implemented!");
    }

    static int compare(TaggedUnitType left, TaggedUnitType right) {
	throw new InterpreterBug(left,
				 "subtypeCompareTo(" + left.getClass() + " " +
				 right.getClass() + ") is not implemented!");
    }

    static int compare(StaticParam left, StaticParam right) {
	return NodeUtil.getName(left).compareTo(NodeUtil.getName(right));
    }

    static int compare(TypeArg left, TypeArg right) {
	return compare(left.getTypeArg(), right.getTypeArg());
    }

    private static int subtypeCompareTo(StaticArg left, StaticArg right) {
	if (left instanceof BoolArg) {
	    //return compare((BoolArg) left, (BoolArg) right);
	} else if (left instanceof DimArg) {
	    //return compare((DimArg) left, (DimArg) right);
	} else if (left instanceof IntArg) {
	    return compare((IntArg) left, (IntArg) right);
	} else if (left instanceof OpArg) {
	    return compare((OpArg) left, (OpArg) right);
	} else if (left instanceof TypeArg) {
	    return compare((TypeArg) left, (TypeArg) right);
	} else if (left instanceof UnitArg) {
	    //return compare((UnitArg) left, (UnitArg) right);
	} else {

	}
	throw new InterpreterBug(left,
				 "subtypeCompareTo(" + left.getClass() + " " +
				 right.getClass() + ") is not implemented!");
    }

    private static int subtypeCompareTo(Type left, Type right) {
	// Commented out cases haven't had their methods implememnted yet,
	// and will stack overflow instead.

	if (left instanceof TraitType) {
	    return compare((TraitType) left, (TraitType) right);
	} else if (left instanceof TraitSelfType) {
            return subtypeCompareTo(((TraitSelfType)left).getNamed(),
                                    ((TraitSelfType)right).getNamed());
	} else if (left instanceof ObjectExprType) {
            int x = typeListComparer.compare(((ObjectExprType)left).getExtended(),
                                             ((ObjectExprType)right).getExtended());
            return x;
	} else if (left instanceof TupleType) {
	    if ( NodeUtil.isVoidType((TupleType)left) )
		return 0;
	    else return compare((TupleType) left, (TupleType) right);
	} else if (left instanceof ArrowType) {
	    return compare((ArrowType) left, (ArrowType) right);
	} else if (left instanceof IntersectionType) {
	    return compare((IntersectionType) left, (IntersectionType) right);
	} else if (left instanceof UnionType) {
	    return compare((UnionType) left, (UnionType) right);
	} else if (left instanceof TaggedDimType) {
	    return compare((TaggedDimType) left, (TaggedDimType) right);
	} else if (left instanceof TaggedUnitType) {
	    return compare((TaggedUnitType) left, (TaggedUnitType) right);
	} else if (left instanceof ArrayType) {
	    return compare((ArrayType) left, (ArrayType) right);
	} else if (left instanceof VarType) {
	    return compare((VarType) left, (VarType) right);
	} else if (left instanceof MatrixType) {
	    return compare((MatrixType) left, (MatrixType) right);
	} else if (left instanceof AnyType) {
	    return 0;
	} else {

	}
	throw new InterpreterBug(left,
				 "subtypeCompareTo(" + left.getClass() + " " +
				 right.getClass() + ") is not implemented!");
    }
}
