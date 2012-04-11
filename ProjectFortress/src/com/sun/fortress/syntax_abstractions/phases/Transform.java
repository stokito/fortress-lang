/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.syntax_abstractions.phases;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.exceptions.MacroError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.syntax_abstractions.rats.RatsUtil;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.Useful;
import edu.rice.cs.plt.tuple.Option;

import java.util.*;

/**
 * Macro expander: Traverses AST node, replacing transformation nodes with
 * the results of their transformer executions.
 */
public class Transform extends TemplateUpdateVisitor {
    /* Map of a name to the macro transformer */
    private Map<String, Transformer> transformers;
    /* Map of a template variable to a structure containing its value and
     * its ellipses nesting depth
     */
    private Map<String, Level> variables;
    /* Map of original variable names to their renamed counterparts. */
    private SyntaxEnvironment syntaxEnvironment;
    /* rename is true if a macro has been invoked.
     * When it is true variables must be renamed hygienically.
     */
    private final boolean rename;

    private Transform(Map<String, Transformer> transformers,
                      Map<String, Level> variables,
                      SyntaxEnvironment syntaxEnvironment,
                      boolean rename) {
        this.transformers = transformers;
        this.variables = variables;
        this.syntaxEnvironment = syntaxEnvironment;
        this.rename = rename;
    }

    /* entry point to this class. Rewrite an AST with syntax abstraction
     * nodes so that only a core Fortress AST is left.
     */
    public static Node transform(GlobalEnvironment env, Node node) {
        Transform transform = new Transform(populateTransformers(env),
                                            /* map of variables and their ellipses depth */
                                            new HashMap<String, Level>(),
                                            /* hygiene */
                                            SyntaxEnvironment.identityEnvironment(), false);
        /* goes to defaultTransformationNodeCase */
        return node.accept(transform);
    }

    /* maps each transformer's name to the transformer */
    private static Map<String, Transformer> populateTransformers(GlobalEnvironment env) {
        final Map<String, Transformer> map = new HashMap<String, Transformer>();
        for (ApiIndex api : env.apis().values()) {
            api.ast().accept(new NodeDepthFirstVisitor_void() {
                @Override
                public void forNamedTransformerDef(NamedTransformerDef that) {
                    map.put(that.getName(), that.getTransformer());
                }
            });
        }
        return map;
    }

    /* Anyone that calls this method should be sure to save the syntax
     * environment on entry to the function and restore the environment
     * on exit.  Something like:
     *
     * void foo(){
     *      SyntaxEnvironment save = getSyntaxEnvironment();
     *      ...
     *      extendSyntaxEnvironment(...);
     *      ...
     *      setSyntaxEnvironment(save);
     * }
     */
    private void extendSyntaxEnvironment(Id from, Id to) {
        syntaxEnvironment = syntaxEnvironment.extend(from, to);
    }

    private SyntaxEnvironment getSyntaxEnvironment() {
        return this.syntaxEnvironment;
    }

    private void setSyntaxEnvironment(SyntaxEnvironment e) {
        this.syntaxEnvironment = e;
    }

    /* generate a new unique id. a.k.a gensym */
    private Id generateId(Id original) {
        return NodeFactory.makeId(original, RatsUtil.getFreshName(original.getText() + "-g"));
    }

    /* generate a new unique id. a.k.a gensym, if the first parameter is not a TemplateGapId */
    private Id generateId(IdOrOpOrAnonymousName maybeTemplateGap, Id original) {
        if (maybeTemplateGap instanceof TemplateGapId) return original;
        else return NodeFactory.makeId(original, RatsUtil.getFreshName(original.getText() + "-g"));
    }

