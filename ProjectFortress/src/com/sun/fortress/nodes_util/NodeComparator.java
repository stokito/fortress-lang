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
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.AnyListComparer;
import com.sun.fortress.useful.ListComparer;
import com.sun.fortress.interpreter.evaluator.InterpreterBug;

public class NodeComparator {
    /* option comparers **************************************************/
    public static int compareOptionalType(Option<Type> a,
                                             Option<Type> b) {
        if (a.isSome() != b.isSome()) {
            return a.isSome() ? 1 : -1;
        }
        if (a.isSome()) {
            return compare(Option.unwrap(a), Option.unwrap(b));
        }
        return 0;
    }

    public static int compareOptionalStaticArg(Option<StaticArg> a,
                                               Option<StaticArg> b) {
        if (a.isSome() != b.isSome()) {
            return a.isSome() ? 1 : -1;
        }
        if (a.isSome()) {
            return compare(Option.unwrap(a), Option.unwrap(b));
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
        new AnyListComparer<ExtentRange>(extentRangeComparer);

    static class KeywordTypeComparer implements Comparator<KeywordType> {
        public int compare(KeywordType left, KeywordType right) {
            return compare(left, right);
        }
    }
    final static KeywordTypeComparer keywordTypeComparer = new KeywordTypeComparer();
    public final static AnyListComparer<KeywordType> keywordTypeListComparer =
        new AnyListComparer<KeywordType>(keywordTypeComparer);

    static class ParamComparer implements Comparator<Param> {
        public int compare(Param left, Param right) {
            return compare(left, right);
        }
    }
    final static ParamComparer paramComparer = new ParamComparer();
    public final static AnyListComparer<Param> paramListComparer =
        new AnyListComparer<Param>(paramComparer);

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
        new AnyListComparer<StaticParam>(staticParamComparer);

    static class StaticArgComparer implements Comparator<StaticArg> {
        public int compare(StaticArg left, StaticArg right) {
            return compare(left, right);
        }
    }
    final static StaticArgComparer staticArgComparer = new StaticArgComparer();
    public final static AnyListComparer<StaticArg> staticArgListComparer =
        new AnyListComparer<StaticArg>(staticArgComparer);

    static class TraitTypeComparer implements Comparator<TraitType> {
        public int compare(TraitType left, TraitType right) {
            return compare(left, right);
        }
    }
    final static TraitTypeComparer traitTypeComparer = new TraitTypeComparer();
    public final static AnyListComparer<TraitType> traitTypeListComparer =
        new AnyListComparer<TraitType>(traitTypeComparer);

    static class TypeComparer implements Comparator<Type> {
        public int compare(Type left, Type right) {
            return compare(left, right);
        }
    }
    final static TypeComparer typeComparer = new TypeComparer();
    public final static AnyListComparer<Type> typeListComparer =
        new AnyListComparer<Type>(typeComparer);

    /* comparing lists ***************************************************/
    public static int compare(List<StaticParam> left, List<StaticParam> right) {
        return staticParamListComparer.compare(left, right);
    }

    /* compare methods ***************************************************/
    public static int compare(DottedId left, DottedId right) {
        return ListComparer.stringListComparer.compare(NodeUtil.toStrings(left),
                                                       NodeUtil.toStrings(right));
    }

    public static int compare(ExtentRange left, ExtentRange right) {
        // TODO Optional parameters on extent ranges are tricky things; perhaps
        // they need not both be present.
        int x = compareOptionalStaticArg(left.getBase(), right.getBase());
        if (x != 0) return x;
        x = compareOptionalStaticArg(left.getSize(), right.getSize());
        if (x != 0) return x;
        return 0;
    }

    public static int compare(FnAbsDeclOrDecl left, FnAbsDeclOrDecl right) {
        FnName fn0 = left.getFnName();
        FnName fn1 = right.getFnName();
        int x = NodeComparator.compare(fn0, fn1);
        if (x != 0)  return x;
        x = compare(left.getStaticParams(), right.getStaticParams());
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
            return left.stringName().compareTo(right.stringName());
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
        return compare(left.getId(), right.getId(),
                       left.getType(), right.getType());
    }

    public static int compare(Param left, Param right) {
        int x = left.getId().getName().compareTo(right.getId().getName());
        if (x != 0) return x;
        if ((left instanceof NormalParam) && (right instanceof NormalParam)) {
            x = compareOptionalType(((NormalParam)left).getType(), ((NormalParam)right).getType());
        }
        if ((left instanceof VarargsParam) && (right instanceof VarargsParam)) {
            x = compare(((VarargsParam)left).getVarargsType().getType(), ((VarargsParam)right).getVarargsType().getType());
        }
        if (x != 0) return x;
        // TODO default expr, mods, must enter into comparison also.
        return x;
    }

    public static int compare(StaticArg left, StaticArg right) {
        return compare((Type) left, right);
    }

    public static int compare(Type left, Type right) {
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
    static int subtypeCompareTo(ArrayType left, ArrayType right) {
        return compare(left.getElement(), right.getElement(),
                       left.getIndices(), right.getIndices());
    }

    static int subtypeCompareTo(ArrowType left, ArrowType right) {
        int x = compare(left.getRange(), right.getRange());
        if (x != 0) return x;
        x = compare(left.getDomain(), right.getDomain());
        if (x != 0) return x;
        if (left.getThrowsClause().isSome() != right.getThrowsClause().isSome())
            return left.getThrowsClause().isSome() ? 1 : -1;
        if (left.getThrowsClause().isSome())
            return traitTypeListComparer.compare(Option.unwrap(left.getThrowsClause()),
                                                 Option.unwrap(right.getThrowsClause()));
        return 0;
    }

    static int subtypeCompareTo(BaseNatStaticArg left, BaseNatStaticArg right) {
        return left.getValue() - right.getValue();
        /* nat types -- difference will not overflow */
    }

    static int subtypeCompareTo(ExponentStaticArg left, ExponentStaticArg right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
   }

    static int subtypeCompareTo(FixedDim left, FixedDim right) {
        return extentRangeListComparer
            .compare(left.getExtents(), right.getExtents());
    }

    static int subtypeCompareTo(IdType left, IdType right) {
        return compare(left.getDottedId(), right.getDottedId());
    }

    static int subtypeCompareTo(Indices left, Indices right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(MatrixType left, MatrixType right) {
        int y = compare(left.getElement(), right.getElement());
        if (y != 0) return y;
        return extentRangeListComparer.compare(left.getDimensions(),
                                               right.getDimensions());
    }

    static int subtypeCompareTo(BaseOprStaticArg left, BaseOprStaticArg right) {
        return compare(left.getFnName(), right.getFnName());
    }

    static int subtypeCompareTo(InstantiatedType left, InstantiatedType right) {
        int c = compare(left.getDottedId(), right.getDottedId());
        if (c != 0) return c;
        return staticArgListComparer.compare(left.getArgs(),
                                             right.getArgs());
    }

    static int subtypeCompareTo(ProductType left, ProductType right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(QuotientType left, QuotientType right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(InversionType left, InversionType right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(ChangeDimType left, ChangeDimType right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(OpDimType left, OpDimType right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
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
        List<TraitType> l = left.getExtendsClause();
        List<TraitType> ol = right.getExtendsClause();
        return traitTypeListComparer.compare(l, ol);
    }

    static int subtypeCompareTo(StaticParam left, StaticParam right) {
        return NodeUtil.getName(left).compareTo(NodeUtil.getName(right));
    }

    static int subtypeCompareTo(TupleType left, TupleType right) {
        // TODO: Handle TupleTypes with varargs and keywords
        return typeListComparer.compare(left.getElements(), right.getElements());
    }

    static int subtypeCompareTo(TypeArg left, TypeArg right) {
        return compare(left.getType(), right.getType());
    }
    static int subtypeCompareTo(Type left, Type right) {
        throw new InterpreterBug(left,
                                 "subtypeCompareTo(" + left.getClass() + " " +
                                 right.getClass() + ") is not implemented!");
    }

    static int subtypeCompareTo(VoidType left, VoidType right) {
        // All voids are equal
        return 0;
    }
}
