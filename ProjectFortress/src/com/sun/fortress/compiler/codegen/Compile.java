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
import java.util.*;
import org.objectweb.asm.*;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

public class Compile extends NodeAbstractVisitor_void {
    ClassWriter cw;
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;
    String className;
    HashMap<String, String> aliasTable;
    String cache = ProjectProperties.BYTECODE_CACHE_DIR + "/";
    String packageName = "";
    String javaLangObject = "java/lang/Object";

    private String makeClassName(TraitDecl t) {
        return packageName + className + "$" + NodeUtil.getName(t).getText();
    }

    @SuppressWarnings("unchecked")
    public void writeClass(String repository, String file, byte[] bytes) {
        String fileName = repository + file.replace('.', '/') + ".class";
        String directoryName = fileName.substring(0, fileName.lastIndexOf('/'));
        try {
            ProjectProperties.ensureDirectoryExists(directoryName);
            FileOutputStream out = new FileOutputStream(fileName);
            out.write(bytes);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void generateMainMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            "([Ljava/lang/String;)V", null, null);
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
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, javaLangObject, "<init>", "()V");
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public Compile() {
        // Should be called only by
        // com.sun.fortress.compiler.nativeInterface.FortressTransformer.transform
        // Should be deleted after moving Compile.writeClass to a better place.
    }

    public Compile(String n) {
        className = n;
        aliasTable = new HashMap<String, String>();
        Debug.debug( Debug.Type.COMPILER, 1, "Compile: Compiling " + className );
    }

    public void dumpClass( String file ) {
        cw.visitEnd();
        writeClass(cache, file, cw.toByteArray());
    }

    private void sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x),
                                "Can't compile " + x);
    }

    private void sayWhat(ASTNode x, String message) {
        throw new CompilerError(NodeUtil.getSpan(x), message);
    }

    public void defaultCase(ASTNode x) {
        System.out.println("defaultCase: " + x + " of class " + x.getClass());
        sayWhat(x);
    }

    public void forComponent(Component x) {
        cw = new ClassWriter(0);
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
        if ( exportsDefaultLibrary ) packageName = "fortress/";
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 packageName + className, null, javaLangObject, null);
        // If this component exports an executable API,
        // generate the main and run methods.
        if ( exportsExecutable ) {
            generateInitMethod();
            generateMainMethod();
        }
        for ( Import i : x.getImports() ) i.accept(this);
        // generate code only for top-level functions and variables
        for ( Decl d : x.getDecls() ) {
            if ( // d instanceof VarDecl || -- NOT YET IMPLEMENTED
                 d instanceof FnDecl )
                d.accept(this);
        }
        dumpClass( packageName + className );

        // generate code only for traits and objects
        for ( Decl d : x.getDecls() ) {
            if ( // d instanceof ObjectDecl || -- NOT YET IMPLEMENTED
                 d instanceof TraitDecl )
                d.accept(this);
        }
    }

    public void forImportNames(ImportNames x) {
        Option<String> foreign = x.getForeignLanguage();
        if ( foreign.isSome() ) {
            if ( foreign.unwrap().equals("java") ) {
                String apiName = x.getApiName().getText();
                for ( AliasedSimpleName n : x.getAliasedNames() ) {
                    Option<IdOrOpOrAnonymousName> aliasId = n.getAlias();
                    if (aliasId.isSome())
                        aliasTable.put(NodeUtil.nameString(aliasId.unwrap()),
                                       apiName + "." +
                                       NodeUtil.nameString(n.getName()));
                }
            }
        }
    }

    public void forDecl(Decl x) {
        if (x instanceof TraitDecl)
            ((TraitDecl) x).accept(this);
        else if (x instanceof FnDecl)
            ((FnDecl) x).accept(this);
        else
            sayWhat(x);
    }

    public void forTraitDecl(TraitDecl x) {
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            x.getExcludesClause().isEmpty() &&    // no excludes clause
            x.getComprisesClause().isNone() &&    // no comprises clause
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getDecls().isEmpty() &&        // no members
            header.getMods() == Modifiers.None && // no modifiers
            ( extendsC.isEmpty() || extendsC.size() == 1); // 0 or 1 super trait
        String parent = "";
        if ( extendsC.isEmpty() ) {
            parent = javaLangObject;
        } else { // if ( extendsC.size() == 1 )
            BaseType parentType = extendsC.get(0).getBaseType();
            if ( parentType instanceof AnyType )
                parent = "fortress/AnyType$Any";
            else if ( parentType instanceof TraitType ) {
                Id name = ((TraitType)parentType).getName();
                Option<APIName> apiName = name.getApiName();
                if ( apiName.isNone() ) parent = name.toString();
                else { // if ( apiName.isSome() )
                    String api = apiName.unwrap().getText();
                    if ( WellKnownNames.exportsDefaultLibrary( api ) )
                        parent += "fortress.";
                    parent += api + "$" + name.getText();
                }
            } else
                sayWhat( parentType, "Invalid type in an extends clause." );
        }
        String classFile = makeClassName(x);
        if ( canCompile ) {
            cw = new ClassWriter(0);
            cw.visit( Opcodes.V1_5,
                      Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                      classFile, null, javaLangObject, new String[] { parent });
            dumpClass( classFile );
        }
    }

    public void forFnDecl(FnDecl x) {
        IdOrOpOrAnonymousName name = x.getHeader().getName();
        Option<Expr> body = x.getBody();
        if ( ! body.isSome() ) {
            sayWhat(x, "Abstract function declarations are not supported.");
        } else if ( name instanceof Id ) {
            mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                ((Id)name).getText(), "()V", null, null);
            mv.visitCode();
            body.unwrap().accept(this);
            mv.visitMaxs(2,1);
            mv.visitEnd();
        } else {
            sayWhat(x, "Operator declarations are not supported.");
        }
    }

    public void forDo(Do x) {
        for ( Block b : x.getFronts() ) {
            b.accept(this);
        }
    }

    public void forBlock(Block x) {
        for ( Expr e : x.getExprs() ) {
            e.accept(this);
        }
    }

    public void forFloatLiteralExpr(FloatLiteralExpr x) {
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "java/lang/Float",
                           "parseFloat",
                           "(Ljava/lang/String;)F");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com/sun.fortress/interpreter/evaluator/values/FFLoat",
                           "make",
                           "(F)Lcom/sun/fortress/interpreter/evaluator/values/FFloat");
    }

    public void forIntLiteralExpr(IntLiteralExpr x) {
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "java/lang/Int",
                           "parseInt",
                           "(Ljava/lang/String;)I");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                           "com/sun.fortress/interpreter/evaluator/values/FInt",
                           "make",
                           "(I)Lcom/sun/fortress/interpreter/evaluator/values/Fnt");
    }


    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        String name = x.getOriginalName().getText();
        if ( aliasTable.containsKey(name) ) {
            String n = aliasTable.get(name);
            // Cheating here.  Need to figure out the type of the function.
            // Also cheating by assuming class is everything before the dot.
            int lastDot = n.lastIndexOf('.');
            String _class = n.substring(0, lastDot);
            String internal_class = _class.replace('.', '/');
            String _method = n.substring(lastDot+1);
            Debug.debug( Debug.Type.COMPILER, 1,
                         "class = " + internal_class + " method = " + _method );
            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
              internal_class, _method,
              "(Lcom/sun/fortress/interpreter/evaluator/values/FString;)Lcom/sun/fortress/interpreter/evaluator/values/FVoid;");
        }
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        x.getArgument().accept(this);
        x.getFunction().accept(this);
        mv.visitInsn(Opcodes.RETURN);
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
}
