/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.typechecker;

import static com.sun.fortress.compiler.Types.BOTTOM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

import junit.framework.TestCase;

import com.sun.fortress.compiler.index.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.Span;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

/**
 *
 */
public abstract class TypeCheckerTestCase extends TestCase {

    private static Span span = NodeFactory.makeSpan("TypeCheckerTestCase bogus");

    public static Type parseType(String s) {
        s = s.trim();
        if (s.contains("->")) {
            int arrowIndex = s.indexOf("->");
            Type left = parseType(s.substring(0, arrowIndex));
            Type right = parseType(s.substring(arrowIndex+2));
            return NodeFactory.makeArrowType(span, left, right);
        }
        if (s.startsWith("(")) {
            List<Type> types = new ArrayList<Type>();
            s = s.substring(1, s.length()-1);
            while(s.contains(",")) {
                int commaIndex = s.indexOf(",");
                types.add(parseType(s.substring(0, commaIndex)));
                s = s.substring(commaIndex+1);
            }
            types.add(parseType(s));
            return NodeFactory.makeTupleType(span, types);
        }
        else {
            return NodeFactory.makeVarType(span, s);
        }
    }

    public static ProperTraitIndex makeTrait(String name,
                                              List<StaticParam> sparams,
                                              String... supers) {
        List<TraitTypeWhere> extendsClause =
            new ArrayList<TraitTypeWhere>(supers.length);
        for (String sup : supers) {
            BaseType supT = (BaseType) parseType(sup);
            extendsClause.add(NodeFactory.makeTraitTypeWhere(span, supT, Option.<WhereClause>none()));
        }
        TraitDecl ast = NodeFactory.makeTraitDecl(span, Modifiers.None, NodeFactory.makeId(span, name),
                                                  sparams, Option.<List<Param>>none(),
                                                  extendsClause, Option.<WhereClause>none(),
                                                  Collections.<Decl>emptyList(),
                                                  Collections.<BaseType>emptyList(),
                                                  Option.<List<NamedType>>none(), false,
                                                  Option.<SelfType>none());
        return new ProperTraitIndex(ast,
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Coercion>emptySet(),
                                    CollectUtil.<IdOrOpOrAnonymousName, DeclaredMethod>emptyRelation(),
                                    CollectUtil.<IdOrOpOrAnonymousName, FunctionalMethod>emptyRelation());
    }

    public static List<StaticParam> makeSparams(StaticParam... params) {
        List<StaticParam> sparams = new ArrayList<StaticParam>(params.length);
        for (StaticParam p : params) {
            sparams.add(p);
        }
        return sparams;
    }

    public static List<StaticArg> makeSargs(StaticArg... args) {
        List<StaticArg> sargs = new ArrayList<StaticArg>(args.length);
        for (StaticArg p : args) {
            sargs.add(p);
        }
        return sargs;
    }

    public static StaticParam makeStaticParam(String param) {
        param = param.trim();
        if (param.indexOf(" ") >= 0) {
            String[] tokens = param.split(" +");
            if (tokens[0].equals("nat")) return NodeFactory.makeNatParam(span, tokens[1]);
            else if (tokens[0].equals("int")) return NodeFactory.makeIntParam(span, tokens[1]);
            else if (tokens[0].equals("bool")) return NodeFactory.makeBoolParam(span, tokens[1]);
            else if (tokens[0].equals("opr")) return NodeFactory.makeOpParam(span, tokens[1]);
            else if (tokens[0].equals("dim")) return NodeFactory.makeDimParam(span, tokens[1]);
            else if (tokens[0].equals("unit")) return NodeFactory.makeUnitParam(span, tokens[1]);
        }
        return NodeFactory.makeTypeParam(span, param);
    }
}
