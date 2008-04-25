/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

import static com.sun.fortress.compiler.typechecker.Types.BOTTOM;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.*;

import junit.framework.TestCase;

import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.ProperTraitIndex;
import com.sun.fortress.nodes.ArgType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.SimpleName;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.nodes._RewriteGenericArrowType;
import com.sun.fortress.nodes_util.NodeFactory;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.tuple.Option;

/**
 *
 */
public abstract class TypeCheckerTestCase extends TestCase {

    public static Type parseType(String s) {
        s = s.trim();
        if (s.contains("->")) {
            int arrowIndex = s.indexOf("->");
            Type left = parseType(s.substring(0, arrowIndex));
            Type right = parseType(s.substring(arrowIndex+2));
            if (left instanceof TupleType) {
                left = NodeFactory.makeArgType(((TupleType)left).getElements());
            } else {
                left = NodeFactory.makeArgType(Collections.singletonList(left));
            }
            List<Type> thrown = Collections.singletonList(BOTTOM);
            return new ArrowType(left, right, Option.some(thrown), false);
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
            return NodeFactory.makeTupleType(types);
        }
        if ("+-/*^!&".indexOf(s) >= 0) {
            return NodeFactory.makeOprArg(s);
        }
        else {
            return NodeFactory.makeIdType(s);
        }
    }

    public static Boolean sub(SubtypeChecker ta, String s, String t) {
        return ta.subtype(parseType(s), parseType(t));
    }

    public static Boolean sub(SubtypeChecker ta, String s, Type t) {
        return ta.subtype(parseType(s), t);
    }

    public static Boolean sub(SubtypeChecker ta, Type s, String t) {
        return ta.subtype(s, parseType(t));
    }

    public static Boolean sub(SubtypeChecker ta, Type s, Type t) {
        return ta.subtype(s, t);
    }

    public static ProperTraitIndex makeTrait(String name,
                                              List<StaticParam> sparams,
                                              String... supers) {
        List<TraitTypeWhere> extendsClause =
            new ArrayList<TraitTypeWhere>(supers.length);
        for (String sup : supers) {
            TraitType supT = (TraitType) parseType(sup);
            extendsClause.add(new TraitTypeWhere(supT, new WhereClause()));
        }
        TraitAbsDeclOrDecl ast = new TraitDecl(NodeFactory.makeId(name), sparams,
                                               extendsClause,
                                               Collections.<Decl>emptyList());
        return new ProperTraitIndex(ast,
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Id, Method>emptyMap(),
                                    Collections.<Function>emptySet(),
                                    CollectUtil.<SimpleName, Method>emptyRelation(),
                                    CollectUtil.<SimpleName, FunctionalMethod>emptyRelation());
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
            if (tokens[0].equals("nat")) return NodeFactory.makeNatParam(tokens[1]);
            else if (tokens[0].equals("int")) return NodeFactory.makeIntParam(tokens[1]);
            else if (tokens[0].equals("bool")) return NodeFactory.makeBoolParam(tokens[1]);
            else if (tokens[0].equals("opr")) return NodeFactory.makeOprParam(tokens[1]);
            else if (tokens[0].equals("dim")) return NodeFactory.makeDimParam(tokens[1]);
            else if (tokens[0].equals("unit")) return NodeFactory.makeUnitParam(tokens[1]);
        }
        return NodeFactory.makeTypeParam(param);
    }
}
