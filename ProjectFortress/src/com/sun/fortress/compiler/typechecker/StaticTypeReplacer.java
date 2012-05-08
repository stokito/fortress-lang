/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.compiler.typechecker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.fortress.exceptions.InterpreterBug;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.scala_src.types.TypeAnalyzer;
import com.sun.fortress.scala_src.useful.STypesUtil;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.NI;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

import static com.sun.fortress.nodes_util.NodeFactory.*;
import static edu.rice.cs.plt.tuple.Option.*;

/**
 * This class will replace all occurrences of a static parameter in a given
 * type with its instantiated static argument. A StaticTypeReplacer is created with
 * some static parameters and corresponding static arguments. Each type P_i in
 * the parameters will be replaced with the corresponding type A_i in the
 * arguments, for every occurrence of P_i in some outer type T.
 *
 * For example, for static parameters [\U, V\], static arguments
 * [\ZZ32, String\], and outer type Pair<V,U>, the type replacer will return
 * type Pair<String, ZZ32>.
 */
public class StaticTypeReplacer extends NodeUpdateVisitor {

    private final static Comparator<IdOrOpOrAnonymousName> IOOOANcomparator = new
        Comparator<IdOrOpOrAnonymousName>() {

            @Override
            public int compare(IdOrOpOrAnonymousName arg0,
                    IdOrOpOrAnonymousName arg1) {
                return NodeComparator.compare(arg0, arg1);
            }
        
    };
    
    /** Map parameter name to the static argument bound to it. */
    private final Map<IdOrOpOrAnonymousName, StaticArg> parameterMap;

    boolean replaceStaticParams = false; // NOT THREAD SAFE
    
    public String toString() {
        return parameterMap.toString();
    }
    
    /** Assume params.size() == args.size() */
    public StaticTypeReplacer(List<StaticParam> params, List<StaticArg> args) {
        if (params.size() != args.size())
	    InterpreterBug.bug("Number of args does not equal number of parameters in StaticTypeReplacer");
        int n = params.size();
        // parameterMap = new HashMap<IdOrOpOrAnonymousName, StaticArg>(n);
        parameterMap = new BATree<IdOrOpOrAnonymousName, StaticArg>(IOOOANcomparator);
        for (int i=0; i<n; ++i) {
            parameterMap.put(params.get(i).getName(), args.get(i));
        }
    }
    
    /**
     * Returns the static type replacer where the substitutions in a
     * have been updated by the static type replacer b.
     * 
     * Not quite composition, for inputs to a that contain variables
     * in the domain of b.
     * 
     * 
     * @param a
     * @param b
     */
    public StaticTypeReplacer(StaticTypeReplacer a, StaticTypeReplacer b) {
        parameterMap = new BATree<IdOrOpOrAnonymousName, StaticArg>(IOOOANcomparator);
        for (IdOrOpOrAnonymousName key : a.parameterMap.keySet()) {
            parameterMap.put(key, (StaticArg) ((a.parameterMap.get(key)).accept(b)) ); 
        }
    }

    public Type replaceIn(Type t) {
        return (Type)t.accept(this); // TODO safe?
    }

    public Type replaceInEverything(Type t) {
        boolean savedReplaceStaticParams = replaceStaticParams;
        replaceStaticParams = true;
        t =  replaceIn(t); // TODO safe?
        replaceStaticParams = savedReplaceStaticParams;
        return t;
    }

    public IdOrOpOrAnonymousName replaceIn(IdOrOpOrAnonymousName t) {
        return (IdOrOpOrAnonymousName)t.accept(this); // TODO safe?
    }

    public StaticParam replaceStaticParam(StaticParam t) {
        return (StaticParam)t.accept(this);
    }

    private Node updateNode(Node that, final IdOrOpOrAnonymousName name) {
        if (name.getApiName().isSome()) { return that; }
        StaticArg outer_arg = parameterMap.get(name);
        if (outer_arg == null) { return that; }
        else {
            // unwrap the StaticArg
            return outer_arg.accept(new NodeAbstractVisitor<Node>() {
                @Override public Node forTypeArg(TypeArg arg) { return arg.getTypeArg(); }
                @Override public Node forIntArg(IntArg arg) { return arg.getIntVal(); }
                @Override public Node forBoolArg(BoolArg arg) { return arg.getBoolArg(); }
                @Override public Node forOpArg(OpArg arg) {
                    if (name instanceof NamedOp) {
                        return NodeFactory.makeOp((NamedOp) name, arg.getId().getText());
                    } else 
                        return arg.getId();
                    }
                @Override public Node forDimArg(DimArg arg) { return arg.getDimArg(); }
                @Override public Node forUnitArg(UnitArg arg) { return arg.getUnitArg(); }
            });
        }
    }

    // ----------- VISITOR METHODS ---------------



    @Override
    public Node forVarType(VarType that) {
        Type t = (Type)  updateNode(that, that.getName());
        if (t == that)
            return t;
        List<StaticParam> that_sp = that.getInfo().getStaticParams();
        if (that_sp.size() == 0) 
            return t;
        List<StaticParam> t_sp = t.getInfo().getStaticParams();
        if (t_sp.size() > 0)
            return t;
        { boolean savedReplaceStaticParams = replaceStaticParams;
        replaceStaticParams = true;
        List<StaticParam> new_sp = recurOnListOfStaticParam(that_sp);
        replaceStaticParams = savedReplaceStaticParams;
        return STypesUtil.insertStaticParams(t, new_sp);
        }
    }

