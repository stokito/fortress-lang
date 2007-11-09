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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.interpreter.env.BetterEnv;
import com.sun.fortress.interpreter.evaluator.BuildEnvironments;
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes_util.UIDMapFactory;
import com.sun.fortress.nodes_util.UIDObject;
import com.sun.fortress.nodes.AbsFnDecl;
import com.sun.fortress.nodes.AbsVarDecl;
import com.sun.fortress.nodes.AliasedDottedName;
import com.sun.fortress.nodes.Api;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.CompilationUnit;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.AbsDeclOrDecl;
import com.sun.fortress.nodes.AbsDecl;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.AbsTraitDecl;
import com.sun.fortress.nodes.DottedName;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportApi;
import com.sun.fortress.nodes.Juxt;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.NodeAbstractVisitor;
import com.sun.fortress.nodes.NormalParam;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.TestDecl;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.DoFront;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.FieldRef;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnDef;
import com.sun.fortress.nodes.For;
import com.sun.fortress.nodes.Generator;
import com.sun.fortress.nodes.GeneratedExpr;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdName;
import com.sun.fortress.nodes.IdType;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.LetFn;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.AbstractNode;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.AbstractObjectExpr;
import com.sun.fortress.nodes.ObjectExpr;
import com.sun.fortress.nodes.OprExpr;
import com.sun.fortress.nodes.QualifiedOpName;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.VarargsParam;
import com.sun.fortress.nodes._RewriteFieldRef;
import com.sun.fortress.nodes._RewriteFnRef;
import com.sun.fortress.nodes._RewriteObjectExpr;
import com.sun.fortress.nodes.Spawn;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.InstantiatedType;
import com.sun.fortress.nodes.QualifiedIdName;
import com.sun.fortress.nodes_util.RewriteHackList;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Voidoid;

import static com.sun.fortress.interpreter.evaluator.ProgramError.errorMsg;
import static com.sun.fortress.interpreter.evaluator.ProgramError.error;
import static com.sun.fortress.interpreter.evaluator.InterpreterBug.bug;

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
public class Desugarer extends Rewrite {

    public final static IdName LOOP_NAME =
        NodeFactory.makeIdName(WellKnownNames.loopMethod);

    private boolean isLibrary;
    private final static boolean debug = false;

    private class Thing {
        int nestedness;
        Thing() { nestedness = objectNestingDepth; }
        /** May assume {@code original} has a non-zero length. */
        Expr replacement(VarRef original) {
            return original;
        }
        public String toString() { return "Thing@"+nestedness; }
    }

    private class Local extends Thing {
        public String toString() { return "Local@"+nestedness; }
    }

    /**
     * Traits need to identify their declaration, for purposes of figuring out
     * what names are in scope, though not necessarily what they mean.
     */
    private class Trait extends Local {
        TraitAbsDeclOrDecl defOrDecl;
        Map<String, Thing> env;
        Trait(TraitAbsDeclOrDecl dod, Map<String, Thing> env) { defOrDecl = dod; this.env = env; }
        public String toString() { return "Trait="+defOrDecl; }
    }


    static IdName filterQID(QualifiedIdName qid) {
        if (qid.getApi().isNone())
            return qid.getName();
        return bug("Not yet prepared for QIDsref'd through self/parent, QID=" + NodeUtil.dump(qid));
    }

    private class Member extends Thing {
        Expr replacement(VarRef original) {
            _RewriteFieldRef fs = new _RewriteFieldRef(original.getSpan(), false,
                                               // Use this constructor
                // here because it is a
                // com.sun.fortress.interpreter.rewrite.
                dottedReference(original.getSpan(),
                                objectNestingDepth - nestedness), filterQID(original.getVar()));
        return fs;
        }

        public String toString() { return "Member@"+nestedness; }
    }

    private class SelfRewrite extends Member {
        String s;
        SelfRewrite(String s) { this.s = s; }
        Expr replacement(VarRef original) {
            Expr expr = dottedReference(original.getSpan(),
                    objectNestingDepth - nestedness);
            return expr;
        }
        public String toString() { return "Self("+s+")@"+nestedness; }
    }


