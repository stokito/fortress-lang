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
import com.sun.fortress.interpreter.evaluator.types.FTypeGeneric;
import com.sun.fortress.interpreter.evaluator.types.FTypeObject;
import com.sun.fortress.interpreter.evaluator.values.Constructor;
import com.sun.fortress.interpreter.evaluator.values.GenericConstructor;
import com.sun.fortress.interpreter.evaluator.values.Parameter;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.interpreter.nodes.CompilationUnit;
import com.sun.fortress.interpreter.nodes.Component;
import com.sun.fortress.interpreter.nodes.DefOrDecl;
import com.sun.fortress.interpreter.nodes.DottedId;
import com.sun.fortress.interpreter.nodes.Expr;
import com.sun.fortress.interpreter.nodes.FieldSelection;
import com.sun.fortress.interpreter.nodes.Fn;
import com.sun.fortress.interpreter.nodes.FnDecl;
import com.sun.fortress.interpreter.nodes.Fun;
import com.sun.fortress.interpreter.nodes.Id;
import com.sun.fortress.interpreter.nodes.IdType;
import com.sun.fortress.interpreter.nodes.LetFn;
import com.sun.fortress.interpreter.nodes.LocalVarDecl;
import com.sun.fortress.interpreter.nodes.Node;
import com.sun.fortress.interpreter.nodes.ObjectDecl;
import com.sun.fortress.interpreter.nodes.ObjectExpr;
import com.sun.fortress.interpreter.nodes.Option;
import com.sun.fortress.interpreter.nodes.Param;
import com.sun.fortress.interpreter.nodes.ParamType;
import com.sun.fortress.interpreter.nodes.Span;
import com.sun.fortress.interpreter.nodes.StaticParam;
import com.sun.fortress.interpreter.nodes.TraitDefOrDecl;
import com.sun.fortress.interpreter.nodes.TypeRef;
import com.sun.fortress.interpreter.nodes.VarDecl;
import com.sun.fortress.interpreter.nodes.VarRefExpr;
import com.sun.fortress.interpreter.useful.BATree;
import com.sun.fortress.interpreter.useful.NI;
import com.sun.fortress.interpreter.useful.StringComparer;


