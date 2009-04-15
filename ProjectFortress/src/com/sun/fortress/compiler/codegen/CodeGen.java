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

import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.*;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

// Note we have a name clash with org.objectweb.asm.Type
// and com.sun.fortress.nodes.Type.  If anyone has a better
// solution than writing out their entire types, please
// shout out.
public class CodeGen extends NodeAbstractVisitor_void {
    ClassWriter cw;
    MethodVisitor mv;
    String className;
    String packageName;
    HashMap<String, String> aliasTable;
    Symbols symbols;
    boolean inATrait = false;
    boolean inAnObject = false;
    int inABlock = 0;

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

    public CodeGen(String n, Symbols s) {
        className = n;
        aliasTable = new HashMap<String, String>();
        symbols = s;
        Debug.debug( Debug.Type.CODEGEN, 1, "Compile: Compiling " + className );
    }

    public void dumpClass( String file ) {
        cw.visitEnd();
        ByteCodeWriter.writeClass(Naming.cache, file, cw.toByteArray());
        Debug.debug( Debug.Type.CODEGEN, 1, "Writing class " + file);
    }

    private <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message + " node = " + x);
    }

    public void defaultCase(ASTNode x) {
        System.out.println("defaultCase: " + x + " of class " + x.getClass());
        sayWhat(x);
    }

    public void forComponent(Component x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forComponent " + x.getName() + NodeUtil.getSpan(x));
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

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 packageName + className, null, Naming.internalObject, null);

        // Always generate the init method
        generateInitMethod();

        // If this component exports an executable API,
        // generate a main method.
        if ( exportsExecutable ) {
            generateMainMethod();
        }
        for ( Import i : x.getImports() ) i.accept(this);

        for ( Decl d : x.getDecls() ) { d.accept(this);}
        dumpClass( packageName + className );
    }

    public void forImportNames(ImportNames x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forImportNames" + x);
        Option<String> foreign = x.getForeignLanguage();
        if ( foreign.isSome() ) {
            if ( foreign.unwrap().equals("java") ) {
                String apiName = x.getApiName().getText();
                for ( AliasedSimpleName n : x.getAliasedNames() ) {
                    Option<IdOrOpOrAnonymousName> aliasId = n.getAlias();
                    if (aliasId.isSome()) {
                        Debug.debug(Debug.Type.CODEGEN,1,"forImportNames " + x +
                                    " aliasing " + NodeUtil.nameString(aliasId.unwrap()) +
                                    " to " + NodeUtil.nameString(n.getName()));

                        aliasTable.put(NodeUtil.nameString(aliasId.unwrap()),
                                       apiName + Naming.dot +
                                       NodeUtil.nameString(n.getName()));
                    }
                }
            }
        }
    }

    public void forDecl(Decl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forDecl" + x);
        if (x instanceof TraitDecl)
            ((TraitDecl) x).accept(this);
        else if (x instanceof FnDecl)
            ((FnDecl) x).accept(this);
        else if (x instanceof ObjectDecl)
            ((ObjectDecl) x).accept(this);
        else if (x instanceof VarDecl)
            ((VarDecl) x).accept(this);
        else
            sayWhat(x);
    }

    private void dumpSigs(List<Decl> decls) {
        Debug.debug(Debug.Type.CODEGEN, 1, "dumpSigs" + decls);
        for (Decl d : decls) {
            Debug.debug(Debug.Type.CODEGEN, 1, "dumpSigs decl =" + d);
            if (d instanceof FnDecl) {
                FnDecl f = (FnDecl) d;
                FnHeader h = f.getHeader();
                IdOrOpOrAnonymousName xname = h.getName();
                IdOrOp name = (IdOrOp) xname;
                String desc = Naming.generateTypeDescriptor(f);
                Debug.debug(Debug.Type.CODEGEN, 1, "about to call visitMethod with" + name.getText() +
                            " and desc " + desc);
                mv = cw.visitMethod(Opcodes.ACC_ABSTRACT + Opcodes.ACC_PUBLIC, name.getText(), desc, null, null);
                mv.visitMaxs(Naming.ignore, Naming.ignore);
                mv.visitEnd();
            } else {
                sayWhat(d);
            }
        }
    }

    private void dumpDecls(List<Decl> decls) {
        Debug.debug(Debug.Type.CODEGEN, 1, "dumpDecls" + decls);
        for (Decl d : decls) {
            if (d instanceof FnDecl) {
                d.accept(this);
            } else {
                sayWhat(d);
            }
        }
    }

    public void forTraitDecl(TraitDecl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forTraitDecl" + x);
        inATrait = true;
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            x.getExcludesClause().isEmpty() &&    // no excludes clause
            x.getComprisesClause().isNone() &&    // no comprises clause
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty(); // no modifiers
        Debug.debug(Debug.Type.CODEGEN, 1, "forTraitDecl" + x + " decls = " + header.getDecls() + " extends = " + extendsC);
        if ( canCompile ) {
            String parent = Naming.emptyString;
            if ( extendsC.isEmpty() ) {
                parent = Naming.internalObject;
            } else {
                Debug.debug(Debug.Type.CODEGEN,1,"forTraitDecl extends size = " + extendsC.size());
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
            Debug.debug(Debug.Type.CODEGEN, 1, "Start writing springboard class " + springBoardClass);
            generateInitMethod();
            Debug.debug(Debug.Type.CODEGEN, 1, "Finished init method " + springBoardClass);
            dumpDecls(header.getDecls());
            Debug.debug(Debug.Type.CODEGEN, 1, "Finished dumpDecls " + springBoardClass);
            dumpClass(springBoardClass);
            // Now lets dump out the functional methods at top level.
            cw = prev;
            dumpDecls(header.getDecls());
            Debug.debug(Debug.Type.CODEGEN, 1, "Finished dumpDecls for parent");
        } else sayWhat( x );
        inATrait = false;
    }

    public void forObjectDecl(ObjectDecl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forObjectDecl" + x);
        inAnObject = true;
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
        if ( canCompile ) {
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
                Debug.debug(Debug.Type.CODEGEN, 1, "header.getDecls:" + header.getDecls());
                
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
        } else sayWhat( x );
        inAnObject = false;
    }

    public void forOpExpr(OpExpr x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forOpExpr " + x + " op = " + x.getOp() + " of class " + x.getOp().getClass() +  " args = " + x.getArgs());
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
        Debug.debug( Debug.Type.CODEGEN, 1,"forOpRef " + x +
                     " info = " + info + "staticArgs = " + staticArgs + " exprType = " + exprType +
                     " lexicalDepth = " + lexicalDepth + " originalName = " + originalName +
                     " overloadings = " + overloadings + " overloadingType = " + overloadingType +
                     " names = " + names);

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
                Debug.debug( Debug.Type.CODEGEN,1,"forOpRef name = " + name + " api = " + apiName);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.getJavaClassForSymbol(newName), newName.getText(),
                                   Naming.emitDesc(exprType.unwrap()));
            } else {
                Debug.debug(Debug.Type.CODEGEN,1,"forOpRef name = " + name);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, name,
                                   Naming.emitDesc(exprType.unwrap()));
            }
        } else
            Debug.debug(Debug.Type.CODEGEN,1,"forOpRef can't compile staticArgs " + x.getStaticArgs().isEmpty() + " overloadings " + x.getOverloadings().isNone());
    }

    public void forFnDecl(FnDecl x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forFnDecl " + x );
        FnHeader header = x.getHeader();
        boolean canCompile =
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty() &&         // no modifiers
            (inABlock == 0);                      // no local functions


        if ( canCompile ) {
            Option<Expr> body = x.getBody();
            IdOrOpOrAnonymousName headerName = header.getName();
            List<Param> params = header.getParams();
            boolean functionalMethod = false;

            for (Param p : params) {
                Debug.debug(Debug.Type.CODEGEN, 1, "iterating params looking for self : param = " + p);
                if (p.getName().getText() == "self")
                    functionalMethod = true;
            }

            Option<com.sun.fortress.nodes.Type> returnType = header.getReturnType();
            if ( body.isNone() ) {
                sayWhat(x, "Abstract function declarations are not supported.");
            } else if ( returnType.isNone() ) {
                sayWhat(x, "Return type is not inferred.");
            } else if (headerName instanceof Op) {
                Op name = (Op) headerName;
                Fixity fixity = name.getFixity();
                boolean isEnclosing = name.isEnclosing();
                Option<APIName> maybe_apiName = name.getApiName();
                
                Debug.debug( Debug.Type.CODEGEN, 1,"forOp " + name + " fixity = " + fixity + " isEnclosing = " + isEnclosing + " class = " + Naming.getJavaClassForSymbol(name));

                if (!functionalMethod && (inAnObject || inATrait)) {   // Dotted Method

                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                        Naming.mangle(name.getText()),
                                        Naming.emitFnDeclDesc(NodeUtil.getParamType(x), returnType.unwrap()),null, null);
                    for (int i = 1; i<=params.size(); i++)
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                } else {
                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                        Naming.mangle(name.getText()),
                                        Naming.emitFnDeclDesc(NodeUtil.getParamType(x), returnType.unwrap()), null, null);
                    for (int i = 0; i < params.size(); i++)
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                }

                if (body.isSome()) {
                     Expr expr = body.unwrap();
                     expr.accept(this);
                }
                if (NodeUtil.isVoidType(returnType.unwrap())) 
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressVoid, Naming.make,
                                       Naming.makeMethodDesc(Naming.emptyString, Naming.descFortressVoid));

                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignore,Naming.ignore);
                mv.visitEnd();
                
            } else if ( headerName instanceof Id ) {
                Id name = (Id) headerName;
                Debug.debug( Debug.Type.CODEGEN, 1,"forId " + name + " class = " + Naming.getJavaClassForSymbol(name));
                if (!functionalMethod && (inAnObject || inATrait)) {   // Dotted Method
                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                        Naming.mangle(name.getText()),
                                        Naming.emitFnDeclDesc(NodeUtil.getParamType(x), returnType.unwrap()),null, null);
                    for (int i = 1; i<=params.size(); i++)
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                } else {
                    mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                        Naming.mangle(name.getText()),
                                        Naming.emitFnDeclDesc(NodeUtil.getParamType(x), returnType.unwrap()), null, null);
                    for (int i = 0; i < params.size(); i++)
                        mv.visitVarInsn(Opcodes.ALOAD, i);
                } 

                if (body.isSome()) {
                     Expr expr = body.unwrap();
                     expr.accept(this);
                }

                if (NodeUtil.isVoidType(returnType.unwrap())) 
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressVoid, Naming.make,
                                       Naming.makeMethodDesc(Naming.emptyString, Naming.descFortressVoid));

                mv.visitInsn(Opcodes.ARETURN);
                mv.visitMaxs(Naming.ignore,Naming.ignore);
                mv.visitEnd();

            } else sayWhat(x);
        } 
    }

    public void forDo(Do x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forDo " + x);
        for ( Block b : x.getFronts() ) {
            b.accept(this);
        }
    }

    public void forBlock(Block x) {
        inABlock++;
        Debug.debug( Debug.Type.CODEGEN, 1,"forBlock " + x);
        for ( Expr e : x.getExprs() ) {
            e.accept(this);
        }
        inABlock--;
    }

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forFnRef " + x);
        String name = x.getOriginalName().getText();
        if ( aliasTable.containsKey(name) ) {
            String n = aliasTable.get(name);
            // Cheating by assuming class is everything before the dot.
            int lastDot = n.lastIndexOf(Naming.dot);
            String internal_class = n.substring(0, lastDot).replace(Naming.dot, Naming.slash);
            String _method = n.substring(lastDot+1);
            Debug.debug( Debug.Type.CODEGEN, 1,
                         "class = " + internal_class + " method = " + _method );
            Option<com.sun.fortress.nodes.Type> type = x.getInfo().getExprType();
            if ( type.isNone() )
                sayWhat( x, "The type of this expression is not inferred." );
            else {
                com.sun.fortress.nodes.Type arrow = type.unwrap();
                if ( arrow instanceof ArrowType )
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, internal_class,
                                       _method, Naming.emitDesc(arrow));
                else {  // if ( ! arrow instanceof ArrowType )
                    Debug.debug( Debug.Type.CODEGEN, 1,
                                 "class = " + internal_class + " method = " + _method +
                                 " type = " + arrow);

                sayWhat( x, "The type of a function reference " +
                             "is not an arrow type." );
                }
            }
        } else {
            List<IdOrOp> names = x.getNames();
            // For now, assuming only 1
            if ( names.size() == 1) {
                IdOrOp fnName = names.get(0);
                Option<APIName> apiName = fnName.getApiName();
                if ( apiName.isSome() ) {
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                       Naming.fortressPackage + Naming.slash + apiName.unwrap().getText(),
                                       fnName.getText(),
                                       Naming.makeMethodDesc(Naming.descFortressString,
                                                      Naming.descFortressVoid));
                } else // if ( apiName.isNone() )
                    sayWhat( x, "API name is not disambiguated." );
            } else // if ( names.size() != 1 )
                sayWhat( x, "Overloaded function references are not supported." );
        }
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"for_RewriteFnApp " + x + " args = " + x.getArgument() + " function = " + x.getFunction());
        // This is a little weird.  If a function takes no arguments the parser gives me a void literal expr
        // however I don't want to be putting a void literal on the stack because it gets in the way.
        if (x.getArgument() instanceof VoidLiteralExpr) {
            x.getFunction().accept(this);
        } else {
            x.getArgument().accept(this);
            x.getFunction().accept(this);
        }
    }

    public void forSubscriptExpr(SubscriptExpr x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forSubscriptExpr " + x);
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        Option<Op> maybe_op = x.getOp();
        List<StaticArg> staticArgs = x.getStaticArgs();
        boolean canCompile = staticArgs.isEmpty() && maybe_op.isSome() && (obj instanceof VarRef);
        if (canCompile) {
            Op op = maybe_op.unwrap();
            VarRef var = (VarRef) obj;
            Id id = var.getVarId();

            Debug.debug(Debug.Type.CODEGEN, 1, "ForSubscriptExpr  " + x + "obj = " + obj + " subs = " + subs + 
                        " op = " + op + " static args = " + staticArgs + " varRef = " + id.getText());

            mv.visitFieldInsn(Opcodes.GETSTATIC, Naming.getJavaClassForSymbol(id) , "default_" + id.getText(),
                              "L" + Naming.getJavaClassForSymbol(id) + Naming.underscore + id.getText() + ";");

            for (Expr e : subs) {
                Debug.debug(Debug.Type.CODEGEN,1, "calling accept on " + e);
                e.accept(this);
            }
                              
            Debug.debug(Debug.Type.CODEGEN,1," We want owner=com/sun/fortress/compiler/codegen/stubs/compiled2/CompilerSystem.default_args and desc = Lcom/sun/fortress/compiler/codegen/stubs/compiled2/CompilerSystem_args");

            Debug.debug(Debug.Type.CODEGEN,1," We got owner=" + Naming.getJavaClassForSymbol(id) + " field = " + "default_" +  id.getText() + " desc= " + 
                        "L" + Naming.getJavaClassForSymbol(id) + "." + id.getText() + ";" );
            Debug.debug(Debug.Type.CODEGEN,1," We have Naming.getJavaClassForSymbol(id)=" + Naming.getJavaClassForSymbol(id));
            Debug.debug(Debug.Type.CODEGEN,1," We have id.getText()" + id.getText());
            Debug.debug(Debug.Type.CODEGEN,1," We have op.getText()" + op.getText());

            
            //            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Naming.getJavaClassForSymbol(id) + "_" + id.getText(), 
            //                               op.getText(),
            //                               "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");

            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Naming.getJavaClassForSymbol(id) + "_" + id.getText(), 
                               Naming.mangle(op.getText()),
                               "(Lcom/sun/fortress/compiler/runtimeValues/FZZ32;)Lcom/sun/fortress/compiler/runtimeValues/FString;");
        } else sayWhat(x);
    }


    public void forIntLiteralExpr(IntLiteralExpr x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forIntLiteral " + x);
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
        Debug.debug( Debug.Type.CODEGEN, 1,"forStringLiteral " + x);
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressString, Naming.make,
                           Naming.makeMethodDesc(Naming.descString, Naming.descFortressString));
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forVoidLiteral " + x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, Naming.internalFortressVoid, Naming.make,
                           Naming.makeMethodDesc(Naming.emptyString, Naming.descFortressVoid));
    }


}