    /* hygiene */
    @Override
    public Node forVarRef(VarRef that) {
        Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
        Id var_result = syntaxEnvironment.lookup(that.getVarId());
        Debug.debug(Debug.Type.SYNTAX,
                    2,
                    "Looking up var ref ",
                    that.getVarId(),
                    " in hygiene environment = ",
                    var_result);
        ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                 NodeUtil.isParenthesized(that),
                                                 exprType_result);
        return forVarRefOnly(that, info, var_result, that.getStaticArgs());
    }

    /* Support renaming for these nodes
     * LValue  ( only for LocalVarDecl ) - done
     * FnDecl  ( only for local function decls ) - done
     * Param   ( only for FnExpr and local function decls ) - done
     * Label - done
     * Typecase - done
     * Catch - done
     * GeneratorClause - all the following nodes can contain GeneratorClause
     *    For - done
     *    While - done
     *    IfClause - done
     *    Accumulator - done
     *    ArrayComprehensionClause - on hold till it's implemented
     *    TestDecl - on hold till it's implemented
     */

    /* this should only be called by a method that has a GeneratorClause */

    private GeneratorClause handleGeneratorClause(GeneratorClause that) {
        List<Id> newIds = Useful.applyToAll(that.getBind(), new Fn<Id, Id>() {
            public Id apply(Id value) {
                Id gen = generateId(value);
                extendSyntaxEnvironment(value, gen);
                return gen;
            }
        });
        Expr init_result = (Expr) recur(that.getInit());
        return NodeFactory.makeGeneratorClause(NodeFactory.makeSpan(newIds, init_result), newIds, init_result);
    }

    @Override
    public Node forIfClause(IfClause that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            GeneratorClause test_result = handleGeneratorClause(that.getTestClause());
            Block body_result = (Block) recur(that.getBody());
            setSyntaxEnvironment(save);
            return forIfClauseOnly(that, that.getInfo(), test_result, body_result);
        } else {
            return super.forIfClause(that);
        }
    }

    @Override
    public Node forFor(For that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            List<GeneratorClause> gens_result = Useful.applyToAll(that.getGens(),
                                                                  new Fn<GeneratorClause, GeneratorClause>() {
                                                                      public GeneratorClause apply(GeneratorClause value) {
                                                                          return handleGeneratorClause(value);
                                                                      }
                                                                  });
            Block body_result = (Block) recur(that.getBody());
            setSyntaxEnvironment(save);
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            return forForOnly(that, info, gens_result, body_result);
        } else {
            return super.forFor(that);
        }
    }

    @Override
    public Node forWhile(While that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            GeneratorClause test_result = handleGeneratorClause(that.getTestExpr());
            Do body_result = (Do) recur(that.getBody());
            setSyntaxEnvironment(save);
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            return forWhileOnly(that, info, test_result, body_result);
        } else {
            return super.forWhile(that);
        }
    }

    @Override
    public Node forAccumulator(Accumulator that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            List<StaticArg> staticArgs_result = recurOnListOfStaticArg(that.getStaticArgs());
            Op accOp_result = (Op) recur(that.getAccOp());
            List<GeneratorClause> gens_result = new ArrayList<GeneratorClause>();
            for (GeneratorClause g : that.getGens()) {
                gens_result.add(handleGeneratorClause(g));
            }
            Expr body_result = (Expr) recur(that.getBody());
            setSyntaxEnvironment(save);
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            return forAccumulatorOnly(that, info, staticArgs_result, accOp_result, gens_result, body_result);
        } else {
            return super.forAccumulator(that);
        }
    }

    @Override
    public Node forCatch(Catch that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Id name_result = (Id) recur(that.getName());
            Id newId = generateId(that.getName(), name_result);
            extendSyntaxEnvironment(name_result, newId);
            List<CatchClause> clauses_result = recurOnListOfCatchClause(that.getClauses());
            setSyntaxEnvironment(save);
            return forCatchOnly(that, that.getInfo(), newId, clauses_result);
        } else {
            return super.forCatch(that);
        }
    }

    @Override
    public Node forExit(Exit that) {
        if (rename) {
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            Option<Id> target_result = recurOnOptionOfId(that.getTarget());
            if (target_result.isSome()) {
                target_result = Option.some(syntaxEnvironment.lookup(target_result.unwrap()));
            }
            Option<Expr> returnExpr_result = recurOnOptionOfExpr(that.getReturnExpr());
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            return forExitOnly(that, info, target_result, returnExpr_result);
        } else {
            return super.forExit(that);
        }
    }

    @Override
    public Node forLabel(Label that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            Id name_result = (Id) recur(that.getName());
            Id newId = generateId(that.getName(), name_result);
            extendSyntaxEnvironment(name_result, newId);
            Block body_result = (Block) recur(that.getBody());
            setSyntaxEnvironment(save);
            return ExprFactory.makeLabel(NodeFactory.makeSpan(that),
                                         NodeUtil.isParenthesized(that),
                                         exprType_result,
                                         newId,
                                         body_result);
        } else {
            return super.forLabel(that);
        }
    }

    @Override
        public Node forTypecaseClause(TypecaseClause that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            if (that.getName().isSome()) {
                Id id = that.getName().unwrap();
                extendSyntaxEnvironment(id, generateId(id));
            }
            TypeOrPattern matchType_result = (TypeOrPattern) recur(that.getMatchType());
            matchType_result.accept(new NodeDepthFirstVisitor_void(){
                @Override
                public void forPlainPatternOnly(PlainPattern pp) {
                    Id id = pp.getName();
                    extendSyntaxEnvironment(id, generateId(id));
                }
            });
            Block body_result = (Block) recur(that.getBody());
            setSyntaxEnvironment(save);
        }
        return super.forTypecaseClause(that);
    }

    @Override
    public Node forFnExpr(FnExpr that) {
        if (rename) {
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            IdOrOpOrAnonymousName name_result = (IdOrOpOrAnonymousName) recur(NodeUtil.getName(that));
            List<StaticParam> staticParams_result = recurOnListOfStaticParam(NodeUtil.getStaticParams(that));
            List<Param> params_result = Useful.applyToAll(NodeUtil.getParams(that), renameParam);
            Option<Type> returnType_result = recurOnOptionOfType(NodeUtil.getReturnType(that));
            Option<WhereClause> where_result = recurOnOptionOfWhereClause(NodeUtil.getWhereClause(that));
            Option<List<Type>> throwsClause_result = recurOnOptionOfListOfType(NodeUtil.getThrowsClause(that));
            Expr body_result = (Expr) recur(that.getBody());
            FnHeader header = (FnHeader) forFnHeaderOnly(that.getHeader(),
                                                         staticParams_result,
                                                         name_result,
                                                         where_result,
                                                         throwsClause_result,
                                                         Option.<Contract>none(),
                                                         params_result,
                                                         returnType_result);
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            Node ret = forFnExprOnly(that, info, header, body_result);
            setSyntaxEnvironment(save);
            return ret;
        } else {
            return super.forFnExpr(that);
        }
    }

    private Fn<Param, Param> renameParam = new Fn<Param, Param>() {
        public Param apply(Param value) {
            final Transform transformer = Transform.this;
            return (Param) value.accept(new TemplateUpdateVisitor() {
                @Override
                public Node forParamOnly(Param that,
                                         ASTNodeInfo info_result,
                                         Id name_result,
                                         Option<TypeOrPattern> type_result,
                                         Option<Expr> defaultExpr_result,
                                         Option<Type> varargsType_result) {
                    if (!NodeUtil.isVarargsParam(that)) {
                        Debug.debug(Debug.Type.SYNTAX,
                                    2,
                                    "Normal param id hash code " + name_result.generateHashCode());
                        Id generatedId = renameId(name_result);
                        return NodeFactory.makeParam(NodeUtil.getSpan(that),
                                                     that.getMods(),
                                                     generatedId,
                                                     type_result,
                                                     defaultExpr_result,
                                                     Option.<Type>none());
                    } else {
                        Debug.debug(Debug.Type.SYNTAX,
                                    2,
                                    "Varargs param id hash code " + name_result.generateHashCode());
                        Id old = (Id) name_result.accept(transformer);
                        Id generatedId = generateId(name_result, old);
                        Debug.debug(Debug.Type.SYNTAX, 2, "Generate new binding for " + old + " = " + generatedId);
                        extendSyntaxEnvironment(old, generatedId);
                        return NodeFactory.makeParam(NodeUtil.getSpan(that),
                                                     that.getMods(),
                                                     generatedId,
                                                     Option.<TypeOrPattern>none(),
                                                     Option.<Expr>none(),
                                                     varargsType_result);
                    }
                }
            });
        }
    };

    /* rename an id and extend the syntax environment only if the id was
     * not a template parameter
     */
    private Id renameId(Id id) {
        return (Id) id.accept(new TemplateUpdateVisitor() {
            @Override
            public Node forTemplateGapId(TemplateGapId tid) {
                return tid.accept(Transform.this);
            }

            @Override
            public Node forId(Id id) {
                Id old = (Id) id.accept(Transform.this);
                Id generatedId = generateId(id, old);
                Debug.debug(Debug.Type.SYNTAX, 2, "Generate new binding for " + old + " = " + generatedId);
                extendSyntaxEnvironment(old, generatedId);
                return generatedId;
            }
        });
    }

    @Override
    public Node forLetFn(LetFn thatLetFn) {
        if (rename) {
            final Transform transformer = this;
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(thatLetFn));
            List<FnDecl> fns_result = Useful.applyToAll(thatLetFn.getFns(), new Fn<FnDecl, FnDecl>() {
                public FnDecl apply(FnDecl fn) {
                    return (FnDecl) fn.accept(new TemplateUpdateVisitor() {
                        @Override
                        public Node forFnDeclOnly(FnDecl that,
                                                  ASTNodeInfo info_result,
                                                  FnHeader header_result,
                                                  IdOrOp name_result,
                                                  Option<Expr> body_result,
                                                  Option<IdOrOp> implementsUnambiguousName_result) {
                            IdOrOp generatedId = name_result;
                            if (generatedId instanceof Id) {
                                Id old = (Id) ((Id) name_result).accept(transformer);
                                generatedId = generateId(name_result, old);
                                extendSyntaxEnvironment(old, (Id) generatedId);
                            }

                            FnHeader new_fnHeader = (FnHeader) header_result.accept(new TemplateUpdateVisitor() {
                                @Override
                                public Node forFnHeaderOnly(FnHeader that,
                                                            List<StaticParam> staticParams_result,
                                                            IdOrOpOrAnonymousName name_result,
                                                            Option<WhereClause> whereClause_result,
                                                            Option<List<Type>> throwsClause_result,
                                                            Option<Contract> contract_result,
                                                            List<Param> params_result,
                                                            Option<Type> returnType_result) {
                                    Id oldName = (Id) ((Id) name_result).accept(transformer);
                                    final Id generatedName = generateId(name_result, oldName);
                                    extendSyntaxEnvironment(oldName, generatedName);
                                    List<Param> new_params_result = Useful.applyToAll(params_result, renameParam);
                                    return super.forFnHeaderOnly(that,
                                                                 staticParams_result,
                                                                 generatedName,
                                                                 whereClause_result,
                                                                 throwsClause_result,
                                                                 contract_result,
                                                                 new_params_result,
                                                                 returnType_result);
                                }
                            });

                            Option<Expr> new_body = transformer.recurOnOptionOfExpr(body_result);
                            return super.forFnDeclOnly(that,
                                                       info_result,
                                                       new_fnHeader,
                                                       generatedId,
                                                       new_body,
                                                       implementsUnambiguousName_result);
                        }
                    });
                }
            });
            Block body_result = (Block) recur(thatLetFn.getBody());
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(thatLetFn),
                                                     NodeUtil.isParenthesized(thatLetFn),
                                                     exprType_result);
            Node ret = forLetFnOnly(thatLetFn, info, body_result, fns_result);
            setSyntaxEnvironment(save);
            return ret;
        } else {
            return super.forLetFn(thatLetFn);
        }
    }

    @Override
    public Node forLocalVarDecl(LocalVarDecl that) {
        if (rename) {
            final Transform transformer = this;
            SyntaxEnvironment save = getSyntaxEnvironment();
            Option<Type> exprType_result = recurOnOptionOfType(NodeUtil.getExprType(that));
            Debug.debug(Debug.Type.SYNTAX, 2, "Transforming local var decl");
            List<LValue> lhs_result = Useful.applyToAll(that.getLhs(), new Fn<LValue, LValue>() {
                public LValue apply(LValue value) {
                    return (LValue) value.accept(new TemplateUpdateVisitor() {
                        @Override
                        public Node forLValueOnly(LValue that,
                                                  ASTNodeInfo info,
                                                  Id name_result,
                                                  Option<TypeOrPattern> type_result) {
                            Id old = (Id) name_result.accept(transformer);
                            Id generatedId = generateId(name_result, old);
                            Debug.debug(Debug.Type.SYNTAX, 2, "Generate new binding for " + old + " = " + generatedId);
                            extendSyntaxEnvironment(old, generatedId);
                            return NodeFactory.makeLValue(NodeUtil.getSpan(that),
                                                          generatedId,
                                                          that.getMods(),
                                                          type_result,
                                                          that.isMutable());
                        }
                    });
                }
            });
            Option<Expr> rhs_result = recurOnOptionOfExpr(that.getRhs());
            Block body_result = (Block) recur(that.getBody());
            /*
            List<Expr> body_result = Useful.applyToAll(recurOnListOfExpr(that.getBody()),
                                                       new Fn<Expr, Expr>(){
                public Expr apply(Expr value){
                    return value.accept( new NodeUpdateVisitor(){
                        public Node forIdOnly(Id that, Option<APIName> api_result) {
                            Debug.debug( Debug.Type.SYNTAX, 2,
                                         "Looking up id " + that +
                                         " in environment " +
                                         syntaxEnvironment );
                            return Transform.this.syntaxEnvironment.lookup(that);
                        }
                    });
                }
            });
            */
            ExprInfo info = NodeFactory.makeExprInfo(NodeUtil.getSpan(that),
                                                     NodeUtil.isParenthesized(that),
                                                     exprType_result);
            Node ret = forLocalVarDeclOnly(that, info, body_result, lhs_result, rhs_result);
            setSyntaxEnvironment(save);
            return ret;
        } else {
            return super.forLocalVarDecl(that);
        }
    }

    /* end hygiene */

    @Override
    public Node forTemplateGapOnly(TemplateGap that,
                                   ASTNodeInfo info,
                                   Id gapId_result,
                                   List<Id> templateParams_result) {
        Debug.debug(Debug.Type.SYNTAX, 3, "Looking up gapid ", gapId_result);
        /* I'm pretty sure the following line should be commented out. Explanation
         * needed..
         */
        // Node n = ((Node) lookupVariable(gapId_result, templateParams_result).get_object()).accept(this);
        /* another annoying cast */
        Node n = ((Node) lookupVariable(gapId_result, templateParams_result).get_object());
        // Debug.debug( Debug.Type.SYNTAX, 3, "Hash code for ", n, " is ", n.generateHashCode() );
        Debug.debug(Debug.Type.SYNTAX, 3, "Result for gapid ", gapId_result, " is ", n.getClass(), " ", n);
        return n;
    }

    /* return the value for a bound variable, and/or return a new Curried object
     * if the value takes parameters
     */
    private Level lookupVariable(Id id, List<Id> params) {
        String variable = id.getText();
        Level binding = this.variables.get(variable);
        if (binding == null) {
            throw new MacroError("Can't find a binding for gap " + id);
        } else {
            if (params.isEmpty()) {
                /*
                if ( binding instanceof List ){
                    return new _RepeatedExpr((List) binding);
                }
                */
                Debug.debug(Debug.Type.SYNTAX, 2, "Found template gap " + binding.get_object());
                return binding;
            } else {
                if (!(binding.get_object() instanceof CurriedTransformer)) {
                    throw new MacroError(
                            "Parameterized template gap is not bound " + "to a CurriedTransformer, instead bound to " +
                            binding.getClass().getName());
                }

                CurriedTransformer curried = (CurriedTransformer) binding.get_object();
                Map<String, Level> vars = new HashMap<String, Level>(curried.getVariables());
                if (curried.getSyntaxParameters().size() != params.size()) {
                    throw new MacroError("Passing " + params.size() + " arguments to a nonterminal that accepts " +
                                         curried.getSyntaxParameters().size() + " arguments.");
                }

                Debug.debug(Debug.Type.SYNTAX, 3, "Template gap " + id.getText() + " has parameters " + params);
                for (int i = 0; i < params.size(); i++) {
                    Id parameter = params.get(i);
                    String name = curried.getSyntaxParameters().get(i);
                    Debug.debug(Debug.Type.SYNTAX, 3, "Adding parameter ", name);
                    vars.put(name, lookupVariable(parameter, new LinkedList<Id>()));
                }
                // return curry((Node)binding, vars);

                /* the type of the transformer (_SyntaxTransformationExpr) doesn't matter */
                Node newNode = new _SyntaxTransformationExpr(NodeFactory.makeExprInfo(NodeFactory.macroSpan),
                                                             vars,
                                                             new LinkedList<String>(),
                                                             curried.getSyntaxTransformer());
                return new Level(binding.getLevel(), newNode.accept(this));
            }
        }
    }

    /* dont need this, this comment tells you not to try to define this method.
     * _CurriedTransformer is dealt with in lookupVariable (the method above)
    @Override public Node for_CurriedTransformer(_CurriedTransformer that);
    */

    @Override
    public Node defaultTransformationNodeCase(_SyntaxTransformation that) {
        if (!that.getSyntaxParameters().isEmpty()) {
            /* needs parameters, curry it! */
            return curry(that.getSyntaxTransformer(), that.getVariables(), that.getSyntaxParameters());
        }
        Debug.debug(Debug.Type.SYNTAX, 1, "Run transformation " + that.getSyntaxTransformer());
        Transformer transformer = lookupTransformer(that.getSyntaxTransformer());
        Map<String, Level> arguments = that.getVariables();
        Map<String, Level> evaluated = new HashMap<String, Level>();
        /* for each variable in the syntax environment, evaluate it
         * and put it in the evaluated map
         */
        for (Map.Entry<String, Level> var : arguments.entrySet()) {
            String varName = var.getKey();
            /*
            if ( var.getValue() instanceof List ){
                Debug.debug( Debug.Type.SYNTAX, 2, "Adding repeated node ", varName );
                env.add( NodeFactory.makeId( varName ), 1, var.getValue() );
            }
            */

            /* argh, this cast shouldn't be needed */
            Level argument = (Level) traverse(var.getValue());

            /* this is almost definately in the wrong place */
            /*
            if ( argument instanceof List ){
                Debug.debug( Debug.Type.SYNTAX, 2, "Adding repeated node ", varName );
                env.add( NodeFactory.makeId( varName ), 1, argument );
            }
            */

            evaluated.put(varName, argument);
            Debug.debug(Debug.Type.SYNTAX, 3, "Argument " + varName + " is " + argument);
        }

        Debug.debug(Debug.Type.SYNTAX, 2, "Invoking transformer ", that.getSyntaxTransformer());
        Node transformed = transformer.accept(new TransformerEvaluator(this.transformers, evaluated));
        checkFullyTransformed(transformed);
        return transformed;
    }

    private Node curry(String original, Map<String, Level> vars, List<String> parameters) {
        return new CurriedTransformer(original, vars, parameters);
    }

    /* find a transformer given its name */
    private Transformer lookupTransformer(String name) {
        if (transformers.get(name) == null) {
            throw new MacroError("Cannot find transformer for " + name);
        }
        return transformers.get(name);
    }

    /* convert an AST or List into a Fortress AST node
     *   - Level: return the same level and traverse the value that is wrapped
     *   - List: traverse all values in the list and splice in the result of
     *     traversing ellipses nodes
     *   - Ellipses: expand ellipses and traverse the result
     *   - Node: visit the node
     *
     * x = 'a : Level(0, 'a)
     * x = '(a) : Level(1, (list 'a))
     * x = '(a b) : Level(1, (list 'a 'b))
     * x = '((a) (b)) : Level(2, (list (list 'a) (list 'b)))
     */
    private Object traverse(Object partial) {
        Debug.debug(Debug.Type.SYNTAX, 2, "Traversing object ", partial.getClass().getName());
        if (partial instanceof Level) {
            Level l = (Level) partial;
            return new Level(l.getLevel(), traverse(l.get_object()));
        } else if (partial instanceof List) {
            List<Object> all = new LinkedList<Object>();
            for (Object o : (List<?>) partial) {
                if (o instanceof _Ellipses) {
                    all.addAll((List<?>) traverse(o));
                } else {
                    all.add(traverse(o));
                }
            }
            return all;
        } else if (partial instanceof _Ellipses) {
            /* expand ellipses and return some new node */
            return traverse(handleEllipses((_Ellipses) partial));
        } else if (partial instanceof Node) {
            return ((Node) partial).accept(this);
        }
        throw new MacroError("Unknown object type " + partial.getClass().getName() + " value: " + partial);
    }

    /* evaluates the stuff on the right hand side of the =>, the transformation
     * expression
     */
    class TransformerEvaluator extends NodeDepthFirstVisitor<Node> {
        private Map<String, Transformer> transformers;
        private Map<String, Level> variables;

        private TransformerEvaluator(Map<String, Transformer> transformers, Map<String, Level> variables) {
            this.transformers = transformers;
            this.variables = variables;
        }

        @Override
        public Node forNodeTransformer(NodeTransformer that) {
            return that.getNode().accept(new Transform(transformers,
                                                       variables,
                                                       SyntaxEnvironment.identityEnvironment(),
                                                       true));
        }

        @Override
        public Node forCaseTransformer(CaseTransformer that) {
            Id gapName = that.getGapName();
            List<CaseTransformerClause> clauses = that.getClauses();
            Level toMatch = lookupVariable(gapName);
            for (CaseTransformerClause clause : clauses) {
                Option<Node> result = matchClause(clause, toMatch);
                if (result.isSome()) {
                    return result.unwrap();
                }
                // else continue;
            }
            throw new MacroError(that, "match failed");
        }

        private Level lookupVariable(Id name) {
            Level obj = variables.get(name.getText());
            if (obj == null) {
                throw new MacroError("Can't find a binding for gap " + name);
            } else if (obj.get_object() instanceof CurriedTransformer) {
                throw new MacroError(name + " cannot accept parameters in a case expression");
            }
            Debug.debug(Debug.Type.SYNTAX,
                        3,
                        "Looking up variable in transformer evaluator: " + name + " and found " + obj);
            return obj;
        }

        /* match a case clause to one of its instantiations, cons or empty */
        private Option<Node> matchClause(CaseTransformerClause clause, Level toMatch) {
            String constructor = clause.getConstructor().getText();
            List<Id> parameters = clause.getParameters();
            int parameterCount = parameters.size();
            Transformer body = clause.getBody();

            if (!(constructor.equals("Cons") && parameterCount == 2) &&
                !(constructor.equals("Empty") && parameterCount == 0)) {
                // Nothing else implemented yet
                throw new MacroError(clause.getConstructor(), "bad case transformer constructor: " + constructor);
            }

            if (toMatch.get_object() instanceof List) {
                List<?> list = (List<?>) toMatch.get_object();
                Debug.debug(Debug.Type.SYNTAX, 2, "Matching Cons constructor to list " + list);
                if (constructor.equals("Cons")) {
                    if (!list.isEmpty()) {
                        Object first = list.get(0);
                        Object rest = list.subList(1, list.size());
                        String firstParam = parameters.get(0).getText();
                        String restParam = parameters.get(1).getText();
                        Map<String, Level> newEnv = new HashMap<String, Level>(variables);
                        newEnv.put(firstParam, new Level(toMatch.getLevel() - 1, first));
                        newEnv.put(restParam, new Level(toMatch.getLevel(), rest));
                        Debug.debug(Debug.Type.SYNTAX, 2, "Head of cons variable " + firstParam + " is " + first);
                        Debug.debug(Debug.Type.SYNTAX, 2, "Tail of cons variable " + restParam + " is " + rest);
                        return Option.wrap(body.accept(new TransformerEvaluator(transformers, newEnv)));
                    }
                } else if (constructor.equals("Empty")) {
                    if (list.isEmpty()) {
                        return Option.wrap(body.accept(this));
                    }
                }
            }
            return Option.<Node>none();
        }
    }

    /* throw an error if n contains any syntax abstraction nodes in it */
    private void checkFullyTransformed(Node n) {
        n.accept(new TemplateNodeDepthFirstVisitor_void() {
            @Override
            public void forTemplateGapOnly(TemplateGap that) {
                throw new MacroError("Transformation left over template gap: " + that);
            }

            @Override
            public void for_SyntaxTransformationOnly(_SyntaxTransformation that) {
                throw new MacroError("Transformation left over transformation application: " + that);
            }
        });
    }

    /* splice in the result of ellipses nodes into lists of expressions */
    @Override
    public List<Expr> recurOnListOfExpr(List<Expr> that) {
        List<Expr> accum = new java.util.ArrayList<Expr>(that.size());
        for (Expr elt : that) {
            if (elt instanceof _Ellipses) {
                for (Node n : handleEllipses((_Ellipses) elt)) {
                    accum.add((Expr) n);
                }
            } else {
                accum.add((Expr) recur(elt));
            }
        }
        return accum;
    }

    /* expand an EllipsesXXX node using the macro by example algorithm */
    private List<Node> handleEllipses(_Ellipses that) {
        if (controllable(that)) {
            List<Node> nodes = new ArrayList<Node>();
            /* for a given expression E that contains a repeated pattern
             * variable i, create N new transforms where N is the number of items
             * that i is bound to. then process the value of the i node with that
             * new environment (which should have one less depth than the 'that'
             */
            for (Transform newEnv : decompose(freeVariables(that.getRepeatedNode()))) {
                nodes.add(that.getRepeatedNode().accept(newEnv));
            }
            return nodes;
        } else {
            throw new MacroError("Invalid ellipses expression: " + that);
        }
    }

    /* return true if the node contains a pattern variable that has a depth
     * greater than 0 (basically if it is a repeated node)
     */
    private boolean controllable(_Ellipses that) {
        for (Id var : freeVariables(that.getRepeatedNode())) {
            if (hasVariable(var) && lookupLevel(var) > 0) {
                return true;
            }
        }
        return false;
    }

    /* the usual thing.. free variables */
    private List<Id> freeVariables(Node node) {
        final List<Id> vars = new ArrayList<Id>();

        node.accept(new NodeDepthFirstVisitor_void() {
            @Override
            public void defaultTemplateGap(TemplateGap t) {
                vars.add(t.getGapId());
            }
        });

        return vars;
    }

    /* whether this is a bound variable */
    private boolean hasVariable(Id id) {
        return this.variables.get(id.getText()) != null;
    }

    /* return the depth for a given variable */
    private int lookupLevel(Id id) {
        String variable = id.getText();
        Level binding = this.variables.get(variable);
        if (binding == null) {
            throw new MacroError("Can't find a binding for gap " + id);
        }
        return binding.getLevel();
    }

    /* convert an environment into a list of environments,
     * one for each value in the list of values
     */
    private List<Transform> decompose(List<Id> freeVariables) {
        List<Transform> all = new ArrayList<Transform>();

        Debug.debug(Debug.Type.SYNTAX, 2, "Free variables in the decomposed list: " + freeVariables);

        int repeats = findRepeatedVar(freeVariables);
        for (int i = 0; i < repeats; i++) {
            Map<String, Level> newVars = new HashMap<String, Level>();

            for (Id var : freeVariables) {
                Level value = lookupVariable(var, new LinkedList<Id>());
                if (value.getLevel() == 0) {
                    newVars.put(var.getText(), new Level(value.getLevel(), value));
                } else {
                    List l = (List) value.get_object();
                    newVars.put(var.getText(), new Level(value.getLevel() - 1, l.get(i)));
                }
            }

            all.add(new Transform(transformers, newVars, this.syntaxEnvironment, true));
        }
        return all;
    }

    /* find the first length of some repeated pattern variable */
    private int findRepeatedVar(List<Id> freeVariables) {
        for (Id var : freeVariables) {
            if (lookupLevel(var) > 0) {
                int size = ((List) lookupVariable(var, new LinkedList<Id>()).get_object()).size();
                Debug.debug(Debug.Type.SYNTAX, 2, "Repeated variable ", var, " size is ", size);
                return size;
            }
        }
        throw new MacroError("No repeated variables!");
    }

    private static class CurriedTransformer implements Node {
        private String original;
        private Map<String, Level> vars;
        private List<String> parameters;

        public CurriedTransformer(String original, Map<String, Level> vars, List<String> parameters) {
            this.original = original;
            this.vars = vars;
            this.parameters = parameters;
        }

        public String getSyntaxTransformer() {
            return original;
        }

        public Map<String, Level> getVariables() {
            return vars;
        }

        public List<String> getSyntaxParameters() {
            return parameters;
        }

        private Object error() {
            throw new MacroError("Dont call this method");
        }

        public Span getSpan() {
            error();
            return null;
        }

        public String at() {
            error();
            return null;
        }

        public String stringName() {
            error();
            return null;
        }

        public <RetType> RetType accept(NodeVisitor<RetType> visitor) {
            error();
            return null;
        }

        public void accept(NodeVisitor_void visitor) {
            error();
        }

        public int generateHashCode() {
            error();
            return 0;
        }

        public java.lang.String serialize() {
            error();
            return null;
        }

        public void serialize(java.io.Writer writer) {
            error();
        }

        public void walk(TreeWalker w) {
            error();
        }
    }
}