    /**
     * Special version with list-editing abilities
     * @param that
     * @return
     */
    @Override
    public List<StaticParam> recurOnListOfStaticParam(List<StaticParam> that) {
        java.util.ArrayList<StaticParam> accum = new java.util.ArrayList<StaticParam>();
        boolean unchanged = true;
        for (StaticParam elt : that) {
            StaticParam update_elt = (StaticParam) recur(elt);
            if (update_elt == null) {
                // these get edited in StaticTypeReplacer.
                unchanged = false;
            } else {
                unchanged &= (elt == update_elt);
                accum.add(update_elt);
            }
        }
        return unchanged ? that : accum;
    }

    @Override
    public Node forStaticParam(StaticParam that) {
        if (replaceStaticParams) {
            /*
             * This seems very wrong.  This code exists to rewrite the static params
             * hanging off the type info of nodes that get rewritten.
             */
            Node sp =  updateNode(that, that.getName());
            List<BaseType> that_extendsClauses = that.getExtendsClause();
            List<BaseType> extendsClauses = recurOnListOfBaseType(that_extendsClauses);
            if (sp != that) {
                if (sp instanceof VarType) {
                    that = NodeFactory.makeStaticParam(that, ((VarType) sp).getName(), extendsClauses);
                } else { // if it is not a VarType, the constraint vanishes
                    return null;
                }
            } else if (that_extendsClauses != extendsClauses) {
                that = NodeFactory.makeStaticParam(that, (Id) (that.getName()), extendsClauses);
            }
            return that;
        }
        else
            return super.forStaticParam(that);
    }
    
    @Override
    public Node forOpArg(OpArg that) {
        StaticArg arg = parameterMap.get(that.getId());
        //System.err.printf("forOpArg lookup: %s %s\n", that, arg);
        return arg == null ? that : arg;
    }

    @Override
    public Node forIntRef(IntRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forBoolRef(BoolRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forDimRef(DimRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forUnitRef(UnitRef that) {
        return updateNode(that, that.getName());
    }

    @Override
    public Node forFnDecl(FnDecl that) {
        // TODO Auto-generated method stub
        return super.forFnDecl(that);
    }

    @Override
    public Node forOpRef(OpRef that) {
        // TODO Auto-generated method stub
        return super.forOpRef(that);
    }

    @Override
    public Node forNamedOp(NamedOp that) {
        Node n = updateNode(that, that);
        if (that == n)
            return that;
        return n;
    }


    /**
     * Do the static arguments and parameters "match" in the limited sense that there
     * are the same number and their kinds match (e.g., TypeArgs match TypeParams,
     * nats match NatParams, etc.)
     * @param static_args
     * @param staticParams
     * @return
    public static Option<ConstraintFormula> argsMatchParams(List<StaticArg> static_args,
                                                            List<StaticParam> static_params, final TypeAnalyzer subtype_checker) {
        if( static_args.size() != static_params.size() ) {
            return Option.none();
        }
        else {
            ConstraintFormula valid= trueFormula();

            Iterable<Pair<StaticParam,StaticArg>> zip = IterUtil.zip(static_params, static_args);
            for(Pair<StaticParam,StaticArg> temp : zip){
                final StaticParam  param = temp.first();
                final StaticArg static_arg = temp.second();
                NodeDepthFirstVisitor<Option<ConstraintFormula>> outer = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                    @Override
                    public Option<ConstraintFormula> defaultCase(Node that) {
                        return InterpreterBug.bug("Static param has been extended since argMatchParams was written");
                    }

                    @Override
                    public Option<ConstraintFormula> forStaticParam(StaticParam thatStaticParam) {
                        return thatStaticParam.getKind().accept( new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                @Override
                                public Option<ConstraintFormula> defaultCase(Node n) { return Option.none(); }

                                @Override
                                public Option<ConstraintFormula> forKindOp(KindOp that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forOpArg(OpArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindBool(KindBool k) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forBoolArg(BoolArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindDim(KindDim that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forDimArg(DimArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindInt(KindInt that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forIntArg(IntArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindNat(KindNat that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forIntArg(IntArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindType(final KindType that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forTypeArg(TypeArg arg) {
                                            Type upperbound = NodeFactory.makeIntersectionType(CollectUtil.asSet(param.getExtendsClause()));
                                            return Option.some(subtype_checker.subtype(arg.getTypeArg(),upperbound));
                                        }
                                    };
                                    return static_arg.accept(inner);
                                }
                                @Override
                                public Option<ConstraintFormula> forKindUnit(KindUnit that) {
                                    NodeDepthFirstVisitor<Option<ConstraintFormula>> inner = new NodeDepthFirstVisitor<Option<ConstraintFormula>>() {
                                        @Override public Option<ConstraintFormula> defaultCase(Node inner_that) {return Option.none();}
                                        @Override public Option<ConstraintFormula> forUnitArg(UnitArg inner_that) {return Option.some(trueFormula());}
                                    };
                                    return static_arg.accept(inner);
                                }
                            });
                    }
                };

                Option<ConstraintFormula> result = param.accept(outer);
                if(result.isSome()){
                    valid = valid.and(result.unwrap());
                }
                else{
                    return Option.none();
                }
            }
            return Option.some(valid);
            }
    }
     */
}
