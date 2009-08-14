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

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.*;

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
import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.syntax_abstractions.ParserMaker.Mangler;
import com.sun.fortress.useful.BA2Tree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.StringHashComparer;

// Note we have a name clash with org.objectweb.asm.Type
// and com.sun.fortress.nodes.Type.  If anyone has a better
// solution than writing out their entire types, please
// shout out.
public class CodeGen extends NodeAbstractVisitor_void {
    CodeGenClassWriter cw;
    CodeGenMethodVisitor mv; // Is this a mistake?  We seem to use it to pass state to methods/visitors.
    final String packageAndClassName;
    private final Map<String, ClassWriter> traitsAndObjects =
        new BATree<String, ClassWriter>(DefaultComparator.normal());
    private final HashMap<String, String> aliasTable;
    private final TypeAnalyzer ta;
    private final ParallelismAnalyzer pa;
    private final Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> topLevelOverloads;
    private HashSet<String> overloadedNamesAndSigs;
    private final List<ObjectDecl> singletonObjects;

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


    public CodeGen(Component c, TypeAnalyzer ta, ParallelismAnalyzer pa, ComponentIndex ci,
                   GlobalEnvironment env) {
        component = c;
        packageAndClassName = NamingCzar.javaPackageClassForApi(c.getName().getText(), "/").toString();
        aliasTable = new HashMap<String, String>();
        this.ta = ta;
        this.pa = pa;
        this.ci = ci;
        this.topLevelOverloads =
            sizePartitionedOverloads(ci.functions());
        this.overloadedNamesAndSigs = new HashSet<String>();
        this.singletonObjects = new ArrayList<ObjectDecl>();
        this.lexEnv = new BATree<String,VarCodeGen>(StringHashComparer.V);
        this.env = env;
        debug( "Compile: Compiling ", packageAndClassName );
    }

    // Create a fresh codegen object for a nested scope.  Technically,
    // we ought to be able to get by with a single lexEnv, because
    // variables ought to be unique by location etc.  But in practice
    // I'm not assuming we have a unique handle for any variable,
    // so we get a fresh CodeGen for each scope to avoid collisions.
    private CodeGen(CodeGen c) {
        this.component = c.component;
        this.cw = c.cw;
        this.mv = c.mv;
        this.packageAndClassName = c.packageAndClassName;
        this.aliasTable = c.aliasTable;
        this.inATrait = c.inATrait;
        this.inAnObject = c.inAnObject;
        this.inABlock = c.inABlock;
        this.ta = c.ta;
        this.pa = c.pa;
        this.ci = c.ci;
        this.env = c.env;
        this.topLevelOverloads = c.topLevelOverloads;
        this.overloadedNamesAndSigs = c.overloadedNamesAndSigs;
        this.singletonObjects = c.singletonObjects;
        this.lexEnv = new BATree<String,VarCodeGen>(c.lexEnv);

    }

