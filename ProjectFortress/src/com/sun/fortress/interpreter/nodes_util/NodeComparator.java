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

import java.util.List;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Voidoid;
import com.sun.fortress.interpreter.useful.ListComparer;

public class NodeComparator {

    public static ListComparer<StaticParam> staticParamListComparer =
        new ListComparer<StaticParam>();

    public static int compare(List<StaticParam> left, List<StaticParam> right) {
        return staticParamListComparer.compare(left, right);
    }

    public static int compare(FnDefOrDecl left, FnDefOrDecl right) {
        return left.compareTo(right);
    }

    public static int compare(Applicable left, Applicable right) {
        return left.applicableCompareTo(right);
    }

    public static int compare(FnName left, FnName right) {
        return left.compareTo(right);
    }
}

//    public static int compareTo(Id left, Id right) {
//        return left.getName().compareTo(right.getName());
//    }
//
//    public static int compareTo(FnName left, FnName right) {
//        Class leftClass = left.getClass();
//        Class rightClass = right.getClass();
//
//        if (leftClass != rightClass) {
//            return leftClass.getName().compareTo(rightClass.getName());
//        }
//        else {
//            return left.stringName().compareTo(right.stringName());
//        }
//    }

//    public static int compareTo(TypeRef left, TypeRef right) {
//        Class leftClass = left.getClass();
//        Class rightClass = right.getClass();
//
//        if (leftClass != rightClass) {
//            return leftClass.getName().compareTo(rightClass.getName());
//        }
//        else {
//            return left.subtypeCompareTo(right);
//        }
//    }

//   Copied code begins here.
//
//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        return elementType.compareTo(((SetType) o).elementType);
//    }

//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        return type.compareTo(((RestType) o).type);
//    }

//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        ParamType x = (ParamType) o;
//        int c = generic.compareTo(x.generic);
//        if (c != 0) {
//            return c;
//        }
//        return StaticArg.typeargListComparer.compare(args, x.args);
//    }

//    @Override
//    int subtypeCompareTo(TypeRef o) {
//        MatrixType x = (MatrixType) o;
//        int y = element.compareTo(x.element);
//        if (y != 0) return y;
//        return ExtentRange.listComparer.compare(dimensions, x.dimensions);
//    }

//    @Override
//    int subtypeCompareTo(TypeRef o) {
//        MapType x = (MapType) o;
//        return Useful.compare(key, x.key, value, x.value);
//    }

//
//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        ListType x = (ListType) o;
//        return element.compareTo(((ListType) o).element);
//    }


//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        return name.compareTo(((IdType) o).name);
//    }


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

//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        ProductDim x = (ProductDim) o;
//        return Useful.compare((TypeRef) multiplier, x.multiplier,
//                              (TypeRef) multiplicand, x.multiplicand); // cast for generics
//    }

//    @Override
//        int subtypeCompareTo(TypeRef o) {
//        return name.compareTo(((NameDim) o).name);
//    }

//       @Override
//    int subtypeCompareTo(TypeRef o) {
//        ExponentDim x = (ExponentDim) o;
//        return Useful.compare((TypeRef) power, x.power, (TypeRef) base, x.base); // casts
//                                                                                    // for
//                                                                                    // generics
//    }

//        @Override
//    int subtypeCompareTo(TypeRef o) {
//        ArrayType a = (ArrayType) o;
//        return Useful.compare(element, a.element, indices, a.indices);
//    }
//        @Override
//    int subtypeCompareTo(TypeRef o) {
//        ArrowType a = (ArrowType) o;
//        int x = range.compareTo(a.range);
//        if (x != 0) {
//            return x;
//        }
//        x = TypeRef.listComparer.compare(domain, a.domain);
//        if (x != 0) {
//            return x;
//        }
//        x = KeywordType.listComparer.compare(keywords, a.keywords);
//        if (x != 0) {
//            return x;
//        }
//        x = TypeRef.listComparer.compare(throws_, a.throws_);
//        return x;
//    }

//
//
//    /*
//     * (non-Javadoc)
//     *
//     * @see java.lang.Comparable#compareTo(java.lang.Object)
//     */
//    public int compareTo(StaticParam o) {
//        Class tclass = getClass();
//        Class oclass = o.getClass();
//        if (oclass != tclass) {
//            return tclass.getName().compareTo(oclass.getName());
//        }
//        return subtypeCompareTo(o);
//    }
//
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
//        /*
//     * (non-Javadoc)
//     *
//     * @see java.lang.Comparable#compareTo(java.lang.Object)
//     */
//    public int compareTo(Indices o) {
//        Class tclass = getClass();
//        Class oclass = o.getClass();
//        if (oclass != tclass) {
//            return tclass.getName().compareTo(oclass.getName());
//        }
//        return subtypeCompareTo(o);
//    }
//
//    public int compareTo(ExtentRange o) {
//        // TODO Optional parameters on extent ranges are tricky things; perhaps
//        // they need not both be present.
//        int x = TypeRef.compareOptional(base, o.base);
//        if (x != 0) {
//            return x;
//        }
//        x = TypeRef.compareOptional(size, o.size);
//        if (x != 0) {
//            return x;
//        }
//
//        return 0;
//    }
//
//    public int compareTo(DottedId other) {
//        return ListComparer.stringListComparer.compare(names, other.names);
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
