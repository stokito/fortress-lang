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

package com.sun.fortress.nodes_util;

import java.io.File;
import java.util.*;
import java.math.BigInteger;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.lambda.Lambda2;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;

import static com.sun.fortress.interpreter.evaluator.ProgramError.error;

import com.sun.fortress.compiler.typechecker.TypeCheckerResult;
import com.sun.fortress.compiler.Types;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.parser_util.precedence_resolver.PrecedenceMap;
import com.sun.fortress.parser_util.FortressUtil;

import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;
import static edu.rice.cs.plt.tuple.Option.unwrap;
import static edu.rice.cs.plt.tuple.Option.wrap;

public class NodeFactory {
    /** Alternatively, you can invoke the AbsFnDecl constructor without a self name */
    public static AbsFnDecl makeAbsFnDecl(Span s, List<Modifier> mods,
            Option<Id> optSelfName, IdOrOpOrAnonymousName name,
            List<StaticParam> staticParams,
            List<Param> params,
            Option<Type> returnType,
            Option<List<BaseType>> throwss,
            WhereClause where,
            Contract contract) {
        String selfName;
        if (optSelfName.isSome()) {
            selfName = Option.unwrap(optSelfName).getText();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new AbsFnDecl(s, mods, name, staticParams, params, returnType,
                throwss, where, contract, selfName);
    }

    public static APIName makeAPIName(Id first, Id rest) {
        List<Id> ids = new ArrayList<Id>();
        ids.add(first);
        if (rest.getApi().isSome()) {
            ids.addAll(Option.unwrap(rest.getApi()).getIds());
        }
        ids.add(new Id(rest.getSpan(), rest.getText()));
        return new APIName(FortressUtil.spanTwo(first, rest), ids);
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api) {
        return new AliasedAPIName(api.getSpan(), api);
    }

    public static AliasedAPIName makeAliasedAPIName(APIName api, Id alias) {
        return new AliasedAPIName(FortressUtil.spanTwo(api, alias), api, Option.some(alias));
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span,
            IdOrOpOrAnonymousName name) {
        return new AliasedSimpleName(span, name, Option.<IdOrOpOrAnonymousName>none());
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span,
            IdOrOpOrAnonymousName name,
            Id alias) {
        return new AliasedSimpleName(span, name,
                Option.<IdOrOpOrAnonymousName>some(alias));
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span, Id id) {
        return new AliasedSimpleName(span, id, Option.<IdOrOpOrAnonymousName>none());
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span, Id id,
            Id alias) {
        return new AliasedSimpleName(span, id, Option.<IdOrOpOrAnonymousName>some(alias));
    }

    /** Alternatively, you can invoke the AbsFnDecl constructor without an alias */
    public static AliasedSimpleName makeAliasedSimpleName(Span span, OpName op) {
        return new AliasedSimpleName(span, op, Option.<IdOrOpOrAnonymousName>none());
    }

    public static AliasedSimpleName makeAliasedSimpleName(Span span, OpName op,
            OpName alias) {
        return new AliasedSimpleName(span, op, Option.<IdOrOpOrAnonymousName>some(alias));
    }

    public static ArrayType makeArrayType(Span span, Type element,
            Option<Indices> ind) {
        Indices indices = Option.unwrap(ind, new Indices(span,
                Collections.<ExtentRange>emptyList()));
        return new ArrayType(span, element, indices);
    }

    public static ExponentType makeExponentType(ExponentType t, Type s) {
        return new ExponentType(t.getSpan(), t.isParenthesized(), s,
                t.getPower());
    }

    public static ProductDim makeProductDim(ProductDim t, DimExpr s, DimExpr u) {
        return new ProductDim(t.getSpan(), t.isParenthesized(), s, u);
    }

    public static QuotientDim makeQuotientDim(QuotientDim t, DimExpr s, DimExpr u) {
        return new QuotientDim(t.getSpan(), t.isParenthesized(), s, u);
    }

    public static ExponentDim makeExponentDim(ExponentDim t, DimExpr s) {
        return new ExponentDim(t.getSpan(), t.isParenthesized(), s,
                t.getPower());
    }

    public static OpDim makeOpDim(OpDim t, DimExpr s) {
        return new OpDim(t.getSpan(), t.isParenthesized(), s, t.getOp());
    }

    public static ArrowType makeArrowType(ArrowType t, Type domain, Type range,
            Option<List<Type>> newThrows) {
        return new ArrowType(t.getSpan(), t.isParenthesized(), domain, range,
                newThrows, t.isIo());
    }

    public static InstantiatedType makeInstantiatedType(InstantiatedType t,
            List<StaticArg> args) {
        return new InstantiatedType(t.getSpan(), t.isParenthesized(),
                t.getName(), args);
    }

    public static InferenceVarType makeInferenceVarType() {
        return new InferenceVarType(new Object());
    }

    public static List<Type> makeInferenceVarTypes(int size) {
        List<Type> result = new ArrayList<Type>(size);
        for (int i = 0; i < size; i++) { result.add(makeInferenceVarType()); }
        return result;
    }

    public static TupleType makeTupleType(TupleType t, List<Type> tys) {
        return new TupleType(t.getSpan(), t.isParenthesized(), tys);
    }

    public static ArgType makeArgType(ArgType t, List<Type> tys,
            Option<VarargsType> varargs,
            List<KeywordType> keywords) {
        return new ArgType(t.getSpan(), t.isParenthesized(), tys, varargs,
                keywords);
    }

    public static VarargsType makeVarargsType(VarargsType t, Type s) {
        return new VarargsType(t.getSpan(), s);
    }

    public static KeywordType makeKeywordType(KeywordType t, Type s) {
        return new KeywordType(t.getSpan(), t.getName(), s);
    }

    public static AndType makeAndType(AndType t, Type first, Type second) {
        return new AndType(t.getSpan(), t.isParenthesized(), first, second);
    }

    public static OrType makeOrType(OrType t, Type first, Type second) {
        return new OrType(t.getSpan(), t.isParenthesized(), first, second);
    }

    public static TaggedDimType makeTaggedDimType(TaggedDimType t, Type s,
            DimExpr u) {
        return new TaggedDimType(t.getSpan(), t.isParenthesized(), s, u,
                t.getUnit());
    }

    public static TaggedUnitType makeTaggedUnitType(TaggedUnitType t, Type s) {
        return new TaggedUnitType(t.getSpan(), t.isParenthesized(), s,
                t.getUnit());
    }

    public static TypeArg makeTypeArg(TypeArg t, Type s) {
        return new TypeArg(t.getSpan(), s);
    }

    public static DimArg makeDimArg(DimArg t, DimExpr s) {
        return new DimArg(t.getSpan(), s);
    }

    public static DimArg makeDimArg(DimExpr s) {
        return new DimArg(s.getSpan(), s);
    }

    public static DimRef makeDimRef(String name) {
        return new DimRef(makeId(name));
    }

    public static UnitArg makeUnitArg(UnitExpr s) {
        return new UnitArg(s.getSpan(), s);
    }

    public static UnitRef makeUnitRef(String name) {
        return new UnitRef(makeId(name));
    }

    public static FixedPointType makeFixedPointType(FixedPointType t, Type s) {
        return new FixedPointType(t.getSpan(), t.isParenthesized(), t.getName(),
                s);
    }

    public static InstantiatedType makeInstantiatedType(Span span, boolean isParenthesized,
                                                        Id name, List<StaticArg> args) {
        return new InstantiatedType(span, isParenthesized, name, args);
    }

    public static InstantiatedType makeInstantiatedType(Span span, boolean isParenthesized,
            Id name, StaticArg... args) {
        return makeInstantiatedType(span, isParenthesized, name, Arrays.asList(args));
    }

    public static InstantiatedType makeInstantiatedType(Id name, StaticArg... args) {
        return makeInstantiatedType(new Span(), false, name, Arrays.asList(args));
    }

    /** Signature separates the first element in order to guarantee a non-empty arg list. */
    public static InstantiatedType makeInstantiatedType(String nameFirst, String... nameRest) {
        return makeInstantiatedType(new Span(), false, makeId(nameFirst, nameRest),
                Collections.<StaticArg>emptyList());
    }

    public static InstantiatedType makeInstantiatedType(String name,
            List<StaticArg> sargs) {
        return new InstantiatedType(new Span(),makeId(name),sargs);
    }

    public static InstantiatedType makeInstantiatedType(Id name,
                                                        List<StaticArg> sargs) {
        return new InstantiatedType(name.getSpan(), name, sargs);
    }

    public static InstantiatedType makeInstantiatedType(Id name) {
        return new InstantiatedType(name.getSpan(), name, Collections.<StaticArg>emptyList());
    }

    public static Type inArrowType(Type type) {
        if (type instanceof ArgType) {
            ArgType ty = (ArgType)type;
            return new ArgType(ty.getSpan(), ty.isParenthesized(),
                    ty.getElements(), ty.getVarargs(),
                    ty.getKeywords(), true);
        } else return type;
    }

    public static ArrowType makeArrowType(Span span, Type domain,
            Type range,
            Option<List<BaseType>> throws_) {
        Option<List<Type>> throwsAsTypeList =
            throws_.isSome() ?
                    Option.<List<Type>>some(new ArrayList<Type>(Option.unwrap(throws_))) :
                        Option.<List<Type>>none();
                    return new ArrowType(span, inArrowType(domain), range, throwsAsTypeList);
    }

    public static ArrowType makeArrowType(Span span, Type domain, Type range) {
        return new ArrowType(span, inArrowType(domain), range,
                Option.<List<Type>>none());
    }

    public static AbstractArrowType makeGenericArrowType(Span span,
            List<StaticParam> staticParams,
            Type domain,
            Type range,
            Option<List<BaseType>> throws_,
            WhereClause where) {
        if (staticParams.isEmpty() && where.getConstraints().isEmpty() && where.getBindings().isEmpty()) {
            return makeArrowType(span, domain, range, throws_);
        }
        Option<List<Type>> throwsAsTypeList =
            throws_.isSome() ?
                    Option.<List<Type>>some(new ArrayList<Type>(Option.unwrap(throws_))) :
                        Option.<List<Type>>none();
                    return new _RewriteGenericArrowType(span, inArrowType(domain), range,
                            throwsAsTypeList, staticParams, where);
    }

    public static AbstractArrowType makeGenericArrowType(
            Span span,
            List<StaticParam> staticParams,
            Type domain,
            Type range) {
        if (staticParams.isEmpty()) {
            return makeArrowType(span, domain, range, Option.<List<BaseType>>none());
        }
        return new _RewriteGenericArrowType(span, inArrowType(domain), range,
                Option.<List<Type>>none(), staticParams, new WhereClause());
    }

    public static KeywordType makeKeywordType(Id name, Type type) {
        return new KeywordType(new Span(), name, type);
    }

    public static ConstructorFnName makeConstructorFnName(GenericWithParams def) {
        return new ConstructorFnName(def.getSpan(), def);
    }

    /** Alternatively, you can invoke the Contract constructor without any parameters */
    public static Contract makeContract() {
        return new Contract(new Span(), Option.<List<Expr>>none(),
                Option.<List<EnsuresClause>>none(),
                Option.<List<Expr>>none());
    }

    public static APIName makeAPIName(Span span, String s) {
        return new APIName(span, Useful.list(new Id(span, s)));
    }

    public static APIName makeAPIName(Span span, Id s) {
        return new APIName(span, Useful.list(s));
    }

    public static APIName makeAPIName(Id s) {
        return new APIName(s.getSpan(), Useful.list(s));
    }

    private static List<Id> stringToIds(String path) {
        List<Id> ids = new ArrayList<Id>();

        StringTokenizer st = new StringTokenizer(path, ".");
        while (st.hasMoreTokens()) {
            String e = st.nextToken();
            ids.add(makeId(e));
        }
        return ids;
    }

    public static APIName makeAPIName(String s) {
        return makeAPIName(stringToIds(s));
    }

    public static APIName makeAPIName(Iterable<Id> ids) {
        return new APIName(FortressUtil.spanAll(ids), IterUtil.asList(ids));
    }

    public static APIName makeAPIName(Id id, Iterable<Id> ids) {
        return makeAPIName(IterUtil.asList(IterUtil.compose(id, ids)));
    }

    public static APIName makeAPIName(Span span, Iterable<Id> ids) {
        return new APIName(span, IterUtil.asList(ids));
    }

    /**
     * Create a APIName from the name of the file with the given path.
     */
    public static APIName makeAPIName(Span span, String path, String delimiter) {
        List<Id> ids = new ArrayList<Id>();
        String file = new File(path).getName();
        if (file.length() <= 4) {
            return error(new Id(span, "_"), "Invalid file name.");
        }
        else {
            for (String n : file.substring(0, file.length()-4).split(delimiter)) {
                ids.add(new Id(span, n));
            }
            return new APIName(span, ids);
        }
    }

    public static Id makeId(Span span, String s) {
        return new Id(span, Option.<APIName>none(), s);
    }

    public static Id makeId(Span span, Id id) {
        return new Id(span, id.getApi(), id.getText());
    }

    public static Id makeId(Iterable<Id> apiIds, Id id) {
        Span span;
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) {
            span = id.getSpan();
            api = Option.none();
        }
        else {
            APIName n = makeAPIName(apiIds);
            span = FortressUtil.spanTwo(n, id);
            api = Option.some(n);
        }
        return new Id(span, api, id.getText());
    }

