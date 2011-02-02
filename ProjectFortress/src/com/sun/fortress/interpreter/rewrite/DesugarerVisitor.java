/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.interpreter.rewrite;

import com.sun.fortress.compiler.WellKnownNames;
import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.exceptions.ProgramError.error;
import static com.sun.fortress.exceptions.ProgramError.errorMsg;
import com.sun.fortress.interpreter.evaluator.Environment;
import com.sun.fortress.nodes.*;
import static com.sun.fortress.nodes_util.DesugarerUtil.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.useful.*;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Wrapper;

import java.io.IOException;
import java.util.*;

public class DesugarerVisitor extends NodeUpdateVisitor implements HasRewrites {

    private static boolean suppressDebugDump;
    private final static boolean debug = false;

    public class Thing implements InterpreterNameRewriter {
        int objectNestedness;
        int lexicalNestedness;

        Thing() {
            objectNestedness = objectNestingDepth;
            lexicalNestedness = lexicalNestingDepth;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object o) {
            if (o == null) return false;
            if (o.getClass() == getClass()) {
                Thing that = (Thing) o;
                if (that.objectNestedness == this.objectNestedness && that.lexicalNestedness == this.lexicalNestedness)
                    return true;
            }
            return false;
        }

        /**
         * May assume {@code original} has a non-zero length.
         */
        public Expr replacement(VarRef original) {
            return ExprFactory.makeVarRef(original, lexicalNestedness);
        }

        public BoolRef replacement(BoolRef original) {
            return NodeFactory.makeBoolRef(original, lexicalNestedness);
        }

        public IntRef replacement(IntRef original) {
            return NodeFactory.makeIntRef(original, lexicalNestedness);
        }

        public Expr replacement(FnRef original) {
            return ExprFactory.makeFnRef(original, lexicalNestedness);
        }

        public Expr replacement(OpRef original) {
            return ExprFactory.makeOpRef(original, lexicalNestedness);
        }

        public VarType replacement(VarType original) {
            return NodeFactory.makeVarType(original, lexicalNestedness);
        }
        //        TraitType replacement(TraitType original) {
        //            return NodeFactory.makeTraitType(original);

        //        }
        public String toString() {
            return "Thing@" + objectNestedness + "/" + lexicalNestedness;
        }
    }

    public class Local extends Thing {
        // Was private, wish to stifle whiny Scala plugin
        public String toString() {
            return "Local@" + objectNestedness + "/" + lexicalNestedness;
        }
    }

    public class FunctionalMethod extends Local {
        FunctionalMethod() {
            super();
            objectNestedness = 0;
            lexicalNestedness = 0;
        }

        public String toString() {
            return "FunctionalMethod@" + objectNestedness + "/" + lexicalNestedness;
        }
    }

    /**
     * Traits need to identify their declaration, for purposes of figuring out
     * what names are in scope, though not necessarily what they mean.
     */
    private class Trait extends Local {
        TraitDecl defOrDecl;
        Map<String, InterpreterNameRewriter> env;

        Trait(TraitDecl dod, Map<String, InterpreterNameRewriter> env) {
            defOrDecl = dod;
            this.env = env;
        }

        public String toString() {
            return "Trait=" + defOrDecl;
        }

        public int hashCode() {
            return super.hashCode();
        }

        public boolean equals(Object o) {
            if (super.equals(o)) {
                Trait that = (Trait) o;
                // Conservative definition for now.
                // Full "equals" applied to "env" would probably not terminate
                // because of cycles.
                return (that.defOrDecl == this.defOrDecl && that.env == this.env);
            }
            return false;
        }
    }


    static Id filterQID(Id qid) {
        if (qid.getApiName().isNone()) return qid;
        return bug("Not yet prepared for QIDs ref'd through self/parent, QID=" + NodeUtil.dump(qid));
    }

    private class Member extends Thing {
        Member() {
            super();
        }

        @Override
        public Expr replacement(VarRef original) {
            return ExprFactory.makeFieldRef(NodeUtil.getSpan(original),
                                            // Use this constructor
                                            // here because it is a
                                            // rewrite.
                                            dottedReference(NodeUtil.getSpan(original),
                                                            objectNestingDepth - objectNestedness),
                                            filterQID(original.getVarId()));
        }

        public String toString() {
            return "Member@" + objectNestedness + "/" + lexicalNestedness;
        }
    }

    private class SelfRewrite extends Member {
        SelfRewrite() {
        }

        public Expr replacement(VarRef original) {
            Expr expr = dottedReference(NodeUtil.getSpan(original), objectNestingDepth - objectNestedness);
            return expr;
        }

        public String toString() {
            return "Self(self)@" + objectNestedness + "/" + lexicalNestedness;
        }
    }

    /**
     * Rewritings in scope.
     */
    private BATree<String, InterpreterNameRewriter> rewrites;

    public Map<String, InterpreterNameRewriter> getRewrites() {
        return rewrites.copy();
    }

    /*
     * The next four methods all do the same thing,
     * but the intent is indicated in case it becomes
     * necessary to maintain separate rewritings (this is
     * an issue if the same name is defined in different
     * ways at different lexical depths; need to check
     * whether that is allowed, or might become allowed).
     */
    public InterpreterNameRewriter var_rewrites_put(String k, InterpreterNameRewriter d) {
        return rewrites.put(k, d);
    }

    public InterpreterNameRewriter obj_rewrites_put(String k, InterpreterNameRewriter d) {
        return rewrites.put(k, d);
    }

    public InterpreterNameRewriter rewrites_put(String k, InterpreterNameRewriter d) {
        return rewrites.put(k, d);
    }

