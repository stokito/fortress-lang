/*******************************************************************************
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
 ******************************************************************************/

package com.sun.fortress.nodes_util;

import java.util.Comparator;
import java.util.List;
import com.sun.fortress.nodes.*;
import com.sun.fortress.interpreter.glue.NativeApp;
import com.sun.fortress.interpreter.glue.NativeApplicable;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Option;
import com.sun.fortress.useful.Voidoid;
import com.sun.fortress.useful.AnyListComparer;
import com.sun.fortress.useful.ListComparer;

public class NodeComparator {
    /* option comparers **************************************************/
    public static int compareOptionalTypeRef(Option<TypeRef> a,
                                             Option<TypeRef> b) {
        if (a.isPresent() != b.isPresent()) {
            return a.isPresent() ? 1 : -1;
        }
        if (a.isPresent()) {
            return compare(a.getVal(), b.getVal());
        }
        return 0;
    }

    /* list comparers ****************************************************/
    static class ExtentRangeComparer implements Comparator<ExtentRange> {
        public int compare(ExtentRange left, ExtentRange right) {
            return compare(left, right);
        }
    }
    final static ExtentRangeComparer extentRangeComparer = new ExtentRangeComparer();
    public final static AnyListComparer<ExtentRange> extentRangeListComparer =
        new AnyListComparer(extentRangeComparer);


    static class KeywordTypeComparer implements Comparator<KeywordType> {
        public int compare(KeywordType left, KeywordType right) {
            return compare(left, right);
        }
    }
    final static KeywordTypeComparer keywordTypeComparer = new KeywordTypeComparer();
    public final static AnyListComparer<KeywordType> keywordTypeListComparer =
        new AnyListComparer(keywordTypeComparer);

    static class ParamComparer implements Comparator<Param> {
        public int compare(Param left, Param right) {
            return compare(left, right);
        }
    }
    final static ParamComparer paramComparer = new ParamComparer();
    public final static AnyListComparer<Param> paramListComparer =
        new AnyListComparer(paramComparer);

    static class StaticParamComparer implements Comparator<StaticParam> {
        public int compare(StaticParam left, StaticParam right) {
            Class tclass = left.getClass();
            Class oclass = right.getClass();
            if (oclass != tclass) {
                return tclass.getName().compareTo(oclass.getName());
            }
            return subtypeCompareTo(left, right);
        }
    }
    public final static StaticParamComparer staticParamComparer =
        new StaticParamComparer();
    public static AnyListComparer<StaticParam> staticParamListComparer =
        new AnyListComparer(staticParamComparer);

    static class StaticArgComparer implements Comparator<StaticArg> {
        public int compare(StaticArg left, StaticArg right) {
            return compare(left, right);
        }
    }
    final static StaticArgComparer staticArgComparer = new StaticArgComparer();
    public final static AnyListComparer<StaticArg> staticArgListComparer =
        new AnyListComparer(staticArgComparer);

    static class TypeRefComparer implements Comparator<TypeRef> {
        public int compare(TypeRef left, TypeRef right) {
            return compare(left, right);
        }
    }
    final static TypeRefComparer typeRefComparer = new TypeRefComparer();
    public final static AnyListComparer<TypeRef> typeRefListComparer =
        new AnyListComparer(typeRefComparer);

    /* comparing lists ***************************************************/
    public static int compare(List<StaticParam> left, List<StaticParam> right) {
        return staticParamListComparer.compare(left, right);
    }

    /* compare methods ***************************************************/
    public static int compare(Applicable left, Applicable right) {
        if (left instanceof FnExpr) {
            int x = Useful.compareClasses(left, right);
            if (x != 0) return x;
            return NodeUtil.getName(((FnExpr)left).getFnName()).compareTo(NodeUtil.getName(((FnExpr)right).getFnName()));
        } else if (left instanceof FnDefOrDecl) {
            int x = Useful.compareClasses(left, right);
            if (x != 0) return x;
            return compare(left, (FnDefOrDecl)right);
        } else if (left instanceof NativeApp) {
            return Useful.compareClasses(left, right);
        } else if (left instanceof NativeApplicable) {
            int x = Useful.compareClasses(left, right);
            if (x != 0) return x;
            NativeApplicable na = (NativeApplicable)right;
            x = ((NativeApplicable)left).stringName().compareTo(na.stringName());
            if (x != 0) return x;
            return NodeUtil.getName(left.getFnName()).compareTo(NodeUtil.getName(na.getFnName()));
        } else {
            throw new Error("NodeComparator.compare(" + left.getClass() + ", "
                            + right.getClass());
        }
    }