    public static Id makeId(Span span, String api, String name) {
        List<Id> apis = new ArrayList<Id>();
        apis.add(makeId(span, api));
        return new Id(span, Option.some(new APIName(span, apis)), name);
    }

    public static Id makeId(Span span, Iterable<Id> apiIds, Id id) {
        Option<APIName> api;
        if (IterUtil.isEmpty(apiIds)) { api = Option.none(); }
        else { api = Option.some(makeAPIName(apiIds)); }
        return new Id(span, api, id.getText());
    }

    public static Id makeId(Span span, Id id, Iterable<Id> ids) {
        Option<APIName> api;
        Id last;
        if (IterUtil.isEmpty(ids)) { api = Option.none(); last = id; }
        else { api = Option.some(makeAPIName(id, IterUtil.skipLast(ids)));
            last = IterUtil.last(ids);
        }
        return new Id(span, api, last.getText());
    }

    public static Id makeId(Span span, APIName api, Id id) {
        return new Id(span, Option.some(api), id.getText());
    }

    /** Assumes {@code ids} is nonempty. */
    public static Id makeId(Iterable<Id> ids) {
        return makeId(IterUtil.skipLast(ids), IterUtil.last(ids));
    }

    public static Id makeId(String nameFirst, String... nameRest) {
        Iterable<Id> ids = IterUtil.compose(makeId(nameFirst),
                IterUtil.map(IterUtil.make(nameRest), STRING_TO_ID));
        return makeId(ids);
    }