    public InterpreterNameRewriter type_rewrites_put(String k, InterpreterNameRewriter d) {
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

    public BASet<String> functionals;

    /**
     * Experimental -- when would this ever apply?
     *
     * @param s
     */
    protected void noteUse(String s, VarRef vre) {
        if (!functionals.contains(s)) Debug.debug(Debug.Type.INTERPRETER, 2, s, " used at ", vre.at());
    }

    /**
     * All the object exprs (this may generalize to nested functions as well)
     * need to have their gensym'd types and constructors registered at the
     * top level.  The top level environment is available to the creator/caller
     * of Disambiguate.
     */
    private List<_RewriteObjectExpr> objectExprs = new ArrayList<_RewriteObjectExpr>();

    DesugarerVisitor(BATree<String, InterpreterNameRewriter> initial,
                     BATree<String, StaticParam> initialGenericScope,
                     BASet<String> initialArrows,
                     BASet<String> initialFunctionals) {
        rewrites = initial;
        arrows = initialArrows;
        visibleGenericParameters = initialGenericScope;
        usedGenericParameters = new BATree<String, StaticParam>(StringHashComparer.V);
        functionals = initialFunctionals;
        // packages = new BASet<String>(StringHashComparer.V);
    }

    /**
     * Returns a new DesugarerVisitor.
     *
     * @param suppressDebugDump normally true for everything but files mentioned on the command line.
     * @param list
     */
    public DesugarerVisitor(boolean suppressDebugDump) {
        this(new BATree<String, InterpreterNameRewriter>(StringHashComparer.V),
             new BATree<String, StaticParam>(StringHashComparer.V),
             new BASet<String>(StringHashComparer.V),
             new BASet<String>(StringHashComparer.V));
        this.suppressDebugDump = suppressDebugDump;
    }

    /**
     * Zero outside of an object, equal to the number of Object[Exprs] or traits
     * enclosing the current node.
     * <p/>
     * When objects are entered, all the symbols defined there are associated
     * with a depth; when a var reference is rewritten, the symbol used depends
     * on the number. If the numbers match, then use the surrounding method's
     * default name for self. If the name is some outer method's name for self,
     * it should suffice to use it (it should work). If the numbers do not
     * match, presumably the symbol's number is smaller than current, and it
     * will be bound in the surrounding environment.
     * <p/>
     * Local variables are normally just captured, because the constructor
     * carries them around, but those from the top level surrounding methods
     * defined in traits are captured differently, and must be noted with a
     * special "*TRAIT" symbol (optimization; if any such methods occur, they
     * should be noted, and if not noted then a specialization step can be
     * dodged later).
     * <p/>
     * Actually, probably not; the construction of trait environments should get
     * this right without this additional frob (this is much, much less hacky).
     */
    int objectNestingDepth;

    /**
     * Zero for toplevel, otherwise equal to the number of enclosing scopes,
     * for some definition of scope.  The definition of scope will be adjusted
     * as is convenient to the implementation.
     */
    int lexicalNestingDepth;

    boolean atTopLevelInsideTraitOrObject = false; // true immediately within a trait/object

    BATree<String, Boolean> immediateDef = null;

    Expr newName(VarRef vre, String s) {
        InterpreterNameRewriter t = rewrites.get(s);

        if (NodeUtil.isSingletonObject(vre)) {
            if (t == null) {
                noteUse(s, vre);
                return vre;
            } else {
                return t.replacement(vre);
            }
        } else {
            if (t == null) {
                noteUse(s, vre);
                return vre;
            } else {
                return t.replacement(vre);
            }
        }
    }

    Expr newName(OpRef vre, String s) {
        InterpreterNameRewriter t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            return t.replacement(vre);
        }

    }