    private static class IsAnArrowName extends NodeAbstractVisitor<Boolean> {

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forLValueBind(com.sun.fortress.nodes.LValueBind)
         */
        @Override
        public Boolean forLValueBind(LValueBind that) {
            return optionTypeIsArrow(that.getType());
        }

//        /* (non-Javadoc)
//         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forAbsVarDecl(com.sun.fortress.nodes.AbsVarDecl)
//         */
//        @Override
//        public Boolean forAbsVarDecl(AbsVarDecl that) {
//            // TODO Auto-generated method stub
//            return super.forAbsVarDecl(that);
//        }
//
//        /* (non-Javadoc)
//         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forVarDecl(com.sun.fortress.nodes.VarDecl)
//         */
//        @Override
//        public Boolean forVarDecl(VarDecl that) {
//            // TODO Auto-generated method stub
//            return super.forVarDecl(that);
//        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forAbsFnDecl(com.sun.fortress.nodes.AbsFnDecl)
         */
        @Override
        public Boolean forAbsFnDecl(AbsFnDecl that) {
            // Return "is a self method"
            return NodeUtil.selfParameterIndex(that) >= 0;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forFnDef(com.sun.fortress.nodes.FnDef)
         */
        @Override
        public Boolean forFnDef(FnDef that) {
         // Return "is a self method"
            return NodeUtil.selfParameterIndex(that) >= 0;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forTestDecl(com.sun.fortress.nodes.TestDecl)
         */
        @Override
        public Boolean forTestDecl(TestDecl that) {
            // FALSE
            return Boolean.FALSE;
        }



        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forNormalParam(com.sun.fortress.nodes.NormalParam)
         */
        @Override
        public Boolean forNormalParam(NormalParam that) {
            return optionTypeIsArrow(that.getType());
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forVarargsParam(com.sun.fortress.nodes.VarargsParam)
         */
        @Override
        public Boolean forVarargsParam(VarargsParam that) {
            return Boolean.FALSE;
        }

        private Boolean optionTypeIsArrow(Option<Type> ot) {
            return (ot.isSome() && Option.unwrap(ot) instanceof ArrowType);
        }

    }

    /**
     * Visitor, returning true if something's name is an "arrow" -- that is, it
     * can be invoked, but is not an object.
     */
    final static IsAnArrowName isAnArrowName = new IsAnArrowName();

    /**
     * Rewritings in scope.
     */
    private BATree<String, Thing> rewrites;

    /**
     * Things that are arrow-typed (used to avoid method-invoking self fields)
     */
    private BASet<String> arrows;

    /**
     * Generic parameters currently in scope.
     */
    private BATree<String, StaticParam> visibleGenericParameters;
    private BATree<String, StaticParam> usedGenericParameters;

    /**
     * All the object exprs (this may generalize to nested functions as well)
     * need to have their gensym'd types and constructors registered at the
     * top level.  The top level environment is available to the creator/caller
     * of Disambiguate.
     */
    private ArrayList<_RewriteObjectExpr> objectExprs = new ArrayList<_RewriteObjectExpr>();

    Desugarer(BATree<String, Thing> initial,
              BATree<String, StaticParam> initialGenericScope,
              BASet<String> initialArrows) {
        rewrites = initial;
        arrows = initialArrows;
        visibleGenericParameters = initialGenericScope;
        usedGenericParameters = new BATree<String, StaticParam>(StringComparer.V);
        // packages = new BASet<String>(StringComparer.V);
    }

    public Desugarer(boolean isLibrary) {
        this(new BATree<String, Thing>(StringComparer.V),
             new BATree<String, StaticParam>(StringComparer.V),
             new BASet<String>(StringComparer.V));
        this.isLibrary = isLibrary;
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
                FTypeObject fto = new FTypeObject(name, env, oe, oe.getDecls(), oe);
                env.putType(name, fto);
                BuildEnvironments.finishObjectTrait(NodeUtil.getTypes(oe.getExtendsClause()),
                                                    null, null, fto, env, oe);
                Constructor con = new Constructor(env, fto, oe,
                                                  NodeFactory.makeIdName(name),
                                                  oe.getDecls(),
                                                  Option.<List<Param>>none());

                env.putValue(name, con);
                con.finishInitializing();
            } else {
                // Generic constructor
                FTypeGeneric fto = new FTypeGeneric(env, oe, oe.getDecls(), oe);
                env.putType(name, fto);
                GenericConstructor con = new GenericConstructor(env, oe, NodeFactory.makeIdName(name));
                env.putValue(name, con);
            }
        }
    }

