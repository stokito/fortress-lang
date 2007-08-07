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

package com.sun.fortress.interpreter.rewrite;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.InterpreterError;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.StringMaker;
import com.sun.fortress.nodes_util.UIDMapFactory;
import com.sun.fortress.nodes_util.UIDObject;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbsDecl;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.DottedId;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.Generator;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.useful.None;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.AbstractObjectExpr;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes_util.RewriteHackList;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;


/**
 * Rewrite the AST to "disambiguate" (given known interpreter
 * treatment of constructors) references to surrounding local
 * variables and object/trait members.  Also rewrite the AST to
 * eliminate generator lists where they occur.  These are desugared in
 * accordance with section 32.8 "Use and definition of Generators" of
 * the Fortress Language Specification.
 *
 * The most important thing to know is that there are two treatments
 * of lexical context; top level, and not.  This can also be carved
 * into "object" and "trait", where top level object and not top level
 * object are treated alike.  (The interpreter does it this way, it
 * could be done the other way.)  For not-top-level, local variables
 * are tied to "self" (rewritten to "*self") so that there need only
 * be set of methods.  Otherwise, if the local environment were tied
 * to the methods (thus, closures) there could be very many of them,
 * and to the extent that a ground type identifies a fixed set of
 * methods, very many types, which thus complicates overloading, etc.
 *
 * Trait/top-level methods can be closures, because there is only one
 * top-level environment, hence no multiplication of methods/types.
 *
 * "Self" is rewritten to "*self" to allow creation of "notself" methods,
 * also to allow creation of explicit getter/setter calls.
 *
 * Because members are not injected into the local environment, any implicit
 * references to parent members in an enclosed object must be made explicit.
 * Each (enclosed) object includes an obfuscated parent reference, "*parent",
 * injected into the environment of the enclosed-object constructor. The
 * difference between the current object nesting depth and a referenced symbol's
 * nesting depth indicates the number of "*parent." that must be prepended (this
 * is the de Bruijn number, it happens).
 *
 * Still lacking -- Each nested object defined inside of generic-anything, needs to
 * be implicitly parameterized by all the surrounding generic parameters that
 * it references (the safe assumption is that it is parameterized by all of them,
 * the clever assumption keeps track of which one it actually uses).
 */
public class Disambiguate extends Rewrite {

    public final static String PARENT_NAME = WellKnownNames.secretParentName;

    public final static Id LOOP_ID = NodeFactory.makeId(WellKnownNames.loopMethod);

    public class Thing {
        int nestedness;

        Thing() {
            nestedness = objectNestingDepth;
        }
        Expr replacement(VarRef original) {
            return original;
        }
        public String toString() { return "Thing@"+nestedness; }
    }

    class Local extends Thing {
        public String toString() { return "Local@"+nestedness; }
    }

    /**
     * Traits need to identify their declaration, for purposes of figuring out
     * what names are in scope, though not necessarily what they mean.
     */
    class Trait extends Local {
        TraitAbsDeclOrDecl defOrDecl;

        Trait(TraitAbsDeclOrDecl dod) {
            defOrDecl = dod;
        }
        public String toString() { return "Trait="+defOrDecl; }

    }

    class Member extends Thing {
        Expr replacement(VarRef original) {
            FieldRef fs = new FieldRef(original.getSpan(), false,
                                               // Use this constructor
                // here because it is a
                // com.sun.fortress.interpreter.rewrite.
                dottedReference(original.getSpan(),
                                objectNestingDepth - nestedness), original.getVar());
        return fs;
        }
        public String toString() { return "Member@"+nestedness; }
        }

    class SelfRewrite extends Thing {
        SelfRewrite(String s) {
            this.s = s;
        }
        String s;
        Expr replacement(VarRef original) {
            Expr expr = dottedReference(original.getSpan(),
                    objectNestingDepth - nestedness);
            return expr;
        }
        public String toString() { return "Self("+s+")@"+nestedness; }

    }

    /**
     * Rewritings in scope.
     */
    BATree<String, Thing> e;

    /**
     * Generic parameters currently in scope.
     */
    BATree<String, StaticParam> visibleGenericParameters;
    BATree<String, StaticParam> usedGenericParameters;

