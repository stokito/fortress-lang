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
import com.sun.fortress.useful.BA2Tree;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.MultiMap;
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
    Symbols symbols;
    boolean inATrait = false;
    boolean inAnObject = false;
    boolean inABlock = false;
    int localsDepth = 0;
    final Component component;
    private final ComponentIndex ci;

    public CodeGen(Component c, Symbols s, TypeAnalyzer ta, ParallelismAnalyzer pa, ComponentIndex ci) {
        component = c;
        packageAndClassName = NamingCzar.javaPackageClassForApi(c.getName().getText(), "/").toString();
        aliasTable = new HashMap<String, String>();
        symbols = s;
        this.ta = ta;
        this.pa = pa;
        this.ci = ci;
        this.topLevelOverloads =
            sizePartitionedOverloads(ci.functions());
        this.overloadedNamesAndSigs = new HashSet<String>();
        this.singletonObjects = new ArrayList<ObjectDecl>();
        this.lexEnv = new BATree<String,VarCodeGen>(StringHashComparer.V);
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
        this.symbols = c.symbols;
        this.inATrait = c.inATrait;
        this.inAnObject = c.inAnObject;
        this.inABlock = c.inABlock;
        this.localsDepth = c.localsDepth;
        this.ta = c.ta;
        this.pa = c.pa;
        this.ci = c.ci;
        this.topLevelOverloads = c.topLevelOverloads;
        this.overloadedNamesAndSigs = c.overloadedNamesAndSigs;
        this.singletonObjects = c.singletonObjects;
        this.lexEnv = new BATree<String,VarCodeGen>(c.lexEnv);
        
    }

    private APIName thisApi() {
        return ci.ast().getName();
    }

    private BATree<String, VarCodeGen> createTaskLexEnvVariables(String taskClass,
                                                                 BATree<String, VarCodeGen> old) {
        BATree<String, VarCodeGen> result = new BATree<String, VarCodeGen>(StringHashComparer.V);
        for (Map.Entry<String, VarCodeGen> entry : old.entrySet()) {

            cw.visitField(Opcodes.ACC_PUBLIC, entry.getKey(),
                          NamingCzar.only.boxedImplDesc(entry.getValue().fortressType, thisApi()),
                          null, null);

            TaskVarCodeGen tvcg = new TaskVarCodeGen(entry.getValue(), taskClass, thisApi());
            result.put(entry.getKey(), tvcg);
        }

        // FIXME result isn't always ZZ32
        cw.visitField(Opcodes.ACC_PUBLIC, "result", NamingCzar.descFortressZZ32,
                      null, null);
        return result;
    }

    private void initializeTaskLexEnv(String task, BATree<String, VarCodeGen> old) {
        mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("instance"));

        mv.visitVarInsn(Opcodes.ASTORE, mv.createLocalVariable(task));

        for (Map.Entry<String, VarCodeGen> entry : old.entrySet()) {
            mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("instance"));
            entry.getValue().pushValue(mv);

            mv.visitFieldInsn(Opcodes.PUTFIELD,
                              task, entry.getKey(),
                              NamingCzar.only.boxedImplDesc(entry.getValue().fortressType, thisApi()));

        }
    }


    // I'm just a stub.  Someday I'll have a body that updates the changed local variables.
    private BATree<String, VarCodeGen> restoreFromTaskLexEnv(BATree<String,VarCodeGen> old, BATree<String,VarCodeGen> task) {
        return task;
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

    private void generateInitMethod(List<Param> params) {
        String init_sig = NamingCzar.only.jvmSignatureFor(params, "V", thisApi());
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", init_sig, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, NamingCzar.internalObject, "<init>", NamingCzar.voidToVoid);
        // TODO initialize fields here.
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }


    private void cgWithNestedScope(ASTNode n) {
        CodeGen cg = new CodeGen(this);
        n.accept(cg);
        this.localsDepth = cg.localsDepth;
    }

    private void addLocalVar( VarCodeGen v ) {
        debug("addLocalVar " + v);
        lexEnv.put(v.name.getText(), v);
        localsDepth += v.sizeOnStack;
    }

    private void addStaticVar( VarCodeGen v ) {
        debug("addLocalVar " + v);
        lexEnv.put(v.name.getText(), v);
    }

    private VarCodeGen addParam(Param p) {
        VarCodeGen v =
            new VarCodeGen.ParamVar(p.getName(), p.getIdType().unwrap(),
                                    localsDepth);
        addLocalVar(v);
        return v;
    }

    private VarCodeGen getLocalVar( IdOrOp nm ) {
        debug("getLocalVar: " + nm);
        VarCodeGen r = lexEnv.get(nm.getText());
        debug("getLocalVar:" + nm + " VarCodeGen = " + r + " of class " + r.getClass());
        if (r==null) return sayWhat(nm, "Can't find lexEnv mapping for local var");
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

    private void dumpDecls(List<Decl> decls) {
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
    private void callStaticSingleOrOverloaded(FnRef x,
            com.sun.fortress.nodes.Type arrow, String pkgAndClassName,
            String methodName) {

        debug("class = " + pkgAndClassName + " method = " + methodName );

        if ( arrow instanceof ArrowType ) {
            addLineNumberInfo(x);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, pkgAndClassName,
                               methodName, NamingCzar.jvmTypeDesc(arrow, component.getName()));
        } else if (arrow instanceof IntersectionType) {
            addLineNumberInfo(x);
            IntersectionType it = (IntersectionType) arrow;
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, pkgAndClassName,
                               OverloadSet.actuallyOverloaded(it, paramCount) ?
                               OverloadSet.oMangle(methodName) :methodName,
                               OverloadSet.getSignature(it, paramCount, ta));
        } else {
                sayWhat( x, "Neither arrow nor intersection type: " + arrow );
        }
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
    private <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }

    private void debug(Object... message){
        Debug.debug(Debug.Type.CODEGEN,1,message);
    }

    public void defaultCase(ASTNode x) {
        System.out.println("defaultCase: " + x + " of class " + x.getClass());
        sayWhat(x);
    }

    public void forBlock(Block x) {
        boolean oldInABlock = inABlock;
        inABlock = true;
        int onStack = 0;
        debug("forBlock ", x);
        for ( Expr e : x.getExprs() ) {
            popAll(onStack);
            e.accept(this);
            onStack = 1;
            // TODO: can we have multiple values on stack in future?
            // Whither primitive types?
            // May require some tracking of abstract stack state.
            // For now we always have 1 pointer on stack and this doesn't
            // matter.
        }
        inABlock=oldInABlock;
    }
    public void forChainExpr(ChainExpr x) {
        debug( "forChainExpr" + x);
        Expr first = x.getFirst();
        List<Link> links = x.getLinks();
        debug( "forChainExpr" + x + " about to call accept on " +
               first + " of class " + first.getClass());
        first.accept(this);
        Iterator<Link> i = links.iterator();
        if (links.size() != 1)
            throw new CompilerError(NodeUtil.getSpan(x), x + "links.size != 1");
        Link link = i.next();
        link.getExpr().accept(this);
        debug( "forChainExpr" + x + " about to call accept on " +
               link.getOp() + " of class " + link.getOp().getClass());
        link.getOp().accept(this);

        debug( "We've got a link " + link + " an op " + link.getOp() +
               " and an expr " + link.getExpr() + " What do we do now");
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
        generateInitMethod(Collections.<Param>emptyList());

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
        debug("forDo ", x);
        int onStack = 0;
        for ( Block b : x.getFronts() ) {
            popAll(onStack);
            b.accept(this);
            onStack = 1;
        }
    }

    public void forFnDecl(FnDecl x) {
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

        for (Param p : params) {
            debug("iterating params looking for self : param = ", p);
            if (p.getName().getText() == "self") {
                functionalMethod = true;
                break;
            }
        }

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
        cg.localsDepth = 0;

        if (!functionalMethod && (inAnObject || inATrait)) {
            // TODO: Add proper type information here based on the
            // enclosing trait/object decl.  For now we can get away
            // with just stashing a null as we're not using it to
            // determine stack sizing or anything similarly crucial.
            cg.addLocalVar(new VarCodeGen.SelfVar(NodeUtil.getSpan(name), null));
        } else {
            // Top-level function or functional method
            // DO NOT special case run() here and make it non-static (that used to happen),
            // as that's wrong.  It's addressed in the executable wrapper code instead.
            modifiers += Opcodes.ACC_STATIC;
        }

        String sig = NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                                                returnType.unwrap(),
                                                component.getName());

        String mname = nonCollidingSingleName(name, sig);

        cg.mv = cw.visitMethod(modifiers, mname, sig,
                               null, null);
        cg.mv.visitCode();

        // Now inside method body.  Generate code for the method body.
        for (Param p : params) {
            VarCodeGen v = cg.addParam(p);
            // v.pushValue(cg.mv);
        }
        body.unwrap().accept(cg);

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

        }

        Option<IdOrOp> iun = x.getImplementsUnambiguousName();

        if (iun.isSome()) {

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
        String mname = NamingCzar.mangleIdentifier(nameString);
        if (overloadedNamesAndSigs.contains(mname+sig)) {
            mname = NamingCzar.only.mangleAwayFromOverload(mname);
        }
        return mname;
    }


    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        debug("forFnRef ", x);
        String name = x.getOriginalName().getText();
        Option<com.sun.fortress.nodes.Type> type = x.getInfo().getExprType();
        if ( type.isNone() ) {
            sayWhat( x, "The type of this FnRef is not inferred." );
            return;
        }
        /* Arrow, or perhaps an intersection if it is an overloaded function. */
        com.sun.fortress.nodes.Type arrow = type.unwrap();

        List<IdOrOp> names = x.getNames();

        /* Note that after pre-processing in the overload rewriter,
         * there is only one name here; this is not an overload check.
         */
        if ( names.size() != 1) {
            sayWhat(x,"Non-unique overloading after rewrite " + x);
            return;
        }
        IdOrOp fnName = names.get(0);
        Option<APIName> apiName = fnName.getApiName();
        String calleePackageAndClass;
        String method;
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
            return;             // Doesn't init callee... and method.
        }
        callStaticSingleOrOverloaded(x, arrow, calleePackageAndClass, method);
    }

    public void forIf(If x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forIf " + x);
        List<IfClause> clauses = x.getClauses();
        Option<Block> elseClause = x.getElseClause();
        if (clauses.size() > 1) throw new CompilerError(NodeUtil.getSpan(x), "Don't know how to compile multiple if clauses yet");

        org.objectweb.asm.Label action = new org.objectweb.asm.Label();
        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        IfClause ifclause = clauses.get(0);
        GeneratorClause cond = ifclause.getTestClause();

        if (!cond.getBind().isEmpty())
            sayWhat(x, "Undesugared generalized if expression.");

        Expr testExpr = cond.getInit();
        debug( "about to accept " + testExpr + " of class " + testExpr.getClass());
        testExpr.accept(this);
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NamingCzar.internalFortressBoolean, "getValue",
                           NamingCzar.makeMethodDesc("", NamingCzar.descBoolean));

        mv.visitJumpInsn(Opcodes.IFNE, action);
        Option<Block> maybe_else = x.getElseClause();
        if (maybe_else.isSome()) {
            maybe_else.unwrap().accept(this);
        }
        mv.visitJumpInsn(Opcodes.GOTO, done);
        mv.visitLabel(action);
        ifclause.getBody().accept(this);
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

        generateInitMethod(params);
        
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
                    NamingCzar.jvmTypeDesc(param_type, component.getName(), false)));
        }
                
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


    // This sets up the parallel task construct.
    // Caveats: We assume that everything has a ZZ32 result
    //          We create separate taskClasses for every task
    public String delegate(ASTNode x) {
        
        // For now we only delegate function applications.
        if (! (x instanceof _RewriteFnApp)) {
            sayWhat(x);
        }

        _RewriteFnApp _rewriteFnApp = (_RewriteFnApp) x;

        Expr function = _rewriteFnApp.getFunction();
        ExprInfo info = function.getInfo();
        Option<Type> exprType = info.getExprType();


        if (!exprType.isSome()) {
            sayWhat(x, "Missing type information for delegate " + x);
        }

        String desc = NamingCzar.jvmTypeDesc(exprType.unwrap(), component.getName());
        List<String> args = CodeGenMethodVisitor.parseArgs(desc);
        String result = CodeGenMethodVisitor.parseResult(desc);

        String className = NamingCzar.gensymTaskName(packageAndClassName);
        debug("delegate creating class " + className + " node = " + x);
        // Create a new environment
        CodeGen cg = new CodeGen(this);
        cg.localsDepth = 0;
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES);
        cg.cw.visitSource(className,null);

        cg.lexEnv = cg.createTaskLexEnvVariables(className, lexEnv);
        cg.cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER + Opcodes.ACC_FINAL,
                    className,null, NamingCzar.fortressBaseTask, null);

        String initDesc = "(";

        for (String arg : args) {
            initDesc = initDesc + arg;
        }
        initDesc = initDesc + ")V";

        debug("initDesc = " + initDesc + "should be (ZZ32)V");

        cg.mv = cg.cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", initDesc, null, null);
        cg.mv.visitCode();

        cg.mv.visitVarInsn(Opcodes.ALOAD, cg.mv.getLocalVariable("instance"));
        cg.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, NamingCzar.fortressBaseTask,
                              "<init>", NamingCzar.voidToVoid);
        cg.mv.visitVarInsn(Opcodes.ALOAD, cg.mv.getLocalVariable("instance"));


        // Arguments

        if (! (function instanceof FnRef) )
            sayWhat(x, "Looking for a FnRef");

        FnRef fnRef = (FnRef) function;
        IdOrOp originalName = fnRef.getOriginalName();        
            
        Function f = symbols.lookupFunctionInComponent(originalName,ci);
        List<Param> params = f.parameters();
        
        System.out.println("LOOKYHERE: f = " + f + " params = " + params);

        int argsIndex = 0;
        int varIndex = 1;
        for (Param p : params) {
            cg.mv.visitVarInsn(Opcodes.ALOAD, varIndex++);
            cg.mv.visitFieldInsn(Opcodes.PUTFIELD, className, p.getName().getText(), args.get(argsIndex++));
        }

        cg.mv.visitInsn(Opcodes.RETURN);
        cg.mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        cg.mv.visitEnd();

        cg.mv = cg.cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                                  "compute", "()V", null, null);

        cg.mv.visitCode();

        x.accept(cg);

        cg.mv.visitVarInsn(Opcodes.ASTORE, cg.mv.createLocalVariable("taskResult"));
        cg.mv.visitVarInsn(Opcodes.ALOAD, cg.mv.getLocalVariable("instance"));
        cg.mv.visitVarInsn(Opcodes.ALOAD, cg.mv.getLocalVariable("taskResult"));

        // Fixme, not all results are ZZ32
        cg.mv.visitFieldInsn(Opcodes.PUTFIELD, className, "result", result);

        cg.mv.visitInsn(Opcodes.RETURN);
        cg.mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        cg.mv.visitEnd();
        cg.dumpClass(className);

        this.lexEnv = restoreFromTaskLexEnv(cg.lexEnv, this.lexEnv);
        return className;
    }

    private void generatePrintResult(CodeGen cg) {
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NamingCzar.internalFortressZZ32,
                           "getValue", "()I");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,"com/sun/fortress/nativeHelpers/simplePrintResult",
                           "nativePrintZZ32",  "(I)V");
    }

    public void forOpExpr(OpExpr x) {
        debug("forOpExpr ", x, " op = ", x.getOp(),
                     " of class ", x.getOp().getClass(),  " args = ", x.getArgs());
        FunctionalRef op = x.getOp();
        List<Expr> args = x.getArgs();
        List<String> tasks = new ArrayList<String>(args.size());

        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
        mv.visitVarInsn(Opcodes.ASTORE, mv.createLocalVariable("TaskArray", "java/util/ArrayList"));

        if (pa.worthParallelizing(x)) {
            for (int i = 0; i < args.size(); i++) {
                String task = delegate(args.get(i));
                tasks.add(i, task);

                mv.visitTypeInsn(Opcodes.NEW, task);
                mv.visitInsn(Opcodes.DUP);
                mv.visitVarInsn(Opcodes.ALOAD, 0);   // This is bogus too, knowing that n is in 0
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, task, "<init>", "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)V");
                mv.visitVarInsn(Opcodes.ASTORE, mv.createLocalVariable(task));

                mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("TaskArray"));
                mv.visitLdcInsn(i);
                mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable(task));
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "add","(ILjava/lang/Object;)V");
            }

            mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable("TaskArray"));
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, tasks.get(0), "invokeAll", "(Ljava/util/Collection;)V");

            for (int i = 0; i < args.size(); i++) {
                mv.visitVarInsn(Opcodes.ALOAD, mv.getLocalVariable(tasks.get(i)));
                mv.visitFieldInsn(Opcodes.GETFIELD, tasks.get(i), "result", NamingCzar.descFortressZZ32);
            }
        } else {

            for (Expr arg : args) {
                arg.accept(this);
            }
        }

        op.accept(this);

    }

    public void forOpRef(OpRef x) {
        ExprInfo info = x.getInfo();
        Option<com.sun.fortress.nodes.Type> exprType = info.getExprType();
        List<StaticArg> staticArgs = x.getStaticArgs();
        int lexicalDepth = x.getLexicalDepth();
        IdOrOp originalName = x.getOriginalName();
        List<IdOrOp> names = x.getNames();
        Option<List<FunctionalRef>> overloadings = x.getOverloadings();
        Option<com.sun.fortress.nodes.Type> overloadingType = x.getOverloadingType();
        debug("forOpRef ", x,
                     " info = ", info, "staticArgs = ", staticArgs, " exprType = ", exprType,
                     " lexicalDepth = ", lexicalDepth, " originalName = ", originalName,
                     " overloadings = ", overloadings,
                     " overloadingType = ", overloadingType, " names = ", names);

        boolean canCompile =
            x.getStaticArgs().isEmpty() &&
            x.getOverloadings().isNone() &&
            names.size() == 1;

        if (!canCompile) {
            debug("forOpRef can't compile; staticArgs ",
                        x.getStaticArgs().isEmpty(), " overloadings ",
                        x.getOverloadings().isNone());
        }
        if (!exprType.isSome()) {
            sayWhat(x, "Missing type information for OpRef " + x);
        }

        String name = NamingCzar.mangleIdentifier(originalName.getText());
        IdOrOp newName = names.get(0);
        Option<APIName> api = newName.getApiName();

        addLineNumberInfo(x);

        String jvmClassName;
        String jvmFunctionName;

        if (api.isSome()) {
            APIName apiName = api.unwrap();
            debug("forOpRef name = ", name,
                  " api = ", apiName);
            jvmClassName =  NamingCzar.jvmClassForSymbol(newName);
            jvmFunctionName = NamingCzar.mangleIdentifier(newName.getText());
        } else {
            jvmClassName = packageAndClassName;
            jvmFunctionName = NamingCzar.mangleIdentifier(name);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, jvmClassName, jvmFunctionName,
                           NamingCzar.jvmTypeDesc(exprType.unwrap(), component.getName()));
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
                           NamingCzar.mangleIdentifier(op.getText()),
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
        generateInitMethod(Collections.<Param>emptyList());
        debug("Finished init method ", springBoardClass);
        dumpDecls(header.getDecls());
        debug("Finished dumpDecls ", springBoardClass);
        dumpClass(springBoardClass);
        // Now lets dump out the functional methods at top level.
        cw = prev;
        cw.visitSource(classFile, null);
        dumpDecls(header.getDecls());
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

    public void for_RewriteFnApp(_RewriteFnApp x) {
        debug("for_RewriteFnApp ", x,
                     " args = ", x.getArgument(), " function = ", x.getFunction());
        // This is a little weird.  If a function takes no arguments the parser gives me a void literal expr
        // however I don't want to be putting a void literal on the stack because it gets in the way.
        int savedParamCount = paramCount;
        try {
            Expr arg = x.getArgument();
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
            x.getFunction().accept(this);
        } finally {
            paramCount = savedParamCount;
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
                apiname = fnapi.unwrap();
                ApiIndex ai = symbols.apis.get(apiname);
                IdOrOp fnnoapi = NodeFactory.makeLocalIdOrOp(fn);
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
                                NamingCzar.mangleIdentifier(name.getText()), desc, null, null);
            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
        }
    }
}