    public static int compare(DottedId left, DottedId right) {
        return ListComparer.stringListComparer.compare(left.getNames(),
                                                       right.getNames());
    }

    public static int compare(ExtentRange left, ExtentRange right) {
        // TODO Optional parameters on extent ranges are tricky things; perhaps
        // they need not both be present.
        int x = compareOptionalTypeRef(left.getBase(), right.getBase());
        if (x != 0) return x;
        x = compareOptionalTypeRef(left.getSize(), right.getSize());
        if (x != 0) return x;
        return 0;
    }

    public static int compare(FnDefOrDecl left, FnDefOrDecl right) {
        FnName fn0 = left.getFnName();
        FnName fn1 = right.getFnName();
        int x = NodeComparator.compare(fn0, fn1);
        if (x != 0)  return x;
        x = Option.<List<StaticParam>>compare(left.getStaticParams(),
                                              right.getStaticParams(),
                                              NodeComparator.staticParamListComparer);
        if (x != 0)  return x;
        x = paramListComparer.compare(left.getParams(), right.getParams());
        return x;
    }

    public static int compare(FnName left, FnName right) {
        Class leftClass = left.getClass();
        Class rightClass = right.getClass();

        if (leftClass != rightClass) {
            return leftClass.getName().compareTo(rightClass.getName());
        }
        else {
            return NodeUtil.stringName(left).compareTo(NodeUtil.stringName(right));
        }
    }

    public static int compare(Id left, Id right) {
        return left.getName().compareTo(right.getName());
    }

    public static int compare(Indices left, Indices right) {
        Class tclass = left.getClass();
        Class oclass = right.getClass();
        if (oclass != tclass) {
            return tclass.getName().compareTo(oclass.getName());
        }
        return subtypeCompareTo(left, right);
    }

    public static int compare(KeywordType left, KeywordType right) {
        return compare(left.getName(), right.getName(),
                       left.getType(), right.getType());
    }

    public static int compare(Param left, Param right) {
        int x = left.getName().getName().compareTo(right.getName().getName());
        if (x != 0) return x;
        x = compareOptionalTypeRef(left.getType(), right.getType());
        if (x != 0) return x;
        // TODO default expr, mods, must enter into comparison also.
        return x;
    }

    public static int compare(StaticArg left, StaticArg right) {
        return compare((TypeRef) left, right);
    }

    public static int compare(TypeRef left, TypeRef right) {
        Class leftClass = left.getClass();
        Class rightClass = right.getClass();

        if (leftClass != rightClass) {
            return leftClass.getName().compareTo(rightClass.getName());
        }
        else {
            return subtypeCompareTo(left, right);
        }
    }

    /* comparing tuples **************************************************/
    public static int compare(Id a, Id b, TypeRef c, TypeRef d) {
        int x = compare(a, b);
        if (x != 0) return x;
        return compare(c, d);
    }

    public static int compare(TypeRef a, TypeRef b, ExtentRange c, ExtentRange d) {
        int x = compare(a, b);
        if (x != 0) return x;
        //        return compare(c, d);
        return compare(c, d);
    }

    public static int compare(TypeRef a, TypeRef b, Indices c, Indices d) {
        int x = compare(a, b);
        if (x != 0) return x;
        //        return compare(c, d);
        return compare(c, d);
    }

    public static int compare(TypeRef a, TypeRef b, TypeRef c, TypeRef d) {
        int x = compare(a, b);
        if (x != 0) return x;
        return compare(c, d);
    }

    /* subtypeCompareTo **************************************************/
    static int subtypeCompareTo(ArrayType left, ArrayType right) {
        return compare(left.getElement(), right.getElement(),
                       left.getIndices(), right.getIndices());
    }

    static int subtypeCompareTo(ArrowType left, ArrowType right) {
        int x = compare(left.getRange(), right.getRange());
        if (x != 0) return x;
        x = typeRefListComparer.compare(left.getDomain(), right.getDomain());
        if (x != 0) return x;
        x = typeRefListComparer.compare(left.getThrowsClause(), right.getThrowsClause());
        return x;
    }