    // We need to expose this because nobody else helping CodeGen can
    // understand unqualified names (esp types) without it!
    public APIName thisApi() {
        return ci.ast().getName();
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

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
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

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "compute",
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
        // TODO Allocate fields
        for (Param p : params) {
            // TODO need to spot for "final" fields.
            String pn = p.getName().getText();
            Type pt = p.getIdType().unwrap();
            cw.visitField(Opcodes.ACC_PRIVATE, pn,
                    NamingCzar.only.jvmTypeDesc(pt, thisApi(), true), null /* for non-generic */, null /* instance has no value */);
        }

        String init_sig = NamingCzar.only.jvmSignatureFor(params, "V", thisApi());
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", init_sig, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, superClass, "<init>", NamingCzar.voidToVoid);

        // TODO Initialize fields.
        int pno = 1;
        for (Param p : params) {
            String pn = p.getName().getText();
            Type pt = p.getIdType().unwrap();

            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, pno);
            mv.visitFieldInsn(Opcodes.PUTFIELD, classFile, pn,
                    NamingCzar.only.jvmTypeDesc(pt, thisApi(), true));
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

    private VarCodeGen addParam(TraitObjectDecl x) {
        Id id = NodeFactory.makeId(NodeUtil.getSpan(x), "self");
        Id tid = (Id)  x.getHeader().getName();
        Type t = NodeFactory.makeTraitType(tid);
        VarCodeGen v =
            new VarCodeGen.ParamVar(id, t, this);
        addLocalVar(v);
        return v;
    }

    private VarCodeGen getLocalVar( IdOrOp nm ) {
        VarCodeGen r = getLocalVarOrNull(nm);
        if (r==null) return sayWhat(nm, "Can't find lexEnv mapping for local var");
        return r;
    }
    private VarCodeGen getLocalVarOrNull( IdOrOp nm ) {
        debug("getLocalVar: " + nm);
        VarCodeGen r = lexEnv.get(nm.getText());
        if (r != null)
            debug("getLocalVar:" + nm + " VarCodeGen = " + r + " of class " + r.getClass());
        else
            debug("getLocalVar:" + nm + " VarCodeGen = null");
        return r;
    }

    private VarCodeGen addTaskVar(ASTNode x, String name, String javaType, String taskClass) {
        NodeUtil.getSpan(x);
        TaskVarCodeGen t = new TaskVarCodeGen(NodeFactory.makeId(NodeUtil.getSpan(x), name),
                                              NamingCzar.fortressTypeForForeignJavaType(javaType),
                                              taskClass, thisApi());
        cw.visitField(Opcodes.ACC_PUBLIC, name, javaType, null, 6847);
        addLocalVar(t);
        return t;
    }

    public void dumpClass( String file ) {
        PrintWriter pw = new PrintWriter(System.out);
        cw.visitEnd();

        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false))
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), true, pw);

        ByteCodeWriter.writeClass(NamingCzar.cache, file, cw.toByteArray());
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

        Pair<String, String> method_and_signature = resolveMethodAndSignature(
                x, arrow, methodName);
        
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
    private Pair<String, String> resolveMethodAndSignature(FunctionalRef x,
            com.sun.fortress.nodes.Type arrow, String methodName) throws Error {
        Pair<String, String> method_and_signature = null;
        
        if ( arrow instanceof ArrowType ) {
            // TODO should this be non-colliding single name instead?
            // answer depends upon how intersection types are normalized.
            // conservative answer is "no".
            methodName = Naming.mangleIdentifier(methodName);
            method_and_signature = new Pair<String, String>(methodName, NamingCzar.jvmMethodDesc(arrow, component.getName()));
            
        } else if (arrow instanceof IntersectionType) {
            IntersectionType it = (IntersectionType) arrow;
            methodName = OverloadSet.actuallyOverloaded(it, paramCount) ?
                    OverloadSet.oMangle(methodName) : Naming.mangleIdentifier(methodName);
                    
            method_and_signature = new Pair<String, String>(methodName,
                    OverloadSet.getSignature(it, paramCount, ta));

        } else {
                sayWhat( x, "Neither arrow nor intersection type: " + arrow );
                throw new Error(); // not reached
        }
        return method_and_signature;
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
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private <T> T sayWhat(Node x) {
        if (x instanceof ASTNode)
            sayWhat((ASTNode) x);
        allSayWhats();
        throw new CompilerError("Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
        allSayWhats();
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }

    private void debug(Object... message){
        Debug.debug(Debug.Type.CODEGEN,1,message);
    }

    private void doStatements(List<Expr> stmts) {
        int onStack = 0;
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
            throw new CompilerError(NodeUtil.getSpan(x), x + "links.size != 1");
        Link link = i.next();
        link.getExpr().accept(this);
        debug( "forChainExpr", x, " about to call accept on ",
               link.getOp(), " of class ", link.getOp().getClass());
        link.getOp().accept(this);

        debug( "We've got a link ", link, " an op ", link.getOp(),
               " and an expr ", link.getExpr(), " What do we do now");
    }

    public void forComponent(Component x) {
        debug("forComponent ",x.getName(),NodeUtil.getSpan(x));
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(packageAndClassName, null);
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

        // Always generate the init method
        generateFieldsAndInitMethod(packageAndClassName, extendedJavaClass, Collections.<Param>emptyList());

        // If this component exports an executable API,
        // generate a main method.
        if ( exportsExecutable ) {
            generateMainMethod();
        }
        for ( Import i : x.getImports() ) {
            i.accept(this);
        }

        // determineOverloadedNames(x.getDecls() );

        // Must do this first, to get local decls right.
        generateTopLevelOverloads();

        /*
         * Must process these first to put them into scope.
         * This probably will generalize to include VarDecl, in which case
         * we probably want some sort of a visitor.
         */
        for (Decl d : x.getDecls()) {
            if (d instanceof ObjectDecl) {
                this.forObjectDeclPrePass((ObjectDecl) d);
            }
        }

        // Static initializer for this class.
        mv = cw.visitMethod(Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);

       // Singletons;

        for (ObjectDecl y : singletonObjects) {
            singletonObjectFieldAndInit(y);
        }

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();

        for ( Decl d : x.getDecls() ) {
            d.accept(this);
        }

        dumpClass( packageAndClassName );
    }

    public void forDecl(Decl x) {
        sayWhat(x, "forDecl");
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

    public void forFnDecl(FnDecl x) {

        /*
         * Cases for FnDecl:
         *
         * 1. top level
         *
         * 2. trait normal method.
         *    a. for trait itself, an abstract method in generated interface
         *       (this may be handled elsewhere)
         *    b. for trait defaults, a static method in SpringBoard
         *
         * 3. trait functional method
         *    a. for trait itself, a mangled-name abstract method with self
         *       removed from the parameter list.
         *       (this may be handled elsewhere)
         *    b. at top level, a functional wrapper with self in original
         *       position, which invokes the interface method with self in
         *       dotted position.  NOTE THE POTENTIAL FOR OVERLOADING.
         *    c. for trait defaults, a mangled-name static method with self in
         *       the first parameter position (in SpringBoard).
         *
         * 4. object normal method
         *    a. a normal dotted method is generated
         *
         * 5. object functional method
         *    a. a mangled-name dotted method is generated with self removed
         *       from the parameter list.
         *    b. at top level, a functional wrapper with self in original
         *       position, which invokes the interface method with self in
         *       dotted position.  NOTE THE POTENTIAL FOR OVERLOADING.
         *       Static overload resolution can be an optimization.
         */


        debug("forFnDecl ", x );
        FnHeader header = x.getHeader();
        boolean canCompile =
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty() &&         // no modifiers
            !inABlock;                            // no local functions

        if ( !canCompile ) sayWhat(x);

        Option<Expr> body = x.getBody();
        IdOrOpOrAnonymousName name = header.getName();
        List<Param> params = header.getParams();
        boolean functionalMethod = false;
        boolean emitUnambiguous = false;

        int selfIndex = 0;
        for (Param p : params) {
            debug("iterating params looking for self : param = ", p);
            if (p.getName().getText() == "self") {
                functionalMethod = true;
                break;
            }
            selfIndex++;
        }
        if (emittingFunctionalMethodWrappers) {
            if (! functionalMethod)
                return; // Not functional = no wrapper needed.

            // bizarrely implemented! return; // TODO not yet implemented.
        }
        // else need to incorrectly emit code for functional methods.
        {

        boolean hasSelf = !functionalMethod && (inAnObject || inATrait);
        boolean savedInAnObject = inAnObject;
        boolean savedInATrait = inATrait;
        boolean savedEmittingFunctionalMethodWrappers = emittingFunctionalMethodWrappers;

        boolean emittingTraitDefault = inATrait;

        try {
            // TODO don't yet actually emit the functional method wrappers.

            inAnObject = false;
            inATrait = false;

            Option<com.sun.fortress.nodes.Type> returnType = header.getReturnType();

            // For now every Fortress entity is made public, with
            // namespace management happening in Fortress-land.  Right?
            // [JWM:] we'll want to clamp down on this long-term, but
            // we have to get nesting right---we generate a pile of class files for
            // one Fortress component
            int modifiers = Opcodes.ACC_PUBLIC;

            if ( body.isNone() )
                sayWhat(x, "Abstract function declarations are not supported.");
            if ( returnType.isNone() )
                sayWhat(x, "Return type is not inferred.");
            if ( !( name instanceof IdOrOp ))
                sayWhat(x, "Unhandled function name.");

            if ( name instanceof Id ) {
                Id id = (Id) name;
                debug("forId ", id,
                        " class = ", NamingCzar.jvmClassForSymbol(id));
            } else if ( name instanceof Op ) {
                Op op = (Op) name;
                Fixity fixity = op.getFixity();
                boolean isEnclosing = op.isEnclosing();
                Option<APIName> maybe_apiName = op.getApiName();
                debug("forOp ", op, " fixity = ", fixity,
                        " isEnclosing = ", isEnclosing,
                        " class = ", NamingCzar.jvmClassForSymbol(op));
            } else {
                sayWhat(x);
            }

            CodeGen cg = new CodeGen(this);

            if (!hasSelf || emittingTraitDefault) {
                // Top-level function or functional method
                // DO NOT special case run() here and make it non-static (that used to happen),
                // as that's wrong.  It's addressed in the executable wrapper code instead.
                modifiers += Opcodes.ACC_STATIC;
            }

            // TODO

            /*
             * Need to modify the signature, depending on circumstances.
             */
            String sig;

            if (emittingTraitDefault &&
                    ! emittingFunctionalMethodWrappers // temporary hack to keep things "working"; ought to use a completely separate code path.
                    ) {
                Type traitType = NodeFactory.makeTraitType((Id) currentTraitObjectDecl.getHeader().getName());
                sig = NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                        NamingCzar.jvmTypeDesc(returnType.unwrap(), component.getName()),
                        0, traitType, component.getName());
            } else {
                sig = NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                        returnType.unwrap(),
                        component.getName());
            }

            // TODO different collision rules for top-level and for methods.
            String mname = nonCollidingSingleName(name, sig);

            cg.mv = cw.visitMethod(modifiers, mname, sig,
                    null, null);
            cg.mv.visitCode();

            // Now inside method body.  Generate code for the method body.
            // Start by binding the parameters and setting up the initial locals.
            VarCodeGen selfVar = null;
            if (hasSelf) {
                // TODO: Add proper type information here based on the
                // enclosing trait/object decl.  For now we can get away
                // with just stashing a null as we're not using it to
                // determine stack sizing or anything similarly crucial.
                if (emittingTraitDefault) {
                    selfVar = cg.addParam(currentTraitObjectDecl);
                } else {
                    selfVar = new VarCodeGen.SelfVar(NodeUtil.getSpan(name), null, cg);
                    cg.addLocalVar(selfVar);
                }
            }
            List<VarCodeGen> paramsGen = new ArrayList<VarCodeGen>(params.size());
            for (Param p : params) {
                VarCodeGen v = cg.addParam(p);
                paramsGen.add(v);
            }
            // Compile the body in the parameter environment
            body.unwrap().accept(cg);
            // Clean up the parameters
            for (int i = paramsGen.size(); i > 0; ) {
                VarCodeGen v = paramsGen.get(--i);
                v.outOfScope(cg.mv);
            }
            if (selfVar != null) selfVar.outOfScope(cg.mv);

            // Method body is complete except for returning final result if any.
            // TODO: Fancy footwork here later on if we need to return a non-pointer;
            // for now every fortress functional returns a single pointer result.
            cg.mv.visitInsn(Opcodes.ARETURN);

            cg.mv.visitMaxs(NamingCzar.ignore,NamingCzar.ignore);
            cg.mv.visitEnd();
            // Method body complete, cg now invalid.

            // TODO need to emit wrappers for unambiguous names
            // Check to see if a wrapper is needed.
            if (topLevelOverloads.containsKey(name)) {
                // Looks like maybe we don't need this after all.
            }

            Option<IdOrOp> iun = x.getImplementsUnambiguousName();

            if (iun.isSome()) {
                // Looks like maybe we don't need this after all.
            }
        } finally {
            inAnObject = savedInAnObject;
            inATrait = savedInATrait;
            emittingFunctionalMethodWrappers = savedEmittingFunctionalMethodWrappers;
        }
        }
    }

    /**
     * Creates a name that will not collide with any overloaded functions
     * (the overloaded name "wins" because it if it is exported, this one is not).
     *
     * @param name
     * @param sig The jvm signature for a method, e.g., (ILjava/lang/Object;)D
     * @return
     */
    private String nonCollidingSingleName(IdOrOpOrAnonymousName name, String sig) {
        String nameString = ((IdOrOp)name).getText();
        String mname = Naming.mangleIdentifier(nameString);
        if (overloadedNamesAndSigs.contains(mname+sig)) {
            mname = NamingCzar.only.mangleAwayFromOverload(mname);
        }
        return mname;
    }


    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        debug("forFnRef ", x);
        if (fnRefIsApply)
            forFunctionalRef(x);
        else {
            // Not entirely sure about this next bit; how are function-valued parameters referenced?
            VarCodeGen fn = getLocalVarOrNull(x.getOriginalName());
            if (fn == null) {
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
                String PCN = pc_and_m.getA() + "/" +
                  Naming.catMangled(
                    method_and_signature.getA() ,
                    Naming.ENVELOPE , // "ENVELOPE"
                    arrow_type);
                /* The suffix will be
                 * (potentially mangled)
                 * functionName<ENVELOPE>closureType (which is an arrow)
                 * 
                 * must generate code for the class with a method apply, that
                 * INVOKE_STATICs prefix.functionName .
                 */
                mv.visitFieldInsn(Opcodes.GETSTATIC, PCN, NamingCzar.closureFieldName, arrow_desc);

            } else {
                sayWhat(x, "Haven't figured out references to local/parameter functions yet");
            }
            
        }
    }

    /**
     * @param x
     */
    public void forFunctionalRef(FunctionalRef x) {

        /* Arrow, or perhaps an intersection if it is an overloaded function. */
        com.sun.fortress.nodes.Type arrow = exprType(x);

        Pair<String, String> calleeInfo = functionalRefToPackageClassAndMethod(x);

        callStaticSingleOrOverloaded(x, arrow, calleeInfo.getA(), calleeInfo.getB());
    }

    /**
     * @param x
     * @return
     */
    private Pair<String, String> functionalRefToPackageClassAndMethod(
            FunctionalRef x) {
        Pair<String, String> calleeInfo;

        String name = x.getOriginalName().getText();
        List<IdOrOp> names = x.getNames();

        /* Note that after pre-processing in the overload rewriter,
         * there is only one name here; this is not an overload check.
         */
        String calleePackageAndClass = "";
        String method = "";

        if ( names.size() != 1) {
            sayWhat(x,"Non-unique overloading after rewrite " + x);
        } else {
            IdOrOp fnName = names.get(0);
            Option<APIName> apiName = fnName.getApiName();

            if (!apiName.isSome()) {
                // NOT Foreign, calls same component.
                // Nothing special to do.
                calleePackageAndClass = packageAndClassName;
                method = fnName.getText();
            } else if (!ForeignJava.only.definesApi(apiName.unwrap())) {
                // NOT Foreign, calls other component.
                calleePackageAndClass =
                    NamingCzar.fortressPackage + "/" + apiName.unwrap().getText();
                method = fnName.getText();
            } else if ( aliasTable.containsKey(name) ) {
                // Foreign function call
                String n = aliasTable.get(name);
                // Cheating by assuming class is everything before the dot.
                int lastDot = n.lastIndexOf(".");
                calleePackageAndClass = n.substring(0, lastDot).replace(".", "/");
                method = n.substring(lastDot+1);
            } else {
                sayWhat(x, "Foreign function " + x + " missing from alias table");
            }
        }
        calleeInfo = new Pair<String, String>(calleePackageAndClass, method);
        return calleeInfo;
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
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NamingCzar.internalFortressBoolean, "getValue",
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
        }
        mv.visitLabel(done);
    }

    public void forImportNames(ImportNames x) {
        debug("forImportNames", x);
        Option<String> foreign = x.getForeignLanguage();
        if ( !foreign.isSome() ) return;
        if ( !foreign.unwrap().equals("java") ) {
            sayWhat(x, "Unrecognized foreign import type (only recognize java): "+
                       foreign.unwrap());
            return;
        }
        String apiName = x.getApiName().getText();
        for ( AliasedSimpleName n : x.getAliasedNames() ) {
            Option<IdOrOpOrAnonymousName> aliasId = n.getAlias();
            if (!(aliasId.isSome())) continue;
            debug("forImportNames ", x,
                  " aliasing ", NodeUtil.nameString(aliasId.unwrap()),
                  " to ", NodeUtil.nameString(n.getName()));
            aliasTable.put(NodeUtil.nameString(aliasId.unwrap()),
                           apiName + "." + NodeUtil.nameString(n.getName()));
        }
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
        if (lhs.size()!=1) {
            sayWhat(d, "Can't yet generate code for bindings of multiple lhs variables");
        }
        LValue v = lhs.get(0);
        if (v.isMutable()) {
            sayWhat(d, "Can't yet generate code for mutable variable declarations.");
        }
        if (!d.getRhs().isSome()) {
            // Just a forward declaration to be bound in subsequent
            // code.  But we need to leave a marker so that the
            // definitions down different control flow paths are
            // consistent; basically we need to create the definition
            // here, and the use that VarCodeGen object for the
            // subsequent true definitions.
            sayWhat(d, "Can't yet handle forward binding declarations.");
        }
        if (!v.getIdType().isSome()) {
            sayWhat(d, "Variable being bound lacks type information!");
        }

        // Introduce variable
        Type ty = v.getIdType().unwrap();
        VarCodeGen vcg = new VarCodeGen.LocalVar(v.getName(), ty, this);
        vcg.prepareAssignValue(mv);

        // Compute rhs
        Expr rhs = d.getRhs().unwrap();
        rhs.accept(this);

        // Perform binding
        vcg.assignValue(mv);

        // Evaluate rest of block with binding in scope
        CodeGen cg = new CodeGen(this);
        cg.addLocalVar(vcg);

        cg.doStatements(d.getBody());

        // Dispose of binding now that we're done
        vcg.outOfScope(mv);
    }

    public void forObjectDecl(ObjectDecl x) {
    }

    public void forObjectDeclPrePass(ObjectDecl x) {
        debug("forObjectDecl", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();

        boolean canCompile =
            // x.getParams().isNone() &&             // no parameters
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            //            header.getDecls().isEmpty() &&        // no members
            header.getMods().isEmpty()         // no modifiers
            // ( extendsC.size() <= 1 ); // 0 or 1 super trait
            ;

        if ( !canCompile ) sayWhat(x);

        boolean savedInAnObject = inAnObject;
        inAnObject = true;
        String [] superInterfaces = NamingCzar.extendsClauseToInterfaces(extendsC, component.getName());

        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
                NodeUtil.getName(x).getText());

        List<Param> params;
        if (x.getParams().isSome()) {
            params = x.getParams().unwrap();

             // Generate the factory method
            String classDesc = NamingCzar.only.internalToDesc(classFile);
            String sig = NamingCzar.only.jvmSignatureFor(params, classDesc, thisApi());
            String init_sig = NamingCzar.only.jvmSignatureFor(params, "V", thisApi());

            String mname = nonCollidingSingleName(x.getHeader().getName(), sig);

            mv = cw.visitMethod(Opcodes.ACC_STATIC,
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
            // Add to list to be initialized in clinit
            singletonObjects.add(x);
        }

        CodeGenClassWriter prev = cw;

        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(classFile, null);

        // Until we resolve the directory hierarchy problem.
        //            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
        //                      classFile, null, NamingCzar.internalObject, new String[] { parent });
        cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
                  classFile, null, NamingCzar.internalObject, superInterfaces);

        // Emit fields here, one per parameter.

        generateFieldsAndInitMethod(classFile, NamingCzar.internalObject, params);

        BATree<String, VarCodeGen> savedLexEnv = lexEnv.copy();

        // need to add locals to the environment, yes.
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
    }

    /**
     * @param classFile
     * @param objectFieldName
     */
    private void singletonObjectFieldAndInit(ObjectDecl x) {
        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
                NodeUtil.getName(x).getText());

        String objectFieldName = x.getHeader().getName().stringName();


        String classDesc = NamingCzar.only.internalToDesc(classFile);

         // Singleton field.
        cw.visitField(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                objectFieldName,
                      classDesc,
                      null,
                      null);

        mv.visitTypeInsn(Opcodes.NEW, classFile);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, classFile, "<init>", NamingCzar.voidToVoid);
        mv.visitFieldInsn(Opcodes.PUTSTATIC, packageAndClassName, objectFieldName, classDesc);

        Id id = (Id)  x.getHeader().getName();

        addStaticVar(new VarCodeGen.SingletonObject(
                    id,
                    NodeFactory.makeTraitType(id),
                    packageAndClassName, objectFieldName, classDesc
                ));
    }

    private void generatePrintArgs(CodeGen cg) {
        cg.mv.visitVarInsn(Opcodes.ALOAD, 1);
        cg.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NamingCzar.internalFortressZZ32,
                              "getValue", "()I");
        cg.mv.visitMethodInsn(Opcodes.INVOKESTATIC,"com/sun/fortress/nativeHelpers/simplePrintArgs",
                              "nativePrintZZ32",  "(I)V");
    }

    private void generatePrintResult(CodeGen cg) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NamingCzar.internalFortressZZ32,
                           "getValue", "()I");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,"com/sun/fortress/nativeHelpers/simplePrintResult",
                           "nativePrintZZ32",  "(I)V");
    }

    // This returns a list rather than a set because the order matters;
    // we should guarantee that we choose a consistent order every time.
    private List<VarCodeGen> getFreeVars(Node n) {
        // Assume all avail vars are used.  Naive!!!  Replace with analysis results.
        return new ArrayList(lexEnv.values());
    }

    private BATree<String, VarCodeGen>
            createTaskLexEnvVariables(String taskClass, List<VarCodeGen> freeVars) {

        BATree<String, VarCodeGen> result =
            new BATree<String, VarCodeGen>(StringHashComparer.V);
        for (VarCodeGen v : freeVars) {
            String name = v.name.getText();
            cw.visitField(Opcodes.ACC_PUBLIC, name,
                          NamingCzar.only.boxedImplDesc(v.fortressType, thisApi()),
                          null, null);
            result.put(name, new TaskVarCodeGen(v, taskClass, thisApi()));
        }
        return result;
    }

    private void generateTaskInit(String initDesc,
                                  List<VarCodeGen> freeVars) {

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", initDesc, null, null);
        mv.visitCode();

        // Call superclass constructor
        mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, NamingCzar.fortressBaseTask,
                              "<init>", NamingCzar.voidToVoid);
        mv.visitVarInsn(Opcodes.ALOAD, mv.getThis());

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
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
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

    // This sets up the parallel task construct.
    // Caveat: We create separate taskClasses for every task
    public String delegate(Expr x, String result, String init, List<VarCodeGen> freeVars) {

        String className = NamingCzar.gensymTaskName(packageAndClassName);

        debug("delegate creating class ", className, " node = ", x,
              " constructor type = ", init, " result type = ", result);

        // Create a new environment, and codegen task class in it.
        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cg.cw.visitSource(className,null);

        cg.lexEnv = cg.createTaskLexEnvVariables(className, freeVars);
        // WARNING: result may need mangling / NamingCzar-ing.
        cg.cw.visitField(Opcodes.ACC_PUBLIC, "result", result, null, null);

        cg.cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL,
                    className, null, NamingCzar.fortressBaseTask, null);

        cg.generateTaskInit(init, freeVars);

        cg.generateTaskCompute(className, x, result);

        cg.dumpClass(className);

        this.lexEnv = restoreFromTaskLexEnv(cg.lexEnv, this.lexEnv);
        return className;
    }

    // Evaluate args in parallel.  (Why is profitability given at a
    // level where we can't ask the qustion here?)
    // Leave the results (in the order given) on the stack.
    public void forExprsParallel(List<? extends Expr> args) {
        final int n = args.size();
        if (n <= 0) return;
        String [] tasks = new String[n];
        String [] results = new String[n];
        int [] taskVars = new int[n];
        // TODO: fix free variable handling here to match delegate() above.

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
            // And their types
            List<Type> freeVarTypes = new ArrayList(freeVars.size());
            for (VarCodeGen v : freeVars) {
                freeVarTypes.add(v.fortressType);
            }

            // Generate descriptor for init method of task
            String init =
                NamingCzar.jvmTypeDescForGeneratedTaskInit(freeVarTypes, component.getName());

            String task = delegate(arg, tDesc, init, freeVars);
            tasks[i] = task;
            results[i] = tDesc;

            mv.visitTypeInsn(Opcodes.NEW, task);
            mv.visitInsn(Opcodes.DUP);
            // Push the free variables in order.
            for (VarCodeGen v : freeVars) {
                v.pushValue(mv);
            }
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, task, "<init>", init);
            mv.visitInsn(Opcodes.DUP);
            int taskVar = mv.createCompilerLocal(Naming.mangleIdentifier(task), "L"+task+";");
            taskVars[i] = taskVar;
            mv.visitVarInsn(Opcodes.ASTORE, taskVar);
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, task, "forkIfProfitable", "()V");
        }
        // arg 0 gets compiled in place, rather than turned into work.
        args.get(0).accept(this);
        // join / perform work locally left to right, leaving results on stack.
        for (int i = 1; i < n; i++) {
            int taskVar = taskVars[i];
            mv.visitVarInsn(Opcodes.ALOAD, taskVar);
            mv.disposeCompilerLocal(taskVar);
            mv.visitInsn(Opcodes.DUP);
            String task = tasks[i];
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, task, "joinOrRun", "()V");
            mv.visitFieldInsn(Opcodes.GETFIELD, task, "result", results[i]);
        }
    }

    public void forOpExpr(OpExpr x) {
        debug("forOpExpr ", x, " op = ", x.getOp(),
                     " of class ", x.getOp().getClass(),  " args = ", x.getArgs());
        FunctionalRef op = x.getOp();
        List<Expr> args = x.getArgs();

        if (pa.worthParallelizing(x)) {
            forExprsParallel(args);
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
              " varRef = ", id.getText());

        addLineNumberInfo(x);
        mv.visitFieldInsn(Opcodes.GETSTATIC, NamingCzar.jvmClassForSymbol(id) ,
                          // "default_" + // NO, not necessary.
                          id.getText(),
                          "L" + NamingCzar.makeInnerClassName(id) + ";");

        for (Expr e : subs) {
            debug("calling accept on ", e);
            e.accept(this);
        }
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           NamingCzar.makeInnerClassName(id),
                           Naming.mangleIdentifier(op.getText()),
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
        if (classFile.equals("fortress/AnyType$Any")) {
            superInterfaces = new String[0];
        }
        CodeGenClassWriter prev = cw;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(classFile, null);
        cw.visit( Opcodes.V1_5,
                  Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                  classFile, null, NamingCzar.internalObject, superInterfaces);
        dumpSigs(header.getDecls());
        dumpClass( classFile );
        // Now lets do the springboard inner class that implements this interface.
        String springBoardClass = classFile + NamingCzar.springBoard;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(springBoardClass, null);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, springBoardClass,
                 null, NamingCzar.internalObject, new String[0] );
        debug("Start writing springboard class ",
              springBoardClass);
        generateFieldsAndInitMethod(springBoardClass, NamingCzar.internalObject, Collections.<Param>emptyList());
        debug("Finished init method ", springBoardClass);
        dumpTraitDecls(header.getDecls());
        debug("Finished dumpDecls ", springBoardClass);
        dumpClass(springBoardClass);
        // Now lets dump out the functional methods at top level.
        cw = prev;
        cw.visitSource(classFile, null);

        emittingFunctionalMethodWrappers = true;
        dumpTraitDecls(header.getDecls());
        emittingFunctionalMethodWrappers = false;

        debug("Finished dumpDecls for parent");
        inATrait = false;
    }

    public void forVarRef(VarRef v) {
        debug("forVarRef ", v, " which had better be local (for the moment)");
        if (v.getStaticArgs().size() > 0) {
            sayWhat(v,"varRef with static args!  That requires non-local VarRefs");
        }
        VarCodeGen vcg = getLocalVar(v.getVarId());
        vcg.pushValue(mv);
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        debug("forVoidLiteral ", x);
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                           NamingCzar.makeMethodDesc("", NamingCzar.descFortressVoid));

    }

    public void forMethodInvocation(MethodInvocation x) {
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
            // TODO call this an error
            domain_type = exprType(arg);
            range_type = exprType(x);
        }

        int savedParamCount = paramCount;
        try {
            // put object on stack
            obj.accept(this);
            // put args on stack
            evalArg(arg);
            String methodClass = NamingCzar.only.jvmTypeDesc(exprType(obj), thisApi(), false);
            String sig = NamingCzar.only.jvmSignatureFor(
                    domain_type,
                    range_type,
                    thisApi()
                    );
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodClass, method.getText(), sig);

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

        return exprType.unwrap();
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

    public void for_RewriteFnApp(_RewriteFnApp x) {
        debug("for_RewriteFnApp ", x,
                     " args = ", x.getArgument(), " function = ", x.getFunction());
        // This is a little weird.  If a function takes no arguments the parser gives me a void literal expr
        // however I don't want to be putting a void literal on the stack because it gets in the way.
        int savedParamCount = paramCount;
        boolean savedFnRefIsApply = fnRefIsApply;
        try {
            Expr arg = x.getArgument();
            fnRefIsApply = false;
            evalArg(arg);
            fnRefIsApply = true;
            x.getFunction().accept(this);
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
                    OverloadSet.TaggedFunctionName tagged_f = new OverloadSet.TaggedFunctionName(apiname, f);
                    byCount.putItem(f.parameters().size(), tagged_f);
                }
            }
        }

        for (Map.Entry<Integer, Set<OverloadSet.TaggedFunctionName>> entry : byCount
                .entrySet()) {
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
    private void generateTopLevelOverloads() {

        for (Map.Entry<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> entry1 : topLevelOverloads.entrySet()) {
            IdOrOpOrAnonymousName  name = entry1.getKey();
            MultiMap<Integer, Function> partitionedByArgCount = entry1.getValue();

            for (Map.Entry<Integer, Set<Function>> entry : partitionedByArgCount
                    .entrySet()) {
               int i = entry.getKey();
               Set<Function> fs = entry.getValue();

               OverloadSet os =
                   new OverloadSet.Local(thisApi(), name,
                                         ta, fs, i);

               os.split(true);

               String s = name.stringName();

               os.generateAnOverloadDefinition(s, cw);

               for (Map.Entry<String, OverloadSet> o_entry : os.getOverloadSubsets().entrySet()) {
                   String ss = o_entry.getKey();
                   ss = s + ss;
                   overloadedNamesAndSigs.add(ss);
               }
           }
        }
    }

    Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>>
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
            IdOrOpOrAnonymousName xname = h.getName();
            IdOrOp name = (IdOrOp) xname;
            String desc = NamingCzar.jvmSignatureFor(f,component.getName());
            debug("about to call visitMethod with", name.getText(),
                  " and desc ", desc);
            mv = cw.visitMethod(Opcodes.ACC_ABSTRACT + Opcodes.ACC_PUBLIC,
                                Naming.mangleIdentifier(name.getText()), desc, null, null);
            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
        }
    }
}