    public static Id makeId(APIName api, Id name) {
        return new Id(FortressUtil.spanTwo(api, name), Option.some(api),
                      name.getText());
    }

    /**
     * Alternatively, you can invoke the FnDef constructor without a selfName
     */
    public static FnDef makeFnDecl(Span s, List<Modifier> mods,
            Option<Id> optSelfName, IdOrOpOrAnonymousName name,
            List<StaticParam> staticParams,
            List<Param> params,
            Option<Type> returnType,
            Option<List<BaseType>> throwss,
            WhereClause where, Contract contract,
            Expr body) {
        String selfName;
        if (optSelfName.isSome()) {
            selfName = Option.unwrap(optSelfName).getText();
        } else {
            selfName = WellKnownNames.defaultSelfName;
        }
        return new FnDef(s, mods, name, staticParams, params, returnType,
                throwss, where, contract, selfName, body);
    }

    public static Id makeId(String string) {
        return new Id(new Span(), string);
    }

    public static final Lambda<String, Id> STRING_TO_ID = new Lambda<String, Id>() {
        public Id value(String arg) { return makeId(arg); }
    };

    public static IdType makeIdType(String string) {
        return makeIdType(new Span(), makeId(string));
    }

    public static IdType makeIdType(Span span, Id id) {
        return new IdType(span, id);
    }