    /**
     * Disambiguating environment map -- in what environment was each trait declared?
     */

    BATree<UIDObject, Map<String, Thing> > traitDisEnvMap = UIDMapFactory.< Map<String, Thing> >make();

    /**
     * All the object exprs (this may generalize to nested functions as well)
     * need to have their gensym'd types and constructors registered at the
     * top level.  The top level environment is available to the creator/caller
     * of Disambiguate.
     */
    private ArrayList<_RewriteObjectExpr> objectExprs = new ArrayList<_RewriteObjectExpr>();

    Disambiguate(BATree<String, Thing> initial, BATree<String, StaticParam> initialGenericScope) {
        e = initial;
        visibleGenericParameters = initialGenericScope;
        usedGenericParameters = new BATree<String, StaticParam>(StringComparer.V);
    }

    public Disambiguate() {
        this(new BATree<String, Thing>(StringComparer.V), new BATree<String, StaticParam>(StringComparer.V));
    }

    /**
     * Zero outside of an object, equal to the number of Object[Exprs] or traits
     * enclosing the current node.
     *
     * When objects are entered, all the symbols defined there are associated
     * with a depth; when a var reference is rewritten, the symbol used depends
     * on the number. If the numbers match, then use the surrounding method's
     * default name for self. If the name is some outer method's name for self,
     * it should suffice to use it (it should work). If the numbers do not
     * match, presumably the symbol's number is smaller than current, and it
     * will be bound in the surrounding environment.
     *
     * Local variables are normally just captured, because the constructor
     * carries them around, but those from the top level surrounding methods
     * defined in traits are captured differently, and must be noted with a
     * special "*TRAIT" symbol (optimization; if any such methods occur, they
     * should be noted, and if not noted then a specialization step can be
     * dodged later).
     *
     * Actually, probably not; the construction of trait environments should get
     * this right without this additional frob (this is much, much less hacky).
     *
     */
    int objectNestingDepth;

    boolean inTrait;

    String currentSelfName = WellKnownNames.defaultSelfName;
    boolean atTopLevelInsideTraitOrObject = false; // true immediately within a trait/object

    /**
     * Used to generate temporary names when rewriting (for example)
     * tuple initializations.
     */
    int tempCount = 0;

    /**
     * Adds, to the supplied environment, constructors for any object
     * expressions encountered the tree(s) processed by this Disambiguator.
     * @param env
     */
    public void registerObjectExprs(BetterEnv env) {
        for (_RewriteObjectExpr oe : objectExprs) {
            String name = oe.getGenSymName();
            List<StaticParam> params = oe.getStaticParams();
            if (params.isEmpty()) {
                // Regular constructor
                FTypeObject fto = new FTypeObject(name, env, oe, oe.getDecls());
                env.putType(name, fto);
                BuildEnvironments.finishObjectTrait(oe.getExtendsClause(), null, null, fto, env, oe);
                Constructor con = new Constructor(env, fto, oe,
                                                  NodeFactory.makeDottedId(name),
                                                  oe.getDecls());
                con.setParams(Collections.<Parameter> emptyList());
                env.putValue(name, con);
                con.finishInitializing();
            } else {
                // Generic constructor
                FTypeGeneric fto = new FTypeGeneric(env, oe, oe.getDecls());
                env.putType(name, fto);
                GenericConstructor con = new GenericConstructor(env, oe);
                env.putValue(name, con);
            }
        }
    }

    Expr newName(VarRef vre, String s) {
        Thing t = e.get(s);
        if (t == null) {
            return vre;
        } else {
            return t.replacement(vre);
        }

    }

    /**
     * Returns the proper name for the object enclosing a method/field; either
     * "self/notself", or "*parent."^N "self", where N is nesting depth.
     *
     * @param s
     * @param i
     * @return
     */
    Expr dottedReference(Span s, int i) {
        if (i == 0) {
            return ExprFactory.makeVarRef(s, WellKnownNames.secretSelfName);
        }
        if (i > 0) {
            return new FieldRef(s, false, dottedReference(s, i - 1),
                                      new Id(s, PARENT_NAME));
        } else {
            throw new Error("Confusion in member reference numbering.");
        }

    }

