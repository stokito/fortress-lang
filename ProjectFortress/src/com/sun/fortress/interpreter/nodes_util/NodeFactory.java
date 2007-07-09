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

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.math.BigInteger;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Fn;
import com.sun.fortress.interpreter.useful.Option;
import com.sun.fortress.interpreter.useful.Some;
import com.sun.fortress.interpreter.useful.None;
import com.sun.fortress.interpreter.useful.Useful;
import com.sun.fortress.interpreter.useful.HasAt;
import com.sun.fortress.interpreter.parser.precedence.resolver.PrecedenceMap;

public class NodeFactory {
    public static AliasedName makeAliasedName(Span span, Id id) {
        return new AliasedName(span, makeName(id.getSpan(), id),
                               new None<FnName>());
    }

    public static AliasedName makeAliasedName(Span span, Id id, DottedId alias) {
        return new AliasedName(span, makeName(id.getSpan(), id),
                               new Some<FnName>(alias));
    }

    public static AliasedName makeAliasedName(Span span, OprName op) {
        return new AliasedName(span, op, new None<FnName>());
    }

    public static AliasedName makeAliasedName(Span span, OprName op, OprName alias) {
        return new AliasedName(span, op, new Some<FnName>(alias));
    }


    /******************************************************
    private static int anonymousFnNameSequence;
    static synchronized int anonymousFnNameSerial() {
        return ++anonymousFnNameSequence;
    }

    public static AnonymousFnName makeAnonymousFnName(Span span) {
        return new AnonymousFnName(span, anonymousFnNameSerial(), new None<HasAt>());
    }

    public static AnonymousFnName makeAnonymousFnName(HasAt at) {
        return new AnonymousFnName(new Span(), anonymousFnNameSerial(),
                                   new Some<HasAt>(at));
    }
    ******************************************************/

    public static ArrayType makeArrayType(Span span, TypeRef element,
                                          Option<FixedDim> ind) {
        FixedDim indices;
        if (ind.isPresent()) indices = (FixedDim)((Some)ind).getVal();
        else indices = new FixedDim(span, Collections.<ExtentRange>emptyList());
        return new ArrayType(span, element, indices);
    }

    public static BaseNatRef makeBaseNatRef(Span span, IntLiteral value) {
        return new BaseNatRef(span, value.getVal().intValue());
    }

    public static BaseOprRef makeBaseOprRef(Span span, Op op) {
        return new BaseOprRef(span, new Opr(span, op));
    }

    public static CharLiteral makeCharLiteral(Span span, String s) {
        return new CharLiteral(span, s, s.charAt(0));
    }

    /******************************************************
    private static int constructorFnNameSequence;
    private static int constructorFnNameSerial() {
        return ++constructorFnNameSequence;
    }
    public static ConstructorFnName makeConstructorFnName(DefOrDecl def) {
        return new ConstructorFnName(def.getSpan(), constructorFnNameSerial(), def);
    }
    ******************************************************/

    public static Contract makeContract() {
        return new Contract(new Span(), Collections.<Expr> emptyList(),
                            Collections.<EnsuresClause> emptyList(),
                            Collections.<Expr> emptyList());
    }

    public static DottedId makeDottedId(Span span, String s) {
        return new DottedId(span, Useful.list(s));
    }

    public static DottedId makeDottedId(Span span, Id s) {
        return new DottedId(span, Useful.list(s.getName()));
    }

    public static DottedId makeDottedId(Span span, Id s, List<Id> ls) {
        return new DottedId(span, Useful.prependMapped(s, ls,
                                                // fn(x) => x.getName()
                                                new Fn<Id, String>() {
                                                    @Override
                                                    public String apply(Id x) {
                                                        return x.getName();
                                                    }
        }));
    }