    public static LValueBind makeLValue(String name, String type) {
        return makeLValue(name, makeIdType(type));
    }

    public static LValueBind makeLValue(Id name, Id type) {
        return new LValueBind(new Span(name.getSpan(), type.getSpan()),
                name,
                Option.some((Type)makeIdType(type.getSpan(),type)),
                new ArrayList<Modifier>(),
                false);
    }

    public static LValueBind makeLValue(Id name, Type type) {
        return new LValueBind(new Span(name.getSpan(), type.getSpan()),
                name,
                Option.some(type),
                new ArrayList<Modifier>(),
                false);
    }

    public static LValueBind makeLValue(Id name, Option<Type> type) {
        return new LValueBind(name.getSpan(),
                name,
                type,
                new ArrayList<Modifier>(),
                false);
    }

    public static LValueBind makeLValue(String name, Type type) {
        return new LValueBind(type.getSpan(), makeId(name), Option.some(type),
                new ArrayList<Modifier>(), false);
    }

    public static LValueBind makeLValue(String name, Type type, List<Modifier> mods) {
        LValueBind result = makeLValue(name, type);
        return makeLValue(result, mods);
    }

    public static LValueBind makeLValue(LValueBind lvb, Id name) {
        return new LValueBind(lvb.getSpan(), name, lvb.getType(), lvb.getMods(),
                lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(), lvb.getType(),
                lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
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

    public static LValueBind makeLValue(LValueBind lvb, Type ty) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                Option.some(ty), lvb.getMods(),
                lvb.isMutable());
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
            boolean mutable) {
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                Option.some(ty), lvb.getMods(), mutable);
    }

    public static LValueBind makeLValue(LValueBind lvb, Type ty,
            List<Modifier> mods) {
        boolean mutable = lvb.isMutable();
        for (Modifier m : mods) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable)
                mutable = true;
        }
        return new LValueBind(lvb.getSpan(), lvb.getName(),
                Option.some(ty), mods, mutable);
    }

    public static LValueBind makeLValue(NormalParam param) {
        return new LValueBind(param.getSpan(), param.getName(),
                param.getType(), param.getMods(), false);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
            ExtentRange dimension) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        return new MatrixType(span, element, dims);
    }

    public static MatrixType makeMatrixType(Span span, Type element,
            ExtentRange dimension,
            List<ExtentRange> dimensions) {
        List<ExtentRange> dims = new ArrayList<ExtentRange>();
        dims.add(dimension);
        dims.addAll(dimensions);
        return new MatrixType(span, element, dims);
    }

    /** Alternatively, you can invoke the ObjectDecl constructor without a span */
    public static ObjectDecl makeObjectDecl(List<Decl> defs2,
            List<Modifier> mods,
            Id name,
            List<StaticParam> stParams,
            Option<List<Param>> params,
            List<TraitTypeWhere> traits,
            Option<List<BaseType>> throws_,
            WhereClause where,
            Contract contract) {
        return new ObjectDecl(new Span(), mods, name, stParams, traits, where,
                params, throws_, contract, defs2);
    }

    private static Option<Fixity> infix = Option.<Fixity>some(new InFixity());
    private static Option<Fixity> prefix = Option.<Fixity>some(new PreFixity());
    private static Option<Fixity> postfix = Option.<Fixity>some(new PostFixity());
    private static Option<Fixity> nofix = Option.<Fixity>some(new NoFixity());
    private static Option<Fixity> multifix = Option.<Fixity>some(new MultiFixity());
    private static Option<Fixity> enclosing = Option.<Fixity>some(new EnclosingFixity());
    private static Option<Fixity> big = Option.<Fixity>some(new BigFixity());
    private static Option<Fixity> unknownFix = Option.<Fixity>none();

    public static Op makeOp(String name) {
        return new Op(new Span(), PrecedenceMap.ONLY.canon(name), unknownFix);
    }

    public static Op makeOp(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), unknownFix);
    }

    public static Op makeOp(Span span, String name, Option<Fixity> fixity) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), fixity);
    }

    public static Op makeOp(Op op, String name) {
        return new Op(op.getSpan(), PrecedenceMap.ONLY.canon(name),
                op.getFixity());
    }

    public static Op makeOpInfix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), infix);
    }

    public static Op makeOpInfix(Op op) {
        return new Op(op.getSpan(), op.getText(), infix);
    }

    public static Op makeOpPrefix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), prefix);
    }

    public static Op makeOpPrefix(Op op) {
        return new Op(op.getSpan(), op.getText(), prefix);
    }

    public static Op makeOpPostfix(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), postfix);
    }

    public static Op makeOpPostfix(Op op) {
        return new Op(op.getSpan(), op.getText(), postfix);
    }

    public static Op makeOpNofix(Op op) {
        return new Op(op.getSpan(), op.getText(), nofix);
    }

    public static Op makeOpMultifix(Op op) {
        return new Op(op.getSpan(), op.getText(), multifix);
    }

    public static Op makeOpEnclosing(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), enclosing);
    }

    public static Op makeOpBig(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), big);
    }

    public static Op makeOpUnknown(Span span, String name) {
        return new Op(span, PrecedenceMap.ONLY.canon(name), unknownFix);
    }

    public static VarargsParam makeVarargsParam(Id name, VarargsType type) {
        return new VarargsParam(name.getSpan(), Collections.<Modifier>emptyList(), name, type);
    }

    public static VarargsParam makeVarargsParam(VarargsParam param, List<Modifier> mods) {
        return new VarargsParam(param.getSpan(), mods, param.getName(),
                param.getVarargsType());
    }

    public static VarargsParam makeVarargsParam(Span span, List<Modifier> mods,
            Id name, VarargsType type) {
        return new VarargsParam(span, mods, name, type);
    }

    public static NormalParam makeAbsParam(Type type) {
        Id id = new Id(type.getSpan(), "_");
        return new NormalParam(type.getSpan(), Collections.<Modifier>emptyList(),
                               id, Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Span span, List<Modifier> mods, Id name,
            Type type) {
        return new NormalParam(span, mods, name, Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Id id, Type type) {
        return new NormalParam(id.getSpan(), Collections.<Modifier>emptyList(),
                id, Option.some(type), Option.<Expr>none());
    }

    public static NormalParam makeParam(Id name) {
        return new NormalParam(name.getSpan(), Collections.<Modifier>emptyList(),
                name, Option.<Type>none(), Option.<Expr>none());
    }

    public static NormalParam makeParam(NormalParam param, Expr expr) {
        return new NormalParam(param.getSpan(), param.getMods(), param.getName(),
                param.getType(), Option.some(expr));
    }

    public static NormalParam makeParam(NormalParam param, List<Modifier> mods) {
        return new NormalParam(param.getSpan(), mods, param.getName(),
                param.getType(), param.getDefaultExpr());
    }

    public static TypeParam makeTypeParam(String name) {
        Span s = new Span();
        return new TypeParam(s, new Id(s, name),
                Collections.<BaseType>emptyList(), false);
    }

    public static TypeParam makeTypeParam(String name, String sup) {
        Span s = new Span();
        List<BaseType> supers = new ArrayList<BaseType>(1);
        supers.add(makeIdType(sup));
        return new TypeParam(s, new Id(s, name), supers);
    }

    public static OpParam makeOpParam(String name) {
        return new OpParam(new Span(), makeOp(name));
    }

    public static BoolParam makeBoolParam(String name) {
        Span s = new Span();
        return new BoolParam(s, new Id(s, name));
    }

    public static DimParam makeDimParam(String name) {
        Span s = new Span();
        return new DimParam(s, new Id(s, name));
    }

    public static UnitParam makeUnitParam(String name) {
        Span s = new Span();
        return new UnitParam(s, new Id(s, name));
    }

    public static IntParam makeIntParam(String name) {
        Span s = new Span();
        return new IntParam(s, new Id(s, name));
    }

    public static NatParam makeNatParam(String name) {
        Span s = new Span();
        return new NatParam(s, new Id(s, name));
    }

    /**
     * Alternatively, you can invoke the ArgType constructor without keywords
     */
    public static ArgType makeArgType(Span span, List<Type> elements,
            Option<VarargsType> varargs) {
        return new ArgType(span, elements, varargs,
                Collections.<KeywordType>emptyList());
    }

    public static ArgType makeArgType(List<Type> elements) {
        return new ArgType(new Span(), elements, Option.<VarargsType>none(),
                Collections.<KeywordType>emptyList());
    }

    public static ArgType makeArgType(Span span, List<Type> elements,
            List<KeywordType> keywordElements,
            Option<VarargsType> varargs)
    {
        return new ArgType(span, elements, varargs, keywordElements);
    }

    public static TupleType makeTupleType(List<Type> elements) {
        return new TupleType(new Span(), elements);
    }

    public static TupleType makeTupleType(Span span, List<Type> elements) {
        return new TupleType(span, elements);
    }

    public static TypeArg makeTypeArg(Type ty) {
        return new TypeArg(ty.getSpan(), ty);
    }

    public static TypeArg makeTypeArg(Span span, String string) {
        return new TypeArg(span, new IdType(span, makeId(span, string)));
    }

    public static TypeArg makeTypeArg(String string) {
        Span span = new Span();
        return new TypeArg(span,
                new IdType(span, makeId(span, string)));
    }

    public static BoolRef makeBoolRef(String string) {
        return new BoolRef(new Span(), makeId(string));
    }

    public static BoolArg makeBoolArg(String string) {
        return new BoolArg(new Span(), makeBoolRef(string));
    }

    public static IntRef makeIntRef(String string) {
        return new IntRef(new Span(), makeId(string));
    }

    public static IntVal makeIntVal(String i) {
        Span span = new Span();
        return new NumberConstraint(span, new IntLiteralExpr(span,
                new BigInteger(i)));
    }

    public static IntArg makeIntArg(String string) {
        return new IntArg(new Span(), makeIntRef(string));
    }

    public static IntArg makeIntArgVal(String i) {
        return new IntArg(new Span(), makeIntVal(i));
    }

    public static OpArg makeOpArg(String string) {
        return new OpArg(new Span(), makeOp(string));
    }

    public static VarDecl makeVarDecl(Span span, Id name, Expr init) {
        LValueBind bind = new LValueBind(span, name, Option.<Type>none(),
                Collections.<Modifier>emptyList(), true);
        return new VarDecl(span, Useful.<LValueBind>list(bind), init);
    }

    public static VarDecl makeVarDecl(Span span, String name, Expr init) {
        LValueBind bind = new LValueBind(span, new Id(span, name),
                Option.<Type>none(),
                Collections.<Modifier>emptyList(), true);
        return new VarDecl(span, Useful.<LValueBind>list(bind), init);
    }

    public static BoolExpr makeInParentheses(BoolExpr be) {
        return be.accept(new NodeAbstractVisitor<BoolExpr>() {
            public BoolExpr forBoolConstant(BoolConstant b) {
                return new BoolConstant(b.getSpan(), true, b.isBool());
            }
            public BoolExpr forBoolRef(BoolRef b) {
                return new BoolRef(b.getSpan(), true, b.getName());
            }
            public BoolExpr forNotConstraint(NotConstraint b) {
                return new NotConstraint(b.getSpan(), true, b.getBool());
            }
            public BoolExpr forOrConstraint(OrConstraint b) {
                return new OrConstraint(b.getSpan(), true, b.getLeft(),
                        b.getRight());
            }
            public BoolExpr forAndConstraint(AndConstraint b) {
                return new AndConstraint(b.getSpan(), true, b.getLeft(),
                        b.getRight());
            }
            public BoolExpr forImpliesConstraint(ImpliesConstraint b) {
                return new ImpliesConstraint(b.getSpan(), true, b.getLeft(),
                        b.getRight());
            }
            public BoolExpr forBEConstraint(BEConstraint b) {
                return new BEConstraint(b.getSpan(), true, b.getLeft(),
                        b.getRight());
            }
            public BoolExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of BoolExpr.");
            }
        });
    }

    public static DimExpr makeInParentheses(DimExpr dim) {
        return dim.accept(new NodeAbstractVisitor<DimExpr>() {
            public DimExpr forBaseDim(BaseDim t) {
                return new BaseDim(t.getSpan(), true);
            }
            public DimExpr forDimRef(DimRef t) {
                return new DimRef(t.getSpan(), true, t.getName());
            }
            public DimExpr forProductDim(ProductDim t) {
                return new ProductDim(t.getSpan(), true, t.getMultiplier(),
                        t.getMultiplicand());
            }
            public DimExpr forQuotientDim(QuotientDim t) {
                return new QuotientDim(t.getSpan(), true, t.getNumerator(),
                        t.getDenominator());
            }
            public DimExpr forExponentDim(ExponentDim t) {
                return new ExponentDim(t.getSpan(), true, t.getBase(),
                        t.getPower());
            }
            public DimExpr forOpDim(OpDim t) {
                return new OpDim(t.getSpan(), true, t.getVal(), t.getOp());
            }
            public DimExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of DimExpr.");
            }
        });
    }

    public static IntExpr makeInParentheses(IntExpr ie) {
        return ie.accept(new NodeAbstractVisitor<IntExpr>() {
            public IntExpr forNumberConstraint(NumberConstraint i) {
                return new NumberConstraint(i.getSpan(), true, i.getVal());
            }
            public IntExpr forIntRef(IntRef i) {
                return new IntRef(i.getSpan(), true, i.getName());
            }
            public IntExpr forSumConstraint(SumConstraint i) {
                return new SumConstraint(i.getSpan(), true, i.getLeft(),
                        i.getRight());
            }
            public IntExpr forMinusConstraint(MinusConstraint i) {
                return new MinusConstraint(i.getSpan(), true, i.getLeft(),
                        i.getRight());
            }
            public IntExpr forProductConstraint(ProductConstraint i) {
                return new ProductConstraint(i.getSpan(), true, i.getLeft(),
                        i.getRight());
            }
            public IntExpr forExponentConstraint(ExponentConstraint i) {
                return new ExponentConstraint(i.getSpan(), true, i.getLeft(),
                        i.getRight());
            }
            public IntExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of IntExpr.");
            }
        });
    }

    public static UnitExpr makeInParentheses(UnitExpr be) {
        return be.accept(new NodeAbstractVisitor<UnitExpr>() {
            public UnitExpr forUnitRef(UnitRef b) {
                return new UnitRef(b.getSpan(), true, b.getName());
            }
            public UnitExpr forProductUnit(ProductUnit i) {
                return new ProductUnit(i.getSpan(), true, i.getLeft(),
                                       i.getRight());
            }
            public UnitExpr forQuotientUnit(QuotientUnit t) {
                return new QuotientUnit(t.getSpan(), true, t.getLeft(),
                                        t.getRight());
            }
            public UnitExpr forExponentUnit(ExponentUnit i) {
                return new ExponentUnit(i.getSpan(), true, i.getLeft(),
                                        i.getRight());
            }
            public UnitExpr defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                           " is not a subtype of UnitExpr.");
            }
        });
    }

    public static Type makeInParentheses(Type ty) {
        return ty.accept(new NodeAbstractVisitor<Type>() {
            public Type forArrowType(ArrowType t) {
                return new ArrowType(t.getSpan(), true, t.getDomain(),
                        t.getRange(), t.getThrowsClause(), t.isIo());
            }
            public Type forArrayType(ArrayType t) {
                return new ArrayType(t.getSpan(), true, t.getElement(),
                        t.getIndices());
            }
            public Type forIdType(IdType t) {
                return new IdType(t.getSpan(), true, t.getName());
            }
            public Type forMatrixType(MatrixType t) {
                return new MatrixType(t.getSpan(), true, t.getElement(),
                        t.getDimensions());
            }
            public Type forInstantiatedType(InstantiatedType t) {
                return new InstantiatedType(t.getSpan(), true, t.getName(),
                        t.getArgs());
            }
            public Type forArgType(ArgType t) {
                return new ArgType(t.getSpan(), true, t.getElements(),
                        t.getVarargs(), t.getKeywords(), t.isInArrow());
            }
            public Type forTupleType(TupleType t) {
                return new TupleType(t.getSpan(), true, t.getElements());
            }
            public Type forVoidType(VoidType t) {
                return new VoidType(t.getSpan(), true);
            }
            public Type forTaggedDimType(TaggedDimType t) {
                return new TaggedDimType(t.getSpan(), true, t.getType(),
                        t.getDim(), t.getUnit());
            }
            public Type forTaggedUnitType(TaggedUnitType t) {
                return new TaggedUnitType(t.getSpan(), true, t.getType(),
                        t.getUnit());
            }
            public Type forDimExpr(DimExpr t) {
                return makeInParentheses(t);
            }
            public Type defaultCase(Node x) {
                return bug(x, "makeInParentheses: " + x.getClass() +
                " is not a subtype of Type.");
            }
        });
    }

    public static OpArg makeInParentheses(OpArg arg) {
        return new OpArg(arg.getSpan(), true, arg.getName());
    }

    public static SyntaxDef makeSyntaxDef(Span s, List<SyntaxSymbol> syntaxSymbols, TransformationDecl transformation) {
        return new SyntaxDef(s, syntaxSymbols, transformation);
    }

    public static IntLiteralExpr makeIntLiteralExpr(int i) {
        return new IntLiteralExpr(BigInteger.valueOf(i));
    }

    public static Expr makeIntLiteralExpr(long i) {
        return new IntLiteralExpr(BigInteger.valueOf(i));
    }

    public static StringLiteralExpr makeStringLiteralExpr(String s) {
        return new StringLiteralExpr(s);
    }

    public static CharLiteralExpr makeCharLiteralExpr(char c) {
        return new CharLiteralExpr(""+c);
    }

    public static VoidLiteralExpr makeVoidLiteralExpr() {
        return new VoidLiteralExpr();
    }

    public static Import makeImportStar(APIName api, List<IdOrOpOrAnonymousName> excepts) {
        return new ImportStar(api, excepts);
    }

    public static FnRef makeFnRef(Span span, Id name) {
        List<Id> ids = new LinkedList<Id>();
        ids.add(name);
        return new FnRef(span, ids);
    }

    public static TightJuxt makeTightJuxt(Span span, List<Expr> exprs) {
        return new TightJuxt(span, exprs);
    }

    public static VarRef makeVarRef(Span span, Id name) {
        return new VarRef(span, true, name);
    }

    public static Expr makeFnRef(Span span, Id name,
            List<StaticArg> staticArgs) {
        List<Id> ids = new LinkedList<Id>();
        ids.add(name);
        return new FnRef(span, ids, staticArgs);
    }

    public static OpName makeEncloserOpName(Span span) {
        Op open = NodeFactory.makeOpEnclosing(span, "<|");
        Op close = NodeFactory.makeOpEnclosing(span, "|>");
        return new Enclosing(FortressUtil.spanTwo(open,close), open,close);
    }

    public static NamedType makeNamedType(APIName api, NamedType type) {
        if (type instanceof IdType) {
            return new IdType(type.getSpan(),
                              type.isParenthesized(),
                              makeId(api, type.getName()));
        } else { // type instanceof InstantiatedType
            InstantiatedType _type = (InstantiatedType)type;
            return new InstantiatedType(_type.getSpan(),
                                        _type.isParenthesized(),
                                        makeId(api, _type.getName()),
                                        _type.getArgs());
        }
    }

    public static _RewriteGenericSingletonType makeGenericSingletonType(Id name, List<StaticParam> params) {
        return new _RewriteGenericSingletonType(name.getSpan(), name, params);
    }



    public static Type makeAndType(List<Type> types) {
        if (types.isEmpty()) {
            return Types.ANY;
        } else if (types.size() == 1) {
            return types.get(0);
        } else {
            return IterUtil.fold(IterUtil.skipFirst(types), types.get(0),
                    new Lambda2<Type, Type, Type>() {
                        public Type value(Type arg0, Type arg1) {
                            return new AndType(arg0, arg1);
                        }
            });
        }
    }

    public static Type makeOrType(Iterable<Type> types) {
        if (IterUtil.isEmpty(types)) {
            return Types.BOTTOM;
        } else if (IterUtil.sizeOf(types) == 1) {
            return IterUtil.first(types);
        } else {
            return IterUtil.fold(IterUtil.skipFirst(types), IterUtil.first(types),
                    new Lambda2<Type, Type, Type>() {
                        public Type value(Type arg0, Type arg1) {
                            return new OrType(arg0, arg1);
                        }
            });
        }
    }

    public static ChainExpr makeChainExpr(Expr lhs, Op op, Expr rhs) {
        List<Pair<Op, Expr>> links = new ArrayList<Pair<Op, Expr>>(1);
        links.add(Pair.make(op, rhs));
        return new ChainExpr(new Span(lhs.getSpan(), rhs.getSpan()), lhs, links);
    }

 public static Import makeImportStar(String apiName) {
  return NodeFactory.makeImportStar(NodeFactory.makeAPIName(apiName), new LinkedList<IdOrOpOrAnonymousName>());
 }

    public static Decl makeFnDecl(String functionName, Id typeName, Expr expression) {
        Id fnName = new Id(functionName);
        List<Param> params = new LinkedList<Param>();
        List<StaticArg> staticArgs = new LinkedList<StaticArg>();
        Type type = new InstantiatedType(typeName, staticArgs);
        params.add(new VarargsParam(new Id("args"), new VarargsType(type)));
        Type returnType = new InstantiatedType(typeName , staticArgs);
        return new FnDef(fnName, params, Option.some(returnType), expression);
    }
}