    public void registerComponent(Component c) {
        // TODO put top-level names in various environments
    }

    public CompilationUnit rewriteComponents(CompilationUnit p) {
        // TODO visit all the components, rewriting as necessary
        // Return the replacement for p.
        return p;
    }

    /**
     * Performs an object-reference-disambiguating com.sun.fortress.interpreter.rewrite of the AST. Any
     * effects on the environment from the toplevel remain visible, otherwise
     * visit only rewrites.
     */
    @Override
    public AbstractNode visit(AbstractNode node) {
        BATree<String, Thing> savedE = e.copy();
        BATree<String, StaticParam> savedVisibleGenerics = visibleGenericParameters.copy();
        BATree<String, StaticParam> savedUsedGenerics = usedGenericParameters.copy();
        BATree<String, Boolean> immediateDef = null;

        boolean savedFnDefIsMethod = atTopLevelInsideTraitOrObject;
        int savedObjectNestingDepth = objectNestingDepth;
        String savedSelfName = currentSelfName;

        if (node instanceof Component) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Component com = (Component) node;
            List<? extends AbsDeclOrDecl> defs = com.getDecls();
            defsToLocals(defs);
            return visitNode(node);

        } else if (node instanceof Api) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Api com = (Api) node;
            List<? extends AbsDeclOrDecl> defs = com.getDecls();
            defsToLocals(defs);
            return visitNode(node);

        } else
            try {
                /*
                 * NOTE: "default" handling for any com.sun.fortress.interpreter.nodes not mentioned explicitly is:
                 * } else {
                 *   atTopLevelInsideTraitOrObject = false;
                 * }
                 * return visitNode(node);
                 * } finally {
                 *   // Reset values saved at entry
                 * }
                 *
                 * Clauses that do not explicitly return, will subsequently recursively
                 * visit their children.  Any clause that does explicitly return should
                 * ensure that the children are also visited.
                 *
                 */

                if (node instanceof VarRef) {
                    VarRef vre = (VarRef) node;
                    Id id = vre.getVar();
                    String s = id.getName();
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }
                    return newName(vre, s);
                } else if (node instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) node;
                    Id id = lvb.getId();
                    if ("_".equals(id.getName())) {
                        return NodeFactory.makeLValue(lvb, new Id(id.getSpan(), "_$" + id.getSpan() ));
                    }
                } else if (node instanceof IdType) {
                    IdType vre = (IdType) node;

                    String s = StringMaker.fromDottedId(vre.getDottedId());
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }

                } else if (node instanceof FieldRef) {
                    atTopLevelInsideTraitOrObject = false;
                    // Believe that any field selection is already
                    // disambiguated. In an ordinary method, "self"
                    // is already defined, in a notself method, "notself"
                    // is defined, and "self" is defined as a local in
                    // an enclosing scope (as is "notnotself", etc, that
                    // intervening notself methods might define).

                    // However, since the LHS of a field selection might
                    // be a method invocation, we DO need to check that.
                    FieldRef fs = (FieldRef) node;

                } else if (node instanceof VarDecl) {
                    atTopLevelInsideTraitOrObject = false;
                    VarDecl vd = (VarDecl) node;
                    List<LValueBind> lhs = vd.getLhs();

                    if (lhs.size() > 1) {
                        // Introduce a temporary, then initialize elements
                        // piece-by-piece.
                        Expr init = vd.getInit();
                        init = (Expr) visitNode(init);
                        lhs = (List<LValueBind>) visitList(lhs);
                        ArrayList<AbstractNode> newdecls = new ArrayList<AbstractNode>(1+lhs.size());
                        String temp = "t$" + (++tempCount);
                        Span at = vd.getSpan();
                        VarDecl new_vd = NodeFactory.makeVarDecl(at, new Id(at, temp), init);
                        newdecls.add(new_vd);
                        int element_index = 0;
                        for (LValueBind lv : lhs) {
                            newdecls.add(new VarDecl(at, Useful.list(lv),
                                    new FieldRef(at, false, init,
                                            new Id(at, "$" + element_index))));
                            element_index++;
                        }
                        return new RewriteHackList(newdecls);
                    }
                    // Leave singleton VarDecl alone, probably, because it gets properly
                    // handled
                    // at the enclosing block.

                } else if (node instanceof FnDef) {
                    // params no longer eligible for com.sun.fortress.interpreter.rewrite.
                    FnDef fndef = (FnDef) node;
                    if (atTopLevelInsideTraitOrObject) {
                        currentSelfName = fndef.getSelfName();
                        e.put(currentSelfName, new SelfRewrite(currentSelfName));
                        }
                    atTopLevelInsideTraitOrObject = false;

                    List<Param> params = fndef.getParams();
                    // fndef.getFnName(); handled at top level.
                    List<StaticParam> tparams = fndef.getStaticParams();

                    paramsToLocals(params);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    AbstractNode n = visitNode(node);
                    dumpIfChange(node, n);
                    return n;

                } else if (node instanceof AbstractObjectExpr) {
                    // all the methods coming from traits are no longer
                    // eligible for com.sun.fortress.interpreter.rewrite.
                    AbstractObjectExpr oe = (AbstractObjectExpr) node;
                    List<? extends AbsDeclOrDecl> defs = oe.getDecls();
                    List<TraitType> xtends = oe.getExtendsClause();
                    // TODO wip

                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    accumulateMembersFromExtends(xtends, e);
                    AbstractNode n = visitNode(node);
                    // REMEMBER THAT THIS IS THE NEW _RewriteObjectExpr!
                    // Implicitly parameterized by either visibleGenericParameters,
                    // or by usedGenericParameters.
                    // Note the keys of a BATree are sorted.
                    n = ExprFactory.make_RewriteObjectExpr((ObjectExpr)n,
                                                          usedGenericParameters);
                    objectExprs.add((_RewriteObjectExpr)n);

                    return n;

                } else if (node instanceof FnExpr) {
                    atTopLevelInsideTraitOrObject = false;
                    FnExpr fndef = (FnExpr) node;
                    List<Param> params = fndef.getParams();
                    paramsToLocals(params);

                } else if (node instanceof LocalVarDecl) {
                    atTopLevelInsideTraitOrObject = false;
                    // defined var is no longer eligible for rewrite.
                    //LocalVarDecl lb = (LocalVarDecl) node;
                    //List<LValue> lvals = lb.getLhs();
                    // TODO wip

                } else if (node instanceof LetFn) {
                    atTopLevelInsideTraitOrObject = false;
                    // defined var is no longer eligible for rewrite.
                    LetFn lf = (LetFn) node;
                    List<FnDef> defs = lf.getFns();
                    defsToLocals(defs);
                    // All the function names are in scope in the function
                    // definitions, and in the body of code that follows them.
                    // TODO wip

                } else if (node instanceof ObjectDecl) {
                    // All the methods and fields defined in object and the
                    // extended traits
                    // are mapped to "self".
                    ObjectDecl od = (ObjectDecl) node;
                    List<? extends AbsDeclOrDecl> defs = od.getDecls();
                    Option<List<Param>> params = od.getParams();
                    List<StaticParam> tparams = od.getStaticParams();
                    List<TraitType> xtends = od.getExtendsClause();
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);
                    paramsToMembers(params);

                    accumulateMembersFromExtends(xtends, e);

                    AbstractNode n = visitNode(node);

                    return n;
                } else if (node instanceof AbsTraitDecl) {
                    AbsTraitDecl td = (AbsTraitDecl) node;
                    List<? extends AbsDecl> defs = td.getDecls();
                    List<StaticParam> tparams = td.getStaticParams();
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    accumulateMembersFromExtends(td);

                    inTrait = true;
                    AbstractNode n = visitNode(node);
                    inTrait = false;
                    return n;
                } else if (node instanceof TraitDecl) {
                    TraitDecl td = (TraitDecl) node;
                    List<? extends Decl> defs = td.getDecls();
                    List<StaticParam> tparams = td.getStaticParams();
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    accumulateMembersFromExtends(td);

                    inTrait = true;
                    AbstractNode n = visitNode(node);
                    inTrait = false;
                    return n;
                } else if (node instanceof For) {
                    For f = (For)node;
                    DoFront df = f.getBody();
                    if (df.isAtomic()) {
                        return NI.nyi("forAtomicDo");
                    }
                    if (df.getLoc().isPresent()) {
                        return NI.nyi("forAtDo");
                    }
                    return visitGeneratorList(f, f.getGens(),
                                              LOOP_ID,
                                              df.getExpr());
                } else {
                    atTopLevelInsideTraitOrObject = false;
                }
                return visitNode(node);
            } finally {
                e = savedE;
                // Copy references from enclosed into enclosing.
                // Set true for all strings s in savedVisible
                // such that s is not in the immediately-defined set
                // (the immediate definition shadows) and where
                // used(s) is true.
                for (String s : savedVisibleGenerics.keySet()) {
                    if (immediateDef == null || ! immediateDef.containsKey(s)) {
                        StaticParam tp  = usedGenericParameters.get(s);
                        if (tp != null)
                            savedUsedGenerics.put(s,tp);
                    }
                }
                visibleGenericParameters = savedVisibleGenerics;
                usedGenericParameters = savedUsedGenerics;
                atTopLevelInsideTraitOrObject = savedFnDefIsMethod;
                objectNestingDepth = savedObjectNestingDepth;
                currentSelfName = savedSelfName;
            }

    }

    /**
     * @param loc   Containing context
     * @param gens  Generators in generator list
     * @return single generator equivalent to the generator list
     */
    Expr visitGeneratorList(HasAt loc, List<Generator> gens,
                                 Id what, Expr body) {
        Span span = body.getSpan();
        for (int i = gens.size()-1; i >= 0; i--) {
            Generator g = gens.get(i);
            Expr loopSel = new FieldRef(g.getSpan(), false, g.getInit(),
                                              what);
            List<Id> binds = g.getBind();
            List<Param> params = new ArrayList<Param>(binds.size());
            for (Id b : binds) params.add(NodeFactory.makeParam(b));
            Expr loopBody = ExprFactory.makeFnExpr(span,params,body);
            span = new Span(span, g.getSpan());
            body = new TightJuxt(span, false, Useful.list(loopSel,loopBody));
        }
        return (Expr)visitNode(body);
    }

    /**
     * @param td
     */
    private void accumulateMembersFromExtends(TraitAbsDeclOrDecl td) {
        accumulateMembersFromExtends(td.getExtendsClause(), traitDisEnvMap.get(td) );
    }

    private void accumulateMembersFromExtends(List<TraitType> xtends, Map<String, Thing> disEnv) {
        Set<String> members = new HashSet<String>();
        Set<String> types = new HashSet<String>();
        Set<AbstractNode> visited = new HashSet<AbstractNode>();
        accumulateTraitsAndMethods(xtends, disEnv, members, types, visited);
        stringsToLocals(types);
        stringsToMembers(members);
    }

    /**
     * @param node
     * @param n
     */
    private void dumpIfChange(AbstractNode old, AbstractNode n) {
        if (false && n != old) {
            try {
                System.err.println("Rewritten method body:");
                NodeUtil.dump(System.err, n);
                System.err.println();
            } catch (IOException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }

        }
    }

    /**
     * @param defs
     */
    private void defsToLocals(List<? extends AbsDeclOrDecl> defs) {
        for (AbsDeclOrDecl d : defs) {
            String s = d.stringName();
            if (d instanceof TraitAbsDeclOrDecl) {
                TraitAbsDeclOrDecl dod = (TraitAbsDeclOrDecl) d;
                traitDisEnvMap.put(dod, e); // dod.setDisEnv(e);
                e.put(s, new Trait(dod));
            } else {
                e.put(s, new Local());
            }
        }
    }

    /**
     * @param params
     */
    private void paramsToLocals(List<? extends Param> params) {
        for (Param d : params) {
            String s = d.getId().getName();
            // "self" is not a local.
            if (! s.equals(currentSelfName))
                e.put(s, new Local());
        }
    }

    /**
     * @param params
     */
    private BATree<String, Boolean> tparamsToLocals(List<StaticParam> params, BATree<String, Boolean> immediateDef) {
        if (!params.isEmpty())
            for (StaticParam d : params) {
                String s = NodeUtil.getName(d);
                e.put(s, new Local());
                visibleGenericParameters.put(s, d);
                immediateDef = addToImmediateDef(immediateDef, s);
            }
        return immediateDef;
    }

    /**
     * @param immediateDef
     * @param s
     * @return
     */
    private BATree<String, Boolean> addToImmediateDef(BATree<String, Boolean> immediateDef, String s) {
        if (immediateDef == null)
            immediateDef = new BATree<String, Boolean>(StringComparer.V);
        immediateDef.put(s, Boolean.FALSE);
        return immediateDef;
    }

    /**
     * @param defs
     */
    private void defsToMembers(List<? extends AbsDeclOrDecl> defs) {
        for (AbsDeclOrDecl d : defs)
            for (String s: NodeUtil.stringNames(d))
            e.put(s, new Member());

    }

    /**
     * @param params
     */
    private void paramsToMembers(Option<List<Param>> params) {
        if (params.isPresent())
            for (Param d : params.getVal()) {
                String s = d.getId().getName();
                e.put(s, new Member());
            }
    }

    /**
     * @param params
     */
    private void stringsToMembers(Collection<String> strings) {
        for (String s : strings) {
            e.put(s, new Member());
        }
    }

    /**
     * @param params
     */
    private void stringsToLocals(Collection<String> strings) {
        for (String s : strings) {
            e.put(s, new Local());
        }
    }

    /**
     * @param xtends
     *            List of types that a trait/object extends
     * @param typeEnv
     *            The environment in which those names are interpreted
     * @param members
     *            (output) all the members transitively introduced by all
     *            extended traits
     * @param types
     *            (output) all the names of all the types transitively extended
     * @param visited
     *            (bookkeeping) to prevent revisiting, and possible looping on
     *            bad inputs
     */
    private void accumulateTraitsAndMethods(List<TraitType> xtends,
            Map<String, Thing> typeEnv, Set<String> members, Set<String> types,
            Set<AbstractNode> visited) {

            for (TypeRef t : xtends) {
                List<Id> names = new ArrayList<Id>();
                // First de-parameterize the type
                if (t instanceof InstantiatedType) {
                    names = (((InstantiatedType) t).getDottedId()).getNames();
                } else if (t instanceof IdType) {
                    names = (((IdType) t).getDottedId()).getNames();;
                } else {
                   // TODO Way too many types; deal with them as necessary.
                   NI.nyi("Object extends something exciting: " + t);
                }
                    if (names.size() == 1) {
                        // TODO we've got to generalize this to DottedId names.
                        String s = names.get(0).getName();
                        Thing th;
                        try {
                            th = typeEnv.get(s);
                        } catch (NullPointerException x) {
                            throw new InterpreterError("Entity "+s+" not found in typeEnv "+typeEnv);
                        }
                        if (th instanceof Trait) {
                            Trait tr = (Trait) th;
                            TraitAbsDeclOrDecl tdod = tr.defOrDecl;
                            if (!(visited.contains(tdod))) {
                                visited.add(tdod);
                                // Process this trait -- add its name, as well
                                // as all the members
                                // types.add(s); // The trait is known by this
                                                // name.
                                for (AbsDeclOrDecl dd : tdod.getDecls()) {
                                    members.add(dd.stringName());
                                }
                                accumulateTraitsAndMethods(tdod.getExtendsClause(),
                                        traitDisEnvMap.get(tdod), members, types,
                                        visited);
                            }
                        } else if (th==null) {
                            /* This was missing the "throw" for a long
                             * time, and adding it back in actually
                             * broke tests/extendAny.fss.  Oddly it
                             * only seems to catch this case; if we
                             * name an actually bogus type that causes
                             * a more meaningful failure elsewhere.
                             * Consequently we leave it commented out
                             * for the moment.
                             */
                            // throw new ProgramError(t,"TypeRef extends non-visible entity " + s);
                        } else {
                            NI.nyi("TypeRef extends something unknown " + s + " = " + th);
                        }
                    } else {
                        NI.nyi("General dotted name");
                    }
            }
    }
}
