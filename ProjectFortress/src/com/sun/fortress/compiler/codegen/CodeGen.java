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
    ClassWriter cw;
    MethodVisitor mv;
    final String packageAndClassName;
    private final HashMap<String, String> aliasTable;
    private final TypeAnalyzer ta;
    private final Map<IdOrOpOrAnonymousName, MultiMap<Integer, Function>> topLevelOverloads;
    private HashSet<String> overloadedNamesAndSigs = new HashSet<String>();

    // lexEnv does not include the top level or object right now, just
    // args and local vars.  Object fields should have been translated
    // to dotted notation at this point, right?  Right?
    BATree<String, VarCodeGen> lexEnv = null;
    Symbols symbols;
    boolean inATrait = false;
    boolean inAnObject = false;
    boolean inABlock = false;
    int localsDepth = 0;
    Component component;
    private final ComponentIndex ci;



    private void generateMainMethod() {

        // We generate two methods.  First a springboard method that creates an
        // instance of the class we are generating, and then the real main method
        // which takes the instance and uses it to pass to the primordial task.

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            NamingCzar.stringArrayToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/sun/fortress/nativeHelpers/systemHelper",
                           "registerArgs", NamingCzar.stringArrayToVoid);

        mv.visitTypeInsn(Opcodes.NEW, packageAndClassName);
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, packageAndClassName, "<init>", "()V");

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, packageAndClassName, "main",
                           "(Lcom/sun/fortress/runtimeSystem/FortressComponent;)V");

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore,NamingCzar.ignore);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            "(Lcom/sun/fortress/runtimeSystem/FortressComponent;)V",
                            null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.primordialTask, "startFortress",
                           "(Lcom/sun/fortress/runtimeSystem/FortressComponent;)Lcom/sun/fortress/runtimeSystem/PrimordialTask;");

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore,NamingCzar.ignore);
        mv.visitEnd();
    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", NamingCzar.voidToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, NamingCzar.internalObject, "<init>", NamingCzar.voidToVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
        mv.visitEnd();
    }

    public CodeGen(Component c, Symbols s, TypeAnalyzer ta, ComponentIndex ci) {
        component = c;
        packageAndClassName = NamingCzar.javaPackageClassForApi(c.getName().getText(), "/").toString();
        aliasTable = new HashMap<String, String>();
        symbols = s;
        this.ta = ta;
        this.ci = ci;
        this.topLevelOverloads = 
            sizePartitionedOverloads(ci.functions());
        
        debug( "Compile: Compiling ", packageAndClassName );
    }

    // Create a fresh codegen object for a nested scope.  Technically,
    // we ought to be able to get by with a single lexEnv, because
    // variables ought to be unique by location etc.  But in practice
    // I'm not assuming we have a unique handle for any variable,
    // so we get a fresh CodeGen for each scope to avoid collisions.
    private CodeGen(CodeGen c) {
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
        this.ci = c.ci;
        this.topLevelOverloads = c.topLevelOverloads;
        if (c.lexEnv == null) {
            this.lexEnv = new BATree<String,VarCodeGen>(StringHashComparer.V);
        } else {
            this.lexEnv = new BATree<String,VarCodeGen>(c.lexEnv);
        }
    }

    private void cgWithNestedScope(ASTNode n) {
        CodeGen cg = new CodeGen(this);
        n.accept(cg);
        this.localsDepth = cg.localsDepth;
    }

    private void addLocalVar( VarCodeGen v ) {
        lexEnv.put(v.name.getText(), v);
        localsDepth += v.sizeOnStack;
    }

    private VarCodeGen addParam(Param p) {
        VarCodeGen v =
            new VarCodeGen.ParamVar(p.getName(), p.getIdType().unwrap(),
                                    localsDepth);
        addLocalVar(v);
        return v;
    }

    private VarCodeGen getLocalVar( IdOrOp nm ) {
        VarCodeGen r = lexEnv.get(nm.getText());
        if (r==null) return sayWhat(nm, "Can't find lexEnv mapping for local var");
        return r;
    }

    public void dumpClass( String file ) {
        PrintWriter pw = new PrintWriter(System.out);
        cw.visitEnd();

        if (ProjectProperties.getBoolean("fortress.bytecode.verify", false))
            CheckClassAdapter.verify(new ClassReader(cw.toByteArray()), true, pw);

        ByteCodeWriter.writeClass(NamingCzar.cache, file, cw.toByteArray());
        debug( "Writing class ", file);
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

    public void forComponent(Component x) {
        debug("forComponent ",x.getName(),NodeUtil.getSpan(x));
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES );
        cw.visitSource(packageAndClassName, null);
        boolean exportsExecutable = false;
        boolean exportsDefaultLibrary = false;

        for ( APIName export : x.getExports() ) {
            if ( WellKnownNames.exportsMain(export.getText()) )
                exportsExecutable = true;
            if ( WellKnownNames.exportsDefaultLibrary(export.getText()) )
                exportsDefaultLibrary = true;
        }

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 packageAndClassName, null, NamingCzar.fortressComponent,
                 null);

        // Always generate the init method
        generateInitMethod();

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
        
        for ( Decl d : x.getDecls() ) {
            d.accept(this);
        }
        
        

        dumpClass( packageAndClassName );
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
                   new OverloadSet.Local(ci.ast().getName(), name,
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

    public void forDecl(Decl x) {
        sayWhat(x, "forDecl");
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
            String desc = Naming.generateTypeDescriptor(f);
            debug("about to call visitMethod with", name.getText(),
                  " and desc ", desc);
            mv = cw.visitMethod(Opcodes.ACC_ABSTRACT + Opcodes.ACC_PUBLIC,
                                NamingCzar.mangleIdentifier(name.getText()), desc, null, null);
            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
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
            header.getMods().isEmpty() && // no modifiers
            extendsC.size() <= 1;
        debug("forTraitDecl", x,
                    " decls = ", header.getDecls(), " extends = ", extendsC);
        if ( !canCompile ) sayWhat(x);
        inATrait = true;
        String [] superInterfaces = NamingCzar.extendsClauseToInterfaces(extendsC);

        // First let's do the interface class
        String classFile = Naming.makeClassName(packageAndClassName, x);
        if (classFile.equals("fortress/AnyType$Any")) {
            superInterfaces = new String[0];
        }
        ClassWriter prev = cw;
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visitSource(classFile, null);
        cw.visit( Opcodes.V1_5,
                  Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                  classFile, null, NamingCzar.internalObject, superInterfaces);
        dumpSigs(header.getDecls());
        dumpClass( classFile );
        // Now lets do the springboard inner class that implements this interface.
        String springBoardClass = classFile + NamingCzar.springBoard;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(springBoardClass, null);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, springBoardClass,
                 null, NamingCzar.internalObject, new String[0] );
        debug("Start writing springboard class ",
              springBoardClass);
        generateInitMethod();
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

    public void forObjectDecl(ObjectDecl x) {
        debug("forObjectDecl", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            x.getParams().isNone() &&             // no parameters
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            //            header.getDecls().isEmpty() &&        // no members
            header.getMods().isEmpty()         && // no modifiers
            ( extendsC.size() <= 1 ); // 0 or 1 super trait
        if ( !canCompile ) sayWhat(x);
        inAnObject = true;
        String [] superInterfaces = NamingCzar.extendsClauseToInterfaces(extendsC);

        if (!header.getDecls().isEmpty()) {
            debug("header.getDecls:", header.getDecls());

            cw.visitField(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
                          "default_args",
                          "LCompilerSystem$args;",
                          null,
                          null);

            mv = cw.visitMethod(Opcodes.ACC_STATIC,
                                "<clinit>",
                                "()V",
                                null,
                                null);

            mv.visitTypeInsn(Opcodes.NEW, "CompilerSystem$args");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "CompilerSystem$args", "<init>", NamingCzar.voidToVoid);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, "CompilerSystem", "default_args", "LCompilerSystem$args;");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(NamingCzar.ignore, NamingCzar.ignore);
            mv.visitEnd();
        }

        String classFile = Naming.makeClassName(packageAndClassName, x);
        ClassWriter prev = cw;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(classFile, null);
        // Until we resolve the directory hierarchy problem.
        //            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
        //                      classFile, null, NamingCzar.internalObject, new String[] { parent });
        cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
                  classFile, null, NamingCzar.internalObject, superInterfaces);

        generateInitMethod();

        for (Decl d : header.getDecls()) {
            d.accept(this);
            // If we have a singleton object we can create a default_xxx accessor for it.
            if (d instanceof ObjectDecl) {
            }
        }
        dumpClass( classFile );
        cw = prev;
        inAnObject = false;
    }

    public void forOpExpr(OpExpr x) {
        debug("forOpExpr ", x, " op = ", x.getOp(),
                     " of class ", x.getOp().getClass(),  " args = ", x.getArgs());
        FunctionalRef op = x.getOp();

        List<Expr> args = x.getArgs();
        for (Expr arg : args) {
            arg.accept(this);
        }
        op.accept(this);
    }

    private void addLineNumberInfo(ASTNode x) {
        addLineNumberInfo(mv, x);
    }

    private void addLineNumberInfo(MethodVisitor m, ASTNode x) {
        org.objectweb.asm.Label bogus_label = new org.objectweb.asm.Label();
        m.visitLabel(bogus_label);
        Span span = NodeUtil.getSpan(x);
        SourceLoc begin = span.getBegin();
        SourceLoc end = span.getEnd();
        String fileName = span.getFileName();
        m.visitLineNumber(begin.getLine(), bogus_label);
    }

    public static String jvmSignatureFor(Function f) {
        String sig;
        List<Param> params = f.parameters();
        sig = "(";
        for (Param p : params ) {                
            Type ty = p.getIdType().unwrap();
            String toType = NamingCzar.only.boxedImplDesc(ty);
            sig += toType;
        }
        sig += ")";
        sig += NamingCzar.only.boxedImplDesc(f.getReturnType());
        return sig;
    }
    
    public void forOpRef(OpRef x) {
        debug("forOpRef " + x );
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

        if (canCompile) {
            String name = NamingCzar.mangleIdentifier(originalName.getText());
            IdOrOp newName = names.get(0);
            Option<APIName> api = newName.getApiName();

            addLineNumberInfo(x);

            if (api.isSome()) {
                APIName apiName = api.unwrap();
                debug("forOpRef name = ", name,
                             " api = ", apiName);
                if (exprType.isSome())
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.getJavaClassForSymbol(newName),
                                       NamingCzar.mangleIdentifier(newName.getText()),
                                       Naming.emitDesc(exprType.unwrap()));
                else
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                       NamingCzar.fortressPackage + "/" + api.unwrap().getText(),
                                       NamingCzar.mangleIdentifier(newName.getText()),
                                       symbols.getTypeSignatureForIdOrOp(newName, component));

            } else {
                if (exprType.isSome())
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, packageAndClassName, NamingCzar.mangleIdentifier(name),
                                       Naming.emitDesc(exprType.unwrap()));
                else
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, packageAndClassName,
                                       NamingCzar.mangleIdentifier(name), symbols.getTypeSignatureForIdOrOp(newName, component));
            }
        } else
            debug("forOpRef can't compile staticArgs ",
                        x.getStaticArgs().isEmpty(), " overloadings ",
                        x.getOverloadings().isNone());

    }

    public void for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl x) {
        /* Note for refactoring -- this code does it the "right" way.
         * And also, this code NEEDS refactoring.
         */
        List<IdOrOp> fns = x.getFns();
        IdOrOp name = x.getName();
        Option<com.sun.fortress.nodes.Type> ot = x.getType();
        Relation<IdOrOpOrAnonymousName, Function> fnrl = ci.functions();

        MultiMap<Integer, OverloadSet.TaggedFunctionName> byCount =
            new MultiMap<Integer,OverloadSet.TaggedFunctionName>();

        for (IdOrOp fn : fns) {

            Option<APIName> fnapi = fn.getApiName();
            PredicateSet<Function> set_of_f;
            APIName apiname;

            if (fnapi.isNone()) {
                apiname = ci.ast().getName();
                set_of_f = fnrl.matchFirst(fn);
            } else {
                apiname = fnapi.unwrap();
                ApiIndex ai = symbols.apis.get(apiname);
                IdOrOp fnnoapi = NodeFactory.makeLocalIdOrOp(fn);
                set_of_f = ai.functions().matchFirst(fnnoapi);
            }

            for (Function f : set_of_f) {
                OverloadSet.TaggedFunctionName tagged_f = new OverloadSet.TaggedFunctionName(apiname, f);
                byCount.putItem(f.parameters().size(), tagged_f);
            }

            for (Map.Entry<Integer, Set<OverloadSet.TaggedFunctionName>> entry : byCount
                    .entrySet()) {
                int i = entry.getKey();
                Set<OverloadSet.TaggedFunctionName> fs = entry.getValue();
                if (fs.size() > 1) {
                    OverloadSet os = new OverloadSet.AmongApis(name,
                            ta, fs, i);

                    os.split(false);
                    os.generateAnOverloadDefinition(name.stringName(), cw);

                }
            }
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

        for (Param p : params) {
            debug("iterating params looking for self : param = ", p);
            if (p.getName().getText() == "self")
                functionalMethod = true;
        }

        Option<com.sun.fortress.nodes.Type> returnType = header.getReturnType();

        // For now every Fortress entity is made public, with
        // namespace management happening in Fortress-land.  Right?
        int modifiers = Opcodes.ACC_PUBLIC;

        if ( body.isNone() )
            sayWhat(x, "Abstract function declarations are not supported.");
        if ( returnType.isNone() )
            sayWhat(x, "Return type is not inferred.");
        if ( !( name instanceof IdOrOp ))
            sayWhat(x, "Unhandled function name.");

        String nameString = ((IdOrOp)name).getText();
        if ( name instanceof Id ) {
            Id id = (Id) name;
            debug("forId ", id,
                  " class = ", Naming.getJavaClassForSymbol(id));
        } else if ( name instanceof Op ) {
            Op op = (Op) name;
            Fixity fixity = op.getFixity();
            boolean isEnclosing = op.isEnclosing();
            Option<APIName> maybe_apiName = op.getApiName();
            debug("forOp ", op, " fixity = ", fixity,
                  " isEnclosing = ", isEnclosing,
                  " class = ", Naming.getJavaClassForSymbol(op));
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
        } else if (!nameString.equals("run")) {
            // Top-level function or functional method
            modifiers += Opcodes.ACC_STATIC;
        }

        String mname = NamingCzar.mangleIdentifier(nameString);
        String sig = Naming.emitFnDeclDesc(NodeUtil.getParamType(x),
                returnType.unwrap());
        
        if (overloadedNamesAndSigs.contains(mname+sig)) {
            mname = NamingCzar.only.mangleAwayFromOverload(mname);
        }
        
        cg.mv = cw.visitMethod(modifiers,
                               mname,
                               sig,
                               null, null);

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

    private void popAll(int onStack) {
        if (onStack > 0) {
            for (; onStack > 1; onStack -= 2) {
                mv.visitInsn(Opcodes.POP2);
            }
            if (onStack==1) {
                mv.visitInsn(Opcodes.POP);
            }
        }
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

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        debug("forFnRef ", x);
        String name = x.getOriginalName().getText();
        Option<com.sun.fortress.nodes.Type> type = x.getInfo().getExprType();
        if ( type.isNone() ) {
            sayWhat( x, "The type of this expression is not inferred." );
        }
        /* Arrow, or perhaps an intersection if it is an overloaded function. */
        com.sun.fortress.nodes.Type arrow = type.unwrap();

        List<IdOrOp> names = x.getNames();

        /* Note that after pre-processing in the overload rewriter, there is
         * only one name here; this is not an overload check.
         */
        if ( names.size() == 1) {
            IdOrOp fnName = names.get(0);
            Option<APIName> apiName = fnName.getApiName();
            if (apiName.isSome() && ForeignJava.only.definesApi(apiName.unwrap())) {

                if ( aliasTable.containsKey(name) ) {
                    String n = aliasTable.get(name);
                    // Cheating by assuming class is everything before the dot.
                    int lastDot = n.lastIndexOf(".");
                    String calleePackageAndClass = n.substring(0, lastDot).replace(".", "/");
                    String _method = n.substring(lastDot+1);


                   callStaticSingleOrOverloaded(x, arrow, calleePackageAndClass, _method);
                } else {
                 sayWhat(x, "Should be a foreign function in Alias table");
                }

            } else {
                // NOT Foreign

                // deal with in component, or in imported api.
                String calleePackageAndClass = apiName.isSome() ?
                        NamingCzar.fortressPackage + "/" + apiName.unwrap().getText()
                        : packageAndClassName;
                String _method = fnName.getText();

                callStaticSingleOrOverloaded(x, arrow, calleePackageAndClass, _method);


            }
        } else {
            sayWhat(x, "Expected to see only one name here");
        }
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
        {
            debug("class = " + pkgAndClassName + " method = " + methodName );

            if ( arrow instanceof ArrowType ) {
                addLineNumberInfo(x);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, pkgAndClassName,
                                   methodName, Naming.emitDesc(arrow));
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

    }

    // paramCount communicates this information from call to function reference,
    // as it's needed to determine type descriptors for methods.
    private int paramCount = -1;

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
        mv.visitFieldInsn(Opcodes.GETSTATIC, Naming.getJavaClassForSymbol(id) , "default_" + id.getText(),
                          "L" + Naming.getJavaClassForSymbol(id) + "$" + id.getText() + ";");



        for (Expr e : subs) {
            debug("calling accept on ", e);
            e.accept(this);
        }
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           Naming.getJavaClassForSymbol(id) + "$" + id.getText(),
                           NamingCzar.mangleIdentifier(op.getText()),
                           "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");
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

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        debug("forVoidLiteral ", x);
        addLineNumberInfo(x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                           NamingCzar.makeMethodDesc("", NamingCzar.descFortressVoid));

    }

    public void forVarRef(VarRef v) {
        debug("forVarRef ", v, " which had better be local (for the moment)");
        if (v.getStaticArgs().size() > 0) {
            sayWhat(v,"varRef with static args!  That requires non-local VarRefs");
        }
        VarCodeGen vcg = getLocalVar(v.getVarId());
        vcg.pushValue(mv);
    }

}