/**
 * Rewrite the AST to "disambiguate" (given known interpreter treatment of
 * constructors) references to surrounding local variables and object/trait
 * members.
 *
 * The most important thing to know is that there are two treatments of lexical
 * context; top level, and not.  This can also be carved into "object" and "trait",
 * where top level object and not top level object are treated alike.  (The
 * interpreter does it this way, it could be done the other way.)  For
 * not-top-level, local variables are tied to "self" (rewritten to "*self")
 * so that there need only be set of methods.  Otherwise, if the local
 * environment were tied to the methods (thus, closures) there could be very
 * many of them, and to the extent that a ground type identifies a fixed set
 * of methods, very many types, which thus complicates overloading, etc.
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

       public class Thing {
        int nestedness;

        Thing() {
            nestedness = objectNestingDepth;
        }
        Expr replacement(VarRefExpr original) {
            return original;
        }
    }

    class Local extends Thing {

    }

    /**
     * Traits need to identify their declaration, for purposes of figuring out
     * what names are in scope, though not necessarily what they mean.
     */
    class Trait extends Local {
        TraitDefOrDecl defOrDecl;

        Trait(TraitDefOrDecl dod) {
            defOrDecl = dod;
        }
    }

    class Member extends Thing {
        Expr replacement(VarRefExpr original) {
        FieldSelection fs = new FieldSelection(original, // Use this constructor
                // here because it is a
                // com.sun.fortress.interpreter.rewrite.
                dottedReference(original.getSpan(),
                                objectNestingDepth - nestedness), original.getVar());
        return fs;
        }
    }

    class SelfRewrite extends Thing {
        SelfRewrite(String s) {
            this.s = s;
        }
        String s;
        Expr replacement(VarRefExpr original) {
            Expr e = dottedReference(original.getSpan(),
                    objectNestingDepth - nestedness);
            return e;
        }

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
     * All the object exprs (this may generalize to nested functions as well)
     * need to have their gensym'd types and constructors registered at the
     * top level.  The top level environment is available to the creator/caller
     * of Disambiguate.
     */
    private ArrayList<ObjectExpr> objectExprs = new ArrayList<ObjectExpr>();

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
     * Adds, to the supplied environment, constructors for any object
     * expressions encountered the tree(s) processed by this Disambiguator.
     * @param e
     */
    public void registerObjectExprs(BetterEnv e) {
        for (ObjectExpr oe : objectExprs) {
            String name = oe.getGenSymName();
            Option<List<StaticParam>> params = oe.getStaticParams();
            if (! params.isPresent()) {
                // Regular constructor
                FTypeObject fto = new FTypeObject(name, e, oe);
                e.putType(name, fto);
                BuildEnvironments.finishObjectTrait(oe.getTraits(), null, null, fto, e, oe);
                Constructor con = new Constructor(e, fto, oe, new Fun(name), oe.getDefs());
                con.setParams(Collections.<Parameter> emptyList());
                e.putValue(name, con);
                con.finishInitializing();
            } else {
                // Generic constructor
                FTypeGeneric fto = new FTypeGeneric(e, oe);
                e.putType(name, fto);
                GenericConstructor con = new GenericConstructor(e, oe);
                e.putValue(name, con);
            }
        }
    }

    Expr newName(VarRefExpr vre, String s) {
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
            return new VarRefExpr(s, WellKnownNames.secretSelfName);
        }
        if (i > 0) {
            return new FieldSelection(s, dottedReference(s, i - 1), Id.make(s,
                    PARENT_NAME));
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
    public Node visit(Node node) {
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
            List<? extends DefOrDecl> defs = com.getDefs();
            defsToLocals(defs);
            return visitNode(node);

        } else
            try {
                /*
                 * NOTE: "default" handling for any com.sun.fortress.interpreter.nodes not metioned explicitly is:
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

                if (node instanceof VarRefExpr) {
                    VarRefExpr vre = (VarRefExpr) node;
                    Id id = vre.getVar();
                    String s = id.getName();
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }

                    return newName(vre, s);

                } else if (node instanceof IdType) {
                    IdType vre = (IdType) node;

                    String s = vre.getName().toString();
                    StaticParam tp = visibleGenericParameters.get(s);
                    if (tp != null) {
                        usedGenericParameters.put(s, tp);
                    }

                } else if (node instanceof FieldSelection) {
                    atTopLevelInsideTraitOrObject = false;
                    // Believe that any field selection is already
                    // disambiguated. In an ordinary method, "self"
                    // is already defined, in a notself method, "notself"
                    // is defined, and "self" is defined as a local in
                    // an enclosing scope (as is "notnotself", etc, that
                    // intervening notself methods might define).

                    // However, since the LHS of a field selection might
                    // be a method invocation, we DO need to check that.
                    FieldSelection fs = (FieldSelection) node;

                } else if (node instanceof VarDecl) {
                    atTopLevelInsideTraitOrObject = false;
                    // Leave VarDecl alone, probably, because it gets properly
                    // handled
                    // at the enclosing block.

                } else if (node instanceof FnDecl) {
                    // params no longer eligible for com.sun.fortress.interpreter.rewrite.
                    FnDecl fndef = (FnDecl) node;
                    if (atTopLevelInsideTraitOrObject) {
                        currentSelfName = fndef.getSelfName();
                        e.put(currentSelfName, new SelfRewrite(currentSelfName));
                        }
                    atTopLevelInsideTraitOrObject = false;

                    List<Param> params = fndef.getParams();
                    // fndef.getFnName(); handled at top level.
                    Option<List<StaticParam>> tparams = fndef.getStaticParams();

                    paramsToLocals(params);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    Node n = visitNode(node);
                    dumpIfChange(node, n);
                    return n;

                } else if (node instanceof ObjectExpr) {
                    // all the methods coming from traits are no longer
                    // eligible for com.sun.fortress.interpreter.rewrite.
                    ObjectExpr oe = (ObjectExpr) node;
                    List<? extends DefOrDecl> defs = oe.getDefs();
                    Option<List<TypeRef>> xtends = oe.getTraits();
                    // TODO wip

                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    accumulateMembersFromExtends(xtends, e);
                    Node n = visitNode(node);
                    // REMEMBER THAT THIS IS THE NEW ObjectExpr!
                    oe = (ObjectExpr) n;
                    // Implicitly parameterized by either visibleGenericParameters,
                    // or by usedGenericParameters.

                    oe.setGenSymName(oe.toString());
                    // Note the keys of a BATree are sorted.
                    oe.setImplicitTypeParameters(usedGenericParameters);

                    objectExprs.add(oe);

                    return n;

                } else if (node instanceof Fn) {
                    atTopLevelInsideTraitOrObject = false;
                    Fn fndef = (Fn) node;
                    List<Param> params = fndef.getParams();
                    paramsToLocals(params);

                } else if (node instanceof LocalVarDecl) {
                    atTopLevelInsideTraitOrObject = false;
                    // defined var is no longer eligible for com.sun.fortress.interpreter.rewrite.
                    //LocalVarDecl lb = (LocalVarDecl) node;
                    //List<LValue> lvals = lb.getLhs();
                    // TODO wip

                } else if (node instanceof LetFn) {
                    atTopLevelInsideTraitOrObject = false;
                    // defined var is no longer eligible for com.sun.fortress.interpreter.rewrite.
                    LetFn lf = (LetFn) node;
                    List<FnDecl> defs = lf.getFns();
                    defsToLocals(defs);
                    // All the function names are in scope in the function
                    // definitions, and in the body of code that follows them.
                    // TODO wip

                } else if (node instanceof ObjectDecl) {
                    // All the methods and fields defined in object and the
                    // extended traits
                    // are mapped to "self".
                    ObjectDecl od = (ObjectDecl) node;
                    List<? extends DefOrDecl> defs = od.getDefs();
                    Option<List<Param>> params = od.getParams();
                    Option<List<StaticParam>> tparams = od.getStaticParams();
                    Option<List<TypeRef>> xtends = od.getTraits();
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);
                    paramsToMembers(params);

                    accumulateMembersFromExtends(xtends, e);

                    Node n = visitNode(node);

                    return n;
                } else if (node instanceof TraitDefOrDecl) {
                    TraitDefOrDecl td = (TraitDefOrDecl) node;
                    List<? extends DefOrDecl> defs = td.getFns();
                    Option<List<StaticParam>> tparams = td.getStaticParams();
                    // TODO wip
                    objectNestingDepth++;
                    atTopLevelInsideTraitOrObject = true;
                    defsToMembers(defs);
                    immediateDef = tparamsToLocals(tparams, immediateDef);

                    accumulateMembersFromExtends(td);

                    inTrait = true;
                    Node n = visitNode(node);
                    inTrait = false;
                    return n;
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
     * @param td
     */
    private void accumulateMembersFromExtends(TraitDefOrDecl td) {
        accumulateMembersFromExtends(td.getExtends_(), td.getDisEnv());
    }

    private void accumulateMembersFromExtends(Option<List<TypeRef>> xtends, Map<String, Thing> disEnv) {
        Set<String> members = new HashSet<String>();
        Set<String> types = new HashSet<String>();
        Set<Node> visited = new HashSet<Node>();
        accumulateTraitsAndMethods(xtends, disEnv, members, types, visited);
        stringsToLocals(types);
        stringsToMembers(members);
    }

    /**
     * @param node
     * @param n
     */
    private void dumpIfChange(Node old, Node n) {
        if (false && n != old) {
            try {
                System.err.println("Rewritten method body:");
                n.dump(System.err);
                System.err.println();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }

    /**
     * @param defs
     */
    private void defsToLocals(List<? extends DefOrDecl> defs) {
        for (DefOrDecl d : defs) {
            String s = d.stringName();
            if (d instanceof TraitDefOrDecl) {
                TraitDefOrDecl dod = (TraitDefOrDecl) d;
                dod.setDisEnv(e);
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
            String s = d.getName().getName();
            e.put(s, new Local());
        }
    }

    /**
     * @param params
     */
    private BATree<String, Boolean> tparamsToLocals(Option<List<StaticParam>> params, BATree<String, Boolean> immediateDef) {
        if (params.isPresent())
            for (StaticParam d : params.getVal()) {
                String s = d.getName();
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
    private void defsToMembers(List<? extends DefOrDecl> defs) {
        for (DefOrDecl d : defs)
            for (String s: d.stringNames())
            e.put(s, new Member());

    }

    /**
     * @param params
     */
    private void paramsToMembers(Option<List<Param>> params) {
        if (params.isPresent())
            for (Param d : params.getVal()) {
                String s = d.getName().getName();
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
    private void accumulateTraitsAndMethods(Option<List<TypeRef>> oxtends,
            Map<String, Thing> typeEnv, Set<String> members, Set<String> types,
            Set<Node> visited) {
        if (oxtends.isPresent()) {
            List<TypeRef> xtends = oxtends.getVal();

            for (TypeRef t : xtends) {
                // First de-parameterize the type
                while (t instanceof ParamType) {
                    t = ((ParamType) t).getGeneric();
                }
                if (t instanceof IdType) {
                    IdType it = (IdType) t;
                    DottedId d = it.getName();
                    List<String> names = d.getNames();
                    if (names.size() == 1) {
                        // TODO we've got to generalize this to DottedId names.
                        String s = names.get(0);
                        Thing th = typeEnv.get(s);
                        if (th instanceof Trait) {
                            Trait tr = (Trait) th;
                            TraitDefOrDecl tdod = tr.defOrDecl;
                            if (!(visited.contains(tdod))) {
                                visited.add(tdod);
                                // Process this trait -- add its name, as well
                                // as all the members
                                // types.add(s); // The trait is known by this
                                                // name.
                                for (DefOrDecl dd : tdod.getFns()) {
                                    members.add(dd.stringName());
                                }
                                accumulateTraitsAndMethods(tdod.getExtends_(),
                                        tdod.getDisEnv(), members, types,
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
                } else {
                    // TODO Way too many types; deal with them as necessary.
                    NI.nyi("Object extends something exciting: " + t);
                }
            }

        }
    }
}
