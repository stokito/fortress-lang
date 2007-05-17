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

/*
 * Utility functions for the Fortress com.sun.fortress.interpreter.parser.
 */

package com.sun.fortress.interpreter.parser;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.fortress.interpreter.evaluator.ProgramError;
import com.sun.fortress.interpreter.nodes.*;
import com.sun.fortress.interpreter.useful.Cons;
import com.sun.fortress.interpreter.useful.Pair;
import com.sun.fortress.interpreter.useful.PureList;

public final class FortressUtil {
    public static void println(String arg) {
        System.out.println(arg);
    }

    public static Contract emptyContract() {
        return new Contract(new Span(), emptyExprs(), emptyEnsuresClauses(),
                            emptyExprs());
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

    public static List<FnDefOrDecl> emptyFnDefOrDecls() {
        return Collections.<FnDefOrDecl>emptyList();
    }

    public static List<Modifier> emptyModifiers() {
        return Collections.<Modifier>emptyList();
    }

    public static List<Param> emptyParams() {
        return Collections.<Param>emptyList();
    }

    public static List<StaticParam> emptyStaticParams() {
        return Collections.<StaticParam>emptyList();
    }

    public static List<TypeRef> emptyTypeRefs() {
        return Collections.<TypeRef>emptyList();
    }

    public static List<WhereClause> emptyWhereClauses() {
        return Collections.<WhereClause>emptyList();
    }

    public static <T> List<T> getListVal(Option<List<T>> o) {
        if (o.isPresent()) return (List<T>)o.getVal();
        else               return Collections.<T>emptyList();
    }

    public static <T> List<T> mkList(T first) {
        List<T> l = new ArrayList<T>();
        l.add(first);
        return l;
    }

    public static <T> List<T> mkList(List<T> all) {
        List<T> l = new ArrayList<T>();
        l.addAll(all);
        return l;
    }

    public static <T> List<T> mkList(T first, T second) {
        List<T> l = new ArrayList<T>();
        l.add(first);
        l.add(second);
        return l;
    }

    public static <T> List<T> mkList(T first, List<T> rest) {
        List<T> l = new ArrayList<T>();
        l.add(first);
        l.addAll(rest);
        return l;
    }

    public static <T> List<T> mkList(List<T> rest, T last) {
        List<T> l = new ArrayList<T>();
        l.addAll(rest);
        l.add(last);
        return l;
    }

    private static void multiple(Modifier m) {
        throw new ProgramError(m, "A modifier must not occur multiple times");
    }
    public static void noDuplicate(List<Modifier> mods) {
        boolean m_atomic   = false;
        boolean m_getter   = false;
        boolean m_hidden   = false;
        boolean m_io       = false;
        boolean m_private  = false;
        boolean m_settable = false;
        boolean m_setter   = false;
        boolean m_test     = false;
        boolean m_value    = false;
        boolean m_var      = false;
        boolean m_wrapped  = false;
        for (Modifier m : mods) {
	    if (m instanceof Modifier.Atomic) {
                if (m_atomic) multiple(m);
                else m_atomic = true;
            } else if (m instanceof Modifier.Getter) {
                if (m_getter) multiple(m);
                else m_getter = true;
            } else if (m instanceof Modifier.Hidden) {
                if (m_hidden) multiple(m);
                else m_hidden = true;
            } else if (m instanceof Modifier.IO) {
                if (m_io) multiple(m);
                else m_io = true;
            } else if (m instanceof Modifier.Private) {
                if (m_private) multiple(m);
                else m_private = true;
            } else if (m instanceof Modifier.Settable) {
                if (m_settable) multiple(m);
                else m_settable = true;
            } else if (m instanceof Modifier.Setter) {
                if (m_setter) multiple(m);
                else m_setter = true;
            } else if (m instanceof Modifier.Test) {
                if (m_test) multiple(m);
                else m_test = true;
            } else if (m instanceof Modifier.Value) {
                if (m_value) multiple(m);
                else m_value = true;
            } else if (m instanceof Modifier.Var) {
                if (m_var) multiple(m);
                else m_var = true;
            } else if (m instanceof Modifier.Wrapped) {
                if (m_wrapped) multiple(m);
                else m_wrapped = true;
            }
        }
    }

    private static boolean compoundOp(String s) {
        return (s.length() > 1 && s.endsWith("=")
                && !s.equals("<=") && !s.equals(">=")
                && !s.equals("=/=") && !s.equals("==="));
    }
    private static boolean validOpChar(char c) {
        return (c == '_' || Character.isUpperCase(c));
    }
    public static boolean validOp(String s) {
        int length = s.length();
        if (length < 2 || compoundOp(s)) return false;
        char start = s.charAt(0);
        if (length == 2 && start == s.charAt(1)) return false;
        if (start == '_' || s.endsWith("_"))     return false;
        for (int i = 0; i < length; i++) {
            if (!validOpChar(s.charAt(i))) return false;
        }
        return true;
    }

    public static boolean getMutable(List<Modifier> mods) {
        for (Modifier m : mods) {
            if (m instanceof Modifier.Var || m instanceof Modifier.Settable)
                return true;
        }
        return false;
    }

    public static void setMutable(List<LValue> vars) {
        for (LValue l : vars) {
            if (l instanceof LValueBind) ((LValueBind)l).setMutable();
            else throw new ProgramError(l, "Unpasting cannot be mutable.");
        }
    }

    public static void setMutable(List<LValue> vars, Span span) {
        for (LValue l : vars) {
           if (l instanceof LValueBind) {
               List<Modifier> mods = new ArrayList<Modifier>();
               mods.add(new Modifier.Var(span));
               ((LValueBind)l).setMods(mods);
           } else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static void setType(List<LValue> vars, TypeRef ty) {
        for (LValue l : vars) {
            if (l instanceof LValueBind) ((LValueBind)l).setType(ty);
            else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static void setType(List<LValue> vars, List<TypeRef> tys) {
        int ind = 0;
        for (LValue l : vars) {
            if (l instanceof LValueBind) {
                ((LValueBind)l).setType(tys.get(ind));
                ind += 1;
            } else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static void setMutableAndType(List<LValue> vars, TypeRef ty) {
        for (LValue l : vars) {
            if (l instanceof LValueBind) {
                LValueBind lv = (LValueBind)l;
                lv.setMutable();
                lv.setType(ty);
            } else throw new ProgramError(l, "Unpasting cannot be mutable.");
        }
    }

    public static void setMutableAndType(List<LValue> vars, Span span,
                                         TypeRef ty) {
        for (LValue l : vars) {
           if (l instanceof LValueBind) {
               List<Modifier> mods = new ArrayList<Modifier>();
               mods.add(new Modifier.Var(span));
               LValueBind lv = (LValueBind)l;
               lv.setMods(mods);
               lv.setType(ty);
           } else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static void setMutableAndType(List<LValue> vars, List<TypeRef> tys) {
        int ind = 0;
        for (LValue l : vars) {
            if (l instanceof LValueBind) {
                LValueBind lv = (LValueBind)l;
                lv.setMutable();
                lv.setType(tys.get(ind));
                ind += 1;
            } else throw new ProgramError(l, "Unpasting cannot be mutable.");
        }
    }

    public static void setMutableAndType(List<LValue> vars, Span span,
                                         List<TypeRef> tys) {
        int ind = 0;
        for (LValue l : vars) {
            if (l instanceof LValueBind) {
               List<Modifier> mods = new ArrayList<Modifier>();
               mods.add(new Modifier.Var(span));
               LValueBind lv = (LValueBind)l;
               lv.setMods(mods);
               lv.setType(tys.get(ind));
               ind += 1;
            } else throw new ProgramError(l, "Unpasting cannot be mutable.");
        }
    }

    public static void setMods(List<LValue> vars, List<Modifier> mods) {
        for (LValue l : vars) {
            if (l instanceof LValueBind) ((LValueBind)l).setMods(mods);
            else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static void setModsAndMutable(List<LValue> vars, List<Modifier> mods) {
        for (LValue l : vars) {
            if (l instanceof LValueBind) {
                LValueBind lv = (LValueBind)l;
                lv.setMods(mods);
                lv.setMutable();
           }
            else throw new ProgramError(l, "LValueBind is expected.");
        }
    }

    public static List<LValue> ids2Lvs(List<Id> ids, List<Modifier> mods,
                                       Option<TypeRef> ty, boolean mutable) {
        List<LValue> lvs = new ArrayList<LValue>();
        for (Id id : ids) {
            lvs.add(new LValueBind(id.getSpan(), id, ty, mods, mutable));
        }
        return lvs;
    }

    public static List<LValue> ids2Lvs(List<Id> ids, List<Modifier> mods,
                                       TypeRef ty, boolean mutable) {
        return ids2Lvs(ids, mods, Some.<TypeRef>make(ty), mutable);
    }

    public static List<LValue> ids2Lvs(List<Id> ids, TypeRef ty,
                                       boolean mutable) {
        return ids2Lvs(ids, FortressUtil.emptyModifiers(), Some.<TypeRef>make(ty),
                mutable);
    }

    public static List<LValue> ids2Lvs(List<Id> ids, List<Modifier> mods) {
        return ids2Lvs(ids, mods, None.<TypeRef>make(), false);
    }

    public static List<LValue> ids2Lvs(List<Id> ids) {
        return ids2Lvs(ids, FortressUtil.emptyModifiers(), None.<TypeRef>make(), false);
    }

    public static List<LValue> ids2Lvs(List<Id> ids, List<Modifier> mods,
                                       List<TypeRef> tys, boolean mutable) {
        List<LValue> lvs = new ArrayList<LValue>();
        int ind = 0;
        for (Id id : ids) {
            lvs.add(new LValueBind(id.getSpan(), id,
                                   Some.<TypeRef>make(tys.get(ind)),
                                   mods, mutable));
            ind += 1;
        }
        return lvs;
    }

    public static List<LValue> ids2Lvs(List<Id> ids, List<TypeRef> tys,
                                       boolean mutable) {
        return ids2Lvs(ids, FortressUtil.emptyModifiers(), tys, mutable);
    }

    public static AbsFnDecl mkAbsFnDecl(Span span, List<Modifier> mods,
                                        Option<Id> receiver,
                                        FnHeaderFront fhf, FnHeaderClause fhc) {
        List<TypeRef> throws_ = FortressUtil.getListVal(fhc.getThrowsClause());
        List<WhereClause> where_ = FortressUtil.getListVal(fhc.getWhereClause());
        Contract contract;
        if (fhc.getContractClause().isPresent())
             contract = (Contract)fhc.getContractClause().getVal();
        else contract = FortressUtil.emptyContract();
        return new AbsFnDecl(span, mods, receiver, fhf.getName(),
                             Some.<StaticParam>makeSomeListOrNone(fhf.getStaticParams()),
                             fhf.getParams(), fhc.getReturnType(), throws_,
                             where_, contract);
    }

    public static AbsFnDecl mkAbsFnDecl(Span span, List<Modifier> mods,
                                        FnHeaderFront fhf, FnHeaderClause fhc) {
        return mkAbsFnDecl(span, mods, None.<Id>make(), fhf, fhc);
    }

    public static AbsFnDecl mkAbsFnDecl(Span span, List<Modifier> mods,
                                        FnName name, List<StaticParam> sparams,
                                        List<Param> params,
                                        FnHeaderClause fhc) {
        List<TypeRef> throws_ = FortressUtil.getListVal(fhc.getThrowsClause());
        List<WhereClause> where_ = FortressUtil.getListVal(fhc.getWhereClause());
        Contract contract;
        if (fhc.getContractClause().isPresent())
             contract = (Contract)fhc.getContractClause().getVal();
        else contract = FortressUtil.emptyContract();
        return new AbsFnDecl(span, mods, None.<Id>make(), name,
                             Some.<StaticParam>makeSomeListOrNone(sparams),
                             params, None.<TypeRef>make(), throws_,
                             where_, contract);
    }

    public static AbsFnDecl mkAbsFnDecl(Span span, List<Modifier> mods,
                                        FnName name, List<Param> params,
                                        TypeRef ty) {
        return new AbsFnDecl(span, mods, None.<Id>make(), name,
                             Some.<StaticParam>makeSomeListOrNone(
                                               FortressUtil.emptyStaticParams()),
                             params, Some.<TypeRef>make(ty),
                             FortressUtil.emptyTypeRefs(),
                             FortressUtil.emptyWhereClauses(),
                             FortressUtil.emptyContract());
    }

    public static FnDecl mkFnDecl(Span span, List<Modifier> mods,
                                  Option<Id> receiver, FnHeaderFront fhf,
                                  FnHeaderClause fhc, Expr expr) {
        List<TypeRef> throws_ = FortressUtil.getListVal(fhc.getThrowsClause());
        List<WhereClause> where_ = FortressUtil.getListVal(fhc.getWhereClause());
        Contract contract;
        if (fhc.getContractClause().isPresent())
             contract = (Contract)fhc.getContractClause().getVal();
        else contract = FortressUtil.emptyContract();
        return new FnDecl(span, mods, receiver, fhf.getName(),
                          Some.<StaticParam>makeSomeListOrNone(fhf.getStaticParams()),
                          fhf.getParams(), fhc.getReturnType(), throws_, where_,
                          contract, expr);
    }

    public static FnDecl mkFnDecl(Span span, List<Modifier> mods, FnName name,
                                  List<StaticParam> sparams, List<Param> params,
                                  FnHeaderClause fhc, Expr expr) {
        List<TypeRef> throws_ = FortressUtil.getListVal(fhc.getThrowsClause());
        List<WhereClause> where_ = FortressUtil.getListVal(fhc.getWhereClause());
        Contract contract;
        if (fhc.getContractClause().isPresent())
             contract = (Contract)fhc.getContractClause().getVal();
        else contract = FortressUtil.emptyContract();
        return new FnDecl(span, mods, None.<Id>make(), name,
                          Some.<StaticParam>makeSomeListOrNone(sparams),
                          params, None.<TypeRef>make(), throws_, where_,
                          contract, expr);
    }

    public static FnDecl mkFnDecl(Span span, List<Modifier> mods,
                                  FnHeaderFront fhf, FnHeaderClause fhc,
                                  Expr expr) {
        return mkFnDecl(span, mods, None.<Id>make(), fhf, fhc, expr);
    }

    public static LocalVarDecl mkLocalVarDecl(Span span, List<LValue> lvs,
                                              Option<Expr> expr) {
        return new LocalVarDecl(span, lvs, expr, emptyExprs());
    }
    public static LocalVarDecl mkLocalVarDecl(Span span, List<LValue> lvs,
                                              Expr expr) {
        return new LocalVarDecl(span, lvs, Some.<Expr>make(expr), emptyExprs());
    }
    public static LocalVarDecl mkLocalVarDecl(Span span, List<LValue> lvs) {
        return new LocalVarDecl(span, lvs, None.<Expr>make(), emptyExprs());
    }

    public static LValueBind mkLValueBind(Span span, Id id, TypeRef ty) {
        return new LValueBind(span, id, Some.<TypeRef>make(ty), emptyModifiers(),
                              false);
    }
    public static LValueBind mkLValueBind(Span span, Id id) {
        return new LValueBind(span, id, None.<TypeRef>make(), emptyModifiers(),
                              false);
    }
    public static LValueBind mkLValueBind(Id id, TypeRef ty,
                                          List<Modifier> mods) {
        return new LValueBind(id.getSpan(), id, Some.<TypeRef>make(ty), mods,
                              getMutable(mods));
    }
    public static LValueBind mkLValueBind(Id id, TypeRef ty) {
        return mkLValueBind(id, ty, emptyModifiers());
    }

// let rec multi_dim_cons (expr : expr)
//                        (dim : int)
//                        (multi : multi_dim_expr) : multi_dim_expr =
//   let elem = multi_dim_elem expr in
//   let span = span_two expr multi in
//     match multi.node_data with
//       | `MultiDimElement _ ->
//           multi_dim_row span dim [ elem; multi ]
//       | `MultiDimRow
//           { node_span = row_span;
//             node_data =
//               { multi_dim_row_dimension = row_dim;
//                 multi_dim_row_elements = elements; } } ->
//           if dim = row_dim
//           then multi_dim_row span dim (elem :: elements)
//           else if dim > row_dim
//           then multi_dim_row span dim [ elem; multi ]
//           else
//             (match elements with
//                | [] -> Errors.internal_error row_span
//                    "empty array/matrix literal"
//                | first::rest ->
//                    multi_dim_row span row_dim
//                      (multi_dim_cons expr dim first :: rest))
    private static MultiDim multiDimElement(Expr expr) {
        return new MultiDimElement(expr.getSpan(), expr);
    }
    private static MultiDimRow addOneMultiDim(MultiDim multi, int dim,
                                              Expr expr){
        Span span = spanTwo(multi, expr);
        MultiDim elem = multiDimElement(expr);
        if (multi instanceof MultiDimElement) {
            List<MultiDim> elems = new ArrayList<MultiDim>();
            elems.add(multi);
            elems.add(elem);
            return new MultiDimRow(span, dim, elems);
        } else if (multi instanceof MultiDimRow) {
            MultiDimRow m = (MultiDimRow)multi;
            int _dim = m.getDimension();
            List<MultiDim> elements = m.getElements();
            if (dim == _dim) {
                elements.add(elem);
                return new MultiDimRow(span, dim, elements);
            } else if (dim > _dim) {
                List<MultiDim> elems = new ArrayList<MultiDim>();
                elems.add(multi);
                elems.add(elem);
                return new MultiDimRow(span, dim, elems);
            } else if (elements.size() == 0) {
                throw new ProgramError(multi, "Empty array/matrix literal.");
            } else { // if (dim < _dim)
                int index = elements.size()-1;
                MultiDim last = elements.get(index);
                elements.set(index, addOneMultiDim(last, dim, expr));
                return new MultiDimRow(span, _dim, elements);
            }
        } else {
            throw new ProgramError(multi,
                                   "MultiDimElement or MulDimRow is expected.");
        }
    }
    public static MultiDim multiDimCons(Expr init,
                                        List<Pair<Integer,Expr>> rest) {
        MultiDim _init = multiDimElement(init);
        if (rest.isEmpty()) {
            throw new ProgramError(init, "multiDimCons: empty rest");
        } else {
            Pair<Integer,Expr> pair = rest.get(0);
            Expr expr = pair.getB();
            List<MultiDim> elems = new ArrayList<MultiDim>();
            elems.add(_init);
            elems.add(multiDimElement(expr));
            MultiDimRow result = new MultiDimRow(spanTwo(_init,expr),
                                                 pair.getA(), elems);
            for (Pair<Integer,Expr> _pair : rest.subList(1, rest.size())) {
                int _dim   = _pair.getA();
                Expr _expr = _pair.getB();
                Span span = spanTwo(result, _expr);
                result = addOneMultiDim(result, _dim, _expr);
            }
            return result;
        }
    }

// let rec unpasting_cons (span : span)
//                        (one : unpasting)
//                        (sep : int)
//                        (two : unpasting) : unpasting =
//   match two.node_data with
//     | `UnpastingBind _ | `UnpastingNest _ -> unpasting_split span sep [one;two]
//     | `UnpastingSplit split ->
//         (match split.node_data with
//            | { unpasting_split_elems = (head :: tail) as elems;
//                unpasting_split_dim = dim; } ->
//                if sep > dim then unpasting_split span sep [one;two]
//                else if sep < dim then
//                  unpasting_split span dim
//                    (unpasting_cons (span_two one head) one sep head :: tail)
//                else (* sep = dim *)
//                  unpasting_split span dim (one :: elems)
//            | _ -> Errors.internal_error span "Empty unpasting.")
    public static Unpasting unpastingCons(Span span, Unpasting one, int sep,
                                          Unpasting two) {
        List<Unpasting> onetwo = new ArrayList<Unpasting>();
        onetwo.add(one);
        onetwo.add(two);
        if (two instanceof UnpastingBind) {
            return new UnpastingSplit(span, onetwo, sep);
        } else if (two instanceof UnpastingSplit) {
            UnpastingSplit split = (UnpastingSplit)two;
            List<Unpasting> elems = split.getElems();
            if (elems.size() != 0) {
                int dim = split.getDim();
                if (sep > dim) {
                    return new UnpastingSplit(span, onetwo, sep);
                } else if (sep < dim) {
                    Unpasting head = elems.get(0);
                    elems.set(0, unpastingCons(spanTwo(one,head),one,sep,head));
                    return new UnpastingSplit(span, elems, dim);
                } else { // sep = dim
                    elems.add(0, one);
                    return new UnpastingSplit(span, elems, dim);
                }
            } else { // elems.size() == 0
                throw new ProgramError(two, "Empty unpasting.");
            }
        } else { //    !(two instanceof UnpastingBind)
                 // && !(two instanceof UnpastingSplit)
            throw new ProgramError(two,
                                   "UnpastingBind or UnpastingSplit expected.");
        }
    }

// let join (one : span) (two : span) : span =
//   match one, two with
//     | None, span | span, None -> span
//     | Some (left,_), Some (_,right) -> Some (left,right)

// let span_two (one : 'a node) (two : 'b node) : span =
//   join one.node_span two.node_span

    public static Span spanTwo(Node s1, Node s2) {
        return new Span(s1.getSpan().getBegin(), s2.getSpan().getEnd());
    }

// let rec span_all (com.sun.fortress.interpreter.nodes : 'a node list) : span =
//   match com.sun.fortress.interpreter.nodes with
//     | [] -> None
//     | node :: rest -> join node.node_span (span_all rest)
    public static Span spanAll(Object[] nodes, int size) {
        if (size == 0) return new Span();
        else { // size != 0
            return new Span(((Node)Array.get(nodes,0)).getSpan().getBegin(),
                            ((Node)Array.get(nodes,size-1)).getSpan().getEnd());
        }
    }

// let build_block (exprs : expr list) : expr list =
//   List_aux.foldr
//     (fun e es ->
//        match e.node_data with
//          | `LetExpr (le,[]) -> [{ e with node_data = `LetExpr (le, es) }]
//          | `LetExpr _ -> raise (Failure "misparsed variable introduction!")
//          | _ -> e :: es)
//     exprs
//     []
//
// let do_block (body : expr list) : expr =
//   let span = span_all body in
//     node span (`FlowExpr (node span (`BlockExpr (build_block body))))
    public static Block doBlock(List<Expr> exprs) {
        Span span = spanAll(exprs.toArray(new Node[0]), exprs.size());
        List<Expr> es = new ArrayList<Expr>();
        Collections.reverse(exprs);
        for (Expr e : exprs) {
            if (e instanceof LetExpr) {
                LetExpr _e = (LetExpr)e;
                if (_e.getBody().isEmpty()) {
                    _e.setBody(es);
                    es = new ArrayList<Expr>();
                    es.add(_e);
                } else {
                    throw new ProgramError(e,"Misparsed variable introduction!");
                }
            } else {
                es.add(0, e);
            }
        }
        return new Block(span, es);
    }

// (* Turn an expr list into a single TightJuxt *)
// let build_primary (p : expr list) : expr =
//   match p with
//     | [e] -> e
//     | _ ->
//         let es = List.rev p in
//           Node.node (span_all es) (`TightJuxt es)
    public static Expr buildPrimary(PureList<Expr> exprs) {
        if (exprs.size() == 1) return ((Cons<Expr>)exprs).getFirst();
        else {
            exprs = exprs.reverse();
            List<Expr> javaList = exprs.toJavaList();
            return new TightJuxt(spanAll(javaList.toArray(new Node[0]),
                                         javaList.size()), javaList);
        }
    }
}
