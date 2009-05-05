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
import java.math.BigInteger;
import java.util.*;
import org.objectweb.asm.*;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.phases.OverloadSet;
import com.sun.fortress.compiler.typechecker.TypeAnalyzer;

import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.ForeignJava;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.StringHashComparer;

// Note we have a name clash with org.objectweb.asm.Type
// and com.sun.fortress.nodes.Type.  If anyone has a better
// solution than writing out their entire types, please
// shout out.
public class CodeGen extends NodeAbstractVisitor_void {
    ClassWriter cw;
    MethodVisitor mv;
    private final String className;
    String packageName;
    String packageAndClassName;
    private final HashMap<String, String> aliasTable;
    private final TypeAnalyzer ta;

    // lexEnv does not include the top level or object right now, just
    // args and local vars.  Object fields should have been translated
    // to dotted notation at this point, right?  Right?
    BATree<String, VarCodeGen> lexEnv = null;
    Symbols symbols;
    boolean inATrait = false;
    boolean inAnObject = false;
    boolean inABlock = false;
    int localsDepth = 0;

    private void generateMainMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            Naming.stringArrayToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/sun/fortress/nativeHelpers/systemHelper",
                           "registerArgs", Naming.stringArrayToVoid);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "run", Naming.voidToFortressVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(Naming.ignore,Naming.ignore);
        mv.visitEnd();
    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", Naming.voidToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Naming.internalObject, "<init>", Naming.voidToVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(Naming.ignore, Naming.ignore);
        mv.visitEnd();
    }

    public CodeGen(String n, Symbols s, TypeAnalyzer ta) {
        className = n;
        aliasTable = new HashMap<String, String>();
        symbols = s;
        this.ta = ta;
        debug( "Compile: Compiling ", className );
    }

    // Create a fresh codegen object for a nested scope.  Technically,
    // we ought to be able to get by with a single lexEnv, because
    // variables ought to be unique by location etc.  But in practice
    // I'm not assuming we have a unique handle for any variable,
    // so we get a fresh CodeGen for each scope to avoid collisions.
    private CodeGen(CodeGen c) {
        this.cw = c.cw;
        this.mv = c.mv;
        this.className = c.className;
        this.packageName = c.packageName;
        this.packageAndClassName = c.packageAndClassName;
        this.aliasTable = c.aliasTable;
        this.symbols = c.symbols;
        this.inATrait = c.inATrait;
        this.inAnObject = c.inAnObject;
        this.inABlock = c.inABlock;
        this.localsDepth = c.localsDepth;
        this.ta = c.ta;
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
        cw.visitEnd();
        ByteCodeWriter.writeClass(Naming.cache, file, cw.toByteArray());
        debug( "Writing class ", file);
    }

    private <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }

    private void debug(Object... message) {
        Debug.debug(Debug.Type.CODEGEN,1,message);
    }

    public void defaultCase(ASTNode x) {
        System.out.println("defaultCase: " + x + " of class " + x.getClass());
        sayWhat(x);
    }

    public void forComponent(Component x) {
        debug("forComponent ",x.getName(),NodeUtil.getSpan(x));
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(className, null);
        boolean exportsExecutable = false;
        boolean exportsDefaultLibrary = false;
        for ( APIName export : x.getExports() ) {
            if ( WellKnownNames.exportsMain(export.getText()) )
                exportsExecutable = true;
            if ( WellKnownNames.exportsDefaultLibrary(export.getText()) )
                exportsDefaultLibrary = true;
        }
        // If this component implements a default library,
        // generate "package fortress;"
        if ( exportsDefaultLibrary )
            packageName = Naming.fortressPackage + Naming.slash;
        else
            packageName = Naming.emptyString;

        packageAndClassName = packageName + className;

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                packageAndClassName, null, Naming.internalObject, null);

        // Always generate the init method
        generateInitMethod();

        // If this component exports an executable API,
        // generate a main method.
        if ( exportsExecutable ) {
            generateMainMethod();
        }
        for ( Import i : x.getImports() ) i.accept(this);

        for ( Decl d : x.getDecls() ) { d.accept(this);}
        dumpClass( packageAndClassName );
    }

    public void forImportNames(ImportNames x) {
        debug("forImportNames", x);
        Option<String> foreign = x.getForeignLanguage();
        if ( foreign.isSome() ) {
            if ( foreign.unwrap().equals("java") ) {
                String apiName = x.getApiName().getText();
                for ( AliasedSimpleName n : x.getAliasedNames() ) {
                    Option<IdOrOpOrAnonymousName> aliasId = n.getAlias();
                    if (aliasId.isSome()) {
                        debug("forImportNames ", x,
                                    " aliasing ", NodeUtil.nameString(aliasId.unwrap()),
                                    " to ", NodeUtil.nameString(n.getName()));
                        aliasTable.put(NodeUtil.nameString(aliasId.unwrap()),
                                       apiName + Naming.dot +
                                       NodeUtil.nameString(n.getName()));
                    }
                }
            }
        }
    }

    public void forDecl(Decl x) {
        debug("forDecl", x);
        if (x instanceof TraitDecl)
            ((TraitDecl) x).accept(this);
        else if (x instanceof FnDecl)
            ((FnDecl) x).accept(this);
        else if (x instanceof ObjectDecl)
            ((ObjectDecl) x).accept(this);
        else if (x instanceof VarDecl)
            ((VarDecl) x).accept(this);
        else if (x instanceof _RewriteFnOverloadDecl)
            System.err.println("Saw an overloaded function " + x );
        else
            sayWhat(x);
    }

    private void dumpSigs(List<Decl> decls) {
        debug("dumpSigs", decls);
        for (Decl d : decls) {
            debug("dumpSigs decl =", d);
            if (d instanceof FnDecl) {
                FnDecl f = (FnDecl) d;
                FnHeader h = f.getHeader();
                IdOrOpOrAnonymousName xname = h.getName();
                IdOrOp name = (IdOrOp) xname;
                String desc = Naming.generateTypeDescriptor(f);
                debug("about to call visitMethod with", name.getText(),
                            " and desc ", desc);
                mv = cw.visitMethod(Opcodes.ACC_ABSTRACT + Opcodes.ACC_PUBLIC, name.getText(), desc, null, null);
                mv.visitMaxs(Naming.ignore, Naming.ignore);
                mv.visitEnd();
            } else {
                sayWhat(d);
            }
        }
    }

    private void dumpDecls(List<Decl> decls) {
        debug("dumpDecls", decls);
        for (Decl d : decls) {
            if (d instanceof FnDecl) {
                d.accept(this);
            } else {
                sayWhat(d);
            }
        }
    }

    public void forTraitDecl(TraitDecl x) {
        debug("forTraitDecl", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            x.getExcludesClause().isEmpty() &&    // no excludes clause
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty(); // no modifiers
        debug("forTraitDecl", x,
                    " decls = ", header.getDecls(), " extends = ", extendsC);
        if ( !canCompile ) sayWhat(x);
        inATrait = true;
        String parent = Naming.emptyString;
        if ( extendsC.isEmpty() ) {
            parent = Naming.internalObject;
        } else {
            debug("forTraitDecl extends size = ",
                  extendsC.size());
            BaseType parentType = extendsC.get(0).getBaseType();
            if ( parentType instanceof AnyType )
                parent = Naming.fortressAny;
            else if ( parentType instanceof TraitType ) {
                Id name = ((TraitType)parentType).getName();
                Option<APIName> apiName = name.getApiName();
                if ( apiName.isNone() ) parent = name.toString();
                else { // if ( apiName.isSome() )
                    String api = apiName.unwrap().getText();
                    if ( WellKnownNames.exportsDefaultLibrary( api ) )
                        parent += Naming.fortressPackage + Naming.dot;
                    parent += api + Naming.underscore + name.getText();
                }
            } else
                sayWhat( parentType, "Invalid type in an extends clause." );
        }

        // First lets do the interface class
        String classFile = Naming.makeClassName(packageName, className, x);
        ClassWriter prev = cw;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(classFile, null);
        cw.visit( Opcodes.V1_5,
                  Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                  classFile, null, Naming.internalObject, new String[] { parent });
        dumpSigs(header.getDecls());
        dumpClass( classFile );
        // Now lets do the springboard inner class that implements this interface.

        String springBoardClass = classFile + Naming.underscore + Naming.springBoard;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visitSource(classFile, null);
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, springBoardClass,
                 null, Naming.internalObject, new String[]{parent + Naming.springBoard} );
        debug("Start writing springboard class ",
              springBoardClass);
        generateInitMethod();
        debug("Finished init method ", springBoardClass);
        dumpDecls(header.getDecls());
        debug("Finished dumpDecls ", springBoardClass);
        dumpClass(springBoardClass);
        // Now lets dump out the functional methods at top level.
        cw = prev;
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
            ( extendsC.isEmpty() || extendsC.size() == 1 ); // 0 or 1 super trait
        if ( !canCompile ) sayWhat(x);
        inAnObject = true;
        String parent = Naming.emptyString;
        if ( extendsC.isEmpty() ) {
            parent = Naming.internalObject;
        } else { // if ( extendsC.size() == 1 )
            BaseType parentType = extendsC.get(0).getBaseType();
            if ( parentType instanceof AnyType )
                parent = Naming.fortressAny;
            else if ( parentType instanceof TraitType ) {
                Id name = ((TraitType)parentType).getName();
                Option<APIName> apiName = name.getApiName();
                if ( apiName.isNone() ) parent = name.toString();
                else { // if ( apiName.isSome() )
                    String api = apiName.unwrap().getText();
                    if ( WellKnownNames.exportsDefaultLibrary( api ) )
                        parent += Naming.fortressPackage + Naming.dot;
                    parent += api + Naming.underscore + name.getText();
                }
            } else
                sayWhat( parentType, "Invalid type in an extends clause." );
        }

        if (!header.getDecls().isEmpty()) {
            debug("header.getDecls:", header.getDecls());

            cw.visitField(Opcodes.ACC_STATIC + Opcodes.ACC_PUBLIC,
                          "default_args",
                          "LCompilerSystem_args;",
                          null,
                          null);

            mv = cw.visitMethod(Opcodes.ACC_STATIC,
                                "<clinit>",
                                "()V",
                                null,
                                null);

            mv.visitTypeInsn(Opcodes.NEW, "CompilerSystem_args");
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "CompilerSystem_args", "<init>", Naming.voidToVoid);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, "CompilerSystem", "default_args", "LCompilerSystem_args;");
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(Naming.ignore, Naming.ignore);
            mv.visitEnd();
        }

        String classFile = Naming.makeClassName(packageName, className, x);
        ClassWriter prev = cw;
        cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        // Until we resolve the directory hierarchy problem.
        //            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
        //                      classFile, null, Naming.internalObject, new String[] { parent });
        cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER+ Opcodes.ACC_FINAL,
                  classFile, null, Naming.internalObject, null);
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
            exprType.isSome() &&
            names.size() == 1;

        if (canCompile) {
            String name = originalName.getText();
            IdOrOp newName = names.get(0);
            Option<APIName> api = newName.getApiName();
            if (api.isSome()) {
                APIName apiName = api.unwrap();
                debug("forOpRef name = ", name,
                             " api = ", apiName);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.getJavaClassForSymbol(newName), newName.getText(),
                                   Naming.emitDesc(exprType.unwrap()));
            } else {
                debug("forOpRef name = ", name);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, name,
                                   Naming.emitDesc(exprType.unwrap()));
            }
        } else
            debug("forOpRef can't compile staticArgs ",
                        x.getStaticArgs().isEmpty(), " overloadings ",
                        x.getOverloadings().isNone());
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
        } else {
            // Top-level function or functional method
            modifiers += Opcodes.ACC_STATIC;
        }

        cg.mv = cw.visitMethod(modifiers,
                               Naming.mangle(nameString),
                               Naming.emitFnDeclDesc(NodeUtil.getParamType(x),
                                                     returnType.unwrap()),
                               null, null);

        // Now inside method body.  Generate code for the method body.
        for (Param p : params) {
            VarCodeGen v = cg.addParam(p);
            // v.pushValue(cg.mv);
        }

        body.unwrap().accept(cg);

        // Method body is complete except for returning final result if any.
        if (NodeUtil.isVoidType(returnType.unwrap()))
            cg.mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                  Naming.internalFortressVoid, Naming.make,
                                  Naming.makeMethodDesc(Naming.emptyString,
                                                        Naming.descFortressVoid));

        cg.mv.visitInsn(Opcodes.ARETURN);
        cg.mv.visitMaxs(Naming.ignore,Naming.ignore);
        cg.mv.visitEnd();
        // Method body complete, cg now invalid.
    }

    public void forDo(Do x) {
        debug("forDo ", x);
        for ( Block b : x.getFronts() ) {
            b.accept(this);
        }
    }

    public void forBlock(Block x) {
        boolean oldInABlock = inABlock;
        inABlock = true;
        debug("forBlock ", x);
        for ( Expr e : x.getExprs() ) {
            e.accept(this);
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

        if ( names.size() == 1) {
            IdOrOp fnName = names.get(0);
            Option<APIName> apiName = fnName.getApiName();
            if (apiName.isSome() && ForeignJava.only.definesApi(apiName.unwrap())) {

                if ( aliasTable.containsKey(name) ) {
                    String n = aliasTable.get(name);
                    // Cheating by assuming class is everything before the dot.
                    int lastDot = n.lastIndexOf(Naming.dot);
                    String internal_class = n.substring(0, lastDot).replace(Naming.dot, Naming.slash);
                    String _method = n.substring(lastDot+1);
                   debug("class = " + internal_class + " method = " + _method );

                     {
                        if ( arrow instanceof ArrowType )
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, internal_class,
                                               _method, Naming.emitDesc(arrow));
                        else {  // if ( ! arrow instanceof ArrowType )

                            Debug.debug( Debug.Type.CODEGEN, 1,
                                         "class = " + internal_class + " method = " + _method +
                                         " type = " + arrow);

                            if (arrow instanceof IntersectionType)
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, internal_class,
                                        _method, OverloadSet.getSignature((IntersectionType) arrow, paramCount, ta));
                            else {
                                sayWhat( x, "Neither arrow nor intersection type: " + arrow );
                            }

                        }
                    }
                } else {
                 sayWhat(x, "Should be a foreign function in Alias table");
                }

            } else {
                // NOT Foreign

                // deal with in component, or in imported api.
                String calleePackageAndClass = apiName.isSome() ?
                        Naming.fortressPackage + Naming.slash + apiName.unwrap().getText()
                        : packageAndClassName;

                if (arrow instanceof ArrowType) {

                    String domainType;
                    String rangeType;
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                            calleePackageAndClass, fnName.getText(), Naming
                                    .emitDesc(arrow));
                } else {
                    sayWhat(x, "Overloaded non-foreign not implemented yet.");
                }

            }
        } else {
            sayWhat(x, "Expected to see only one name here");
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

        mv.visitFieldInsn(Opcodes.GETSTATIC, Naming.getJavaClassForSymbol(id) , "default_" + id.getText(),
                          "L" + Naming.getJavaClassForSymbol(id) + Naming.underscore + id.getText() + ";");

        for (Expr e : subs) {
            debug("calling accept on ", e);
            e.accept(this);
        }

        debug(" We want owner=com/sun/fortress/compiler/codegen/stubs/compiled2/CompilerSystem.default_args and desc = Lcom/sun/fortress/compiler/codegen/stubs/compiled2/CompilerSystem_args");

        debug(" We got owner=", Naming.getJavaClassForSymbol(id),
              " field = ", "default_",  id.getText(), " desc= ","L",
              Naming.getJavaClassForSymbol(id), ".", id.getText(), ";" );
        debug(" We have Naming.getJavaClassForSymbol(id)=",
              Naming.getJavaClassForSymbol(id));
        debug(" We have id.getText()", id.getText());
        debug(" We have op.getText()", op.getText());


        //            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Naming.getJavaClassForSymbol(id) + "_" + id.getText(),
        //                               op.getText(),
        //                               "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           Naming.getJavaClassForSymbol(id) + "_" + id.getText(),
                           Naming.mangle(op.getText()),
                           "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");
    }


    public void forIntLiteralExpr(IntLiteralExpr x) {
        debug("forIntLiteral ", x);
        BigInteger bi = x.getIntVal();
        // This might not work.
        int y = bi.intValue();
        switch (y) {
        case 0: mv.visitInsn(Opcodes.ICONST_0); break;
        case 1: mv.visitInsn(Opcodes.ICONST_1); break;
        case 2: mv.visitInsn(Opcodes.ICONST_2); break;
        case 3: mv.visitInsn(Opcodes.ICONST_3); break;
        case 4: mv.visitInsn(Opcodes.ICONST_4); break;
        case 5: mv.visitInsn(Opcodes.ICONST_5); break;
        default: mv.visitLdcInsn(y); break;
        }

        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressZZ32, Naming.make,
                           Naming.makeMethodDesc(Naming.descInt, Naming.descFortressZZ32));
    }


    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FString and push it on the stack.
        debug("forStringLiteral ", x);
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressString, Naming.make,
                           Naming.makeMethodDesc(Naming.descString, Naming.descFortressString));
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        debug("forVoidLiteral ", x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressVoid, Naming.make,
                           Naming.makeMethodDesc(Naming.emptyString, Naming.descFortressVoid));
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