    public static FloatLiteral makeFloatLiteral(Span span, String s) {
        BigInteger intPart;
        BigInteger numerator;
        int denomBase;
        int denomPower;

        // Trim leading zeroes
        while (s.length() > 1 && s.charAt(0) == '0') {
            s = s.substring(1);
        }

        int dotLoc = s.indexOf('.');
        int underLoc = s.indexOf('_');

        if (dotLoc == -1) {
            // No fraction part.
            numerator = BigInteger.ZERO;
            denomBase = 1;
            denomPower = 0;
            int base;
            String digits;

            if (underLoc == -1) {
                digits = s;
                base = 10;
            } else {
                digits = s.substring(0, underLoc);
                // Base other, no ".", parse as BigInteger and convert.
                String base_digits = s.substring(underLoc + 1);

                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            digits = dozenalHack(digits, base);
            intPart = new BigInteger(digits, base);

        } else {
            // There is a fraction part.

            int base;

            if (underLoc == -1) {
                base = 10;
                underLoc = s.length();
            } else {
                String base_digits = s.substring(underLoc + 1);
                if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                    base = Unicode.numberToValue(base_digits);
                } else {
                    base = Integer.parseInt(base_digits);
                }
            }
            {
                String digits = s.substring(0, dotLoc);
                if (digits.length() > 0) {
                    digits = dozenalHack(digits, base);
                    intPart = new BigInteger(digits, base);
                } else {
                    intPart = BigInteger.ZERO;
                }

                digits = s.substring(dotLoc + 1, underLoc);

                // TODO Getting the rounding and overflow dead right is hard.
                while (digits.length() > 1 && digits.endsWith("0")) {
                    digits = digits.substring(0, digits.length() - 1);
                }

                if (digits.length() == 0 || "0".equals(digits)) {
                    numerator = BigInteger.ZERO;
                    denomBase = 1;
                    denomPower = 0;

                } else {
                    digits = dozenalHack(digits, base);
                    numerator = new BigInteger(digits, base);
                    denomBase = base;
                    denomPower = digits.length();
                }
            }
        }
        return new FloatLiteral(span, s,
                                intPart, numerator, denomBase, denomPower);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params, Expr body) {
        return makeFnExpr(span, params, new None<TypeRef>(),
                          Collections.<TypeRef>emptyList(), body);
    }

    public static FnExpr makeFnExpr(Span span, List<Param> params,
                                    Option<TypeRef> returnType,
                                    List<TypeRef> throwsClause, Expr body) {
        return new FnExpr(span, new AnonymousFnName(span),
                          new None<List<StaticParam>>(), params, returnType,
                          Collections.<WhereClause>emptyList(), throwsClause,
                          body);
    }

    public static Fun makeFun(Span span, String string) {
        return new Fun(span, new Id(span, string));
    }

    /**
     * Call this only for names that have no location. (When/if this constructor
     * disappears, it will be because we have a better plan for those names, and
     * its disappearance will identify all those places that need updating).
     */
    public static Fun makeFun(String string) {
        Span span = new Span();
        return new Fun(span, new Id(span, string));
    }

    public static Id makeId(String string) {
        return new Id(new Span(), string);
    }

    public static IdType makeIdType(Span span, Id id) {
        return new IdType(span, makeDottedId(span, id));
    }

    public static IntLiteral makeIntLiteral(Span span, BigInteger val) {
        return new IntLiteral(span, val.toString(), val);
    }

    public static IntLiteral makeIntLiteral(Span span, String s) {
        BigInteger val;
        int underLoc = s.indexOf('_');
        if (underLoc == -1) {
            val = new BigInteger(s);
        } else {
            String digits = s.substring(0, underLoc);
            String base_digits = s.substring(underLoc + 1);
            int base;
            if (!Unicode.charactersOverlap(base_digits, "0123456789")) {
                base = Unicode.numberToValue(base_digits);
            } else {
                base = Integer.parseInt(base_digits);
            }
            digits = dozenalHack(digits, base);
            val = new BigInteger(digits, base);
        }
        return new IntLiteral(span, s, val);
    }

   static String dozenalHack(String digits, int base) {
        if (base == 12 && Unicode.charactersOverlap(digits, "xXeE")) {
            digits = digits.replace('x', 'A');
            digits = digits.replace('X', 'A');
            digits = digits.replace('e', 'B');
            digits = digits.replace('E', 'B');
        }
        return digits;
   }

