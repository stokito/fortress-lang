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

/*
 * Utility functions for the Fortress com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;

import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.useful.Cons;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.PureList;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.exceptions.ProgramError;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;

public final class FortressUtil {
    public static void log(BufferedWriter writer, Span span, String msg) {
        try {
            writer.write( span + "\n    " + msg + "\n" );
        } catch (IOException error) {
            error("Writing to a log file for the parser failed!");
        }
    }

    public static void println(String arg) {
        System.out.println(arg);
    }

    public static List<Decl> emptyDecls() {
        return Collections.<Decl>emptyList();
    }

    public static List<EnsuresClause> emptyEnsuresClauses() {
        return Collections.<EnsuresClause>emptyList();
    }

    public static List<Expr> emptyExprs() {
        return Collections.<Expr>emptyList();
    }

    public static List<Param> emptyParams() {
        return Collections.<Param>emptyList();
    }

    public static List<StaticParam> emptyStaticParams() {
        return Collections.<StaticParam>emptyList();
    }

    public static List<BaseType> emptyTraitTypes() {
        return Collections.<BaseType>emptyList();
    }

    public static List<TraitTypeWhere> emptyTraitTypeWheres() {
        return Collections.<TraitTypeWhere>emptyList();
    }

    public static List<Type> emptyTypes() {
        return Collections.<Type>emptyList();
    }

    private static Effect effect = NodeFactory.makeEffect(NodeFactory.makeSpan("singleton"));

    public static Effect emptyEffect() {
        return effect;
    }

    public static <T> List<T> getListVal(Option<List<T>> o) {
        return o.unwrap(Collections.<T>emptyList());
    }

    public static <U, T extends U> List<U> mkList(T first) {
        List<U> l = new ArrayList<U>();
        l.add(first);
        return l;
    }

    public static <U, T extends U> List<U> mkList(List<T> all) {
        List<U> l = new ArrayList<U>();
        l.addAll(all);
        return l;
    }

    public static <U, T extends U> List<U> mkList(T first, T second) {
        List<U> l = new ArrayList<U>();
        l.add(first);
        l.add(second);
        return l;
    }

    public static <U, T extends U> List<U> mkList(U first, List<T> rest) {
        List<U> l = new ArrayList<U>();
        l.add(first);
        l.addAll(rest);
        return l;
    }

    public static <U, T extends U> List<U> mkList(List<T> rest, U last) {
        List<U> l = new ArrayList<U>();
        l.addAll(rest);
        l.add(last);
        return l;
    }

    public static List<Type> toTypeList(List<BaseType> tys) {
        List<Type> result = new ArrayList<Type>();
        for (BaseType ty : tys) {
            result.add((Type)ty);
        }
        return result;
    }

    public static Option<List<Type>> toTypeList(Option<List<BaseType>> tys) {
        return tys.apply(new OptionVisitor<List<BaseType>, Option<List<Type>>>() {
            public Option<List<Type>> forSome(List<BaseType> l) {
                return Option.<List<Type>>some(new ArrayList<Type>(l));
            }
            public Option<List<Type>> forNone() { return Option.<List<Type>>none(); }
        });
    }

}