    BoolRef newName(BoolRef vre, String s) {
        InterpreterNameRewriter t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            return t.replacement(vre);
        }

    }

    IntRef newName(IntRef vre, String s) {
        InterpreterNameRewriter t = rewrites.get(s);
        if (t == null) {
            return vre;
        } else {
            return t.replacement(vre);
        }

    }

    NamedType newType(VarType nt, String s) {

        InterpreterNameRewriter t = rewrites.get(s);
        if (t == null) {
            return nt;
        } else {
            return t.replacement(nt);
        }
    }

    //   NamedType newType(TraitType nt, String s) {
    //
    //       Thing t = rewrites.get(s);
    //       if (t == null) {
    //           return nt;
    //       } else {
    //           return t.replacement(nt);
    //       }
    //   }

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
            return ExprFactory.makeVarRef(s, WellKnownNames.secretSelfName, -1);
        }
        if (i > 0) {
            return ExprFactory.makeFieldRef(s, dottedReference(s, i - 1), NodeFactory.makeId(s,
                                                                                             WellKnownNames.secretParentName));
        } else {
            throw new Error("Confusion in member reference numbering.");
        }

    }

    /**
     * Inserts visible top-level names into the maps used by the desugarer.
     *
     * @param tlnode
     */
    public void preloadTopLevel(CompilationUnit tlnode) {
        if (tlnode instanceof Component) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Component com = (Component) tlnode;
            List<Decl> defs = com.getDecls();
            defsToLocals(defs);
            functionalMethodsOfDefsToLocals(defs);
        } else if (tlnode instanceof Api) {
            // Iterate over definitions, collecting mapping from name
            // to node.
            // TODO - we may need to separate this out some more because of
            // circular dependences between type names. See above.
            Api com = (Api) tlnode;
            List<Decl> defs = com.getDecls();
            defsToLocals(defs);
            functionalMethodsOfDefsToLocals(defs);
        }

    }

    public boolean injectAtTopLevel(String putName,
                                    String getName,
                                    Map<String, InterpreterNameRewriter> getFrom,
                                    Set<String> excluded) {
        InterpreterNameRewriter th = getFrom.get(getName);
        InterpreterNameRewriter old = rewrites.get(putName);
        /* Empty means do  add */
        if (old == null) {
            rewrites_put(putName, th);
            return true;
        }
        /* Equal means no change */
        if (old.equals(th)) return false;
        excluded.add(putName);
        rewrites.remove(putName);

        return true;

    }

    /**
     * The helper visitor takes the place of "super.forWhatever".
     * This simplifies translation of code from the reflective visitor,
     * and also allows easy spearation (below) of the normal recursion from
     * the within-trait-or-object recursion (visitNode, visitNodeTO).
     */
    final NodeUpdateVisitor helper = new NodeUpdateVisitor() {
        @Override
        public Node recur(Node that) {
            return DesugarerVisitor.this.recur(that);
        }

        /**
         * Optionally splices in the contents of a RewriteHackList
         * returned from a visit; otherwise, it is just like the supertype's
         * implementation.  Inspection (and testing) of the code reveals
         * that RewriteHackList are only returned into lists of Decl, so this
         * is the only override that is necessary.
         */
        @Override
        public List<Decl> recurOnListOfDecl(List<Decl> that) {
            List<Decl> accum = new java.util.ArrayList<Decl>();
            boolean unchanged = true;
            for (Decl elt : that) {
                Node node = recur(elt);

                if (node instanceof RewriteHackList) {
                    /*
                     * Assume change, if someone went to the trouble to return
                     * one of these.
                     */
                    unchanged = false;
                    for (Node nnode : ((RewriteHackList) node).getNodes()) {
                        accum.add((Decl) nnode);
                    }
                } else {
                    Decl update_elt = (Decl) node;
                    unchanged &= (elt == update_elt);
                    accum.add(update_elt);
                }
            }
            return unchanged ? that : accum;
        }

    };

    /**
     * Explicit recursion, from anything that is not a trait or an object.
     * All things that can appear at the top level of a trait or object use
     * explicit recursive calls.
     *
     * @param that
     * @return
     */
    public AbstractNode visitNode(Node that) {
        atTopLevelInsideTraitOrObject = false;
        return (AbstractNode) that.accept(helper);
    }

    /**
     * Explicit recursion from a trait or object.
     * Leaves atTopLevelInsideTraitOrObject unchanged.
     *
     * @param that
     * @return
     */
    public AbstractNode visitNodeTO(Node that) {
        return (AbstractNode) that.accept(helper);
    }

    /**
     * Recursion into descendant nodes is wrapped in a save/restore/filter
     * of the various maps and state variables set in desugarer's walk over
     * the AST.
     */
    @Override
    public Node recur(Node that) {

        BATree<String, InterpreterNameRewriter> savedE = rewrites;
        rewrites = rewrites.copy();

        BASet<String> savedA = arrows;
        arrows = arrows.copy();

        BATree<String, StaticParam> savedVisibleGenerics = visibleGenericParameters;
        visibleGenericParameters = visibleGenericParameters.copy();

        BATree<String, StaticParam> savedUsedGenerics = usedGenericParameters;
        usedGenericParameters = usedGenericParameters.copy();

        BATree<String, Boolean> savedImmediateDef = immediateDef;
        immediateDef = null;

        boolean savedFnDeclIsMethod = atTopLevelInsideTraitOrObject;
        int savedObjectNestingDepth = objectNestingDepth;
        int savedLexicalNestingDepth = lexicalNestingDepth;

        /*
         * TRUE RECURSION HERE
         */
        Node returned_node = that.accept(this);

        rewrites = savedE;
        arrows = savedA;
        // Copy references from enclosed into enclosing.
        // Set true for all strings s in savedVisible
        // such that s is not in the immediately-defined set
        // (the immediate definition shadows) and where
        // used(s) is true.
        for (String s : savedVisibleGenerics.keySet()) {
            if (immediateDef == null || !immediateDef.containsKey(s)) {
                StaticParam tp = usedGenericParameters.get(s);
                if (tp != null) savedUsedGenerics.put(s, tp);
            }
        }
        visibleGenericParameters = savedVisibleGenerics;
        usedGenericParameters = savedUsedGenerics;
        atTopLevelInsideTraitOrObject = savedFnDeclIsMethod;
        objectNestingDepth = savedObjectNestingDepth;
        lexicalNestingDepth = savedLexicalNestingDepth;
        immediateDef = savedImmediateDef;

        return returned_node;
    }

    /**
     * Redirects the call to the helper to avoid code duplication;
     * that code pasted here would have exactly the same effect.
     */
    @Override
    public List<Decl> recurOnListOfDecl(List<Decl> that) {
        return helper.recurOnListOfDecl(that);
    }


    @Override
    public Node forComponent(Component com) {
        List<Decl> defs = com.getDecls();
        defsToLocals(defs);

        if (debug && !this.suppressDebugDump) System.err.println("BEFORE\n" + NodeUtil.dump(com));

        APIName name_result = (APIName) recur(com.getName());
        List<Import> imports_result = recurOnListOfImport(com.getImports());
        List<APIName> exports_result = recurOnListOfAPIName(com.getExports());
        List<Decl> decls_result = recurOnListOfDecl(com.getDecls());

        /* decls_result could be an immutable list, so we must create
         * a new one and modify that
         */
        List<Decl> new_decls_result = new ArrayList<Decl>(decls_result);

        new_decls_result.add(NodeFactory.make_RewriteObjectExprDecl(NodeUtil.getSpan(com), objectExprs));
        new_decls_result.add(NodeFactory.make_RewriteFunctionalMethodDecl(NodeUtil.getSpan(com),
                                                                          Useful.list(functionals)));

        AbstractNode nn = NodeFactory.makeComponent(NodeUtil.getSpan(com),
                                                    name_result,
                                                    imports_result,
                                                    new_decls_result,
                                                    com.is_native(),
                                                    exports_result);

        if (debug && !this.suppressDebugDump) System.err.println("AFTER\n" + NodeUtil.dump(nn));

        return nn;
    }

    @Override
    public Node forAmbiguousMultifixOpExpr(AmbiguousMultifixOpExpr op) {
        // NEB: This code is temporary. Very soon the static end will
        // remove these nodes and they should never appear at this
        // phase of execution. However, now we simply create an OpExpr.
        Node node = ExprFactory.makeOpExpr(NodeUtil.getSpan(op),
                                           NodeUtil.isParenthesized(op),
                                           NodeUtil.getExprType(op),
                                           op.getInfix_op(),
                                           op.getArgs());
        return visitNode(node);
    }

    @Override
    public Node forApi(Api com) {
        List<Decl> defs = com.getDecls();
        defsToLocals(defs);
        return visitNode(com);
    }

    @Override
    public Node forOpRef(OpRef vre) {
        String s = NodeUtil.stringName(vre.getOriginalName());
        Expr expr = newName(vre, s);
        return visitNode(expr);
    }

    @Override
    public Node forBoolRef(BoolRef vre) {
        String s = NodeUtil.nameString(vre);
        BoolRef expr = newName(vre, s);
        return visitNode(expr);
    }

    @Override
    public Node forIntRef(IntRef vre) {
        String s = NodeUtil.nameString(vre);
        IntRef expr = newName(vre, s);
        return visitNode(expr);
    }

    @Override
    public Node forVarRef(VarRef vre) {
        if (NodeUtil.isSingletonObject(vre)) {
            String s = NodeUtil.stringName(vre.getVarId());
            return visitNode(newName(vre, s));
        }

        String s = vrToString(vre);
        StaticParam tp = visibleGenericParameters.get(s);
        if (tp != null) {
            usedGenericParameters.put(s, tp);
        }
        Expr update = newName(vre, s);
        return update;
    }

    @Override
    public Node forVarType(VarType vre) {
        Id id = vre.getName();
        Node node = vre;
        if (id.getApiName().isNone()) {
            String s = NodeUtil.nameString(id);
            StaticParam tp = visibleGenericParameters.get(s);
            if (tp != null) {
                usedGenericParameters.put(s, tp);
            }
            node = newType(vre, s);
        } else {
            // Rewrite lexical nesting depth to zero if api-qualified.
            node = NodeFactory.makeVarType(vre, Environment.TOP_LEVEL);
        }

        return visitNode(node);

    }


    @Override
    public Node forFieldRef(FieldRef that) {
        atTopLevelInsideTraitOrObject = false;
        // Believe that any field selection is already
        // disambiguated. In an ordinary method, "self"
        // is already defined, in a notself method, "notself"
        // is defined, and "self" is defined as a local in
        // an enclosing scope (as is "notnotself", etc, that
        // intervening notself methods might define).

        // However, since the LHS of a field selection might
        // be a method invocation, we DO need to check that.
        return visitNode(that);
    }

    @Override
    public Node forLValue(LValue lvb) {
        Id id = lvb.getName();
        if ("_".equals(id.getText())) {
            Id newId = NodeFactory.makeId(NodeUtil.getSpan(id), WellKnownNames.tempForUnderscore(id));
            return NodeFactory.makeLValue(lvb, newId);
        }
        return visitNode(lvb);
    }

    @Override
    public Node forMathPrimary(MathPrimary node) {
        if (looksLikeMethodInvocation(node)) return translateJuxtOfDotted(node);
        else return visitNode(node);
    }

    @Override
    public Node forJuxt(Juxt node) {
        if (node.isTight()) {
            if (looksLikeMethodInvocation(node)) return translateJuxtOfDotted(node);
            else return visitNode(node);
        } else return visitNode(node);
    }

    @Override
    public Node forTraitType(TraitType vre) {
        return visitNode(vre);
    }

    @Override
    public Node forTraitTypeWhere(TraitTypeWhere vre) {
        lexicalNestingDepth++;
        Option<WhereClause> owc = vre.getWhereClause();
        if (owc.isSome()) {
            WhereClause wc = owc.unwrap();
            List<WhereBinding> lwb = wc.getBindings();

            for (WhereBinding wb : lwb) {
                type_rewrites_put(wb.getName().getText(), new Local());
            }

            /* Handcoded visit to avoid a "recur" visit on the WhereClause
              The binding action of a WhereClause in a TraitTypeWhere seems
              to be different.
            */

            WhereClause nwc = (WhereClause) visitNode(wc);
            BaseType t = vre.getBaseType();
            BaseType nt = (BaseType) recur(t);
            if (t == nt && wc == nwc) return vre;
            else return NodeFactory.makeTraitTypeWhere(nt, Wrapper.<WhereClause>make(nwc));
        } else {
            return visitNode(vre);
        }


    }

    @Override
    public Node forWhereClause(WhereClause wc) {
        lexicalNestingDepth++;

        List<WhereBinding> lwb = wc.getBindings();
        for (WhereBinding wb : lwb) {
            type_rewrites_put(wb.getName().getText(), new Local());
        }

        return visitNode(wc);
    }


    @Override
    public Node forFnDecl(FnDecl fndef) {
        if (atTopLevelInsideTraitOrObject) {
            var_rewrites_put(WellKnownNames.defaultSelfName, new SelfRewrite());
        }
        atTopLevelInsideTraitOrObject = false;
        lexicalNestingDepth++;

        List<Param> params = NodeUtil.getParams(fndef);
        // NodeUtil.getFnName(fndef); handled at top level.
        List<StaticParam> tparams = NodeUtil.getStaticParams(fndef);

        paramsToLocals(params);
        immediateDef = tparamsToLocals(tparams, immediateDef);

        Option<Contract> _contract = NodeUtil.getContract(fndef);
        Option<List<Expr>> _requires;
        Option<List<EnsuresClause>> _ensures;
        Option<List<Expr>> _invariants;
        if (_contract.isSome()) {
            _requires = _contract.unwrap().getRequiresClause();
            _ensures = _contract.unwrap().getEnsuresClause();
            _invariants = _contract.unwrap().getInvariantsClause();
        } else {
            _requires = Option.<List<Expr>>none();
            _ensures = Option.<List<EnsuresClause>>none();
            _invariants = Option.<List<Expr>>none();
        }
        // List<Expr> _exprs = new ArrayList<Expr>();

        AbstractNode n;

        if (fndef.getBody().isSome() && (_ensures.isSome() || _requires.isSome() || _invariants.isSome())) {
            List<Expr> exprs = new ArrayList<Expr>();
            exprs.add(fndef.getBody().unwrap());
            Block b = ExprFactory.makeBlock(NodeUtil.getSpan(_contract.unwrap()), exprs);
            if (_invariants.isSome()) b = translateInvariants(_invariants, b);
            if (_ensures.isSome()) b = translateEnsures(_ensures, b);
            if (_requires.isSome()) b = translateRequires(_requires, b);

            // Remove the original contract, add the translation
            FnDecl f = NodeFactory.makeFnDecl(NodeUtil.getSpan(fndef),
                                              NodeUtil.getMods(fndef),
                                              NodeUtil.getName(fndef),
                                              NodeUtil.getStaticParams(fndef),
                                              NodeUtil.getParams(fndef),
                                              NodeUtil.getReturnType(fndef),
                                              NodeUtil.getThrowsClause(fndef),
                                              NodeUtil.getWhereClause(fndef),
                                              Option.<Contract>none(),
                                              Option.<Expr>some(b));

            n = visitNode(f);
        } else {
            n = visitNode(fndef);
        }

        dumpIfChange(fndef, n);
        return n;

    }

    @Override
    public Node forVarDecl(VarDecl vd) {
        atTopLevelInsideTraitOrObject = false;
        List<LValue> lhs = vd.getLhs();

        if (lhs.size() > 1) {
            // Introduce a temporary, then initialize elements
            // piece-by-piece.
            if (vd.getInit().isNone()) return bug(vd, "Variable definition should have an expression.");
            Expr init = vd.getInit().unwrap();
            init = (Expr) recur(init);
            lhs = (List<LValue>) recurOnListOfLValue(lhs);
            ArrayList<VarDecl> newdecls = new ArrayList<VarDecl>(1 + lhs.size());
            String temp = WellKnownNames.tempTupleName(vd);
            Span at = NodeUtil.getSpan(vd);
            VarDecl new_vd = NodeFactory.makeVarDecl(at, temp, init);
            newdecls.add(new_vd);
            int element_index = 0;
            for (LValue lv : lhs) {
                Id newName = NodeFactory.makeId(at, "$" + element_index);
                newdecls.add(NodeFactory.makeVarDecl(at, Useful.list(lv), Option.<Expr>some(ExprFactory.makeFieldRef(at,
                                                                                                                     init,
                                                                                                                     newName))));
                element_index++;
            }
            return new RewriteHackList(newdecls);
        }
        // Leave singleton VarDecl alone, probably, because it gets properly
        // handled
        // at the enclosing block.
        return visitNode(vd);

    }


    @Override
    public Node for_RewriteObjectExpr(_RewriteObjectExpr oe) {
        List<Decl> defs = NodeUtil.getDecls(oe);
        List<BaseType> xtends = NodeUtil.getTypes(NodeUtil.getExtendsClause(oe));
        objectNestingDepth++;
        lexicalNestingDepth++;
        atTopLevelInsideTraitOrObject = true;
        defsToMembers(defs);
        accumulateMembersFromExtends(xtends, rewrites);
        AbstractNode n = visitNodeTO(oe);
        return n;
    }

    @Override
    public Node forObjectExpr(ObjectExpr oe) {
        // TODO wip

        // REMEMBER THAT THIS IS THE NEW _RewriteObjectExpr!
        // Implicitly parameterized by either visibleGenericParameters,
        // or by usedGenericParameters.
        // Note the keys of a BATree are sorted.
        _RewriteObjectExpr rwoe = (_RewriteObjectExpr) recur(ExprFactory.make_RewriteObjectExpr((ObjectExpr) oe,
                                                                                                usedGenericParameters));
        objectExprs.add(rwoe);

        return visitNode(ExprFactory.make_RewriteObjectExprRef(rwoe));
    }

    @Override
    public Node forCatch(Catch that) {
        String s = ((Catch) that).getName().stringName();
        lexicalNestingDepth++;
        var_rewrites_put(s, new Local());
        return visitNode(that);
    }

    @Override
    public Node forFnExpr(FnExpr fndef) {
        atTopLevelInsideTraitOrObject = false;
        lexicalNestingDepth++;
        List<Param> params = NodeUtil.getParams(fndef);
        paramsToLocals(params);
        return visitNode(fndef);
    }

    @Override
    public Node forLetFn(LetFn lf) {
        atTopLevelInsideTraitOrObject = false;
        lexicalNestingDepth++;
        // defined var is no longer eligible for rewrite.
        List<Decl> defs = new ArrayList<Decl>();
        for (FnDecl d : lf.getFns()) {
            defs.add((Decl) d);
        }
        defsToLocals(defs);
        // All the function names are in scope in the function
        // definitions, and in the body of code that follows them.
        // TODO wip
        return visitNode(lf);

    }

    @Override
    public Node forLocalVarDecl(LocalVarDecl lvd) {
        atTopLevelInsideTraitOrObject = false;
        lexicalNestingDepth++;
        lvaluesToLocal(lvd.getLhs());
        // not quite right because initializing exprs are evaluated in the wrong context.
        // TODO wip

        return visitNode(lvd);
    }

    @Override
    public Node forObjectDecl(ObjectDecl od) {
        // All the methods and fields defined in object and the
        // extended traits
        // are mapped to "self".
        List<Decl> defs = NodeUtil.getDecls(od);
        Option<List<Param>> params = NodeUtil.getParams(od);
        List<StaticParam> tparams = NodeUtil.getStaticParams(od);
        List<BaseType> xtends = NodeUtil.getTypes(NodeUtil.getExtendsClause(od));

        // TODO wip
        lexicalNestingDepth++;
        objectNestingDepth++;
        atTopLevelInsideTraitOrObject = true;

        defsToMembers(defs);
        immediateDef = tparamsToLocals(tparams, immediateDef);
        paramsToMembers(params);

        accumulateMembersFromExtends(xtends, rewrites);

        AbstractNode n = visitNodeTO(od);

        return n;
    }

    @Override
    public Node forTraitDecl(TraitDecl td) {
        List<Decl> defs = NodeUtil.getDecls(td);
        List<StaticParam> tparams = NodeUtil.getStaticParams(td);
        // TODO wip
        lexicalNestingDepth++;
        objectNestingDepth++;
        atTopLevelInsideTraitOrObject = true;
        defsToMembers(defs);
        immediateDef = tparamsToLocals(tparams, immediateDef);

        accumulateMembersFromExtends(td);

        AbstractNode n = visitNodeTO(td);
        return n;
    }

    @Override
    public Node forSpawn(Spawn s) {
        Expr body = s.getBody();
        Span sp = NodeUtil.getSpan(s);
        // If the user writes Spawn(foo) instead of Spawn(foo()) we get an inexplicable error
        // message.  We might want to put in a check for that someday.

        Node rewrittenExpr = visit(body);

        Expr in_fn =
            ExprFactory.makeVarRef(sp,
                NodeFactory.makeId(sp,
                                   WellKnownNames.fortressBuiltin(),
                                   WellKnownNames.thread));
        List<StaticArg> args = new ArrayList<StaticArg>();
        args.add(
            NodeFactory.makeTypeArg(sp,
                NodeFactory.makeVarType(sp,
                    NodeFactory.makeId(sp,
                                       WellKnownNames.anyTypeLibrary(),
                                       WellKnownNames.anyTypeName),
                    Environment.TOP_LEVEL)));

        _RewriteFnRef fn = ExprFactory.make_RewriteFnRef(NodeUtil.getSpan(s), false, Option.<Type>none(), in_fn, args);

        List<Param> params = Collections.emptyList();
        FnExpr fnExpr = ExprFactory.makeFnExpr(sp, params, (Expr) rewrittenExpr);

        return visitNode(ExprFactory.make_RewriteFnApp(NodeUtil.getSpan(s), fn, fnExpr));
    }

    @Override
    public Node forGrammarDecl(GrammarDecl node) {
        return node;
    }

    private List<Id> collectIds(TypeOrPattern t) {
        List<Id> ids = new ArrayList<Id>();
        if (t instanceof Pattern) {
            for (PatternBinding pb : ((Pattern)t).getPatterns().getPatterns()) {
                if (pb instanceof PlainPattern) {
                    PlainPattern that = (PlainPattern)pb;
                    ids.add(that.getName());
                    if (that.getIdType().isSome())
                        ids.addAll(collectIds(that.getIdType().unwrap()));
                } else if (pb instanceof NestedPattern) {
                    ids.addAll(collectIds(((NestedPattern)pb).getPat()));
                }
            }
        }
        return ids;
    }

    @Override
    public Node forTypecaseClause(TypecaseClause that) {
        if (that.getName().isSome()) {
            IdsToLocals(Useful.list(that.getName().unwrap()));
        }
        TypeOrPattern matchType = that.getMatchType();
        IdsToLocals(collectIds(matchType));
        return visitNode(that);
    }

    /**
     * Performs an object-reference-disambiguating com.sun.fortress.interpreter.rewrite of the AST. Any
     * effects on the environment from the toplevel remain visible, otherwise
     * visit only rewrites.
     */
    //@Override
    public Node visit(AbstractNode node) {
        return recur(node);
    }

    private String vrToString(VarRef vre) {
        Iterable<Id> ids = NodeUtil.getIds(vre.getVarId());
        // TODO this omits the leading API name
        String s = IterUtil.last(ids).getText();
        return s;
    }

    /**
     * Given List<Id>, generate tuple of corresponding VarRef
     */
    Expr tupleForIdList(List<Id> binds) {
        if (binds.size() == 1) return ExprFactory.makeVarRef(binds.get(0));

        List<Expr> refs = new ArrayList<Expr>(binds.size());

        for (Id b : binds) {
            refs.add(ExprFactory.makeVarRef(b));
        }

        return ExprFactory.makeTupleExpr(new Span(NodeUtil.getSpan(binds.get(0)), NodeUtil.getSpan(binds.get(
                binds.size() - 1))), refs);
    }

    /**
     * Given expr e, return (fn () => e)
     */
    Expr thunk(Expr e) {
        return ExprFactory.makeFnExpr(NodeUtil.getSpan(e), Collections.<Param>emptyList(), e);
    }

    /**
     * @param td
     */
    private void accumulateMembersFromExtends(TraitDecl td) {
        accumulateMembersFromExtends(NodeUtil.getTypes(NodeUtil.getExtendsClause(td)), rewrites);
    }

    private void accumulateMembersFromExtends(List<BaseType> xtends, Map<String, InterpreterNameRewriter> disEnv) {
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
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

    /**
     * @param defs
     */
    private void defsToLocals(List<Decl> defs) {
        for (Decl d : defs) {
            if (d instanceof TraitDecl) {
                String s = d.stringName();
                TraitDecl dod = (TraitDecl) d;
                type_rewrites_put(s, new Trait(dod, rewrites));
            } else if (d instanceof ObjectDecl) {
                ObjectDecl od = (ObjectDecl) d;
                Option<List<Param>> params = NodeUtil.getParams(od);
                /*
                 *  Add the object itself (either constructor or singleton,
                 *  we don't care much) to the locals.
                 *
                 *  If it is a constructor, it is also an arrow type.
                 *
                 */
                String s = NodeUtil.getName(od).getText();
                obj_rewrites_put(s, new Local());

                if (params.isNone()) arrows.remove(s);
                else arrows.add(s);
            } else {
                for (String s : NodeUtil.stringNames(d)) {
                    var_rewrites_put(s, new Local());
                }
            }
        }
    }

    /**
     * @param defs
     */
    private void functionalMethodsOfDefsToLocals(List<Decl> defs) {
        for (Decl d : defs) {
            if (d instanceof TraitObjectDecl) {
                TraitObjectDecl dod = (TraitObjectDecl) d;
                List<Decl> tdecls = NodeUtil.getDecls(dod);
                handlePossibleFM(tdecls);
            } else {
                // Thankfully, do nothing.
            }
        }
    }

    private void handlePossibleFM(List<Decl> tdecls) {
        for (Decl adod : tdecls) {
            ArrowOrFunctional aof = adod.accept(IsAnArrowName.isAnArrowName);
            if (aof == ArrowOrFunctional.FUNCTIONAL) {
                // Only certain things can be a functional method.
                FnDecl fadod = (FnDecl) adod;
                String s = NodeUtil.getName(fadod).stringName();
                arrows.add(s);
                functionals.add(s);
                var_rewrites_put(s, new FunctionalMethod());
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
            if (!s.equals(WellKnownNames.defaultSelfName)) {
                var_rewrites_put(s, new Local());

            }
        }
    }

    private void IdsToLocals(List<Id> lid) {
        for (Id d : lid) {
            String s = d.getText();
            var_rewrites_put(s, new Local());
        }
    }

    private NodeAbstractVisitor_void localVisitor = new NodeAbstractVisitor_void() {

        @Override
        public void forExtentRange(ExtentRange that) {
            super.forExtentRange(that);
        }

        @Override
        public void forLValue(LValue that) {
            that.getName().accept(this);
        }

        @Override
        public void forId(Id that) {
            String s = that.getText();
            var_rewrites_put(s, new Local());
        }

    };

    /**
     * @param params
     */
    private void lvaluesToLocal(List<? extends LValue> params) {
        for (LValue param : params) {
            param.accept(localVisitor);
        }
    }

    /**
     * @param params
     */
    private BATree<String, Boolean> tparamsToLocals(List<StaticParam> params, BATree<String, Boolean> immediateDef) {
        if (!params.isEmpty()) for (StaticParam d : params) {
            String s = NodeUtil.getName(d);
            // OpParams are not real members
            // if (! (d instanceof OpParam))
            type_rewrites_put(s, new Local());
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
        if (immediateDef == null) immediateDef = new BATree<String, Boolean>(StringHashComparer.V);
        immediateDef.put(s, Boolean.FALSE);
        return immediateDef;
    }

    /**
     * @param defs
     */
    private void defsToMembers(List<Decl> defs) {
        for (Decl dd : defs) {

            if (dd instanceof VarDecl) {
                //visited.add(tdod);
            } else {
                String sdd = dd.stringName();
                ArrowOrFunctional aof = dd.accept(IsAnArrowName.isAnArrowName);

                if (aof != ArrowOrFunctional.NEITHER) {

                    arrows.add(sdd);
                    if (aof == ArrowOrFunctional.FUNCTIONAL) {
                        functionals.add(sdd);

                        continue; // NOT a regular member, do not add.
                    }
                } else {
                    arrows.remove(sdd);
                }
            }
            for (String s : NodeUtil.stringNames(dd)) {
                var_rewrites_put(s, new Member());
            }
        }

    }

    /**
     * @param params
     */
    private void paramsToMembers(Option<List<Param>> params) {
        if (params.isSome()) for (Param d : params.unwrap()) {
            String s = d.getName().getText();
            var_rewrites_put(s, new Member());
            ArrowOrFunctional aof = d.accept(IsAnArrowName.isAnArrowName);
            if (aof != ArrowOrFunctional.NEITHER) {
                arrows.add(s);
                if (aof == ArrowOrFunctional.FUNCTIONAL) {
                    functionals.add(s);
                    throw new Error("Don't think this can happen");

                }
            } else arrows.remove(s);
        }
    }


    /**
     * @param params
     */
    private void stringsToMembers(Collection<String> strings) {
        for (String s : strings) {
            var_rewrites_put(s, new Member());
            arrows.remove(s);
        }
    }

    /**
     * @param params
     */
    private void stringsToLocals(Collection<String> strings) {
        for (String s : strings) {
            type_rewrites_put(s, new Local());
            arrows.remove(s);
        }
    }

    /**
     * @param xtends  List of types that a trait/object extends
     * @param typeEnv The environment in which those names are interpreted
     * @param members (output) all the members transitively introduced by all
     *                extended traits
     * @param types   (output) all the names of all the types transitively extended
     * @param visited (bookkeeping) to prevent revisiting, and possible looping on
     *                bad inputs
     */
    private void accumulateTraitsAndMethods(List<BaseType> xtends,
                                            Map<String, InterpreterNameRewriter> typeEnv,
                                            Set<String> members,
                                            Set<String> types,
                                            Set<String> arrow_names,
                                            Set<String> not_arrow_names,
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
            boolean hasApi = name.getApiName().isSome();
            // The map is not consistent; the stored names lack API qualifiers.
            // TODO fix this inconsistency.
            if (true || !hasApi) {
                // TODO we've got to generalize this to qualified names.
                String s = name.getText();
                InterpreterNameRewriter th;
                try {
                    th = typeEnv.get(s);
                }
                catch (NullPointerException x) {
                    String msg = errorMsg("Entity ", s, " not found in typeEnv ", typeEnv);
                    th = bug(msg);
                }
                if (th instanceof Trait) {
                    Trait tr = (Trait) th;
                    TraitDecl tdod = tr.defOrDecl;
                    if (!(visited.contains(tdod))) {
                        visited.add(tdod);
                        // Process this trait -- add its name, as well
                        // as all the members
                        // types.add(s); // The trait is known by this
                        // name.
                        for (Decl dd : NodeUtil.getDecls(tdod)) {
                            String sdd = dd.stringName();
                            if (dd instanceof VarDecl) {
                                //visited.add(tdod);
                            } else {
                                ArrowOrFunctional aof = dd.accept(IsAnArrowName.isAnArrowName);

                                if (aof != ArrowOrFunctional.NEITHER) {
                                    arrow_names.add(sdd);
                                    if (aof == ArrowOrFunctional.FUNCTIONAL) {
                                        functionals.add(sdd);
                                        var_rewrites_put(sdd, new FunctionalMethod());
                                        continue; // do not add as a member
                                    }
                                } else {
                                    not_arrow_names.add(sdd);
                                }
                            }
                            members.add(sdd);
                        }
                        accumulateTraitsAndMethods(NodeUtil.getTypes(NodeUtil.getExtendsClause(tdod)),
                                                   tr.env,
                                                   members,
                                                   types,
                                                   arrow_names,
                                                   not_arrow_names,
                                                   visited);
                    }
                } else if (th instanceof Object) {
                    error(t, errorMsg("Attempt to extend object type ", s, ", saw ", th));
                } else if (th == null) {
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
        //Id qidn = first.getVarId();

        // Optimistic casts here, will need revisiting in the future,
        // perhaps FieldRefs are too general
        // Recursive visits here
        Node expr = visit(first);
        List<Expr> visitedArgs = recurOnListOfExpr(exprs.subList(1, exprs.size()));
        if (expr instanceof VarRef) {
            VarRef vre = (VarRef) expr;
            if (vre.getLexicalDepth() == -1) {
                return ExprFactory.makeMethodInvocation(NodeUtil.getSpan(node),
                                                        false,
                                                        NodeUtil.getExprType(node),
                                                        ExprFactory.makeVarRef(NodeUtil.getSpan(node),
                                                                               WellKnownNames.secretSelfName),
                                                        // this will rewrite in the future.
                                                        (Id) vre.getVarId(),
                                                        visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteralExpr(
                                                                NodeUtil.getSpan(node)) : //TODO wrong span
                                                        visitedArgs.size() == 1 ?
                                                        visitedArgs.get(0) :
                                                        ExprFactory.makeTupleExpr(NodeFactory.makeSpan("impossible",
                                                                                                       visitedArgs),
                                                                                  visitedArgs));
            }
        } else if (expr instanceof FieldRef) {

            FieldRef selfDotSomething = (FieldRef) visit(first);

            return ExprFactory.makeMethodInvocation(NodeUtil.getSpan(node),
                                                    false,
                                                    NodeUtil.getExprType(node),
                                                    selfDotSomething.getObj(),
                                                    // this will rewrite in the future.
                                                    selfDotSomething.getField(),
                                                    visitedArgs.size() == 0 ?
                                                    ExprFactory.makeVoidLiteralExpr(NodeUtil.getSpan(node)) :
                                                    //TODO wrong span
                                                    visitedArgs.size() == 1 ?
                                                    visitedArgs.get(0) :
                                                    ExprFactory.makeTupleExpr(NodeFactory.makeSpan("impossible",
                                                                                                   visitedArgs),
                                                                              visitedArgs));
        }

        throw new Error("Not there yet.");


    }

    private AbstractNode translateJuxtOfDotted(MathPrimary node) {
        VarRef first = (VarRef) node.getFront();
        //Id qidn = first.getVarId();

        // Optimistic casts here, will need revisiting in the future,
        // perhaps FieldRefs are too general
        // Recursive visits here
        FieldRef selfDotSomething = (FieldRef) visit(first);
        //        List<MathItem> visitedArgs = visitList(exprs);
        Node arg = visit(((ExprMI) node.getRest().get(0)).getExpr());

        return ExprFactory.makeMethodInvocation(NodeUtil.getSpan(node),
                                                false,
                                                NodeUtil.getExprType(node),
                                                selfDotSomething.getObj(),
                                                // this will rewrite in the future.
                                                selfDotSomething.getField(),
                                                (Expr) arg
                                                /*
                                            visitedArgs.size() == 0 ? ExprFactory.makeVoidLiteralExpr(NodeUtil.getSpan(node)) : // wrong span
                                            visitedArgs.size() == 1 ? visitedArgs.get(0) :
                                                new TupleExpr(visitedArgs)
                                                */);
    }

    private Block translateRequires(Option<List<Expr>> _requires, Block b) {
        List<Expr> r = _requires.unwrap(Collections.<Expr>emptyList());
        for (Expr e : r) {
            Span sp = NodeUtil.getSpan(e);
            GeneratorClause cond = ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(), e);
            If _if = ExprFactory.makeIf(sp, NodeFactory.makeIfClause(sp, cond, b), ExprFactory.makeThrow(sp,
                                                                                                         "CallerViolation"));
            b = ExprFactory.makeBlock(sp, _if);
        }
        return b;
    }

    private Block translateEnsures(Option<List<EnsuresClause>> _ensures, Block b) {
        List<EnsuresClause> es = _ensures.unwrap(Collections.<EnsuresClause>emptyList());
        for (EnsuresClause e : es) {
            Span sp = NodeUtil.getSpan(e);
            Id t1 = gensymId("t1");
            Block inner_block = ExprFactory.makeBlock(sp, ExprFactory.makeVarRef(sp, WellKnownNames.outcome));
            GeneratorClause cond;
            cond = ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(), e.getPost());
            If _inner_if = ExprFactory.makeIf(sp,
                                              NodeFactory.makeIfClause(sp, cond, inner_block),
                                              ExprFactory.makeThrow(sp, WellKnownNames.calleeViolationException));

            cond = ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(), (Expr) ExprFactory.makeVarRef(sp, t1));
            If _if = ExprFactory.makeIf(sp,
                                        NodeFactory.makeIfClause(sp, cond, ExprFactory.makeBlock(sp, _inner_if)),
                                        ExprFactory.makeBlock(sp, ExprFactory.makeVarRef(sp, WellKnownNames.outcome)));
            LocalVarDecl r = ExprFactory.makeLocalVarDecl(sp, NodeFactory.makeId(sp, WellKnownNames.outcome), b, _if);
            Option<Expr> _pre = e.getPre();
            LocalVarDecl provided_lvd = ExprFactory.makeLocalVarDecl(sp,
                                                                     t1,
                                                                     _pre.unwrap(ExprFactory.makeVarRef(sp,
                                                                                                        WellKnownNames.fortressLibrary(),
                                                                                                        "true")),
                                                                     ExprFactory.makeBlock(sp, r));
            b = ExprFactory.makeBlock(sp, provided_lvd);
        }
        return b;
    }

    private Block translateInvariants(Option<List<Expr>> _invariants, Block b) {
        for (Expr e : _invariants.unwrap(Collections.<Expr>emptyList())) {
            Span sp = NodeUtil.getSpan(e);
            Id t1 = gensymId("t1");
            Id t_outcome = gensymId(WellKnownNames.outcome);
            Id t2 = gensymId("t2");

            Expr chain = (Expr) ExprFactory.makeChainExpr(sp,
                                                          (Expr) ExprFactory.makeVarRef(sp, t1),
                                                          NodeFactory.makeOpInfix(sp,
                                                                                  WellKnownNames.fortressLibrary(),
                                                                                  "="),
                                                          // new Op("="),
                                                          (Expr) ExprFactory.makeVarRef(sp, t2));
            GeneratorClause gen_chain = ExprFactory.makeGeneratorClause(sp, Useful.<Id>list(), chain);
            If _post = ExprFactory.makeIf(sp, NodeFactory.makeIfClause(sp, gen_chain, ExprFactory.makeBlock(sp,
                                                                                                            ExprFactory.makeVarRef(
                                                                                                                    sp,
                                                                                                                    WellKnownNames.outcome))),
                                          ExprFactory.makeThrow(sp, WellKnownNames.calleeViolationException));
            LocalVarDecl r2 = ExprFactory.makeLocalVarDecl(sp, t2, e, _post);
            LocalVarDecl r1 = ExprFactory.makeLocalVarDecl(NodeFactory.makeId(sp, WellKnownNames.outcome), b, r2);

            b = ExprFactory.makeBlock(sp, ExprFactory.makeLocalVarDecl(sp, t1, e, r1));
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
            if (rewrites.get(s) instanceof Member && !arrows.contains(s)) return true;
        }
        return false;
    }

    private boolean looksLikeMethodInvocation(MathPrimary node) {
        Expr first = node.getFront();
        if (first instanceof VarRef) {
            VarRef vr = (VarRef) first;
            String s = vrToString(vr);
            if (rewrites.get(s) instanceof Member && !arrows.contains(s) && node.getRest().size() == 1 &&
                node.getRest().get(0) instanceof ExprMI) return true;
        }
        return false;
    }

    public Set<String> getTopLevelRewriteNames() {
        return rewrites.keySet();
    }

}