    public static LValueBind makeLValue(LValueBind lvb, Id id) {
        return new LValueBind(lvb.getSpan(), id, lvb.getType(), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof Modifier.Var || m instanceof Modifier.Settable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods,
                                            boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                              mods, mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), lvb.getMods(),
                              lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, TypeRef ty,
                                        List<Modifier> mods) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                              new Some<TypeRef>(ty), mods, lvb.isMutable());
    }

    public static LetExpr makeLetExpr(LetExpr expr, List<Expr> body) {
        if (expr instanceof GeneratedExpr) {
            GeneratedExpr exp = (GeneratedExpr) expr;
            return new GeneratedExpr(exp.getSpan(), body, exp.getExpr(),
                                     exp.getGens());
        } else if (expr instanceof LetFn) {
            return new LetFn(expr.getSpan(), body, ((LetFn)expr).getFns());
        } else if (expr instanceof LocalVarDecl) {
            LocalVarDecl exp = (LocalVarDecl) expr;
            return new LocalVarDecl(exp.getSpan(), body, exp.getLhs(),
                                    exp.getRhs());
        } else {
            throw new Error(expr.getClass() + " is not a subtype of LetExpr.");
        }
    }

    public static MatrixType makeMatrixType(Span span, TypeRef element,
                                            ExtentRange dimension,
                                            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        return new MatrixType(span, element, dims);
    }

    public static Name makeName(Span span, Id id) {
        return new Name(span, new Some<Id>(id), new None<Op>());
    }

    public static Name makeName(Span span, Op op) {
        return new Name(span, new None<Id>(), new Some<Op>(op));
    }

    public static NatParam makeNatParam(String name) {
        return new NatParam(new Span(), new Id(new Span(), name));
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name));
    }

    public static OprExpr makeOprExpr(Span span, OprName op) {
        return new OprExpr(span, op, new ArrayList<Expr>());
    }

    public static OprExpr makeOprExpr(Span span, OprName op, Expr arg) {
        List<Expr> es = new ArrayList<Expr>();
        es.add(arg);
        return new OprExpr(span, op, es);
    }

    public static OprExpr makeOprExpr(Span span, OprName op, Expr first,
                                      Expr second) {
        List<Expr> es = new ArrayList<Expr>();
        es.add(first);
        es.add(second);
        return new OprExpr(span, op, es);
    }

    public static Param makeParam(Span span, List<Modifier> mods, Id name,
                                  TypeRef type) {
        return new Param(span, mods, name, new Some<TypeRef>(type),
                         new None<Expr>());
    }

    public static Param makeParam(Id name, TypeRef type) {
        return new Param(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         new Some<TypeRef>(type), new None<Expr>());
    }

    public static Param makeParam(Id name) {
        return new Param(name.getSpan(), Collections.<Modifier>emptyList(), name,
                         new None<TypeRef>(), new None<Expr>());
    }

    public static Param makeParam(Param param, Expr expr) {
        return new Param(param.getSpan(), param.getMods(), param.getName(),
                         param.getType(), new Some<Expr>(expr));
    }

    public static Param makeParam(Param param, List<Modifier> mods) {
        return new Param(param.getSpan(), mods, param.getName(),
                         param.getType(), param.getDefaultExpr());
    }

    /*
    public static QuotientStaticArg makeQuotientStaticArg(Span span,
                                                          StaticArg numerator,
                                                          StaticArg denominator) {
        return new QuotientStaticArg(span, ...);
    }
    */

    public static SimpleTypeParam makeSimpleTypeParam(String name) {
        return new SimpleTypeParam(new Span(), new Id(new Span(), name),
                                   new None<List<TypeRef>>(), false);
    }

    public static SubscriptExpr makeSubscriptExpr(Span span, Expr obj,
                                                  List<Expr> subs) {
        return new SubscriptExpr(span, obj, subs, None.<Enclosing>make());
    }

    public static TightJuxt makeTightJuxt(Span span, Expr first, Expr second) {
        return new TightJuxt(span, Useful.list(first, second));
    }

    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeDottedId(span, string)));
    }

    public static VarDecl makeVarDecl(Span span, Id id, Expr init) {
        return new VarDecl(span, Useful.<LValue>list(
                                new LValueBind(span, id,
                                               new None<TypeRef>(),
                                               Collections.<Modifier>emptyList(),
                                               true)),
                           init);
    }

    public static VarRefExpr makeVarRefExpr(Span span, String s) {
        return new VarRefExpr(span, new Id(span, s));
    }

    public static VoidLiteral makeVoidLiteral(Span span) {
        return new VoidLiteral(span, "");
    }
}