    static int subtypeCompareTo(BaseNatRef left, BaseNatRef right) {
        return left.getValue() - right.getValue();
        /* nat types -- difference will not overflow */
    }

    static int subtypeCompareTo(ExponentStaticArg left, ExponentStaticArg right) {
        return compare((TypeRef)left.getPower(), right.getPower(),
                       left.getBase(), right.getBase());
        // casts for generics
    }

    static int subtypeCompareTo(FixedDim left, FixedDim right) {
        return extentRangeListComparer
            .compare(left.getExtents(), right.getExtents());
    }

    static int subtypeCompareTo(IdType left, IdType right) {
        return compare(left.getName(), right.getName());
    }

    static int subtypeCompareTo(Indices left, Indices right) {
        throw new Error("subtypeCompareTo(" + left.getClass() + " " +
                        right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(ListType left, ListType right) {
        return compare(left.getElement(), right.getElement());
    }

    static int subtypeCompareTo(MapType left, MapType right) {
        return compare(left.getKey(), right.getKey(),
                       left.getValue(), right.getValue());
    }

    static int subtypeCompareTo(MatrixType left, MatrixType right) {
        int y = compare(left.getElement(), right.getElement());
        if (y != 0) return y;
        return extentRangeListComparer.compare(left.getDimensions(),
                                               right.getDimensions());
    }

    static int subtypeCompareTo(BaseOprRef left, BaseOprRef right) {
        return compare(left.getName(), right.getName());
    }

    static int subtypeCompareTo(ParamType left, ParamType right) {
        int c = compare(left.getGeneric(), right.getGeneric());
        if (c != 0) return c;
        return staticArgListComparer.compare(left.getArgs(),
                                             right.getArgs());
    }

    static int subtypeCompareTo(ProductDimType left, ProductDimType right) {
        return compare((TypeRef)left.getMultiplier(), right.getMultiplier(),
                       (TypeRef)left.getMultiplicand(), right.getMultiplicand());
        // cast for generics
    }

    static int subtypeCompareTo(QuotientDimType left, QuotientDimType right) {
        // TODO Don't I need to worry about reducing the fraction?
        return compare((TypeRef)left.getNumerator(), right.getNumerator(),
                       (TypeRef)left.getDenominator(), right.getDenominator());
        // cast for generics
    }

    static int subtypeCompareTo(QuotientStaticArg left, QuotientStaticArg right) {
        // TODO Don't I need to worry about reducing the fraction?
        return compare(left.getNumerator(), right.getNumerator(),
                       left.getDenominator(), right.getDenominator());
    }

    static int subtypeCompareTo(VarargsType left, VarargsType right) {
        return compare(left.getType(), right.getType());
    }

    static int subtypeCompareTo(SimpleTypeParam left, SimpleTypeParam right) {
        int x = NodeUtil.getName(left).compareTo(NodeUtil.getName(right));
        if (x != 0) {
            return x;
        }
        if (left.isAbsorbs() != right.isAbsorbs()) {
            return left.isAbsorbs() ? 1 : -1;
        }
        if (left.getExtendsClause().isPresent() != right.getExtendsClause().isPresent()) {
            return left.getExtendsClause().isPresent() ? 1 : -1;
        }
        if (!left.getExtendsClause().isPresent()) {
            return 0;
        }
        List<TypeRef> l = left.getExtendsClause().getVal();
        List<TypeRef> ol = right.getExtendsClause().getVal();
        return typeRefListComparer.compare(l, ol);
    }

    static int subtypeCompareTo(StaticParam left, StaticParam right) {
        return NodeUtil.getName(left).compareTo(NodeUtil.getName(right));
    }

    static int subtypeCompareTo(TupleType left, TupleType right) {
        return typeRefListComparer.compare(left.getElements(), right.getElements());
    }

    static int subtypeCompareTo(TypeArg left, TypeArg right) {
        return compare(left.getType(), right.getType());
    }
    static int subtypeCompareTo(TypeRef left, TypeRef right) {
        throw new Error("subtypeCompareTo(" + left.getClass() + " " +
                        right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(VectorType left, VectorType right) {
        // TODO Don't I need to worry about reducing the fraction?
        return compare(left.getElement(), right.getElement(),
                       left.getDim(), right.getDim());
    }

    static int subtypeCompareTo(VoidType left, VoidType right) {
        // All voids are equal
        return 0;
    }
}