    Expr newName(VarRef vre, String s) {
        Thing t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            return t.replacement(vre);
        }

    }

//    Iterable<Id> newName(Iterable<Id> ids, String s) {
//        Thing t = e.get(s);
//        if (t == null) {
//            return ids;
//        } else {
//            return t.replacement(ids);
//        }
//    }

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
            return new _RewriteFieldRef(s, false, dottedReference(s, i - 1),
                                      new IdName(s, new Id(s, WellKnownNames.secretParentName)));
        } else {
            throw new Error("Confusion in member reference numbering.");
        }

    }

    public void preloadTopLevel(CompilationUnit tlnode) {
        if (tlnode instanceof  Component) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Component com = (Component) tlnode;
            List<? extends AbsDeclOrDecl> defs = com.getDecls();
            defsToLocals(defs);
        } else if (tlnode instanceof Api) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Api com = (Api) tlnode;
            List<? extends AbsDeclOrDecl> defs = com.getDecls();
            defsToLocals(defs);
        }

    }

    public boolean injectAtTopLevel(String putName, String getName, Desugarer getFrom, Set<String>excluded) {
        Thing th = getFrom.rewrites.get(getName);
        Thing old = rewrites.get(putName);
        /* Equal means no change */
        if (old == th)
            return false;
        /* Empty means do  add */
        if (old == null) {
            rewrites.put(putName, th);
            return true;
        }
        excluded.add(putName);
        rewrites.remove(putName);

        return true;

    }

    /**
     * Performs an object-reference-disambiguating com.sun.fortress.interpreter.rewrite of the AST. Any
     * effects on the environment from the toplevel remain visible, otherwise
     * visit only rewrites.
     */
    @Override
    public AbstractNode visit(AbstractNode node) {
//        BATree<String, Thing> savedE = rewrites.copy();
//        BASet<String> savedA = arrows.copy();
//        BATree<String, StaticParam> savedVisibleGenerics = visibleGenericParameters.copy();
//        BATree<String, StaticParam> savedUsedGenerics = usedGenericParameters.copy();
//        BATree<String, Boolean> immediateDef = null;

        BATree<String, Thing> savedE = rewrites;
        rewrites = rewrites.copy();

        BASet<String> savedA = arrows;
        arrows = arrows.copy();

        BATree<String, StaticParam> savedVisibleGenerics = visibleGenericParameters;
        visibleGenericParameters = visibleGenericParameters.copy();

        BATree<String, StaticParam> savedUsedGenerics = usedGenericParameters;
        usedGenericParameters = usedGenericParameters.copy();

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

            // This next bit is not going to be used.
//            List<Import> imports = com.getImports();
//            for (Import imp : imports) {
//                if (imp instanceof ImportApi) {
//                    ImportApi impapi = (ImportApi) imp;
//                    List<AliasedDottedName> ladn = impapi.getApis();
//                    for (AliasedDottedName adn : ladn) {
//                        DottedName dn = adn.getApi();
//                        Option<DottedName> odn = adn.getAlias();
//                        dn = odn.isSome() ? Option.unwrap(odn, dn) : dn;
//                        packages.add(NodeUtil.nameString(dn));
//                    }
//                }
//            }

            if (debug && ! isLibrary)
                System.err.println("BEFORE\n" + NodeUtil.dump(node));

            AbstractNode nn = visitNode(node);

            if (debug && ! isLibrary)
                System.err.println("AFTER\n" + NodeUtil.dump(nn));


            return nn;

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
                 * NOTE: "default" handling for any nodes not mentioned explicitly is:
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

                    String s = vrToString(vre);
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }
                    Expr update = newName(vre, s);
                    return update;
                } else if (node instanceof OprExpr) {
                    node = cleanupOprExpr((OprExpr)node);
                } else if (node instanceof _RewriteFnRef) {

                    _RewriteFnRef fr = (_RewriteFnRef) node;

                } else if (node instanceof TightJuxt&& looksLikeMethodInvocation((Juxt) node)) {
                    return translateJuxtOfDotted((Juxt) node);

                  // This is a duplicate of the rewriting that occurs
                  // in RewriteInAbsenceOfTypeInfo


                }
                else if (node instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) node;
                    Id id = lvb.getName().getId();
                    if ("_".equals(id.getText())) {
                        Id newId = new Id(id.getSpan(), "_$" + id.getSpan());
                        return NodeFactory.makeLValue(lvb, newId);
                    }
                } else if (node instanceof IdType) {
                    IdType vre = (IdType) node;
                    String s = NodeUtil.nameString(vre.getName());
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

                    // Rewrite this to a getter?
                } else if ( node instanceof Assignment) {

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
                        VarDecl new_vd = NodeFactory.makeVarDecl(at, temp, init);
                        newdecls.add(new_vd);
                        int element_index = 0;
                        for (LValueBind lv : lhs) {
                            IdName newName = NodeFactory.makeIdName(at,
                                                                    "$" + element_index);
                            newdecls.add(new VarDecl(at, Useful.list(lv),
                                    new _RewriteFieldRef(at, false, init, newName)));
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
                        rewrites.put(currentSelfName, new SelfRewrite(currentSelfName));
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
                    List<TraitType> xtends = NodeUtil.getTypes(oe.getExtendsClause());
                    // TODO wip

                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    accumulateMembersFromExtends(xtends, rewrites);
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
                    List<TraitType> xtends = NodeUtil.getTypes(od.getExtendsClause());
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);
                    paramsToMembers(params);

                    accumulateMembersFromExtends(xtends, rewrites);

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
                    Do doBlock = new Do(df.getSpan(),Useful.list(df));
                    return visitGeneratorList(f, f.getGens(), LOOP_NAME, doBlock);
                } else if (node instanceof GeneratedExpr) {
                    GeneratedExpr ge = (GeneratedExpr)node;
                    return visitGeneratorList(ge, ge.getGens(), LOOP_NAME, ge.getExpr());
                } else if (node instanceof Spawn) {
                    return translateSpawn((Spawn)node);
                } else {
                    atTopLevelInsideTraitOrObject = false;
                }
                return visitNode(node);
            } finally {
                rewrites = savedE;
                arrows = savedA;
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

    private String vrToString(VarRef vre) {
        Iterable<Id> ids = NodeUtil.getIds(vre.getVar());

        String s = IterUtil.first(ids).getText();
        return s;
    }

    /**
     * @param loc   Containing context
     * @param gens  Generators in generator list
     * @return single generator equivalent to the generator list
     */
    Expr visitGeneratorList(HasAt loc, List<Generator> gens,
                                 IdName what, Expr body) {
        Span span = body.getSpan();
        for (int i = gens.size()-1; i >= 0; i--) {
            Generator g = gens.get(i);
            Expr loopSel = new FieldRef(g.getSpan(), false, g.getInit(), what);
            List<IdName> binds = g.getBind();
            List<Param> params = new ArrayList<Param>(binds.size());
            for (IdName b : binds) params.add(NodeFactory.makeParam(b));
            Expr loopBody = ExprFactory.makeFnExpr(span,params,body);
            span = new Span(span, g.getSpan());
            body = new TightJuxt(span, false, Useful.list(loopSel,loopBody));
        }
        return (Expr)visitNode(body);
    }

    /** Given expr e, return (fn () => e) */
    Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(e.getSpan(), Collections.EMPTY_LIST, e);
    }

    private Expr cleanupOprExpr(OprExpr opExp) {
        if (opExp.getOps().size() != 1) {
            return bug(opExp,
                errorMsg("OprExpr with multiple operators ",opExp));
        }
        List<Expr> args = opExp.getArgs();
        if (args.size() <= 1) return opExp;
        QualifiedOpName qop = opExp.getOps().get(0);
        if (!OprUtil.isInfixOrPrefix(qop)) return opExp;
        boolean prefix = OprUtil.hasPrefixColon(qop);
        boolean suffix = OprUtil.hasSuffixColon(qop);
        if (!prefix && !suffix) return opExp;
        qop = OprUtil.noColon(qop);
        Iterator<Expr> i = args.iterator();
        Expr res = i.next();
        Span sp = opExp.getSpan();
        while (i.hasNext()) {
            Expr arg = (Expr)i.next();
            if (prefix) {
                res = thunk(res);
            }
            if (suffix) {
                arg = thunk(arg);
            }
            res = ExprFactory.makeOprExpr(sp,qop,res,arg);
        }
        return res;
    }


    /**
     * @param td
     */
    private void accumulateMembersFromExtends(TraitAbsDeclOrDecl td) {
        accumulateMembersFromExtends(NodeUtil.getTypes(td.getExtendsClause()),
                rewrites);
    }

    private void accumulateMembersFromExtends(List<TraitType> xtends, Map<String, Thing> disEnv) {
        Set<String> members = new HashSet<String>();
        Set<String> types = new HashSet<String>();
        Set<String> arrow_names = new HashSet<String>();
        Set<String> not_arrow_names = new HashSet<String>();
        Set<AbstractNode> visited = new HashSet<AbstractNode>();
        accumulateTraitsAndMethods(xtends, disEnv, members, types, arrow_names, not_arrow_names, visited);
        stringsToLocals(types);
        stringsToMembers(members);
        arrows.removeAll(not_arrow_names);
        arrows.addAll(arrow_names);
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
                rewrites.put(s, new Trait(dod, rewrites));
            } else {
                rewrites.put(s, new Local());
            }
        }
    }

    /**
     * @param params
     */
    private void paramsToLocals(List<? extends Param> params) {
        for (Param d : params) {
            String s = d.getName().getId().getText();
            // "self" is not a local.
            if (! s.equals(currentSelfName)) {
                rewrites.put(s, new Local());

            }
        }
    }

    /**
     * @param params
     */
    private BATree<String, Boolean> tparamsToLocals(List<StaticParam> params, BATree<String, Boolean> immediateDef) {
        if (!params.isEmpty())
            for (StaticParam d : params) {
                String s = NodeUtil.getName(d);
                rewrites.put(s, new Local());
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
        for (AbsDeclOrDecl d : defs) {
            for (String s: NodeUtil.stringNames(d))
            rewrites.put(s, new Member());
        }

    }

    /**
     * @param params
     */
    private void paramsToMembers(Option<List<Param>> params) {
        if (params.isSome())
            for (Param d : Option.unwrap(params)) {
                String s = d.getName().getId().getText();
                rewrites.put(s, new Member());
                if (d.accept(isAnArrowName))
                    arrows.add(s);
                else
                    arrows.remove(s);
            }
    }



    /**
     * @param params
     */
    private void stringsToMembers(Collection<String> strings) {
        for (String s : strings) {
            rewrites.put(s, new Member());
            arrows.remove(s);
        }
    }

    /**
     * @param params
     */
    private void stringsToLocals(Collection<String> strings) {
        for (String s : strings) {
            rewrites.put(s, new Local());
            arrows.remove(s);
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
            Set<String> arrow_names, Set<String> not_arrow_names,
            Set<AbstractNode> visited) {

            for (Type t : xtends) {
                QualifiedIdName qName = null;
                // First de-parameterize the type
                if (t instanceof InstantiatedType) {
                    qName = ((InstantiatedType) t).getName();
                } else if (t instanceof IdType) {
                    qName = ((IdType) t).getName();
                } else {
                   // TODO Way too many types; deal with them as necessary.
                    bug(t, errorMsg("Object extends something exciting: ", t));
                }
                if (qName.getApi().isNone()) {
                    // TODO we've got to generalize this to qualified names.
                    String s = qName.getName().getId().getText();
                    Thing th;
                    try {
                        th = typeEnv.get(s);
                    } catch (NullPointerException x) {
                        String msg = errorMsg("Entity ", s, " not found in typeEnv ",
                                              typeEnv);
                        th = bug(msg);
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
                                String sdd = dd.stringName();
                                if (dd instanceof VarDecl) {
                                    visited.add(tdod);
                                } else if (dd instanceof AbsVarDecl) {
                                    visited.add(tdod);
                                } else {
                                    if (dd.accept(isAnArrowName)) {
                                        arrow_names.add(sdd);
                                    } else {
                                        not_arrow_names.add(sdd);
                                    }
                                }
                                members.add(sdd);
                            }
                            accumulateTraitsAndMethods(NodeUtil.getTypes(tdod.getExtendsClause()),
                                                       tr.env, members, types,
                                                       arrow_names, not_arrow_names,
                                                       visited);
                        }
                    } else if (th instanceof Object) {
                        error(t, errorMsg("Attempt to extend object type ", s));
                    }
                    else if (th==null) {
                        /* This was missing the "throw" for a long
                         * time, and adding it back in actually
                         * broke tests/extendAny.fss.  Oddly it
                         * only seems to catch this case; if we
                         * name an actually bogus type that causes
                         * a more meaningful failure elsewhere.
                         * Consequently we leave it commented out
                         * for the moment.
                         */
                        // error(t,"Type extends non-visible entity " + s);
                    } else {
                        error(t, errorMsg("Attempt to extend type ", s, " (which maps to the following Thing: ", th, ")"));
                    }
                } else {
                    NI.nyi("General qualified name");
                }
            }
    }

    private AbstractNode translateJuxtOfDotted(Juxt node) {
        List<Expr> exprs = node.getExprs();
        VarRef first = (VarRef) exprs.get(0);
        QualifiedIdName qidn = first.getVar();

        // Optimistic casts here, will need revisiting in the future,
        // perhaps FieldRefs are too general
        // Recursive visits here
        _RewriteFieldRef selfDotSomething = (_RewriteFieldRef) visit(first);
        List<Expr> visitedArgs = visitList(exprs.subList(1, exprs.size()));

        return new MethodInvocation(node.getSpan(),
                                false,
                                selfDotSomething.getObj(), // this will rewrite in the future.
                                (IdName) selfDotSomething.getField(),
                                visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteral(node.getSpan()) : // wrong span
                                visitedArgs.size() == 1 ? visitedArgs.get(0) :
                                    new TupleExpr(visitedArgs));
    }

    private AbstractNode translateSpawn(Spawn s) {
        Expr body = s.getBody();
        Span sp   = s.getSpan();
        // If the user writes Spawn(foo) instead of Spawn(foo()) we get an inexplicable error
        // message.  We might want to put in a check for that someday.

        AbstractNode rewrittenExpr =  visit(body);

        Expr in_fn = new VarRef(sp, new QualifiedIdName(new IdName(sp, new Id("PrimitiveThread"))));
        List<StaticArg> args = new ArrayList<StaticArg>();
        args.add(new TypeArg(new IdType( new QualifiedIdName(sp, new IdName(sp, new Id("Any"))))));

        _RewriteFnRef fn = new _RewriteFnRef(in_fn, args);

        List<Param> params = new ArrayList<Param>();
        FnExpr fnExpr = new FnExpr(sp, params, (Expr) rewrittenExpr);
        List<Expr> exprs = new ArrayList<Expr>();
        exprs.add(fn);

        exprs.add(fnExpr);

        TightJuxt juxt = new TightJuxt(s.getSpan(), false, exprs);
        return juxt;
    }

    private boolean looksLikeMethodInvocation(Juxt node) {
        Expr first = node.getExprs().get(0);
        if (first instanceof VarRef) {
            VarRef vr = (VarRef) first;
            String s = vrToString(vr);
            if (rewrites.get(s) instanceof Member && ! arrows.contains(s))
                return true;
        }
        return false;
    }

    public Set<String> getTopLevelRewriteNames() {
        return rewrites.keySet();
    }
}
