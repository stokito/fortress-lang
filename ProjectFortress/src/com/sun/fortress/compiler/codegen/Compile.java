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


import com.sun.fortress.compiler.nativeInterface.MyClassLoader;
import com.sun.fortress.nodes.*;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import java.util.*;
import org.objectweb.asm.*;
import edu.rice.cs.plt.tuple.Option;

public class Compile extends NodeAbstractVisitor_void {
    ClassWriter cw;
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;
    String className;
    MyClassLoader loader;

    HashMap<String, String> aliasTable;

    private void generateMainMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "run", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0,1);
        mv.visitEnd();
    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public Compile(String n) {
        loader = new MyClassLoader();
        cw = new ClassWriter(0);
        className = n;
        aliasTable = new HashMap<String, String>();
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER, className, null, "java/lang/Object", null);
        generateInitMethod();
        generateMainMethod();
    }

    public void dumpClass() {
        cw.visitEnd();
        loader.writeClass(className, cw.toByteArray());
    }

    private void sayWhat(Node x) {
        throw new RuntimeException("Can't compile " + x);
    }

    private void sayWhat(String x) {
        throw new RuntimeException(x);
    }

    public void defaultCase(Node x) {
        System.out.println("defaultCase" + x + " of class" + x.getClass());
        sayWhat(x);
    }

    public void forComponent(Component x) {
        CanCompile c = new CanCompile();
        if (x.accept(c)) {
            List<Import> imports = x.getImports();
            for (Import i : imports) {
                i.accept(this);
            }
            List<Decl> decls = x.getDecls();
            for (Decl d : decls) {
                forDecl(d);
            }
            dumpClass();
        }
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        Expr function = x.getFunction();
        Expr argument = x.getArgument();
        argument.accept(this);
        function.accept(this);
        mv.visitInsn(Opcodes.RETURN);
    }

    public void forBlock(Block x) {
        List<Expr> exprs = x.getExprs();
        for (Expr e : exprs) e.accept(this);
    }

    public void forDo(Do x) {
        List<Block> fronts = x.getFronts();
        for (Block b : fronts) {
            b.accept(this);
        }
    }
        
    public void forFnDecl(FnDecl x) {
        Id unambiguousName = x.getUnambiguousName();
        FnHeader header = x.getHeader();
        IdOrOpOrAnonymousName name = header.getName();
        Id idName = (Id) name;
        String realName = idName.getText();
        Option<Expr> body = x.getBody();
        if (!body.isSome()) {
            throw new RuntimeException("Don't know how to compile a null function yet");
        } else {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, realName, "()V", null, null);
            mv.visitCode();
            body.unwrap().accept(this);
            mv.visitMaxs(2,1);
            mv.visitEnd();
        }
    }

    public void forDecl(Decl x) {
        if (x instanceof FnDecl)
            forFnDecl((FnDecl) x);
        else {
            sayWhat(x);
        }
    }


    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FString and push it on the stack.
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
            "com/sun/fortress/interpreter/evaluator/values/FString", 
            "make", 
            "(Ljava/lang/String;)Lcom/sun/fortress/interpreter/evaluator/values/FString;");
    }

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        IdOrOp originalName = x.getOriginalName();
        String name = originalName.getText();
        if (aliasTable.containsKey(name)) {
            String n = aliasTable.get(name);
            // Cheating here.  Need to figure out the type of the function.
            // Also cheating by assuming class is everything before the dot.
            int lastDot = n.lastIndexOf('.');
            String _class = n.substring(0, lastDot);
            String internal_class = _class.replace('.', '/');
            String _method = n.substring(lastDot+1);
            System.out.println("class = " + internal_class + " method = " + _method);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
              internal_class, _method, 
              "(Lcom/sun/fortress/interpreter/evaluator/values/FString;)Lcom/sun/fortress/interpreter/evaluator/values/FVoid;");
        }
    }

    private String getStringFromIdOrOpOrAnonymousName(IdOrOpOrAnonymousName idOrOp) {
        if (idOrOp instanceof Id) {
            Id name = (Id) idOrOp;
            String n = name.getText();
            return n;
        } else {
            sayWhat(idOrOp);
            return "error";
        }
    }

    public void forImportNames(ImportNames x) {
        Option<String> foreign = x.getForeignLanguage();
        if (foreign.isSome()) {
            String f = foreign.unwrap();
            if (f.equals("java")) {
                String apiName = x.getApiName().getText();
                List<AliasedSimpleName> aliasedNames = x.getAliasedNames();
                for (AliasedSimpleName n : aliasedNames) {
                    Option<IdOrOpOrAnonymousName> aliasId = n.getAlias();
                    if (aliasId.isSome()) {
                        String alias = getStringFromIdOrOpOrAnonymousName(aliasId.unwrap());
                        String name =  getStringFromIdOrOpOrAnonymousName(n.getName());
                        String aliasedName = apiName + "." + name;
                        aliasTable.put(alias, aliasedName);
                    }
                }
            }
        }
    }
}
