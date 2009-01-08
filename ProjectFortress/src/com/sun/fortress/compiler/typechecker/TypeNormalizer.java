/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.typechecker;
import com.sun.fortress.nodes.*;
import java.util.List;
import edu.rice.cs.plt.tuple.Option;

/**
 * This class is responsible for simplifying types, to aid
 * in error reporting, etc. 
 */
public class TypeNormalizer extends NodeUpdateVisitor { 
    public static Type normalize(Type t) { 
        return (Type)t.accept(new TypeNormalizer());
    }

    public Node forIntersectionTypeOnly(IntersectionType that, 
                                        TypeInfo info_result, 
                                        List<Type> elements_result) 
    {
        if (elements_result.size() == 1) { 
            return elements_result.get(0);
        } else { 
            return super.forIntersectionTypeOnly(that, 
                                                 info_result, 
                                                 elements_result);
        }
    }

    public Node forUnionTypeOnly(UnionType that, 
                                 TypeInfo info_result, 
                                 List<Type> elements_result) 
    {
        if (elements_result.size() == 1) { 
            return elements_result.get(0);
        } else { 
            return super.forUnionTypeOnly(that, 
                                          info_result, 
                                          elements_result);
        }
    }

    public Node forTupleTypeOnly(TupleType that, 
                                 TypeInfo info_result,
                                 List<Type> elements_result, 
                                 Option<Type> varargs_result, 
                                 List<KeywordType> keywords_result) 
    { 
        if (varargs_result.isNone() && 
            keywords_result.size() == 0 &&
            elements_result.size() == 1)
            { 
                return elements_result.get(0);
            }
        else { 
            return super.forTupleTypeOnly(that, info_result, elements_result,
                                          varargs_result, keywords_result);
        }
    }
}