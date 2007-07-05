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

package com.sun.fortress.interpreter.nodes_util;

import java.util.Comparator;
import java.util.List;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Voidoid;
import com.sun.fortress.interpreter.useful.AnyListComparer;
import com.sun.fortress.interpreter.useful.ListComparer;

public class NodeComparator {
    /** list comparers */
    public static final ListComparer<ExtentRange> extentRangeListComparer =
        new ListComparer<ExtentRange>();

    public static final ListComparer<KeywordType> keywordTypeListComparer =
        new ListComparer<KeywordType>();

    public final static ListComparer<Param> paramListComparer =
        new ListComparer<Param>();

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

    public static int compare(List<StaticParam> left, List<StaticParam> right) {
        return staticParamListComparer.compare(left, right);
    }

    static int subtypeCompareTo(StaticParam left, StaticParam right) {
        return NodeUtil.getName(left).compareTo(NodeUtil.getName(right));
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
        return TypeRef.listComparer.compare(l, ol);
    }

    public static int compare(Applicable left, Applicable right) {
        return left.applicableCompareTo(right);
    }

    public static int compare(DottedId left, DottedId right) {
        return ListComparer.stringListComparer.compare(left.getNames(),
                                                       right.getNames());
    }

    public static int compare(ExtentRange left, ExtentRange right) {
        // TODO Optional parameters on extent ranges are tricky things; perhaps
        // they need not both be present.
        int x = TypeRef.compareOptional(left.getBase(), right.getBase());
        if (x != 0) return x;
        x = TypeRef.compareOptional(left.getSize(), right.getSize());
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
        return c.compareTo(d);
        //        return compare(c, d);
    }

    public static int compare(TypeRef a, TypeRef b, ExtentRange c, ExtentRange d) {
        //        int x = compare(a, b);
        int x = a.compareTo(b);
        if (x != 0) return x;
        //        return compare(c, d);
        return c.compareTo(d);
    }

    public static int compare(TypeRef a, TypeRef b, Indices c, Indices d) {
        //        int x = compare(a, b);
        int x = a.compareTo(b);
        if (x != 0) return x;
        //        return compare(c, d);
        return c.compareTo(d);
    }

    public static int compare(TypeRef a, TypeRef b, TypeRef c, TypeRef d) {
        //        int x = compare(a, b);
        int x = a.compareTo(b);
        if (x != 0) return x;
        //        return compare(c, d);
        return c.compareTo(d);
    }

    /* subtypeCompareTo **************************************************/
    static int subtypeCompareTo(ArrayType left, ArrayType right) {
        return compare(left.getElement(), right.getElement(),
                       left.getIndices(), right.getIndices());
    }

    static int subtypeCompareTo(ArrowType left, ArrowType right) {
        int x = compare(left.getRange(), right.getRange());
        if (x != 0) return x;
        x = TypeRef.listComparer.compare(left.getDomain(), right.getDomain());
        if (x != 0) return x;
        x = keywordTypeListComparer.compare(left.getKeywords(),
                                            right.getKeywords());
        if (x != 0) return x;
        x = TypeRef.listComparer.compare(left.getThrows_(), right.getThrows_());
        return x;
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

    static int subtypeCompareTo(ParamType left, ParamType right) {
        int c = compare(left.getGeneric(), right.getGeneric());
        if (c != 0) return c;
        return StaticArg.typeargListComparer.compare(left.getArgs(),
                                                     right.getArgs());
    }

    static int subtypeCompareTo(RestType left, RestType right) {
        return compare(left.getType(), right.getType());
    }

    static int subtypeCompareTo(SetType left, SetType right) {
        return compare(left.getElementType(), right.getElementType());
    }

    static int subtypeCompareTo(TupleType left, TupleType right) {
        return TypeRef.listComparer.compare(left.getElements(), right.getElements());
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

//   Copied code begins here.
//
//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        // TODO Auto-generated method stub
//        NYI("subtypeCompareTo for " + this.getClass().getName());
//        return 0;
//    }

//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        QuotientDim x = (QuotientDim) o;
//        // TODO Don't I need to worry about reducing the fraction?
//        return Useful.compare((TypeRef) numerator, x.numerator,
//                              (TypeRef) denominator, x.denominator); // cast for generics
//    }

//        int subtypeCompareTo(TypeRef o) {
//        ProductDim x = (ProductDim) o;
//        return Useful.compare((TypeRef) multiplier, x.multiplier,
//                              (TypeRef) multiplicand, x.multiplicand); // cast for generics
//    }

//        int subtypeCompareTo(TypeRef o) {
//        return name.compareTo(((NameDim) o).name);
//    }

//    int subtypeCompareTo(TypeRef o) {
//        ExponentDim x = (ExponentDim) o;
//        return Useful.compare((TypeRef) power, x.power, (TypeRef) base, x.base); // casts
//                                                                                    // for
//                                                                                    // generics
//    }

//    public int compareTo(Param o) {
//        int x = getName().getName().compareTo(o.getName().getName());
//        if (x != 0) return x;
//        x = TypeRef.compareOptional(getType(), o.getType());
//        if (x != 0) return x;
//        // TODO default expr, mods, must enter into comparison also.
//        return x;
//    }
//
//    public int compareTo(KeywordType o) {
//        return Useful.compare(name, o.name, type, o.type);
//    }
//
//        public int compareTo(FnDefOrDecl a1) {
//        FnDefOrDecl a0 = this;
//
//        FnName fn0 = a0.getFnName();
//        FnName fn1 = a1.getFnName();
//        int x = fn0.compareTo(fn1);
//        if (x != 0)  return x;
//
//        x = Option.<List<StaticParam>>compare(a0.getStaticParams(), a1.getStaticParams(), StaticParam.listComparer);
//
//        if (x != 0)  return x;
//
//        x = Param.listComparer.compare(a0.getParams(), a1.getParams());
//        return x;
//
//    }
