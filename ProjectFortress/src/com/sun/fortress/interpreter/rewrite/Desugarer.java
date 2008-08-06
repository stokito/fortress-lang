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
import com.sun.fortress.interpreter.evaluator.Environment;
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
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.RewriteHackList;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.OprUtil;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.ExtentRange;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.LValueBind;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.TightJuxt;
import com.sun.fortress.nodes.TraitAbsDeclOrDecl;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.UnpastingBind;
import com.sun.fortress.nodes.UnpastingSplit;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes._RewriteFieldRef;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.HasAt;
import com.sun.fortress.useful.NI;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.Useful;
import com.sun.fortress.useful.Voidoid;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import static com.sun.fortress.nodes_util.DesugarerUtil.*;

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

    private boolean isLibrary;
    private final static boolean debug = false;

    private class Thing {
        int objectNestedness;
        int lexicalNestedness;
        Thing() {
            objectNestedness = objectNestingDepth;
            lexicalNestedness = lexicalNestingDepth;
        }

        public boolean equals(Object o) {
            if (o.getClass() == getClass()) {
                Thing that = (Thing) o;
                if (that.objectNestedness == this.objectNestedness &&
                        that.lexicalNestedness == this.lexicalNestedness)
                    return true;
            }
            return false;
        }

        /** May assume {@code original} has a non-zero length. */
        Expr replacement(VarRef original) {
            return NodeFactory.makeVarRef(original, lexicalNestedness);
        }

        Expr replacement(FnRef original) {
            return NodeFactory.makeFnRef(original, lexicalNestedness);
        }
        Expr replacement(OpRef original) {
            return NodeFactory.makeOpRef(original, lexicalNestedness);
        }
        VarType replacement(VarType original) {
             return NodeFactory.makeVarType(original, lexicalNestedness);
        }
        TraitType replacement(TraitType original) {
            return NodeFactory.makeTraitType(original, lexicalNestedness);
        }
        public String toString() { return "Thing@"+objectNestedness+"/"+lexicalNestedness; }
    }

    private class Local extends Thing {
        public String toString() { return "Local@"+objectNestedness+"/"+lexicalNestedness; }
    }

    public class FunctionalMethod extends Local {
        public String toString() { return "FunctionalMethod@"+objectNestedness+"/"+lexicalNestedness; }
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

        public boolean equals (Object o) {
            if (super.equals(o)) {
                Trait that = (Trait) o;
                // Conservative definition for now.
                // Full "equals" applied to "env" would probably not terminate
                // because of cycles.
                return (that.defOrDecl == this.defOrDecl &&
                        that.env == this.env);
            }
            return false;
        }
    }


    static Id filterQID(Id qid) {
        if (qid.getApi().isNone())
            return qid;
        return bug("Not yet prepared for QIDs ref'd through self/parent, QID=" + NodeUtil.dump(qid));
    }

    private class Member extends Thing {
        private boolean isTransient = false;
        Member() { super(); }
        Member(boolean _transient) {
            super();
            if (_transient) isTransient = true;
        }
        @Override
        Expr replacement(VarRef original) {
            if (isTransient) {
                return original;
            } else {
                return new _RewriteFieldRef(original.getSpan(), false,
                                            // Use this constructor
                                            // here because it is a
                                            // com.sun.fortress.interpreter.rewrite.
                                            dottedReference(original.getSpan(),
                                                            objectNestingDepth - objectNestedness),
                                            filterQID(original.getVar()));
            }
        }

        public String toString() {
            if (isTransient)
                return "TransientMember@"+objectNestedness+"/"+lexicalNestedness;
            else
                return "Member@"+objectNestedness+"/"+lexicalNestedness;
        }
    }

    private class SelfRewrite extends Member {
        String s;
        SelfRewrite(String s) { this.s = s; }
        Expr replacement(VarRef original) {
            Expr expr = dottedReference(original.getSpan(),
                    objectNestingDepth - objectNestedness);
            return expr;
        }
        public String toString() { return "Self("+s+")@"+objectNestedness+"/"+lexicalNestedness; }
    }


    public static class ArrowOrFunctional {
        public String toString() {
            return s;
        }
        ArrowOrFunctional(String s) {
            this.s = s;
        }
        private final String s;
    };
    public final static ArrowOrFunctional NEITHER = new ArrowOrFunctional("NEITHER");
    public final static ArrowOrFunctional ARROW = new ArrowOrFunctional("ARROW");
    public final static ArrowOrFunctional FUNCTIONAL = new ArrowOrFunctional("FUNCTIONAL");

    public static class IsAnArrowName extends NodeAbstractVisitor<ArrowOrFunctional> {

        @Override
        public ArrowOrFunctional defaultCase(Node that) {
            return NEITHER;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forLValueBind(com.sun.fortress.nodes.LValueBind)
         */
        @Override
        public ArrowOrFunctional forLValueBind(LValueBind that) {
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
        public ArrowOrFunctional forAbsFnDecl(AbsFnDecl that) {
            // Return "is a self method"
            return NodeUtil.selfParameterIndex(that) >= 0  ? FUNCTIONAL : NEITHER;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forFnDef(com.sun.fortress.nodes.FnDef)
         */
        @Override
        public ArrowOrFunctional forFnDef(FnDef that) {
         // Return "is a self method"
            return NodeUtil.selfParameterIndex(that) >= 0 ? FUNCTIONAL : NEITHER;
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forTestDecl(com.sun.fortress.nodes.TestDecl)
         */
        @Override
        public ArrowOrFunctional forTestDecl(TestDecl that) {
            // FALSE
            return NEITHER;
        }



        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forNormalParam(com.sun.fortress.nodes.NormalParam)
         */
        @Override
        public ArrowOrFunctional forNormalParam(NormalParam that) {
            return optionTypeIsArrow(that.getType());
        }

        /* (non-Javadoc)
         * @see com.sun.fortress.nodes.NodeAbstractVisitor#forVarargsParam(com.sun.fortress.nodes.VarargsParam)
         */
        @Override
        public ArrowOrFunctional forVarargsParam(VarargsParam that) {
            return NEITHER;
        }

        private ArrowOrFunctional optionTypeIsArrow(Option<Type> ot) {
            return ot.unwrap(null) instanceof ArrowType ? ARROW : NEITHER;
        }



    }

    /**
     * Visitor, returning true if something's name is an "arrow" -- that is, it
     * can be invoked, but is not an object.
     */
    public final static IsAnArrowName isAnArrowName = new IsAnArrowName();

    /**
     * Rewritings in scope.
     */
    private BATree<String, Thing> rewrites;

    public Thing rewrites_put(String k, Thing d) {
        return rewrites.put(k, d);
    }

    /**
     * Things that are arrow-typed (used to avoid method-invoking self fields)
     */
    private BASet<String> arrows;

    /**
     * Generic parameters currently in scope.
     */
    private BATree<String, StaticParam> visibleGenericParameters;
    private BATree<String, StaticParam> usedGenericParameters;
    public BASet<String> topLevelUses;
    public BASet<String> functionals;

    protected void noteUse(Thing t, String s) {
        if (t.lexicalNestedness == 0 || functionals.contains(s))
            topLevelUses.add(s);
    }
    /**
     * Experimental -- when would this ever apply?
     * @param s
     */
    protected void noteUse(String s, VarRef vre) {
        if (functionals.contains(s))
            topLevelUses.add(s);
        else
            System.err.println(s + " used at " + vre.at());
    }
    /**
     * All the object exprs (this may generalize to nested functions as well)
     * need to have their gensym'd types and constructors registered at the
     * top level.  The top level environment is available to the creator/caller
     * of Disambiguate.
     */
    private ArrayList<_RewriteObjectExpr> objectExprs = new ArrayList<_RewriteObjectExpr>();

    Desugarer(BATree<String, Thing> initial,
              BATree<String, StaticParam> initialGenericScope,
              BASet<String> initialArrows,
              BASet<String> initialTopLevelUses,
              BASet<String> initialFunctionals) {
        rewrites = initial;
        arrows = initialArrows;
        visibleGenericParameters = initialGenericScope;
        usedGenericParameters = new BATree<String, StaticParam>(StringComparer.V);
        topLevelUses = initialTopLevelUses;
        functionals = initialFunctionals;
        // packages = new BASet<String>(StringComparer.V);
    }

    public Desugarer(boolean isLibrary) {
        this(new BATree<String, Thing>(StringComparer.V),
             new BATree<String, StaticParam>(StringComparer.V),
             new BASet<String>(StringComparer.V),
             new BASet<String>(StringComparer.V),
             new BASet<String>(StringComparer.V)
             );
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

    /**
     * Zero for toplevel, otherwise equal to the number of enclosing scopes,
     * for some definition of scope.  The definition of scope will be adjusted
     * as is convenient to the implementation.
     */
    int lexicalNestingDepth;

    boolean inTrait;

    String currentSelfName = WellKnownNames.defaultSelfName;
    boolean atTopLevelInsideTraitOrObject = false; // true immediately within a trait/object

    /**
     * Adds, to the supplied environment, constructors for any object
     * expressions encountered in the tree(s) processed by this Disambiguator.
     * @param env
     */
    public void registerObjectExprs(Environment env) {
        for (_RewriteObjectExpr oe : objectExprs) {
            String name = oe.getGenSymName();
            List<StaticParam> params = oe.getStaticParams();
            if (params.isEmpty()) {
                // Regular constructor
                FTypeObject fto = new FTypeObject(name, env, oe, oe.getParams(),
                                                  oe.getDecls(), oe);
                env.putType(name, fto);
                BuildEnvironments.finishObjectTrait(NodeUtil.getTypes(oe.getExtendsClause()),
                                                    null, null, fto, env, oe);
                Constructor con = new Constructor(env, fto, oe,
                                                  NodeFactory.makeId(name),
                                                  oe.getDecls(),
                                                  Option.<List<Param>>none());

                env.putValue(name, con);
                con.finishInitializing();
            } else {
                // Generic constructor
                FTypeGeneric fto = new FTypeGeneric(env, oe, oe.getDecls(), oe);
                env.putType(name, fto);
                GenericConstructor con = new GenericConstructor(env, oe, NodeFactory.makeId(name));
                env.putValue(name, con);
            }
        }
    }

    Expr newName(VarRef vre, String s) {
        Thing t = rewrites.get(s);
        if (t == null) {
            noteUse(s, vre);
            return vre;
        } else {
            noteUse(t,s);
            return t.replacement(vre);
        }

    }

    Expr newName(OpRef vre, String s) {
        Thing t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            noteUse(t,s);
            return t.replacement(vre);
        }

    }

    Expr newName(_RewriteObjectRef vre, String s) {
        Thing t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            noteUse(t,s);
        }
        return vre;
    }

   NamedType newType(VarType nt, String s) {

        Thing t = rewrites.get(s);
        if (t == null) {
            return nt;
        } else {
            noteUse(t,s);
            return t.replacement(nt);
        }
    }

   NamedType newType(TraitType nt, String s) {

       Thing t = rewrites.get(s);
       if (t == null) {
           return nt;
       } else {
           noteUse(t,s);
           return t.replacement(nt);
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
                                      new Id(s,WellKnownNames.secretParentName));
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
            functionalMethodsOfDefsToLocals(defs);
        } else if (tlnode instanceof Api) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Api com = (Api) tlnode;
            List<? extends AbsDeclOrDecl> defs = com.getDecls();
            defsToLocals(defs);
            functionalMethodsOfDefsToLocals(defs);
        }

    }

    public boolean injectAtTopLevel(String putName, String getName, Desugarer getFrom, Set<String>excluded) {
        Thing th = getFrom.rewrites.get(getName);
        Thing old = rewrites.get(putName);
        /* Empty means do  add */
        if (old == null) {
            rewrites_put(putName, th);
            return true;
        }
       /* Equal means no change */
        if (old.equals(th))
            return false;
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
        int savedLexicalNestingDepth = lexicalNestingDepth;
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
//                    List<AliasedAPIName> ladn = impapi.getApis();
//                    for (AliasedAPIName adn : ladn) {
//                        APIName dn = adn.getApi();
//                        Option<APIName> odn = adn.getAlias();
//                        dn = odn.unwrap(dn);
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
                } else if (node instanceof OpRef) {
                    OpRef vre = (OpRef) node;
                    String s = NodeUtil.stringName(vre.getOriginalName());
                    Expr update = newName(vre, s);
                    return update;
                } else if (node instanceof _RewriteObjectRef) {
                    _RewriteObjectRef vre = (_RewriteObjectRef) node;
                    String s = NodeUtil.stringName(vre.getObj());
                    Expr update = newName(vre, s);
                    return update;
                } else if (node instanceof AmbiguousMultifixOpExpr ) {
                	// NEB: This code is temporary. Very soon the static end will
                	// remove these nodes and they should never appear at this
                	// phase of execution. However, now we simply create an OpExpr.
                	AmbiguousMultifixOpExpr op = (AmbiguousMultifixOpExpr)node;
                	node = new OpExpr(op.getSpan(), op.isParenthesized(), op.getMultifix_op(), op.getArgs());
                } else if (node instanceof _RewriteFnRef) {

                    _RewriteFnRef fr = (_RewriteFnRef) node;

                } else if (node instanceof VarType) {
                    VarType vre = (VarType) node;
                    String s = NodeUtil.nameString(vre.getName());
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }
                    node = newType(vre, s);

                } else if (node instanceof TraitType) {
                    TraitType vre = (TraitType) node;
                    String s = NodeUtil.nameString(vre.getName());
                    node = newType(vre, s);

                } else if (node instanceof TightJuxt&& looksLikeMethodInvocation((Juxt) node)) {
                    return translateJuxtOfDotted((Juxt) node);

                  // This is a duplicate of the rewriting that occurs
                  // in RewriteInAbsenceOfTypeInfo


                } else if (node instanceof MathPrimary&& looksLikeMethodInvocation((MathPrimary) node)) {
                    return translateJuxtOfDotted((MathPrimary) node);

                  // This is a duplicate of the rewriting that occurs
                  // in RewriteInAbsenceOfTypeInfo


                }
                else if (node instanceof LValueBind) {
                    LValueBind lvb = (LValueBind) node;
                    Id id = lvb.getName();
                    if ("_".equals(id.getText())) {
                        Id newId = new Id(id.getSpan(), "_$" + id.getSpan());
                        return NodeFactory.makeLValue(lvb, newId);
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
                } else if (node instanceof Assignment) {

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
                        String temp = gensym();
                        Span at = vd.getSpan();
                        VarDecl new_vd = NodeFactory.makeVarDecl(at, temp, init);
                        newdecls.add(new_vd);
                        int element_index = 0;
                        for (LValueBind lv : lhs) {
                            Id newName = new Id(at, "$" + element_index);
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
                        rewrites_put(currentSelfName, new SelfRewrite(currentSelfName));
                    }
                    atTopLevelInsideTraitOrObject = false;
                    lexicalNestingDepth++;

                    List<Param> params = fndef.getParams();
                    // fndef.getFnName(); handled at top level.
                    List<StaticParam> tparams = fndef.getStaticParams();

                    paramsToLocals(params);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    Contract _contract = fndef.getContract();
                    Option<List<Expr>> _requires = _contract.getRequires();
                    Option<List<EnsuresClause>> _ensures = _contract.getEnsures();
                    Option<List<Expr>> _invariants = _contract.getInvariants();
                    List<Expr> _exprs = new ArrayList<Expr>();
                    AbstractNode n = visitNode(node);

                    if (_ensures.isSome() || _requires.isSome() || _invariants.isSome()) {
                        List<Expr> exprs = new ArrayList<Expr>();
                        exprs.add(fndef.getBody());
                        Block b = new Block(exprs);
                        if (_invariants.isSome()) b = translateInvariants(_invariants, b);
                        if (_ensures.isSome())    b = translateEnsures(_ensures, b);
                        if (_requires.isSome())   b = translateRequires(_requires, b);

                        FnDef f = new FnDef(fndef.getSpan(), fndef.getMods(),
                                            fndef.getName(),
                                            fndef.getStaticParams(), fndef.getParams(),
                                            fndef.getReturnType(), fndef.getThrowsClause(),
                                            fndef.getWhere(), fndef.getContract(),
                                            WellKnownNames.defaultSelfName, b);

                        n = visitNode(f);
                    }

                    dumpIfChange(node, n);
                    return n;

                } else if (node instanceof AbstractObjectExpr) {
                    // all the methods coming from traits are no longer
                    // eligible for com.sun.fortress.interpreter.rewrite.
                    AbstractObjectExpr oe = (AbstractObjectExpr) node;
                    List<? extends AbsDeclOrDecl> defs = oe.getDecls();
                    List<BaseType> xtends = NodeUtil.getTypes(oe.getExtendsClause());
                    // TODO wip

                    objectNestingDepth++;
                    lexicalNestingDepth++;
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
                    lexicalNestingDepth++;
                    FnExpr fndef = (FnExpr) node;
                    List<Param> params = fndef.getParams();
                    paramsToLocals(params);

                } else if (node instanceof Catch) {
                    String s = ((Catch) node).getName().stringName();
                    lexicalNestingDepth++;
                    rewrites_put(s, new Local());

                } else if (node instanceof LocalVarDecl) {
                    atTopLevelInsideTraitOrObject = false;
                    lexicalNestingDepth++;

                    LocalVarDecl lvd = (LocalVarDecl) node;

                    List<LValue> lhs = lvd.getLhs();
                    Option<Expr> rhs = lvd.getRhs();
                    List<Expr> body = lvd.getBody();

                    lvaluesToLocal(lhs);
                    // not quite right because initializing exprs are evaluated in the wrong context.

                    // TODO wip

                } else if (node instanceof LetFn) {
                    atTopLevelInsideTraitOrObject = false;
                    lexicalNestingDepth++;
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
                    List<BaseType> xtends = NodeUtil.getTypes(od.getExtendsClause());

                    // TODO wip
                    lexicalNestingDepth++;
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
                    lexicalNestingDepth++;
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
                    lexicalNestingDepth++;
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    accumulateMembersFromExtends(td);

                    inTrait = true;
                    AbstractNode n = visitNode(node);
                    inTrait = false;
                    return n;
                } else if (node instanceof If) {
                    If i = (If)node;
                    return visitIf(i);
                } else if (node instanceof While) {
                    While w = (While)node;
                    return visitWhile(w);
                } else if (node instanceof For) {
                    For f = (For)node;
                    DoFront df = f.getBody();
                    Do doBlock = new Do(df.getSpan(),Useful.list(df));
                    return visitLoop(f.getSpan(), f.getGens(), doBlock);
                } else if (node instanceof GeneratedExpr) {
                    GeneratedExpr ge = (GeneratedExpr)node;
                    return visitLoop(ge.getSpan(), ge.getGens(), ge.getExpr());
                } else if (node instanceof Accumulator) {
                    Accumulator ac = (Accumulator)node;
                    node = visitAccumulator(ac.getSpan(), ac.getGens(),
                                            ac.getOpr(), ac.getBody(),
                                            ac.getStaticArgs());
                    return node;
                } else if (node instanceof Spawn) {
                    node = translateSpawn((Spawn)node);
                } else if ( node instanceof GrammarDef) {
                    return node;
                } else if (node instanceof Typecase) {
                    Typecase tc = (Typecase) node;
                    List<Id> lid = tc.getBindIds();
                    Option<Expr> oe = tc.getBindExpr();
                    // Not quite right because this will remove them from arrows,
                    // but perhaps they ARE arrows, depending on the typecase.
                    IdsToLocals(lid);

                    if (oe.isNone()) {
                        node = ExprFactory.makeTypecase(tc, lid, (Expr) tupleForIdList(lid));
                    }

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
                lexicalNestingDepth = savedLexicalNestingDepth;
                currentSelfName = savedSelfName;
            }

    }

    private String vrToString(VarRef vre) {
        Iterable<Id> ids = NodeUtil.getIds(vre.getVar());

        String s = IterUtil.first(ids).getText();
        return s;
    }

    /**
     * Given List<Id>, generate tuple of corresponding VarRef
     */
    Expr tupleForIdList(List<Id> binds) {
        if (binds.size() == 1)
            return ExprFactory.makeVarRef(binds.get(0));

        List<Expr> refs = new ArrayList<Expr>(binds.size());

        for (Id b : binds)
            refs.add(ExprFactory.makeVarRef(b));

        return ExprFactory.makeTuple(
                new Span(binds.get(0).getSpan(),
                         binds.get(binds.size()-1).getSpan()),
                refs);
    }

    /**
     *  Given generalized if expression, desugar into __cond calls (binding)
     *  where required.
     */
    private Expr visitIf(If i) {
        Expr result = null;
        if (i.getElseClause().isSome()) {
            result = i.getElseClause().unwrap();
        }
        List<IfClause> clauses = i.getClauses();
        int n = clauses.size();
        if (n <= 0) bug(i,"if with no clauses!");
        // Traverse each clause and desugar it into an if or a __cond as appropriate.
        for (--n; n >= 0; --n) {
            result = addIfClause(clauses.get(n), result);
        }
        return (Expr)visitNode(result);
    }

    /**
     * Add an if clause to a (potentially) pre-existing else clause.
     * The else clase can be null, or can be an if expression.
     **/
    private Expr addIfClause(IfClause c, Expr elsePart) {
        GeneratorClause g = c.getTest();
        if (g.getBind().size() > 0) {
            // if binds <- expr then body else elsePart end desugars to
            // __cond(expr, fn (binds) => body, elsePart)
            ArrayList<Expr> args = new ArrayList<Expr>(3);
            args.add(g.getInit());
            args.add(bindsAndBody(g,c.getBody()));
            if (elsePart != null) args.add(thunk(elsePart));
            return new TightJuxt(c.getSpan(), false,
                                 Useful.list(COND_NAME,
                                             ExprFactory.makeTuple(c.getSpan(),args)));
        }
        // if expr then body else elsePart end is preserved
        // (but we replace elif chains by nesting).
        if (elsePart==null) {
            return ExprFactory.makeIf(c.getSpan(), c);
        } else {
            return ExprFactory.makeIf(c.getSpan(), c, ExprFactory.makeBlock(elsePart));
        }
    }

    /**
     * Desugar a generalized While clause.
     **/
    private Expr visitWhile(While w) {
        GeneratorClause g = w.getTest();
        if (g.getBind().size() > 0) {
            // while binds <- expr  do body end
            // desugars to
            // while __whileCond(expr, fn (binds) => body) do end
            ArrayList<Expr> args = new ArrayList<Expr>(2);
            args.add(g.getInit());
            args.add(bindsAndBody(g,w.getBody()));
            Expr cond =
                new TightJuxt(g.getSpan(), false,
                              Useful.list(WHILECOND_NAME,
                                          ExprFactory.makeTuple(w.getSpan(),args)));
            w = ExprFactory.makeWhile(w.getSpan(),cond);
        }
        return (Expr)visitNode(w);
    }

    /**
     * @param loc   Containing context
     * @param gens  Generators in generator list
     * @return single generator equivalent to the generator list
     * Desugars as follows:
     * body, empty  =>  body
     * body, x <- exp, gs  => exp.loop(fn x => body, gs)
     */
    Expr visitLoop(Span span, List<GeneratorClause> gens, Expr body) {
        for (int i = gens.size()-1; i >= 0; i--) {
            GeneratorClause g = gens.get(i);
            Expr loopBody = bindsAndBody(g, body);
            Expr loopSel = ExprFactory.makeFieldRef(g.getSpan(),
                                                    g.getInit(), LOOP_NAME);
            body = new TightJuxt(span, false, Useful.list(loopSel,loopBody));
        }
        // System.out.println("Desugared to "+body.toStringVerbose());
        return (Expr)visitNode(body);
    }

    private Expr visitAccumulator(Span span, List<GeneratorClause> gens,
                                  OpName op, Expr body,
                                  List<StaticArg> staticArgs) {
        body = visitGenerators(span, gens, body);
        Expr opexp = ExprFactory.makeOpExpr(span,op,staticArgs);
        Expr res = new TightJuxt(span, false,
                                 Useful.list(BIGOP_NAME,
                                             ExprFactory.makeTuple(opexp,body)));
        return (Expr)visitNode(res);
    }

    /** Given expr e, return (fn () => e) */
    Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(e.getSpan(),
                                      Collections.<Param>emptyList(), e);
    }

    /**
     * @param td
     */
    private void accumulateMembersFromExtends(TraitAbsDeclOrDecl td) {
        accumulateMembersFromExtends(NodeUtil.getTypes(td.getExtendsClause()),
                rewrites);
    }

    private void accumulateMembersFromExtends(List<BaseType> xtends, Map<String, Thing> disEnv) {
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
            if (d instanceof TraitAbsDeclOrDecl) {
                String s = d.stringName();
                TraitAbsDeclOrDecl dod = (TraitAbsDeclOrDecl) d;
                rewrites_put(s, new Trait(dod, rewrites));
            } else if (d instanceof ObjectDecl) {
                ObjectDecl od = (ObjectDecl) d;
                Option<List<Param>> params = od.getParams();
                /*
                 *  Add the object itself (either constructor or singleton,
                 *  we don't care much) to the locals.
                 *
                 *  If it is a constructor, it is also an arrow type.
                 *
                 */
                String s = od.getName().getText();
                rewrites_put(s, new Local());

                if (params.isNone())
                    arrows.remove(s);
                else
                    arrows.add(s);
            } else {
                for (String s : NodeUtil.stringNames(d)) {
                    rewrites_put(s, new Local());
                }
            }
        }
    }

    /**
     * @param defs
     */
    private void functionalMethodsOfDefsToLocals(List<? extends AbsDeclOrDecl> defs) {
        for (AbsDeclOrDecl d : defs) {
            if (d instanceof TraitObjectAbsDeclOrDecl) {
                TraitObjectAbsDeclOrDecl dod = (TraitObjectAbsDeclOrDecl) d;
                List <? extends AbsDeclOrDecl> tdecls = dod.getDecls();
                handlePossibleFM(tdecls);
            } else {
                // Thankfully, do nothing.
            }
        }
    }

    private void handlePossibleFM(List<? extends AbsDeclOrDecl> tdecls) {
        for (AbsDeclOrDecl adod : tdecls) {
            ArrowOrFunctional aof = adod.accept(isAnArrowName);
                if (aof == FUNCTIONAL) {
                    // Only certain things can be a functional method.
                    FnAbsDeclOrDecl fadod = (FnAbsDeclOrDecl) adod;
                    String s = fadod.getName().stringName();
                    arrows.add(s);
                    functionals.add(s);
                    rewrites_put(s, new FunctionalMethod());
                }

        }
    }


    /**
     * @param params
     */
    private void paramsToLocals(List<? extends Param> params) {
        for (Param d : params) {
            String s = d.getName().getText();
            // "self" is not a local.
            if (! s.equals(currentSelfName)) {
                rewrites_put(s, new Local());

            }
        }
    }

    private void IdsToLocals(List<Id> lid) {
        for (Id d : lid) {
            String s = d.getText();
            rewrites_put(s, new Local());
        }
    }

    private NodeAbstractVisitor_void localVisitor = new  NodeAbstractVisitor_void() {

        @Override
        public void forExtentRange(ExtentRange that) {
            // TODO Auto-generated method stub
            super.forExtentRange(that);
        }

        @Override
        public void forLValueBind(LValueBind that) {
            that.getName().accept(this);
        }

        @Override
        public void forId(Id that) {
            String s = that.getText();
            rewrites_put(s, new Local());
        }

        @Override
        public void forUnpastingBind(UnpastingBind that) {
            that.getName().accept(this);
            for (ExtentRange er : that.getDim())
                er.accept(this);
        }

        @Override
        public void forUnpastingSplit(UnpastingSplit that) {
            for (Unpasting up : that.getElems())
                up.accept(this);
        }

    };

    /**
     * @param params
     */
    private void lvaluesToLocal(List<? extends LValue> params) {
        for (LValue param : params)
            param.accept(localVisitor);
    }

    /**
     * @param params
     */
    private BATree<String, Boolean> tparamsToLocals(List<StaticParam> params, BATree<String, Boolean> immediateDef) {
        if (!params.isEmpty())
            for (StaticParam d : params) {
                String s = NodeUtil.getName(d);
                rewrites_put(s, new Local());
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
        for (AbsDeclOrDecl dd : defs) {

            if (dd instanceof VarDecl) {
                //visited.add(tdod);
            } else if (dd instanceof AbsVarDecl) {
                //visited.add(tdod);
            } else {
                String sdd = dd.stringName();
                ArrowOrFunctional aof = dd.accept(isAnArrowName);

                if (aof != NEITHER) {

                    arrows.add(sdd);
                    if (aof == FUNCTIONAL) {
                        functionals.add(sdd);
                    }
                } else {
                    arrows.remove(sdd);
                }
            }
            for (String s: NodeUtil.stringNames(dd))
                rewrites_put(s, new Member());
        }

    }

    /**
     * @param params
     */
    private void paramsToMembers(Option<List<Param>> params) {
        if (params.isSome())
            for (Param d : params.unwrap()) {
                String s = d.getName().getText();
                rewrites_put(s, new Member(NodeUtil.isTransient(d)));
                ArrowOrFunctional aof = d.accept(isAnArrowName);
                if (aof != NEITHER) {
                    arrows.add(s);
                    if (aof == FUNCTIONAL) {
                        functionals.add(s);
                        throw new Error("Don't think this can happen");

                    }
                } else
                    arrows.remove(s);
            }
    }



    /**
     * @param params
     */
    private void stringsToMembers(Collection<String> strings) {
        for (String s : strings) {
            rewrites_put(s, new Member());
            arrows.remove(s);
        }
    }

    /**
     * @param params
     */
    private void stringsToLocals(Collection<String> strings) {
        for (String s : strings) {
            rewrites_put(s, new Local());
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
    private void accumulateTraitsAndMethods(List<BaseType> xtends,
            Map<String, Thing> typeEnv, Set<String> members, Set<String> types,
            Set<String> arrow_names, Set<String> not_arrow_names,
            Set<AbstractNode> visited) {

            for (Type t : xtends) {
                Id name = null;
                // First de-parameterize the type
                if (t instanceof TraitType) {
                    name = ((TraitType) t).getName();
                } else if (t instanceof VarType) {
                    name = ((VarType) t).getName();
                } else if (t instanceof AnyType) {
                    name = INTERNAL_ANY_NAME;
                } else {
                   // TODO Way too many types; deal with them as necessary.
                    bug(t, errorMsg("Object extends something exciting: ", t));
                }
                if (name.getApi().isNone()) {
                    // TODO we've got to generalize this to qualified names.
                    String s = name.getText();
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
                                    //visited.add(tdod);
                                } else if (dd instanceof AbsVarDecl) {
                                    //visited.add(tdod);
                                } else {
                                    ArrowOrFunctional aof = dd.accept(isAnArrowName);

                                    if (aof != NEITHER) {
                                        arrow_names.add(sdd);
                                        if (aof == FUNCTIONAL) {
                                            functionals.add(sdd);
                                            rewrites_put(sdd, new FunctionalMethod());
                                        }
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
                        error(t, errorMsg("Attempt to extend object type ", s, ", saw ", th));
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
        Id qidn = first.getVar();

        // Optimistic casts here, will need revisiting in the future,
        // perhaps FieldRefs are too general
        // Recursive visits here
        AbstractNode expr = visit(first);
        List<Expr> visitedArgs = visitList(exprs.subList(1, exprs.size()));
        if (expr instanceof VarRef) {
            VarRef vre = (VarRef) expr;
            if (vre.getLexicalDepth() == -1) {
                return new MethodInvocation(node.getSpan(),
                        false,
                        ExprFactory.makeVarRef(node.getSpan(), WellKnownNames.secretSelfName), // this will rewrite in the future.
                        (Id) vre.getVar(),
                        visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteralExpr(node.getSpan()) : // wrong span
                        visitedArgs.size() == 1 ? visitedArgs.get(0) :
                            new TupleExpr(visitedArgs));
            }
        } else  if (expr instanceof _RewriteFieldRef) {

            _RewriteFieldRef selfDotSomething = (_RewriteFieldRef) visit(first);

            return new MethodInvocation(node.getSpan(),
                                    false,
                                    selfDotSomething.getObj(), // this will rewrite in the future.
                                    (Id) selfDotSomething.getField(),
                                    visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteralExpr(node.getSpan()) : // wrong span
                                    visitedArgs.size() == 1 ? visitedArgs.get(0) :
                                        new TupleExpr(visitedArgs));
        }

        throw new Error("Not there yet.");


    }

    private AbstractNode translateJuxtOfDotted(MathPrimary node) {
        VarRef first = (VarRef) node.getFront();
        Id qidn = first.getVar();

        // Optimistic casts here, will need revisiting in the future,
        // perhaps FieldRefs are too general
        // Recursive visits here
        _RewriteFieldRef selfDotSomething = (_RewriteFieldRef) visit(first);
        //        List<MathItem> visitedArgs = visitList(exprs);
        AbstractNode arg = visit(((ExprMI)node.getRest().get(0)).getExpr());

        return new MethodInvocation(node.getSpan(),
                                false,
                                selfDotSomething.getObj(), // this will rewrite in the future.
                                (Id) selfDotSomething.getField(),
                                    (Expr)arg
                                    /*
                                visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteralExpr(node.getSpan()) : // wrong span
                                visitedArgs.size() == 1 ? visitedArgs.get(0) :
                                    new TupleExpr(visitedArgs)
                                    */
                                    );
    }

    private AbstractNode translateSpawn(Spawn s) {
        Expr body = s.getBody();
        Span sp   = s.getSpan();
        // If the user writes Spawn(foo) instead of Spawn(foo()) we get an inexplicable error
        // message.  We might want to put in a check for that someday.

        AbstractNode rewrittenExpr =  visit(body);

        Expr in_fn = new VarRef(sp, new Id("Thread"));
        List<StaticArg> args = new ArrayList<StaticArg>();
        args.add(new TypeArg(new VarType(sp, new Id("Any"))));

        _RewriteFnRef fn = new _RewriteFnRef(in_fn, args);

        List<Param> params = new ArrayList<Param>();
        FnExpr fnExpr = new FnExpr(sp, params, (Expr) rewrittenExpr);
        List<Expr> exprs = new ArrayList<Expr>();
        exprs.add(fn);

        exprs.add(fnExpr);

        TightJuxt juxt = new TightJuxt(s.getSpan(), false, exprs);
        return juxt;
    }

    private Block translateRequires(Option<List<Expr>> _requires, Block b)  {
        List<Expr> r = _requires.unwrap(Collections.<Expr>emptyList());
        for (Expr e : r) {
            Span sp = e.getSpan();
            GeneratorClause cond =
                ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(), e);
            If _if = ExprFactory.makeIf(sp, new IfClause(sp,cond,b),
                                        ExprFactory.makeThrow(sp,"CallerViolation"));
            b = ExprFactory.makeBlock(sp, _if);
        }
        return b;
    }

    private Block translateEnsures(Option<List<EnsuresClause>> _ensures, Block b) {
        List<EnsuresClause> es = _ensures.unwrap(Collections.<EnsuresClause>emptyList());
        for (EnsuresClause e : es) {
            Span sp = e.getSpan();
            Id t1 = gensymId("t1");
            Block inner_block =
                ExprFactory.makeBlock(sp,
                                      ExprFactory.makeVarRef(sp, "result"));
            GeneratorClause cond;
            cond = ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(),
                                                   e.getPost());
            If _inner_if =
                ExprFactory.makeIf(sp,
                                   new IfClause(sp, cond, inner_block),
                                   ExprFactory.makeThrow(sp, "CalleeViolation"));

            cond = ExprFactory.makeGeneratorClause(sp,
                                                   Useful.<Id>list(), (Expr) ExprFactory.makeVarRef(sp,t1));
            If _if = ExprFactory.makeIf(sp, new IfClause(sp, cond,
                                                         ExprFactory.makeBlock(sp,_inner_if)),
                                        ExprFactory.makeBlock(sp,ExprFactory.makeVarRef(sp,"result")));
            LocalVarDecl r = ExprFactory.makeLocalVarDecl(sp, NodeFactory.makeId(sp,"result"), b, _if);
            Option<Expr> _pre = e.getPre();
            LocalVarDecl provided_lvd = ExprFactory.makeLocalVarDecl(sp, t1, _pre.unwrap(ExprFactory.makeVarRef("true")),
                                                                     ExprFactory.makeBlock(sp, r));
            b = ExprFactory.makeBlock(sp, provided_lvd);
        }
        return b;
    }

    private Block translateInvariants(Option<List<Expr>> _invariants, Block b) {
        for (Expr e : _invariants.unwrap(Collections.<Expr>emptyList())) {
            Span sp = e.getSpan();
            Id t1 = gensymId("t1");
            Id t_result = gensymId("result");
            Id t2 = gensymId("t2");

            Expr chain = (Expr) ExprFactory.makeChainExpr(sp, (Expr) ExprFactory.makeVarRef(sp,t1),
                                                          new Op("="),
                                                          (Expr) ExprFactory.makeVarRef(sp, t2));
            GeneratorClause gen_chain = ExprFactory.makeGeneratorClause(sp,
                                                                        Useful.<Id>list(), chain);
            If _post =
                ExprFactory.makeIf(sp, new IfClause(sp,gen_chain,
                                                    ExprFactory.makeBlock(sp,
                                                                          ExprFactory.makeVarRef(sp,
                                                                                                 "result"))),
                                          ExprFactory.makeThrow(sp, "CalleeViolation"));
            LocalVarDecl r2 = ExprFactory.makeLocalVarDecl(sp, t2, e, _post);
            LocalVarDecl r1 = ExprFactory.makeLocalVarDecl(NodeFactory.makeId(sp, "result"), b, r2);

            b = ExprFactory.makeBlock(sp, ExprFactory.makeLocalVarDecl(sp, t1,e,r1));
        }

        return b;
    }

    private boolean looksLikeMethodInvocation(Juxt node) {
        Expr first = node.getExprs().get(0);

        if (first instanceof VarRef) {
            VarRef vr = (VarRef) first;
            String s = vrToString(vr);

            /*
             * If the var ref will be rewritten to self.var (or self.parent.var,
             * etc) and if the var ref IS NOT an arrow type (a function), then
             * it looks like a method invocation.
             */
            if (rewrites.get(s) instanceof Member && ! arrows.contains(s))
                return true;
        }
        return false;
    }

    private boolean looksLikeMethodInvocation(MathPrimary node) {
        Expr first = node.getFront();
        if (first instanceof VarRef) {
            VarRef vr = (VarRef) first;
            String s = vrToString(vr);
            if (rewrites.get(s) instanceof Member && ! arrows.contains(s) &&
                node.getRest().size() == 1 &&
                node.getRest().get(0) instanceof ExprMI)
                return true;
        }
        return false;
    }

    public Set<String> getTopLevelRewriteNames() {
        return rewrites.keySet();
    }
}
