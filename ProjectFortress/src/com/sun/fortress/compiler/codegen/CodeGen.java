/*******************************************************************************
    Copyright 2009 Sun Microsystems, Inc.,
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
package com.sun.fortress.compiler.codegen;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.AnalyzeResult;
import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.OverloadSet;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.InstantiatingClassloader;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.syntax_abstractions.ParserMaker.Mangler;
import com.sun.fortress.useful.BA2Tree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.StringHashComparer;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.CollectUtil;
import edu.rice.cs.plt.collect.Relation;

// Note we have a name clash with org.objectweb.asm.Type
// and com.sun.fortress.nodes.Type.  If anyone has a better
// solution than writing out their entire types, please
// shout out.
public class CodeGen extends NodeAbstractVisitor_void implements Opcodes {
    CodeGenClassWriter cw;
    CodeGenMethodVisitor mv; // Is this a mistake?  We seem to use it to pass state to methods/visitors.
    final String packageAndClassName;
    private String traitOrObjectName; // set to name of current trait or object, as necessary.
    private String springBoardClass; // set to name of trait default methods class, if we are emitting it.

    // traitsAndObjects appears to be dead code.
    // private final Map<String, ClassWriter> traitsAndObjects =
    //     new BATree<String, ClassWriter>(DefaultComparator.normal());
    private final TypeAnalyzer ta;
    private final ParallelismAnalyzer pa;
    private final FreeVariables fv;
    private final Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> topLevelOverloads;
    private final MultiMap<String, Function> exportedToUnambiguous;
    private Set<String> overloadedNamesAndSigs;

    // lexEnv does not include the top level or object right now, just
    // args and local vars.  Object fields should have been translated
    // to dotted notation at this point, right?  Right?  (No, not.)
    private BATree<String, VarCodeGen> lexEnv;
    boolean inATrait = false;
    boolean inAnObject = false;
    boolean inABlock = false;
    private boolean emittingFunctionalMethodWrappers = false;
    private TraitObjectDecl currentTraitObjectDecl = null;

    private boolean fnRefIsApply = false; // FnRef is either apply or closure

    final Component component;
    private final ComponentIndex ci;
    private GlobalEnvironment env;
    private final JarOutputStream jos;

    private static final int NO_SELF = -1;

    // Create a fresh codegen object for a nested scope.  Technically,
    // we ought to be able to get by with a single lexEnv, because
    // variables ought to be unique by location etc.  But in practice
    // I'm not assuming we have a unique handle for any variable,
    // so we get a fresh CodeGen for each scope to avoid collisions.
    private CodeGen(CodeGen c) {
        this.cw = c.cw;
        this.mv = c.mv;
        this.packageAndClassName = c.packageAndClassName;
        this.traitOrObjectName = c.traitOrObjectName;
        this.springBoardClass = c.springBoardClass;

        this.ta = c.ta;
        this.pa = c.pa;
        this.fv = c.fv;
        this.topLevelOverloads = c.topLevelOverloads;
        this.exportedToUnambiguous = c.exportedToUnambiguous;
        this.overloadedNamesAndSigs = c.overloadedNamesAndSigs;

        this.lexEnv = new BATree<String,VarCodeGen>(c.lexEnv);

        this.inATrait = c.inATrait;
        this.inAnObject = c.inAnObject;
        this.inABlock = c.inABlock;
        this.emittingFunctionalMethodWrappers = c.emittingFunctionalMethodWrappers;
        this.currentTraitObjectDecl = c.currentTraitObjectDecl;
        this.jos = c.jos;

        this.component = c.component;
        this.ci = c.ci;
        this.env = c.env;

    }


    public CodeGen(Component c,
                   TypeAnalyzer ta, ParallelismAnalyzer pa, FreeVariables fv,
                   ComponentIndex ci, GlobalEnvironment env) {
        component = c;
        packageAndClassName = NamingCzar.javaPackageClassForApi(c.getName());
        String dotted = NamingCzar.javaPackageClassForApi(c.getName(), ".");
        try {
            this.jos = new JarOutputStream(new BufferedOutputStream( new FileOutputStream(NamingCzar.cache + dotted + ".jar")));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.ta = ta;
        this.pa = pa;
        this.fv = fv;
        this.ci = ci;
        this.exportedToUnambiguous = new MultiMap<String, Function> ();

        /*
         * Find every exported name, and make an entry mapping name to
         * API declarations, so that unambiguous names from APIs can
         * also be emitted.
         */
        List<APIName> exports = c.getExports();
        for (APIName apiname:exports) {
            ApiIndex api_index = env.api(apiname);
            Relation<IdOrOpOrAnonymousName, Function> fns = api_index.functions();
            for (IdOrOpOrAnonymousName name : fns.firstSet()) {
                if (name instanceof IdOrOp) {
                    Set<Function> defs = fns.matchFirst(name);
                    for (Function def : defs) {
                        IdOrOpOrAnonymousName ua_name = def.unambiguousName();
                        if (ua_name instanceof IdOrOp) {
                            IdOrOp ioo_name = (IdOrOp) name;
                            IdOrOp ioo_ua_name = (IdOrOp) ua_name;
                            if (! ioo_name.equals(ioo_ua_name)) {
                                // Add mapping ioo_name -> def to MultiMap
                                exportedToUnambiguous.putItem(
                                        ioo_name.getText(),
                                        def);
                            }
                        }
                    }
                }
            }
        }

        this.topLevelOverloads =
            sizePartitionedOverloads(ci.functions());

        this.overloadedNamesAndSigs = new HashSet<String>();
        this.lexEnv = new BATree<String,VarCodeGen>(StringHashComparer.V);
        this.env = env;


        debug( "Compile: Compiling ", packageAndClassName );
    }


    // We need to expose this because nobody else helping CodeGen can
    // understand unqualified names (esp types) without it!
    public APIName thisApi() {
        return ci.ast().getName();
    }

    /** Factor out method call path so that we do it right
        everywhere we invoke a dotted method of any kind. */
    private void methodCall(IdOrOp method,
                            TraitType receiverType,
                            Type domainType, Type rangeType) {
        int opcode;
        if (ta.typeCons(receiverType).unwrap().ast() instanceof TraitDecl &&
            !NamingCzar.fortressTypeIsSpecial(receiverType)) {
            opcode = INVOKEINTERFACE;
        } else {
            opcode = INVOKEVIRTUAL;
        }
        String sig = NamingCzar.jvmSignatureFor(domainType, rangeType, thisApi());
        String methodClass = NamingCzar.jvmTypeDesc(receiverType, thisApi(), false);
        String methodName = method.getText();
        mv.visitMethodInsn(opcode, methodClass, methodName, sig);
    }

    private void generateMainMethod() {

        // We generate two methods.  First a springboard static main()
        // method that creates an instance of the class we are
        // generating, and then invokes the runExecutable(...) method
        // on that instance---this is RTS code that sets up
        // command-line argument access and initializes the work
        // stealing infrastructure.
        //
        // The second method is the compute() method, which is invoked
        // by the work stealing infrastructure after it starts up, and
        // simply calls through to the static run() method that must
        // occur in this component.  Without this little trampoline,
        // we need to special case the run() method during code
        // generation and the result is not reentrant (ie if we call
        // run() recursively we lose).

        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            NamingCzar.stringArrayToVoid, null, null);
        mv.visitCode();
        // new packageAndClassName()
        mv.visitTypeInsn(Opcodes.NEW, packageAndClassName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, packageAndClassName, "<init>",
                           NamingCzar.voidToVoid);

        // .runExecutable(args)
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           NamingCzar.fortressExecutable,
                           NamingCzar.fortressExecutableRun,
                           NamingCzar.fortressExecutableRunType);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore,NamingCzar.ignore);
        mv.visitEnd();
        // return

        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC, "compute",
                            NamingCzar.voidToVoid, null, null);
        mv.visitCode();
        // Call through to static run method in this component.
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, packageAndClassName, "run",
                           NamingCzar.voidToFortressVoid);
        // Discard the FVoid that results
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }

    private void generateFieldsAndInitMethod(String classFile, String superClass, List<Param> params) {
        // Allocate fields
        for (Param p : params) {
            // TODO need to spot for "final" fields.  Right now we assume final.
            String pn = p.getName().getText();
            Type pt = p.getIdType().unwrap();
            cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, pn,
                    NamingCzar.jvmTypeDesc(pt, thisApi(), true), null /* for non-generic */, null /* instance has no value */);
        }

        String init_sig = NamingCzar.jvmSignatureFor(params, "V", thisApi());
        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC, "<init>", init_sig, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClass, "<init>", NamingCzar.voidToVoid);

        // Initialize fields.
        int pno = 1;
        for (Param p : params) {
            String pn = p.getName().getText();
            Type pt = p.getIdType().unwrap();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, pno);
            mv.visitFieldInsn(Opcodes.PUTFIELD, classFile, pn,
                    NamingCzar.jvmTypeDesc(pt, thisApi(), true));
            pno++;
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }


    private void cgWithNestedScope(ASTNode n) {
        CodeGen cg = new CodeGen(this);
        n.accept(cg);
    }

    private void addLocalVar( VarCodeGen v ) {
        debug("addLocalVar ", v);
        lexEnv.put(v.name.getText(), v);
    }

    private void addStaticVar( VarCodeGen v ) {
        debug("addStaticVar ", v);
        lexEnv.put(v.name.getText(), v);
    }

    private VarCodeGen addParam(Param p) {
        VarCodeGen v =
            new VarCodeGen.ParamVar(p.getName(), p.getIdType().unwrap(), this);
        addLocalVar(v);
        return v;
    }

    private VarCodeGen addSelf() {
        Id tid = (Id)currentTraitObjectDecl.getHeader().getName();
        Id id = NodeFactory.makeId(NodeUtil.getSpan(tid), "self");
        Type t = NodeFactory.makeTraitType(tid);
        VarCodeGen v = new VarCodeGen.ParamVar(id, t, this);
        addLocalVar(v);
        return v;
    }

    // Always needs context-sensitive null handling anyway.  TO FIX.
    // private VarCodeGen getLocalVar( ASTNode ctxt, IdOrOp nm ) {
    //     VarCodeGen r = getLocalVarOrNull(nm);
    //     if (r==null) return sayWhat(ctxt, "Can't find lexEnv mapping for local var");
    //     return r;
    // }

    private VarCodeGen getLocalVarOrNull( IdOrOp nm ) {
        debug("getLocalVar: ", nm);
        VarCodeGen r = lexEnv.get(NamingCzar.idOrOpToString(nm));
        if (r != null)
            debug("getLocalVar:", nm, " VarCodeGen = ", r, " of class ", r.getClass());
        else
            debug("getLocalVar:", nm, " VarCodeGen = null");
        return r;
    }

    public void dumpClass( String unmangled_file_name ) {
        PrintWriter pw = new PrintWriter(System.out);
        cw.visitEnd();

        String file = Naming.mangleFortressIdentifier(unmangled_file_name);

        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false))
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), true, pw);

        ByteCodeWriter.writeJarredClass(jos, file, cw.toByteArray());
        debug( "Writing class ", file);
    }

    private void popAll(int onStack) {
        if (onStack == 0) return;
        for (; onStack > 1; onStack -= 2) {
            mv.visitInsn(Opcodes.POP2);
        }
        if (onStack==1) {
            mv.visitInsn(Opcodes.POP);
        }
    }

    private void dumpTraitDecls(List<Decl> decls) {
        debug("dumpDecls", decls);
        for (Decl d : decls) {
            if (!(d instanceof FnDecl)) {
                sayWhat(d);
                return;
            }
            d.accept(this);
        }
    }


    private void addLineNumberInfo(ASTNode x) {
        addLineNumberInfo(mv, x);
    }

    private void addLineNumberInfo(CodeGenMethodVisitor m, ASTNode x) {
        org.objectweb.asm.Label bogus_label = new org.objectweb.asm.Label();
        m.visitLabel(bogus_label);
        Span span = NodeUtil.getSpan(x);
        SourceLoc begin = span.getBegin();
        SourceLoc end = span.getEnd();
        String fileName = span.getFileName();
        m.visitLineNumber(begin.getLine(), bogus_label);
    }

    /**
     * @param x
     * @param arrow
     * @param pkgAndClassName
     * @param methodName
     */
    private void callStaticSingleOrOverloaded(FunctionalRef x,
            com.sun.fortress.nodes.Type arrow, String pkgAndClassName,
            String methodName) {

        debug("class = ", pkgAndClassName, " method = ", methodName );
        addLineNumberInfo(x);

        Pair<String, String> method_and_signature =
            resolveMethodAndSignature(x, arrow, methodName);

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, pkgAndClassName,
                method_and_signature.getA(), method_and_signature.getB());

    }

    /**
     * @param x
     * @param arrow
     * @param methodName
     * @return
     * @throws Error
     */
    private Pair<String, String> resolveMethodAndSignature(ASTNode x,
            com.sun.fortress.nodes.Type arrow, String methodName) throws Error {
        Pair<String, String> method_and_signature = null;
        String signature = null;

        if ( arrow instanceof ArrowType ) {
            // TODO should this be non-colliding single name instead?
            // answer depends upon how intersection types are normalized.
            // conservative answer is "no".
            // methodName = Naming.mangleIdentifier(methodName);
            signature = NamingCzar.jvmMethodDesc(arrow, component.getName());

        } else if (arrow instanceof IntersectionType) {
            IntersectionType it = (IntersectionType) arrow;
            methodName = OverloadSet.actuallyOverloaded(it, paramCount) ?
                    OverloadSet.oMangle(methodName) : methodName;

            signature = OverloadSet.getSignature(it, paramCount, ta);

        } else {
                sayWhat( x, "Neither arrow nor intersection type: " + arrow );
        }
        return new Pair<String,String>(methodName, signature);
    }

    // paramCount communicates this information from call to function reference,
    // as it's needed to determine type descriptors for methods.
    private int paramCount = -1;

    /**
     * @param y
     */
    private void pushInteger(int y) {
        switch (y) {
        case 0:
            mv.visitInsn(Opcodes.ICONST_0);
            break;
        case 1:
            mv.visitInsn(Opcodes.ICONST_1);
            break;
        case 2:
            mv.visitInsn(Opcodes.ICONST_2);
            break;
        case 3:
            mv.visitInsn(Opcodes.ICONST_3);
            break;
        case 4:
            mv.visitInsn(Opcodes.ICONST_4);
            break;
        case 5:
            mv.visitInsn(Opcodes.ICONST_5);
            break;
        default:
            mv.visitLdcInsn(y);
            break;
        }

    }

    private void allSayWhats() {
        return; // This is a great place for a breakpoint!
    }

    private <T> T sayWhat(ASTNode x) {
        allSayWhats();
        throw new CompilerError(x, "Can't compile " + x);
    }

    private <T> T sayWhat(Node x) {
        if (x instanceof ASTNode)
            sayWhat((ASTNode) x);
        allSayWhats();
        throw new CompilerError("Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
        allSayWhats();
        throw new CompilerError(x, message + " node = " + x);
    }

    private void debug(Object... message){
        Debug.debug(Debug.Type.CODEGEN,1,message);
    }

    private void doStatements(List<Expr> stmts) {
        int onStack = 0;
        if (stmts.isEmpty()) {
            pushVoid();
            return;
        }
        for ( Expr e : stmts ) {
            popAll(onStack);
            e.accept(this);
            onStack = 1;
            // TODO: can we have multiple values on stack in future?
            // Whither primitive types?
            // May require some tracking of abstract stack state.
            // For now we always have 1 pointer on stack and this doesn't
            // matter.
        }
    }

    public void defaultCase(Node x) {
        System.out.println("defaultCase: " + x + " of class " + x.getClass());
        sayWhat(x);
    }

    public void forImportStar(ImportStar x) {
        // do nothing, don't think there is any code go generate
    }

    public void forBlock(Block x) {
        if (x.isAtomicBlock()) {
            sayWhat(x, "Can't generate code for atomic block yet.");
        }
        boolean oldInABlock = inABlock;
        inABlock = true;
        debug("forBlock ", x);
        doStatements(x.getExprs());
        inABlock=oldInABlock;
    }

    public void forChainExpr(ChainExpr x) {
        debug( "forChainExpr", x);
        Expr first = x.getFirst();
        List<Link> links = x.getLinks();
        debug( "forChainExpr", x, " about to call accept on ",
               first, " of class ", first.getClass());
        first.accept(this);
        Iterator<Link> i = links.iterator();
        if (links.size() != 1)
            throw new CompilerError(x, x + "links.size != 1");
        Link link = i.next();
        link.getExpr().accept(this);
        debug( "forChainExpr", x, " about to call accept on ",
               link.getOp(), " of class ", link.getOp().getClass());
        int savedParamCount = paramCount;
        try {
            // TODO is this the general formula?
            paramCount = links.size() + 1;
            link.getOp().accept(this);
        } finally {
            paramCount = savedParamCount;
        }

        debug( "We've got a link ", link, " an op ", link.getOp(),
               " and an expr ", link.getExpr(), " What do we do now");
    }

    public void forComponent(Component x) {
        debug("forComponent ",x.getName(),NodeUtil.getSpan(x));
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        boolean exportsExecutable = false;
        boolean exportsDefaultLibrary = false;

        for ( APIName export : x.getExports() ) {
            if ( WellKnownNames.exportsMain(export.getText()) )
                exportsExecutable = true;
            if ( WellKnownNames.exportsDefaultLibrary(export.getText()) )
                exportsDefaultLibrary = true;
        }

        String extendedJavaClass =
            exportsExecutable ? NamingCzar.fortressExecutable :
                                NamingCzar.fortressComponent ;

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 packageAndClassName, null, extendedJavaClass,
                 null);

        for ( Import i : x.getImports() ) {
            i.accept(this);
        }

        // Always generate the init method
        generateFieldsAndInitMethod(packageAndClassName, extendedJavaClass, Collections.<Param>emptyList());

        // If this component exports an executable API,
        // generate a main method.
        if ( exportsExecutable ) {
            generateMainMethod();
        }
        // determineOverloadedNames(x.getDecls() );

        // Must do this first, to get local decls right.
        overloadedNamesAndSigs = generateTopLevelOverloads(thisApi(), topLevelOverloads, ta, cw, this);

        /* Need wrappers for the API, too. */
        generateUnambiguousWrappersForApi();

        // Must process top-level values next to make sure fields end up in scope.
        for (Decl d : x.getDecls()) {
            if (d instanceof ObjectDecl) {
                this.forObjectDeclPrePass((ObjectDecl) d);
            } else if (d instanceof VarDecl) {
                this.forVarDeclPrePass((VarDecl)d);
            }
        }

        // Static initializer for this class.
        // Since all top-level fields and singleton objects are singleton inner classes,
        // this does nothing.
        mv = cw.visitCGMethod(Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();

        for ( Decl d : x.getDecls() ) {
            d.accept(this);
        }

        dumpClass( packageAndClassName );

        try {
            jos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void forDecl(Decl x) {
        sayWhat(x, "Can't handle decl class "+x.getClass().getName());
    }


    public void forDo(Do x) {
        // TODO: these ought to occur in parallel!
        debug("forDo ", x);
        int onStack = 0;
        for ( Block b : x.getFronts() ) {
            popAll(onStack);
            b.accept(this);
            onStack = 1;
        }
    }

    // TODO: arbitrary-precision version of FloatLiteralExpr, correct
    // handling of types other than double (float should probably just
    // truncate, but we want to warn if we lose bits I expect).
    public void forFloatLiteralExpr(FloatLiteralExpr x) {
        debug("forFloatLiteral ", x);
        double val = x.getIntPart().doubleValue() +
            (x.getNumerator().doubleValue() /
             Math.pow(x.getDenomBase(), x.getDenomPower()));
        mv.visitLdcInsn(val);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           NamingCzar.internalFortressRR64, NamingCzar.make,
                           NamingCzar.makeMethodDesc(NamingCzar.descDouble,
                                                     NamingCzar.descFortressRR64));
    }

    private void generateGenericMethodClass(FnDecl x, IdOrOp name,
                                            int selfIndex) {
        /*
         * Different plan for static parameter decls;
         * instead of a method name, we are looking for an
         * inner class name, similar to how these are constructed
         * for traits.
         *
         * The inner class name has the form
         *
         * PKG.componentGEAR$function[\t1;t2;n3;o4\]ENVELOPEarrow[\d1;d2;r\]
         *
         * where
         * PKG is package name
         * component is component name
         * GEAR is Unicode "GEAR"
         * function is function name
         * t1, t2, n3, o4 encode static parameter kinds
         * ENVELOPE is unicode Envelope (just like a closure)
         * arrow is "Arrow", the stem on a generic arrow type
         * d1, d2, r are the type parameters of the arrow type.
         *
         * These classes will have all the attributes required of a
         * closure class, except that the static parameters will be
         * dummies to be replaced at instantiation time.
         */

        /*
         * Need to modify the
         * signature, depending on
         * circumstances.
         */

        Map<String, String> xlation = new HashMap<String, String>();
        String sparams_part = genericDecoration(x, xlation);

        FnDecl y = x;
        x = (FnDecl) x.accept(new GenericNumberer(xlation));

        // Get rewritten parts.
        FnHeader header = x.getHeader();
        List<Param> params = header.getParams();
        Type returnType = header.getReturnType().unwrap();
        Expr body = x.getBody().unwrap();

        String sig =
            NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                                       returnType, component.getName());

        ArrowType at = fndeclToType(x);
        String arrow_type = NamingCzar.jvmTypeDesc(at, thisApi(), false);
        String mname;

        // TODO different collision rules for top-level and for
        // methods. (choice of mname)

        if (selfIndex != NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex);
            mname = fmDottedName(singleName(name), selfIndex);
        } else {
            mname = nonCollidingSingleName(name, sig);
        }

        String PCN = packageAndClassName + Naming.GEAR +"$" +
                        mname + sparams_part + Naming.ENVELOPE + "$" + arrow_type;

        System.err.println(PCN);

        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);

        String staticClass = PCN.replaceAll("[.]", "/");
        // This creates the closure bits
        InstantiatingClassloader.closureClassPrefix(PCN, cg.cw, staticClass);

        // Code below cribbed from top-level/functional/ordinary method
        int modifiers = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC ;

        cg.generateActualMethodCode(modifiers, mname, sig, params, selfIndex,
                                    selfIndex != NO_SELF, body);

        cg.dumpClass(PCN);
    }

    private void generateTraitDefaultMethod(FnDecl x, IdOrOp name,
                                            List<Param> params,
                                            int selfIndex,
                                            Type returnType,
                                            boolean inAMethod,
                                            Expr body) {
        int modifiers = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;

        Type traitType = NodeFactory
            .makeTraitType((Id) currentTraitObjectDecl
                           .getHeader().getName());

        /* Signature includes explicit leading self
           First version of sig includes duplicate self for
           functional methods, which is then cut out.
        */
        String sig = NamingCzar.jvmSignatureFor(
                         NodeUtil.getParamType(x),
                         NamingCzar.jvmTypeDesc(returnType,
                                                component.getName()),
                         0,
                         traitType,
                         component.getName());

        // TODO different collision rules for top-level and for
        // methods.
        String mname;
        int n = params.size();
        if (selfIndex != NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex+1);
            mname = fmDottedName(singleName(name), selfIndex);
        } else {
            mname = nonCollidingSingleName(name, sig);
            n++;
        }

        CodeGen cg = new CodeGen(this);
        cg.generateActualMethodCode(modifiers, mname, sig, params, selfIndex,
                                    true, body);

        /*
         * Next emit an abstract redirecter, this makes life better
         * for our primitive type story.
         */

        // Dotted method; downcast self and
        // forward to static method in springboard class
        // with explicit self parameter.
        InstantiatingClassloader.forwardingMethod(cw, mname, ACC_PUBLIC, 0,
                                                  springBoardClass, mname, INVOKESTATIC,
                                                  sig, n, true);
    }

    private void generateFunctionalBody(FnDecl x, IdOrOp name,
                                        List<Param> params,
                                        int selfIndex,
                                        Type returnType,
                                        boolean inAMethod,
                                        boolean savedInAnObject,
                                        Expr body) {
        /* options here:
         *  - functional method in object
         *  - normal method in object
         *  - top level
         */
        int modifiers = Opcodes.ACC_PUBLIC;

        /*
         * Need to modify the
         * signature, depending on
         * circumstances.
         */
        String sig = NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                                                returnType, component.getName());

        String mname;

        // TODO different collision rules for top-level and for
        // methods. (choice of mname)

        if (selfIndex != NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex);
            mname = fmDottedName(singleName(name), selfIndex);
        } else {
            mname = nonCollidingSingleName(name, sig);
        }

        if (!savedInAnObject) {
            // trait default OR top level.
            // DO NOT special case run() here and make it non-static
            // (that used to happen), as that's wrong. It's
            // addressed in the executable wrapper code instead.
            modifiers |= Opcodes.ACC_STATIC;
        }

        CodeGen cg = new CodeGen(this);
        cg.generateActualMethodCode(modifiers, mname, sig, params, selfIndex,
                                    inAMethod, body);

        generateAllWrappersForFn(x, params, sig, modifiers, mname);
    }


    /**
     * @param x
     * @param params
     * @param selfIndex
     * @param sig
     * @param modifiers
     * @param mname
     */
    private void generateAllWrappersForFn(FnDecl x, List<Param> params,
            String sig, int modifiers,
            String mname) {
        CodeGen cg = new CodeGen(this);
        /* This code generates forwarding wrappers for
         * the (local) unambiguous name of the function.
         */

        // unambiguous within component
        String wname = NamingCzar.idOrOpToString(x.getUnambiguousName());
        cg.generateWrapperMethodCode(modifiers, mname, wname, sig, params);
    }


    /**
     * @param params
     * @param sig
     * @param modifiers
     * @param mname
     * @param cg
     * @param sf
     */
    private void generateUnambiguousWrappersForApi() {

        for (Map.Entry<String, Set<Function>> entry : exportedToUnambiguous
                .entrySet()) {
            Set<Function> sf = entry.getValue();
            for (Function function : sf) {
                List<Param> params = function.parameters();

                String sig = NamingCzar.jvmSignatureFor(
                        NodeUtil.getParamType(params, function.getSpan()),
                        function.getReturnType().unwrap(),
                        component.getName());

                String mname = NamingCzar.idOrOpToString(function.name()); // entry.getKey();

                String function_ua_name = NamingCzar.idOrOpToString(function.unambiguousName());
                generateWrapperMethodCode(
                        Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, mname,
                        function_ua_name, sig, params);
            }
        }
    }

    /** Generate an actual Java method and body code from an Expr.
     *  Should be done within a nested codegen as follows:
     *  new CodeGen(this).generateActualMethodCode(...);
     */
    private void generateWrapperMethodCode(int modifiers, String mname, String wname, String sig,
                                          List<Param> params) {

        // ignore virtual, now.
        if (0 == (Opcodes.ACC_STATIC & modifiers))
            return;

        mv = cw.visitCGMethod(modifiers, wname, sig, null, null);
        mv.visitCode();

        // Need to copy the parameter across for the wrapper call.
        // Modifiers should tell us how to do the call, maybe?
        // Invokestatic, for now.

        int i = 0;

        for (Param p : params) {
            // Type ty = p.getIdType().unwrap();
            mv.visitVarInsn(Opcodes.ALOAD, i);
            i++;
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, packageAndClassName, mname, sig);

        methodReturnAndFinish();
    }

    /** Generate an actual Java method and body code from an Expr.
     *  Should be done within a nested codegen as follows:
     *  new CodeGen(this).generateActualMethodCode(...);
     */
    private void generateActualMethodCode(int modifiers, String mname, String sig,
                                          List<Param> params, int selfIndex,
                                          boolean inAMethod, Expr body) {

        mv = cw.visitCGMethod(modifiers, mname, sig, null, null);
        mv.visitCode();

        // Now inside method body. Generate code for the method
        // body. Start by binding the parameters and setting up the
        // initial locals.
        VarCodeGen selfVar = null;
        if (inAMethod) {
            selfVar = addSelf();
        }
        List<VarCodeGen> paramsGen = new ArrayList<VarCodeGen>(params.size());
        int index = 0;
        for (Param p : params) {
            if (index != selfIndex) {
                VarCodeGen v = addParam(p);
                paramsGen.add(v);
            }
            index++;
        }
        // Compile the body in the parameter environment

        body.accept(this);
        try {
            exitMethodScope(selfIndex, selfVar, paramsGen);
        } catch (Throwable t) {
            throw new Error("\n"+NodeUtil.getSpan(body)+": Error trying to close method scope.",t);
        }
    }

    private static final Modifiers fnDeclCompilableModifiers =
        Modifiers.GetterSetter.combine(Modifiers.IO).combine(
            Modifiers.Private).combine(Modifiers.Abstract);

    public void forFnDecl(FnDecl x) {
        /*
         * Cases for FnDecl:
         *
         * 1. top level
         *
         * 2. trait normal method. a. for trait itself, an abstract method in
         * generated interface (this may be handled elsewhere) b. for trait
         * defaults, a static method in SpringBoard
         *
         * 3. trait functional method a. for trait itself, a mangled-name
         * abstract method with self removed from the parameter list. (this may
         * be handled elsewhere) b. at top level, a functional wrapper with self
         * in original position, which invokes the interface method with self in
         * dotted position. NOTE THE POTENTIAL FOR OVERLOADING. c. for trait
         * defaults, a mangled-name static method with self in the first
         * parameter position (in SpringBoard).
         *
         * 4. object normal method a. a normal dotted method is generated
         *
         * 5. object functional method a. a mangled-name dotted method is
         * generated with self removed from the parameter list. b. at top level,
         * a functional wrapper with self in original position, which invokes
         * the interface method with self in dotted position. NOTE THE POTENTIAL
         * FOR OVERLOADING. Static overload resolution can be an optimization.
         */

        debug("forFnDecl ", x);
        FnHeader header = x.getHeader();

        List<Param> params = header.getParams();
        int selfIndex = selfParameterIndex(params);

        IdOrOpOrAnonymousName name = header.getName();

        if (emittingFunctionalMethodWrappers) {
            if (selfIndex==NO_SELF)
                return; // Not functional = no wrapper needed.
        }

        Option<com.sun.fortress.nodes.Type> optReturnType = header.getReturnType();

        if (optReturnType.isNone())
            sayWhat(x, "Return type is not inferred.");

        com.sun.fortress.nodes.Type returnType = optReturnType.unwrap();

        if (name instanceof Id) {
            Id id = (Id) name;
            debug("forId ", id, " class = ", NamingCzar.jvmClassForSymbol(id));
        } else if (name instanceof Op) {
            Op op = (Op) name;
            Fixity fixity = op.getFixity();
            boolean isEnclosing = op.isEnclosing();
            Option<APIName> maybe_apiName = op.getApiName();
            debug("forOp ", op, " fixity = ", fixity, " isEnclosing = ",
                    isEnclosing, " class = ", NamingCzar.jvmClassForSymbol(op));
        } else {
            sayWhat(x, "Unhandled function name.");
        }

        List<StaticParam> sparams = header.getStaticParams();

        boolean canCompile =
            (sparams.isEmpty() || // no static parameter
             !(inAnObject || inATrait || emittingFunctionalMethodWrappers)) &&
        header.getWhereClause().isNone() && // no where clause
        header.getThrowsClause().isNone() && // no throws clause
        header.getContract().isNone() && // no contract
            header.getMods().remove(fnDeclCompilableModifiers).isEmpty() && // no unhandled modifiers
        !inABlock; // no local functions

        if (!canCompile)
            sayWhat(x, "Don't know how to compile this kind of FnDecl.");

        Option<Expr> optBody = x.getBody();
        if (optBody.isNone()) {
            if (inATrait) return; // Nothing concrete to do; dumpSigs already generated abstract signature.
            sayWhat(x, "Abstract function declarations are only supported in traits.");
        }
        Expr body = optBody.unwrap();

        boolean inAMethod = inAnObject || inATrait;
        boolean savedInAnObject = inAnObject;
        boolean savedInATrait = inATrait;
        boolean savedEmittingFunctionalMethodWrappers = emittingFunctionalMethodWrappers;

        try {
            inAnObject = false;
            inATrait = false;

            // For now every Fortress entity is made public, with
            // namespace management happening in Fortress-land. Right?
            // [JWM:] we'll want to clamp down on this long-term, but
            // we have to get nesting right---we generate a pile of
            // class files for one Fortress component

            if (! sparams.isEmpty()) {
                generateGenericMethodClass(x, (IdOrOp)name,
                                           selfIndex);
            } else if (emittingFunctionalMethodWrappers) {
                functionalMethodWrapper(x, (IdOrOp)name,
                                        params, selfIndex, returnType, savedInATrait);
            } else if (savedInATrait) {
                generateTraitDefaultMethod(x, (IdOrOp)name,
                                           params, selfIndex, returnType, inAMethod, body);
            } else {
                generateFunctionalBody(x, (IdOrOp)name,
                                       params, selfIndex, returnType, inAMethod,
                                       savedInAnObject, body);
            }

        } finally {
            inAnObject = savedInAnObject;
            inATrait = savedInATrait;
            emittingFunctionalMethodWrappers = savedEmittingFunctionalMethodWrappers;
        }

    }

    private ArrowType fndeclToType(FnDecl x) {
        FnHeader fh = x.getHeader();
        Type rt = fh.getReturnType().unwrap();
        List<Param> lp = fh.getParams();
        Type dt = null;
        switch (lp.size()) {
        case 0:
            dt = NodeFactory.makeVoidType(x.getInfo().getSpan());
            break;
        case 1:
            dt = lp.get(0).getIdType().unwrap(); // TODO varargs
            break;
        default:
            dt = NodeFactory.makeTupleType(Useful.applyToAll(lp, new Fn<Param,Type>() {
                @Override
                public Type apply(Param x) {
                    return x.getIdType().unwrap(); // TODO varargs
                }}));
            break;
        }
        return NodeFactory.makeArrowType(NodeFactory.makeSpan(dt,rt), dt, rt);
    }

    NodeAbstractVisitor<String> spkTagger = new NodeAbstractVisitor<String> () {

        @Override
        public String forKindBool(KindBool that) {
            return Naming.BALLOT_BOX_WITH_CHECK;
        }

        @Override
        public String forKindDim(KindDim that) {
            return Naming.SCALES;
        }

        @Override
        public String forKindInt(KindInt that) {
            return Naming.MUSIC_SHARP;
        }

        @Override
        public String forKindNat(KindNat that) {
            // nats and ints go with same encoding; no distinction in args
            return Naming.MUSIC_SHARP;
        }

        @Override
        public String forKindOp(KindOp that) {
            return Naming.HAMMER_AND_PICK;
        }

        @Override
        public String forKindType(KindType that) {
            return Naming.YINYANG;
        }

        @Override
        public String forKindUnit(KindUnit that) {
            return Naming.ATOM;
        }

        @Override
        public String forBoolBase(BoolBase b) {
            return b.isBoolVal() ? "T" : "F";
        }

        @Override
        public String forBoolRef(BoolRef b) {
            return b.getName().getText();
        }

        @Override
        public String forBoolBinaryOp(BoolBinaryOp b) {
            BoolExpr l = b.getLeft();
            BoolExpr r = b.getRight();
            Op op = b.getOp();
            return l.accept(this) + Naming.ENTER + r.accept(this) + Naming.ENTER + op.getText();
        }

        @Override
        public String forBoolUnaryOp(BoolUnaryOp b) {
            BoolExpr v = b.getBoolVal();
            Op op = b.getOp();
            return v.accept(this) + Naming.ENTER + op.getText();
        }

        /* These need to return encodings of Fortress types. */
        @Override
        public String forBoolArg(BoolArg that) {
            BoolExpr arg = that.getBoolArg();

            return Naming.BALLOT_BOX_WITH_CHECK + arg.accept(this);
        }

        @Override
        public String forDimArg(DimArg that) {
            DimExpr arg = that.getDimArg();
            return Naming.SCALES;
        }

        @Override
        public String forIntBase(IntBase b) {
            return String.valueOf(b.getIntVal());
        }

        @Override
        public String forIntRef(IntRef b) {
            return b.getName().getText();
        }

        @Override
        public String forIntBinaryOp(IntBinaryOp b) {
            IntExpr l = b.getLeft();
            IntExpr r = b.getRight();
            Op op = b.getOp();
            return l.accept(this) + Naming.ENTER + r.accept(this) + Naming.ENTER + op.getText();
        }

       @Override
        public String forIntArg(IntArg that) {
            IntExpr arg = that.getIntVal();
            return Naming.MUSIC_SHARP + arg.accept(this);
        }

        @Override
        public String forOpArg(OpArg that) {
            FunctionalRef arg = that.getName();
            // TODO what about static args here?
            IdOrOp name = arg.getNames().get(0);
            return Naming.HAMMER_AND_PICK + name.getText();
        }

        @Override
        public String forTypeArg(TypeArg that) {
            Type arg = that.getTypeArg();
            // Pretagged with type information
            String s =  NamingCzar.makeArrowDescriptor(arg, thisApi());
            return s;
        }

        @Override
        public String forUnitArg(UnitArg that) {
            UnitExpr arg = that.getUnitArg();
            return Naming.ATOM;
        }


    };

    private String genericDecoration(FnDecl x, Map<String, String> xlation) {
        List<StaticParam> sparams = x.getHeader().getStaticParams();
        return genericDecoration(sparams, xlation);
    }


    /**
     * @param xlation
     * @param sparams
     * @return
     */
    private String genericDecoration(List<StaticParam> sparams,
            Map<String, String> xlation
            ) {
        if (sparams.size() == 0)
            return "";

        String frag = Naming.LEFT_OXFORD;
        int index = 1;
        for (StaticParam sp : sparams) {
            StaticParamKind spk = sp.getKind();

            IdOrOp spn = sp.getName();
            String tag = spk.accept(spkTagger) + index;
            xlation.put(spn.getText(), tag);
            frag += tag + ";";
            index++;
        }
        // TODO Auto-generated method stub
        return Useful.substring(frag, 0, -1) + Naming.RIGHT_OXFORD;
    }

    private String genericDecoration(List<StaticArg> sargs) {
        // TODO we need to make the conventions for Arrows and other static types converge.
        if (sargs.size() == 0)
            return "";
        String frag = Naming.LEFT_OXFORD;
        int index = 1;
        for (StaticArg sp : sargs) {
            String tag = sp.accept(spkTagger);
            frag += tag;
            frag += ";";
            index++;
        }
        // TODO Why are we mangling this?
        // return Naming.mangleFortressIdentifier(Useful.substring(frag,0,-1) + Naming.RIGHT_OXFORD);
        return Useful.substring(frag,0,-1) + Naming.RIGHT_OXFORD;
    }

    /**
     * @param selfIndex
     * @param cg
     * @param selfVar
     * @param paramsGen
     */
    private void exitMethodScope(int selfIndex, VarCodeGen selfVar, List<VarCodeGen> paramsGen) {
        for (int i = paramsGen.size() - 1; i >= 0; i--) {
            if (i != selfIndex) {
                VarCodeGen v = paramsGen.get(i);
                v.outOfScope(mv);
            }
        }
        if (selfVar != null)
            selfVar.outOfScope(mv);

        methodReturnAndFinish();
    }

    /**
     * @param x
     * @param params
     * @param selfIndex
     * @param name
     * @param savedInATrait
     * @param returnType
     */
    private void functionalMethodWrapper(FnDecl x, IdOrOp name,
                                         List<Param> params,
                                         int selfIndex,
                                         com.sun.fortress.nodes.Type returnType,
                                         boolean savedInATrait) {
        int modifiers = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC;
        String sig =
            NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x), returnType,
                                       component.getName());

        // TODO different collision rules for top-level and for methods.
        String mname = nonCollidingSingleName(name, sig);
        String dottedName = fmDottedName(singleName(name), selfIndex);

        int invocation = savedInATrait ? INVOKEINTERFACE : INVOKEVIRTUAL;

        InstantiatingClassloader.forwardingMethod(cw,
                         mname, modifiers, selfIndex,
                         traitOrObjectName, dottedName, invocation,
                         sig, params.size(), true);


        generateAllWrappersForFn(x, params, sig, modifiers, mname);

    }

    /**
     * @param cg
     */

    private void methodReturnAndFinish() {
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }

    /**
     * TODO: surely this is derivable from the arrow type, which maintains the selfParameterIndex?
     * Are those inconsistent with one another?
     * @param params
     * @return
     */
    private int selfParameterIndex(List<Param> params) {
        int selfIndex = NO_SELF;
        int i = 0;
        for (Param p : params) {
            if (p.getName().getText() == "self") {
                selfIndex = i;
                break;
            }
            i++;
        }
        return selfIndex;
    }

    public void forFnExpr(FnExpr x) {
        debug("forFnExpr ", x);
        FnHeader header = x.getHeader();
        Expr body = x.getBody();
        List<Param> params = header.getParams();
        Option<Type> returnType = header.getReturnType();
        if (!returnType.isSome())
            throw new CompilerError(x, "No return type");
        Type rt = returnType.unwrap();


        //      Create the Class
        String desc = NamingCzar.makeAbstractArrowDescriptor(params, rt, thisApi());
        String idesc = NamingCzar.makeArrowDescriptor(params, rt, thisApi());
        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cg.cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        String className = NamingCzar.gensymArrowClassName(Naming.deDot(thisApi().getText()));

        debug("forFnExpr className = ", className, " desc = ", desc);
        List<VarCodeGen> freeVars = getFreeVars(body);
        cg.lexEnv = cg.createTaskLexEnvVariables(className, freeVars);
        cg.cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                    className, null, desc, new String[] {idesc});

        // Generate the constructor (initializes captured free vars from param list)
        String init = taskConstructorDesc(freeVars);
        cg.generateTaskInit(desc, init, freeVars);

        String applyDesc = NamingCzar.jvmSignatureFor(params, NamingCzar.jvmTypeDesc(rt, thisApi()),
                                                      thisApi());

        // Generate the apply method
        // System.err.println(idesc+".apply"+applyDesc+" gen in "+className);
        cg.mv = cg.cw.visitCGMethod(Opcodes.ACC_PUBLIC, Naming.APPLY_METHOD, applyDesc, null, null);
        cg.mv.visitCode();

        // Since we call this virtually we need a slot for the arrow implementation of this object.
        cg.mv.reserveSlot0();
        for (Param p : params) {
            cg.addParam(p);
        }

        body.accept(cg);

        cg.methodReturnAndFinish();
        cg.dumpClass(className);

        constructWithFreeVars(className, freeVars, init);
    }


    /**
     * Creates a name that will not collide with any overloaded functions
     * (the overloaded name "wins" because if it is exported, this one is not).
     *
     * @param name
     * @param sig The jvm signature for a method, e.g., (ILjava/lang/Object;)D
     * @return
     */
    private String nonCollidingSingleName(IdOrOpOrAnonymousName name, String sig) {
        String mname = singleName(name);
        if (overloadedNamesAndSigs.contains(mname+sig)) {
            mname = NamingCzar.mangleAwayFromOverload(mname);
        }
        return mname;
    }

    /**
     * Method name, with symbolic-freedom-mangling applied
     *
     * @param name
     * @return
     */
    private String singleName(IdOrOpOrAnonymousName name) {
        String nameString = NamingCzar.idOrOpToString((IdOrOp)name);
        String mname = nameString; // Naming.mangleIdentifier(nameString);
        return mname;
    }

    // belongs in Naming perhaps
    private static String fmDottedName(String name, int selfIndex) {
        // HACK.  Need to be able to express some fmDotteds in Java source code
        // thus, must transmogrify everything that is not A-Z to something else.

//        if (!isBoring(name)) {
//            name = makeBoring(name);
//        }
        //
        name = name + Naming.INDEX + selfIndex;
        return name;
    }


    private static boolean isBoring(String name) {
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0 ? Character.isJavaIdentifierStart(ch) : Character.isJavaIdentifierPart(ch))
                continue;
            return false;
        }
        return true;
    }

    private static String makeBoring(String name) {
        StringBuffer b = new StringBuffer();

        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0 ? Character.isJavaIdentifierStart(ch) : Character.isJavaIdentifierPart(ch)) {
                b.append(ch);
            } else {
                b.append('x');
                b. append(Integer.toHexString(ch));
            }
        }
        return b.toString();
    }

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        debug("forFnRef ", x);
        if (fnRefIsApply) {
            forFunctionalRef(x);
            return;
        }

        // Not entirely sure about this next bit; how are function-valued parameters referenced?
        VarCodeGen fn = getLocalVarOrNull(x.getOriginalName());
        if (fn != null) {
            sayWhat(x, "Haven't figured out references to local/parameter functions yet");
            return;
        }

        // need to deal with generics.
        List<StaticArg> sargs = x.getStaticArgs();

        // Get it from top level.
        Pair<String, String> pc_and_m= functionalRefToPackageClassAndMethod(x);
        // If it's an overloaded type, oy.
        com.sun.fortress.nodes.Type arrow = exprType(x);
        // Capture the overloading foo, mutilate that into the name of the thing that we want.
        Pair<String, String> method_and_signature = resolveMethodAndSignature(
                x, arrow, pc_and_m.getB());
        /* we now have package+class, method name, and signature.
         * Emit a static reference to a field in package/class/method+envelope+mangled_sig.
         * Classloader will see this, and it will trigger demangling of the name, to figure
         * out the contents of the class to be loaded.
         */
        String arrow_desc = NamingCzar.jvmTypeDesc(arrow, thisApi(), true);
        String arrow_type = NamingCzar.jvmTypeDesc(arrow, thisApi(), false);
        String PCN = pc_and_m.getA() + "$" +

            method_and_signature.getA() +
            Naming.ENVELOPE + "$"+ // "ENVELOPE"
            arrow_type;
        /* The suffix will be
         * (potentially mangled)
         * functionName<ENVELOPE>closureType (which is an arrow)
         *
         * must generate code for the class with a method apply, that
         * INVOKE_STATICs prefix.functionName .
         */
        mv.visitFieldInsn(Opcodes.GETSTATIC, PCN, NamingCzar.closureFieldName, arrow_desc);
    }

    /**
     * @param x
     */
    public void forFunctionalRef(FunctionalRef x) {
        debug("forFunctionalRef ", x);

        List<StaticArg> sargs = x.getStaticArgs();

        String decoration = genericDecoration(sargs);

        /* Arrow, or perhaps an intersection if it is an overloaded function. */
        com.sun.fortress.nodes.Type arrow = exprType(x);

        debug("forFunctionalRef ", x, " arrow = ", arrow);

        Pair<String, String> calleeInfo = functionalRefToPackageClassAndMethod(x);

        String pkgClass = calleeInfo.getA();

        if (decoration.length() > 0) {
            /*
             * TODO, BUG, need to use arrow type of uninstantiated generic!
             * This is necessary because otherwise it is difficult (impossible?)
             * to figure out the name of the template class that will be
             * expanded later.
             */

            String arrow_type = NamingCzar.jvmTypeDesc(arrow, thisApi(), false);
            pkgClass = pkgClass + Naming.GEAR + "$" +
                    calleeInfo.getB() +
                    decoration +
                    Naming.ENVELOPE + "$" +
                    arrow_type // TODO fix this.
                    ;
        }

        callStaticSingleOrOverloaded(x, arrow, pkgClass, calleeInfo.getB());
    }

    /**
     * @param x
     * @return
     */
    private Pair<String, String> functionalRefToPackageClassAndMethod(
            FunctionalRef x) {
        List<IdOrOp> names = x.getNames();

        if ( names.size() != 1) {
            return sayWhat(x,"Non-unique overloading after rewrite " + x);
        }
        return idToPackageClassAndName(names.get(0));
    }

    private Pair<String, String> idToPackageClassAndName(IdOrOp fnName) {
        return NamingCzar.idToPackageClassAndName(fnName, thisApi());
    }
    public void forIf(If x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forIf ", x);
        List<IfClause> clauses = x.getClauses();
        Option<Block> elseClause = x.getElseClause();

        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        org.objectweb.asm.Label falseBranch = new org.objectweb.asm.Label();
        for (IfClause ifclause : clauses) {

            GeneratorClause cond = ifclause.getTestClause();

            if (!cond.getBind().isEmpty())
                sayWhat(x, "Undesugared generalized if expression.");

            // emit code for condition and to check resulting boolean
            Expr testExpr = cond.getInit();
            debug( "about to accept ", testExpr, " of class ", testExpr.getClass());
            testExpr.accept(this);
            addLineNumberInfo(x);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               NamingCzar.internalFortressBoolean, "getValue",
                               NamingCzar.makeMethodDesc("", NamingCzar.descBoolean));
            mv.visitJumpInsn(Opcodes.IFEQ, falseBranch);

            // emit code for condition true
            ifclause.getBody().accept(this);
            mv.visitJumpInsn(Opcodes.GOTO, done);

            // control goes to following label if condition false (and we continue tests)
            mv.visitLabel(falseBranch);
            falseBranch = new org.objectweb.asm.Label();
        }
        Option<Block> maybe_else = x.getElseClause();
        if (maybe_else.isSome()) {
            maybe_else.unwrap().accept(this);
        } else {
            pushVoid();
        }
        mv.visitLabel(done);
    }

    public void forImportNames(ImportNames x) {
        // No longer need to set up alias table; rely on ForeignJava exclusively instead.
    }

    public void forIntLiteralExpr(IntLiteralExpr x) {
        debug("forIntLiteral ", x);
        BigInteger bi = x.getIntVal();
        // This might not work.
        int l = bi.bitLength();
        if (l < 32) {
            int y = bi.intValue();
            addLineNumberInfo(x);
            pushInteger(y);
            addLineNumberInfo(x);

            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    NamingCzar.internalFortressZZ32, NamingCzar.make,
                    NamingCzar.makeMethodDesc(NamingCzar.descInt,
                                              NamingCzar.descFortressZZ32));
        } else if (l < 64) {
            long yy = bi.longValue();
            addLineNumberInfo(x);
            mv.visitLdcInsn(yy);
            addLineNumberInfo(x);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                    NamingCzar.internalFortressZZ64, NamingCzar.make,
                    NamingCzar.makeMethodDesc(NamingCzar.descLong,
                                              NamingCzar.descFortressZZ64));
        }
    }

    public void forLocalVarDecl(LocalVarDecl d) {
        debug("forLocalVarDecl", d);
        List<LValue> lhs = d.getLhs();
        if (!d.getRhs().isSome()) {
            // Just a forward declaration to be bound in subsequent
            // code.  But we need to leave a marker so that the
            // definitions down different control flow paths are
            // consistent; basically we need to create the definition
            // here, and the use that VarCodeGen object for the
            // subsequent true definitions.
            sayWhat(d, "Can't yet handle forward binding declarations.");
        }
        int n = lhs.size();
        List<VarCodeGen> vcgs = new ArrayList(n);
        for (LValue v : lhs) {
            if (v.isMutable()) {
                sayWhat(d, "Can't yet generate code for mutable variable declarations.");
            }
            if (!v.getIdType().isSome()) {
                sayWhat(d, "Variable being bound lacks type information!");
            }

            // Introduce variable
            Type ty = v.getIdType().unwrap();
            VarCodeGen vcg = new VarCodeGen.LocalVar(v.getName(), ty, this);
            vcgs.add(vcg);
        }

        // Compute rhs
        List<Expr> rhss;
        Expr rhs = d.getRhs().unwrap();
        if (n==1) {
            rhss = Collections.singletonList(rhs);
        } else if (rhs instanceof TupleExpr &&
                   !((TupleExpr)rhs).getVarargs().isSome() &&
                   ((TupleExpr)rhs).getKeywords().isEmpty() &&
                   ((TupleExpr)rhs).getExprs().size() == n) {
            rhss = ((TupleExpr)rhs).getExprs();
        } else {
            sayWhat(d, "Can't yet generate multiple-variable bindings unless rhs is a manifest tuple of the same size.");
            return;
        }

        if (false && pa.worthParallelizing(rhs)) {
            forExprsParallel(rhss, vcgs);
        } else {
            forExprsSerial(rhss,vcgs);
        }

        // Evaluate rest of block with binding in scope
        CodeGen cg = new CodeGen(this);
        for (VarCodeGen vcg : vcgs) {
            cg.addLocalVar(vcg);
        }

        cg.doStatements(d.getBody().getExprs());

        // Dispose of bindings now that we're done
        // Do this in reverse order of creation.
        for (int i = n-1; i >= 0; i--) {
            vcgs.get(i).outOfScope(mv);
        }
    }

    public void forObjectDecl(ObjectDecl x) {
        TraitTypeHeader header = x.getHeader();
        emittingFunctionalMethodWrappers = true;
        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
                                                         NamingCzar.idToString(NodeUtil.getName(x)));
        debug("forObjectDecl ",x," classFile = ", classFile);
        traitOrObjectName = classFile;
        dumpTraitDecls(header.getDecls());
        emittingFunctionalMethodWrappers = false;
        traitOrObjectName = null;
    }

    public void forObjectDeclPrePass(ObjectDecl x) {
        debug("forObjectDeclPrePass ", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();

        boolean canCompile =
            // x.getParams().isNone() &&             // no parameters
            // header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            //            header.getDecls().isEmpty() &&        // no members
            header.getMods().isEmpty()         // no modifiers
            // ( extendsC.size() <= 1 ); // 0 or 1 super trait
            ;

        if ( !canCompile ) sayWhat(x);

        Map<String, String> xlation = new HashMap<String, String>();
        String sparams_part = genericDecoration(header.getStaticParams(), xlation);

        // Rewrite the generic.
        if (sparams_part.length() > 0 ) {
            ObjectDecl y = x;
            x = (ObjectDecl) y.accept(new GenericNumberer(xlation));
        }
        
        boolean savedInAnObject = inAnObject;
        inAnObject = true;
        String [] superInterfaces =
            NamingCzar.extendsClauseToInterfaces(extendsC, component.getName());
        String abstractSuperclass;
        if (superInterfaces.length > 0) {
            abstractSuperclass = superInterfaces[0] + NamingCzar.springBoard;
        } else {
            abstractSuperclass = NamingCzar.internalObject;
        }
        Id classId = NodeUtil.getName(x);
        String classFile =
            NamingCzar.jvmClassForToplevelTypeDecl(classId,sparams_part,packageAndClassName);
        traitOrObjectName = classFile;
        String classDesc = NamingCzar.internalToDesc(classFile);
        debug("forObjectDeclPrePass ",x," classFile = ", classFile);


        boolean hasParameters = x.getParams().isSome();
        List<Param> params;
        if (hasParameters) {
            params = x.getParams().unwrap();
            String init_sig = NamingCzar.jvmSignatureFor(params, "V", thisApi());

             // Generate the factory method
            String sig = NamingCzar.jvmSignatureFor(params, classDesc, thisApi());

            String mname = nonCollidingSingleName(x.getHeader().getName(), sig);

            mv = cw.visitCGMethod(Opcodes.ACC_STATIC,
                    mname,
                    sig,
                    null,
                    null);

            mv.visitTypeInsn(Opcodes.NEW, classFile);
            mv.visitInsn(Opcodes.DUP);

            // iterate, pushing parameters, beginning at zero.
           // TODO actually handle N>0 parameters.

            int stack_offset = 0;
            for (Param p : params) {
                // when we unbox, this will be type-dependent
                mv.visitVarInsn(Opcodes.ALOAD, stack_offset);
                stack_offset++;
            }

            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classFile, "<init>", init_sig);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
        } else { // singleton
            params = Collections.<Param>emptyList();
        }

        CodeGenClassWriter prev = cw;

        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        // Until we resolve the directory hierarchy problem.
        //            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
        //                      classFile, null, NamingCzar.internalObject, new String[] { parent });
        cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL,
                  classFile, null, abstractSuperclass, superInterfaces);

        if (!hasParameters) {
            // Singleton; generate field in class to hold sole instance.
            cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
                          NamingCzar.SINGLETON_FIELD_NAME, classDesc,
                          null /* for non-generic */, null /* instance has no value */);
        }

        // Emit fields here, one per parameter.
        generateFieldsAndInitMethod(classFile, abstractSuperclass, params);

        if (!hasParameters) {
            MethodVisitor imv = cw.visitMethod(Opcodes.ACC_STATIC,
                                               "<clinit>",
                                               NamingCzar.voidToVoid,
                                               null,
                                               null);

            imv.visitTypeInsn(Opcodes.NEW, classFile);
            imv.visitInsn(Opcodes.DUP);
            imv.visitMethodInsn(Opcodes.INVOKESPECIAL, classFile,
                                "<init>", NamingCzar.voidToVoid);
            imv.visitFieldInsn(Opcodes.PUTSTATIC, classFile,
                               NamingCzar.SINGLETON_FIELD_NAME, classDesc);
            imv.visitInsn(Opcodes.RETURN);
            imv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            imv.visitEnd();

            addStaticVar(new VarCodeGen.StaticBinding(
                                 classId, NodeFactory.makeTraitType(classId),
                                 classFile,
                                 NamingCzar.SINGLETON_FIELD_NAME, classDesc));
        }

        BATree<String, VarCodeGen> savedLexEnv = lexEnv.copy();

        // need to add locals to the environment.
        // each one has name, mangled with a preceding "$"
        for (Param p : params) {
            Type param_type = p.getIdType().unwrap();
            String objectFieldName = p.getName().getText();
            Id id =
               NodeFactory.makeId(NodeUtil.getSpan(p.getName()), objectFieldName);
            addStaticVar(new VarCodeGen.FieldVar(id,
                    param_type,
                    classFile,
                    objectFieldName,
                    NamingCzar.jvmTypeDesc(param_type, component.getName(), true)));
        }

        currentTraitObjectDecl = x;
        for (Decl d : header.getDecls()) {
            // This does not work yet.
            d.accept(this);
        }

        lexEnv = savedLexEnv;
        dumpClass( classFile );
        cw = prev;
        inAnObject = savedInAnObject;
        traitOrObjectName = null;
    }

    // This returns a list rather than a set because the order matters;
    // we should guarantee that we choose a consistent order every time.
    private List<VarCodeGen> getFreeVars(Node n) {
        BASet<IdOrOp> allFvs = fv.freeVars(n);
        List<VarCodeGen> vcgs = new ArrayList<VarCodeGen>();
        if (allFvs == null) sayWhat((ASTNode)n," null free variable information!");
        for (IdOrOp v : allFvs) {
            VarCodeGen vcg = getLocalVarOrNull(v);
            if (vcg != null) vcgs.add(vcg);
        }
        return vcgs;
    }

    private BATree<String, VarCodeGen>
            createTaskLexEnvVariables(String taskClass, List<VarCodeGen> freeVars) {

        BATree<String, VarCodeGen> result =
            new BATree<String, VarCodeGen>(StringHashComparer.V);
        for (VarCodeGen v : freeVars) {
            String name = v.name.getText();
            cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, name,
                          NamingCzar.jvmTypeDesc(v.fortressType, thisApi()),
                          null, null);
            result.put(name, new TaskVarCodeGen(v, taskClass, thisApi()));
        }
        return result;
    }

    private void generateTaskInit(String baseClass,
                                  String initDesc,
                                  List<VarCodeGen> freeVars) {

        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC, "<init>", initDesc, null, null);
        mv.visitCode();

        // Call superclass constructor
        mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, baseClass,
                              "<init>", NamingCzar.voidToVoid);
        // mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());

        // Stash away free variables Warning: freeVars contains
        // VarCodeGen objects from the parent context, we must look
        // these up again in the child context or we'll get incorrect
        // code (or more usually the compiler will complain).
        int varIndex = 1;
        for (VarCodeGen v0 : freeVars) {
            VarCodeGen v = lexEnv.get(v0.name.getText());
            v.prepareAssignValue(mv);
            mv.visitVarInsn(Opcodes.ALOAD, varIndex++);
            v.assignValue(mv);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }

    private void generateTaskCompute(String className, Expr x, String result) {
        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                                  "compute", "()V", null, null);
        mv.visitCode();

        mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());

        x.accept(this);

        mv.visitFieldInsn(Opcodes.PUTFIELD, className, "result", result);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }

    // I'm just a stub.  Someday I'll have a body that updates the changed local variables.
    private BATree<String, VarCodeGen> restoreFromTaskLexEnv(BATree<String,VarCodeGen> old, BATree<String,VarCodeGen> task) {
        return task;
    }

    public String taskConstructorDesc(List<VarCodeGen> freeVars) {
        // And their types
        List<Type> freeVarTypes = new ArrayList(freeVars.size());
        for (VarCodeGen v : freeVars) {
            freeVarTypes.add(v.fortressType);
        }
        return NamingCzar.jvmTypeDescForGeneratedTaskInit(freeVarTypes, component.getName());
    }

    // This sets up the parallel task construct.
    // Caveat: We create separate taskClasses for every task
    public String delegate(Expr x, String result, String init, List<VarCodeGen> freeVars) {

        String className = NamingCzar.gensymTaskName(packageAndClassName);

        debug("delegate creating class ", className, " node = ", x,
              " constructor type = ", init, " result type = ", result);

        // Create a new environment, and codegen task class in it.
        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cg.cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        cg.lexEnv = cg.createTaskLexEnvVariables(className, freeVars);
        // WARNING: result may need mangling / NamingCzar-ing.
        cg.cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL, "result", result, null, null);

        cg.cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL,
                    className, null, NamingCzar.fortressBaseTask, null);

        cg.generateTaskInit(NamingCzar.fortressBaseTask, init, freeVars);

        cg.generateTaskCompute(className, x, result);

        cg.dumpClass(className);

        this.lexEnv = restoreFromTaskLexEnv(cg.lexEnv, this.lexEnv);
        return className;
    }

    public void constructWithFreeVars(String cname, List<VarCodeGen> freeVars, String sig) {
            mv.visitTypeInsn(Opcodes.NEW, cname);
            mv.visitInsn(Opcodes.DUP);
            // Push the free variables in order.
            for (VarCodeGen v : freeVars) {
                v.pushValue(mv);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, cname, "<init>", sig);
    }

    // Evaluate args in parallel.  (Why is profitability given at a
    // level where we can't ask the qustion here?)
    // Leave the results (in the order given) on the stack when vcgs==null;
    // otherwise use the provided vcgs to bind corresponding values.
    public void forExprsParallel(List<? extends Expr> args, List<VarCodeGen> vcgs) {
        final int n = args.size();
        if (n <= 0) return;
        String [] tasks = new String[n];
        String [] results = new String[n];
        int [] taskVars = new int[n];

        if (vcgs != null && vcgs.size() != n) {
            sayWhat(args.get(0), "Internal error: number of args does not match number of consumers.");
        }

        // Push arg tasks from right to left, so
        // that local evaluation of args will proceed left to right.
        // IMPORTANT: ALWAYS fork and join stack fashion,
        // ie always join with the most recent fork first.
        for (int i = n-1; i > 0; i--) {
            Expr arg = args.get(i);
            // Make sure arg has type info (we'll need it to generate task)
            Option<Type> ot = NodeUtil.getExprType(arg);
            if (!ot.isSome())
                sayWhat(arg, "Missing type information for argument " + arg);
            Type t = ot.unwrap();
            String tDesc = NamingCzar.jvmTypeDesc(t, component.getName());
            // Find free vars of arg
            List<VarCodeGen> freeVars = getFreeVars(arg);

            // Generate descriptor for init method of task
            String init = taskConstructorDesc(freeVars);

            String task = delegate(arg, tDesc, init, freeVars);
            tasks[i] = task;
            results[i] = tDesc;

            constructWithFreeVars(task, freeVars, init);

            mv.visitInsn(Opcodes.DUP);
            int taskVar = mv.createCompilerLocal(task, // Naming.mangleIdentifier(task),
                    NamingCzar.internalToDesc(task));
            taskVars[i] = taskVar;
            mv.visitVarInsn(Opcodes.ASTORE, taskVar);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, task, "forkIfProfitable", "()V");
        }
        // arg 0 gets compiled in place, rather than turned into work.
        if (vcgs != null) vcgs.get(0).prepareAssignValue(mv);
        args.get(0).accept(this);
        if (vcgs != null) vcgs.get(0).assignValue(mv);

        // join / perform work locally left to right, leaving results on stack.
        for (int i = 1; i < n; i++) {
            if (vcgs != null) vcgs.get(i).prepareAssignValue(mv);
            int taskVar = taskVars[i];
            mv.visitVarInsn(Opcodes.ALOAD, taskVar);
            mv.disposeCompilerLocal(taskVar);
            mv.visitInsn(Opcodes.DUP);
            String task = tasks[i];
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, task, "joinOrRun", "()V");
            mv.visitFieldInsn(Opcodes.GETFIELD, task, "result", results[i]);
            if (vcgs != null) vcgs.get(i).assignValue(mv);
        }
    }

    // Evaluate args serially, from left to right.
    // Leave the results (in the order given) on the stack when vcgs==null;
    // otherwise use the provided vcgs to bind corresponding values.
    public void forExprsSerial(List<? extends Expr> args, List<VarCodeGen> vcgs) {
        if (vcgs == null) {
            for (Expr arg : args) {
                arg.accept(this);
            }
        } else {
            int n = args.size();
            if (args.size() != vcgs.size()) {
                sayWhat(args.get(0), "Internal error: number of args does not match number of consumers.");
            }
            for (int i = 0; i < n; i++) {
                VarCodeGen vcg = vcgs.get(i);
                vcg.prepareAssignValue(mv);
                args.get(i).accept(this);
                vcg.assignValue(mv);
            }
        }
    }

    public void forOpExpr(OpExpr x) {
        debug("forOpExpr ", x, " op = ", x.getOp(),
                     " of class ", x.getOp().getClass(),  " args = ", x.getArgs());
        FunctionalRef op = x.getOp();
        List<Expr> args = x.getArgs();

        if (pa.worthParallelizing(x)) {
            forExprsParallel(args, null);
        } else {
            for (Expr arg : args) {
                arg.accept(this);
            }
        }

        op.accept(this);

    }

    public void forOpRef(OpRef x) {
        forFunctionalRef(x);
   }

    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FString and push it on the stack.
        debug("forStringLiteral ", x);
        addLineNumberInfo(x);
        mv.visitLdcInsn(x.getText());
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressString, NamingCzar.make,
                           NamingCzar.makeMethodDesc(NamingCzar.descString, NamingCzar.descFortressString));
    }

    public void forSubscriptExpr(SubscriptExpr x) {
        // TODO: FIX!!  Only works for string subscripting.  Why does this
        // AST node still exist at all at this point in compilation??
        // It ought to be turned into a MethodInvocation.
        // JWM 9/4/09
        debug("forSubscriptExpr ", x);
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        Option<Op> maybe_op = x.getOp();
        List<StaticArg> staticArgs = x.getStaticArgs();
        boolean canCompile = staticArgs.isEmpty() && maybe_op.isSome() && (obj instanceof VarRef);
        if (!canCompile) { sayWhat(x); return; }
        Op op = maybe_op.unwrap();
        VarRef var = (VarRef) obj;
        Id id = var.getVarId();

        debug("ForSubscriptExpr  ", x, "obj = ", obj,
              " subs = ", subs, " op = ", op, " static args = ", staticArgs,
              " varRef = ", NamingCzar.idToString(id));

        var.accept(this);

        for (Expr e : subs) {
            debug("calling accept on ", e);
            e.accept(this);
        }
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           NamingCzar.makeInnerClassName(id),
                           // Naming.mangleIdentifier(opToString(op)),
                           NamingCzar.opToString(op),
                           "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");
    }

    public void forTraitDecl(TraitDecl x) {
        debug("forTraitDecl", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            // NOTE: Presence of excludes or comprises clauses should not
            // affect code generation once type checking is complete.
            // x.getExcludesClause().isEmpty() &&    // no excludes clause
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty() ; // && // no modifiers
            // extendsC.size() <= 1;
        debug("forTraitDecl", x,
                    " decls = ", header.getDecls(), " extends = ", extendsC);
        if ( !canCompile ) sayWhat(x);
        inATrait = true;
        currentTraitObjectDecl = x;
        String [] superInterfaces = NamingCzar.extendsClauseToInterfaces(extendsC, component.getName());

        // First let's do the interface class
        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
                                                         NodeUtil.getName(x).getText());
        String abstractSuperclass;
        traitOrObjectName = classFile;
        if (classFile.equals("fortress/AnyType$Any")) {
            superInterfaces = new String[0];
            abstractSuperclass = NamingCzar.FValueType;
        } else {
            abstractSuperclass = superInterfaces[0] + NamingCzar.springBoard;
        }
        CodeGenClassWriter prev = cw;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        cw.visit( Opcodes.V1_5,
                  Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE,
                  classFile, null, NamingCzar.internalObject, superInterfaces);
        dumpSigs(header.getDecls());
        dumpClass( classFile );

        // Now lets do the springboard inner class that implements this interface.
        springBoardClass = classFile + NamingCzar.springBoard;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        // Springboard *must* be abstract if any methods / fields are abstract!
        // In general Springboard must not be directly instantiable.
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT, springBoardClass,
                 null, abstractSuperclass, new String[] { classFile } );
        debug("Start writing springboard class ",
              springBoardClass);
        generateFieldsAndInitMethod(springBoardClass, abstractSuperclass,
                                    Collections.<Param>emptyList());
        debug("Finished init method ", springBoardClass);
        dumpTraitDecls(header.getDecls());
        debug("Finished dumpDecls ", springBoardClass);
        dumpClass(springBoardClass);
        // Now lets dump out the functional methods at top level.
        cw = prev;

        emittingFunctionalMethodWrappers = true;
        dumpTraitDecls(header.getDecls());
        emittingFunctionalMethodWrappers = false;

        debug("Finished dumpDecls for parent");
        inATrait = false;
        traitOrObjectName = null;
        springBoardClass = null;
    }

    public void forVarDecl(VarDecl v) {
        // Assumption: we already dealt with this VarDecl in pre-pass.
        // Therefore we can just skip it.
        debug("forVarDecl ",v," should have been seen during pre-pass.");
    }

    /** Supposed to be called with nested codegen context. */
    private void generateVarDeclInnerClass(VarDecl x, String classFile, String tyDesc, Expr exp) {
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL,
                  classFile, null, NamingCzar.internalSingleton, null );
        cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
                      NamingCzar.SINGLETON_FIELD_NAME, tyDesc, null, null);
        mv = cw.visitCGMethod(Opcodes.ACC_STATIC,
                            "<clinit>", NamingCzar.voidToVoid, null, null);
        exp.accept(this);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, classFile,
                          NamingCzar.SINGLETON_FIELD_NAME, tyDesc);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
        dumpClass( classFile );
    }

    private void forVarDeclPrePass(VarDecl v) {
        List<LValue> lhs = v.getLhs();
        Option<Expr> oinit = v.getInit();
        if (lhs.size() != 1) {
            sayWhat(v,"VarDecl "+v+" tupled lhs not handled.");
        }
        if (!oinit.isSome()) {
            debug("VarDecl ", v, " skipping abs var decl.");
            return;
        }
        LValue lv = lhs.get(0);
        if (lv.isMutable()) {
            sayWhat(v,"VarDecl "+v+" mutable bindings not yet handled.");
        }
        Id var = lv.getName();
        Type ty = lv.getIdType().unwrap();
        Expr exp = oinit.unwrap();
        String classFile = NamingCzar.jvmClassForToplevelDecl(var, packageAndClassName);
        String tyDesc = NamingCzar.jvmTypeDesc(ty, thisApi());
        debug("VarDeclPrePass ", var, " : ", ty, " = ", exp);
        new CodeGen(this).generateVarDeclInnerClass(v, classFile, tyDesc, exp);

        addStaticVar(
            new VarCodeGen.StaticBinding(var, ty, classFile,
                                         NamingCzar.SINGLETON_FIELD_NAME, tyDesc));
    }

    public void forVarRef(VarRef v) {
        if (v.getStaticArgs().size() > 0) {
            sayWhat(v,"varRef with static args!  That requires non-local VarRefs.  We can't deal for now.");
        }
        Id id = v.getVarId();
        VarCodeGen vcg = getLocalVarOrNull(id);
        if (vcg == null) {
            debug("forVarRef fresh import ", v);
            Type ty = NodeUtil.getExprType(v).unwrap();
            String tyDesc = NamingCzar.jvmTypeDesc(ty, thisApi());
            String className = NamingCzar.jvmClassForToplevelDecl(id, packageAndClassName);
            vcg = new VarCodeGen.StaticBinding(id, ty,
                                               className,
                                               NamingCzar.SINGLETON_FIELD_NAME,
                                               tyDesc);
            addStaticVar(vcg);
        }
        debug("forVarRef ", v , " Value = ", vcg);
        addLineNumberInfo(v);
        vcg.pushValue(mv);
    }

    private void pushVoid() {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                           NamingCzar.makeMethodDesc("", NamingCzar.descFortressVoid));
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        debug("forVoidLiteral ", x);
        addLineNumberInfo(x);
        pushVoid();
    }

    public void forMethodInvocation(MethodInvocation x) {
        debug("forMethodInvocation ", x,
              " obj = ", x.getObj(),
              " method = ", x.getMethod(),
              " static args = ", x.getStaticArgs(),
              " args = ", x.getArg());
        Id method = x.getMethod();
        Expr obj = x.getObj();
        List<StaticArg> sargs = x.getStaticArgs();
        Expr arg = x.getArg();

        Option<Type> mt = x.getOverloadingType();
        Type domain_type;
        Type range_type;
        if ((mt.isSome())) {
            ArrowType sigtype = (ArrowType) mt.unwrap();
            domain_type = sigtype.getDomain();
            range_type = sigtype.getRange();
        } else {
            // TODO: some method applications (particularly those
            // introduced during getter desugaring) don't have an
            // OverloadingType.  Fix?  Or live with it?
            domain_type = exprType(arg);
            range_type = exprType(x);
        }

        Type receiverType = exprType(obj);
        if (!(receiverType instanceof TraitType)) {
            sayWhat(x, "receiver type "+receiverType+" is not TraitType in " + x);
        }

        int savedParamCount = paramCount;
        try {
            // put object on stack
            obj.accept(this);
            // put args on stack
            evalArg(arg);
            methodCall(method, (TraitType)receiverType, domain_type, range_type);
        } finally {
            paramCount = savedParamCount;
        }

    }

    /**
     * @param expr
     * @return
     */
    private Type exprType(Expr expr) {
        Option<Type> exprType = expr.getInfo().getExprType();

        if (!exprType.isSome()) {
            sayWhat(expr, "Missing type information for " + expr);
        }

        Type ty = exprType.unwrap();
        if (ty instanceof TraitSelfType)
            ty = ((TraitSelfType)ty).getNamed();

        return ty;
    }
    private Option<Type> exprOptType(Expr expr) {
        Option<Type> exprType = expr.getInfo().getExprType();

        return exprType;
    }

    /**
     * @param arg
     */
    private void evalArg(Expr arg) {
        if (arg instanceof VoidLiteralExpr) {
            paramCount = 0;
        } else if (arg instanceof TupleExpr) {
            TupleExpr targ = (TupleExpr) arg;
            List<Expr> exprs = targ.getExprs();
            for (Expr expr : exprs) {
                expr.accept(this);
            }
            paramCount = exprs.size();
        } else {
            paramCount = 1; // for now; need to dissect tuple and do more.
            arg.accept(this);
        }
    }

    private void generateHigherOrderCall(Type t) {
        if (!(t instanceof ArrowType)) {
            sayWhat(t,"Higher-order call to non-arrow type " + t);
        }
        ArrowType at = (ArrowType)t;
        String desc = NamingCzar.makeArrowDescriptor(at, thisApi());
        String sig = NamingCzar.jvmSignatureFor(at,thisApi());
        // System.err.println(desc+".apply"+sig+" call");
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, desc,
                           Naming.APPLY_METHOD, sig);
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        debug("for_RewriteFnApp ", x,
                     " args = ", x.getArgument(), " function = ", x.getFunction(),
              " function class = ", x.getFunction());
        // This is a little weird.  If a function takes no arguments the parser gives me a void literal expr
        // however I don't want to be putting a void literal on the stack because it gets in the way.
        int savedParamCount = paramCount;
        boolean savedFnRefIsApply = fnRefIsApply;
        try {
            Expr arg = x.getArgument();
            Expr fn = x.getFunction();
            if (!(fn instanceof FunctionalRef)) {
                // Higher-order call.
                fn.accept(this); // Puts the VarRef function on the stack.
            }
            fnRefIsApply = false;
            evalArg(arg);
            fnRefIsApply = true;
            if (!(fn instanceof FunctionalRef)) {
                generateHigherOrderCall(exprType(fn));
            } else {
                x.getFunction().accept(this);
            }
        } finally {
            paramCount = savedParamCount;
            fnRefIsApply = savedFnRefIsApply;
        }
    }

    public void for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl x) {
        /* Note for refactoring -- this code does it the "right" way.
         * And also, this code NEEDS refactoring.
         */
        List<IdOrOp> fns = x.getFns();
        IdOrOp name = x.getName();
        Option<com.sun.fortress.nodes.Type> ot = x.getType();
        com.sun.fortress.nodes.Type ty = ot.unwrap();
        Relation<IdOrOpOrAnonymousName, Function> fnrl = ci.functions();

        MultiMap<Integer, OverloadSet.TaggedFunctionName> byCount =
            new MultiMap<Integer,OverloadSet.TaggedFunctionName>();

        for (IdOrOp fn : fns) {

            Option<APIName> fnapi = fn.getApiName();
            PredicateSet<Function> set_of_f;
            APIName apiname;

            if (fnapi.isNone()) {
                apiname = thisApi();
                set_of_f = fnrl.matchFirst(fn);
            } else {

                IdOrOp fnnoapi = NodeFactory.makeLocalIdOrOp(fn);
                apiname = fnapi.unwrap();
                ApiIndex ai = env.api(apiname);
                set_of_f = ai.functions().matchFirst(fnnoapi);
            }

            for (Function f : set_of_f) {
                /* This guard should be unnecessary when proper overload
                   disambiguation is working.  Right now, the types are
                   "too accurate" which causes a call to an otherwise
                   non-existent static method.
                */
                if (OverloadSet.functionInstanceofType(f, ty, ta)) {
                    OverloadSet.TaggedFunctionName tagged_f =
                        new OverloadSet.TaggedFunctionName(apiname, f);
                    byCount.putItem(f.parameters().size(), tagged_f);
                }
            }
        }

        for (Map.Entry<Integer, Set<OverloadSet.TaggedFunctionName>> entry :
                 byCount.entrySet()) {
            int i = entry.getKey();
            Set<OverloadSet.TaggedFunctionName> fs = entry.getValue();
            if (fs.size() > 1) {
                OverloadSet os = new OverloadSet.AmongApis(thisApi(), name,
                                                           ta, fs, i);

                os.split(false);
                os.generateAnOverloadDefinition(name.stringName(), cw);

            }
        }

    }


    /**
     * Creates overloaded functions for any overloads present at the top level
     * of this component.  Top level overloads are those that might be exported;
     * Reference overloads are rewritten into _RewriteFnOverloadDecl nodes
     * and generated in the normal visits.
     */
    public static Set<String> generateTopLevelOverloads(APIName api_name,
            Map<IdOrOpOrAnonymousName,MultiMap<Integer, Function>> size_partitioned_overloads,
            TypeAnalyzer ta,
            CodeGenClassWriter cw, CodeGen cg
            ) {

        Set<String> overloaded_names_and_sigs = new HashSet<String>();

        for (Map.Entry<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> entry1 :
                 size_partitioned_overloads.entrySet()) {
            IdOrOpOrAnonymousName  name = entry1.getKey();
            MultiMap<Integer, Function> partitionedByArgCount = entry1.getValue();

            for (Map.Entry<Integer, Set<Function>> entry :
                     partitionedByArgCount.entrySet()) {
               int i = entry.getKey();
               Set<Function> fs = entry.getValue();

               OverloadSet os =
                   new OverloadSet.Local(api_name, name,
                                         ta, fs, i);

               os.split(true);

               String s = name.stringName();
               String s2 = NamingCzar.apiAndMethodToMethod(api_name, s);

               os.generateAnOverloadDefinition(s2, cw);
               if (cg != null) {
                   /* Need to check if the overloaded function happens to match
                    * a name in an API that this component exports; if so,
                    * generate a forwarding wrapper from the
                    */
               }

               for (Map.Entry<String, OverloadSet> o_entry : os.getOverloadSubsets().entrySet()) {
                   String ss = o_entry.getKey();
                   ss = s + ss;
                   overloaded_names_and_sigs.add(ss);
               }
           }
        }
        return overloaded_names_and_sigs;
    }

    public static Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>>
       sizePartitionedOverloads(Relation<IdOrOpOrAnonymousName, Function> fns) {

        Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> result =
            new HashMap<IdOrOpOrAnonymousName, MultiMap<Integer, Function>>();

        for (IdOrOpOrAnonymousName name : fns.firstSet()) {
            Set<Function> defs = fns.matchFirst(name);
            if (defs.size() <= 1) continue;

            MultiMap<Integer, Function> partitionedByArgCount =
                new MultiMap<Integer, Function>();

            for (Function d : defs) {
                partitionedByArgCount.putItem(d.parameters().size(), d);
            }

            for (Function d : defs) {
                Set<Function> sf = partitionedByArgCount.get(d.parameters().size());
                if (sf != null && sf.size() <= 1)
                    partitionedByArgCount.remove(d.parameters().size());
            }
            if (partitionedByArgCount.size() > 0)
                result.put(name, partitionedByArgCount);
        }

        return result;
    }

    private List<Decl> topSortDeclsByDependencies(List<Decl> decls) {
        HashMap<IdOrOp, TopSortItemImpl<Decl>> varToNode =
            new HashMap<IdOrOp, TopSortItemImpl<Decl>>(2 * decls.size());
        List<TopSortItemImpl<Decl>> nodes = new ArrayList(decls.size());
        for (Decl d : decls) {
            TopSortItemImpl<Decl> node =
                new TopSortItemImpl<Decl>(d);
            nodes.add(node);
            if (d instanceof VarDecl) {
                VarDecl vd = (VarDecl)d;
                for (LValue lv : vd.getLhs()) {
                    varToNode.put(lv.getName(), node);
                }
            } else if (d instanceof TraitObjectDecl) {
                TraitObjectDecl tod = (TraitObjectDecl)d;
                IdOrOpOrAnonymousName name = tod.getHeader().getName();
                if (name instanceof IdOrOp) {
                    varToNode.put((IdOrOp)name, node);
                }
            } else {
                sayWhat(d, " can't sort non-value-creating decl by dependencies.");
            }
        }
        for (TopSortItemImpl<Decl> node : nodes) {
            for (IdOrOp freeVar : fv.freeVars(node.x)) {
                TopSortItemImpl<Decl> dest = varToNode.get(freeVar);
                if (dest != null && dest != node) {
                    dest.edgeTo(node);
                }
            }
        }
        // TODO: can't handle cycles!
        nodes = TopSort.depthFirst(nodes);
        List<Decl> result = new ArrayList(nodes.size());
        for (TopSortItemImpl<Decl> node : nodes) {
            result.add(node.x);
        }
        return result;
    }

    /**
     * Traits compile to interfaces.  These are all the abstract methods that
     * the interface will require.
     *
     * @param decls
     */
    private void dumpSigs(List<Decl> decls) {
        debug("dumpSigs", decls);
        for (Decl d : decls) {
            debug("dumpSigs decl =", d);
            if (!(d instanceof FnDecl)) {
                sayWhat(d);
                return;
            }

            FnDecl f = (FnDecl) d;
            FnHeader h = f.getHeader();

            List<Param> params = h.getParams();
            int selfIndex = selfParameterIndex(params);
            boolean  functionalMethod = selfIndex != NO_SELF;

            IdOrOpOrAnonymousName xname = h.getName();
            IdOrOp name = (IdOrOp) xname;

            String desc = NamingCzar.jvmSignatureFor(f,component.getName());
            if (functionalMethod) {
                desc = Naming.removeNthSigParameter(desc, selfIndex);
            }

            // TODO what about overloading collisions in an interface?
            // it seems wrong to publicly mangle.
            String mname = functionalMethod ? fmDottedName(
                            singleName(name), selfIndex) : nonCollidingSingleName(
                                    name, desc);

            mv = cw.visitCGMethod(Opcodes.ACC_ABSTRACT + Opcodes.ACC_PUBLIC,
                                mname, desc, null, null);

            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
        }
    }

}
