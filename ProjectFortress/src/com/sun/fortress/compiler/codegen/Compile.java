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
import org.objectweb.asm.Type;
import edu.rice.cs.plt.tuple.Option;

import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.interpreter.evaluator.types.*;
import com.sun.fortress.interpreter.evaluator.values.*;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

public class Compile extends NodeAbstractVisitor_void {
    ClassWriter cw;
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;
    String className;
    Boolean debug = true;
    HashMap<String, String> aliasTable;
    static String dollar = "$";
    static Character dot = '.';
    static Character slash = '/';
    static String packageName = "";
    String cache = ProjectProperties.BYTECODE_CACHE_DIR + slash;

    // Classes: internal names
    // (Section 2.1.2 in ASM 3.0: A Java bytecode engineering library)
    String internalFloat  = Type.getInternalName(float.class);
    String internalInt    = Type.getInternalName(int.class);
    String internalObject = Type.getInternalName(Object.class);
    String internalString = Type.getInternalName(String.class);

    // Classes: type descriptors
    // (Section 2.1.3 in ASM 3.0: A Java bytecode engineering library)
    private String internalToDesc(String type) {
        return "L" + type + ";";
    }
    private String makeMethodDesc(String param, String result) {
        return "(" + param + ")" + result;
    }
    private String makeArrayDesc(String element) {
        return "[" + element;
    }
    String descFloat  = Type.getDescriptor(float.class);
    String descInt    = Type.getDescriptor(int.class);
    String descString = internalToDesc(internalString);
    String descVoid   = Type.getDescriptor(void.class);
    String stringArrayToVoid = makeMethodDesc(makeArrayDesc(descString), descVoid);
    String voidToVoid        = makeMethodDesc("", descVoid);

    // fortress types
    String fortressPackage = "fortress";
    String fortressAny = fortressPackage + slash + WellKnownNames.anyTypeLibrary() +
                         dollar + WellKnownNames.anyTypeName;

    // fortress interpreter types: internal names
    private String makeFortressInternal(String type) {
        return "com/sun/fortress/interpreter/evaluator/values/F" + type;
    }
    String internalFortressFloat  = makeFortressInternal("FLoat");
    String internalFortressInt    = makeFortressInternal("Int");
    String internalFortressString = makeFortressInternal("String");
    String internalFortressVoid   = makeFortressInternal("Void");

    // fortress interpreter types: type descriptors
    String descFortressFloat  = internalToDesc(internalFortressFloat);
    String descFortressInt    = internalToDesc(internalFortressInt);
    String descFortressString = internalToDesc(internalFortressString);
    String descFortressVoid   = internalToDesc(internalFortressVoid);

    String voidToFortressVoid = makeMethodDesc("", descFortressVoid);

    private String emitFnDeclDesc(com.sun.fortress.nodes.Type domain,
                                  com.sun.fortress.nodes.Type range) {
        String emitDomain = NodeUtil.isVoidType(domain) ? ""     : emitDesc(domain);
        String emitRange  = NodeUtil.isVoidType(range)  ? descFortressVoid : emitDesc(range);
        return makeMethodDesc(emitDomain, emitRange);
    }

    private String emitDesc(com.sun.fortress.nodes.Type type) {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                sayWhat( x );
            }
            public String forArrowType(ArrowType t) {
                return makeMethodDesc(emitDesc(t.getDomain()),
                                      emitDesc(t.getRange()));
            }
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) ) return descFortressVoid;
                else                          return sayWhat( t );
            }
            public String forTraitType(TraitType t) {
                if ( t.getName().getText().equals("String") )
                    return descFortressString;
                else if ( t.getName().getText().equals("Float") )
                    return descFortressFloat;
                else
                    return sayWhat( t );
            }
        });
    }

    private String makeClassName(TraitObjectDecl t) {
        return packageName + className + dollar + NodeUtil.getName(t).getText();
    }

    @SuppressWarnings("unchecked")
    public void writeClass(String repository, String file, byte[] bytes) {
        String fileName = repository + file.replace(dot, slash) + ".class";
        writeClass(bytes, fileName);
    }

    /**
     * @param bytes
     * @param fileName
     */
    public static void writeClass(byte[] bytes, String fileName) {
        String directoryName = fileName.substring(0, fileName.lastIndexOf(slash));
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
                            stringArrayToVoid, null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "run", voidToFortressVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 1);
        mv.visitEnd();
    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", voidToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalObject, "<init>", voidToVoid);
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

    private <T> T sayWhat(ASTNode x) {
        throw new CompilerError(NodeUtil.getSpan(x), "Can't compile " + x);
    }

    private <T> T sayWhat(ASTNode x, String message) {
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
        if ( exportsDefaultLibrary ) packageName = fortressPackage + slash;
        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 packageName + className, null, internalObject, null);

        // Always generate the init method
        generateInitMethod();

        // If this component exports an executable API,
        // generate the main and run methods.

        if ( exportsExecutable ) {
            generateMainMethod();
        }
        for ( Import i : x.getImports() ) i.accept(this);
        // generate code only for top-level functions and variables
        for ( Decl d : x.getDecls() ) {
            if ( d instanceof VarDecl || d instanceof FnDecl )
                d.accept(this);
        }
        dumpClass( packageName + className );

        // generate code only for traits and objects
        for ( Decl d : x.getDecls() ) {
            if ( d instanceof ObjectDecl || d instanceof TraitDecl )
                d.accept(this);
            else if ( ! (d instanceof VarDecl || d instanceof FnDecl) )
                sayWhat(d);
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
                                       apiName + dot +
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
            header.getMods().isEmpty()         && // no modifiers
            ( extendsC.isEmpty() || extendsC.size() == 1); // 0 or 1 super trait
        if ( canCompile ) {
            String parent = "";
            if ( extendsC.isEmpty() ) {
                parent = internalObject;
            } else { // if ( extendsC.size() == 1 )
                BaseType parentType = extendsC.get(0).getBaseType();
                if ( parentType instanceof AnyType )
                    parent = fortressAny;
                else if ( parentType instanceof TraitType ) {
                    Id name = ((TraitType)parentType).getName();
                    Option<APIName> apiName = name.getApiName();
                    if ( apiName.isNone() ) parent = name.toString();
                    else { // if ( apiName.isSome() )
                        String api = apiName.unwrap().getText();
                        if ( WellKnownNames.exportsDefaultLibrary( api ) )
                            parent += fortressPackage + dot;
                        parent += api + dollar + name.getText();
                    }
                } else
                    sayWhat( parentType, "Invalid type in an extends clause." );
            }
            String classFile = makeClassName(x);
            cw = new ClassWriter(0);
            cw.visit( Opcodes.V1_5,
                      Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                      classFile, null, internalObject, new String[] { parent });
            dumpClass( classFile );
        } else sayWhat( x );
    }

    public void forDo(Do x) {
        List<Block> fronts = x.getFronts();
        for (Block b : fronts) {
            b.accept(this);
        }
    }


    public void forObjectDecl(ObjectDecl x) {
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            x.getParams().isNone() &&             // no parameters
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getDecls().isEmpty() &&        // no members
            header.getMods().isEmpty()         && // no modifiers
            ( extendsC.isEmpty() || extendsC.size() == 1 ); // 0 or 1 super trait
        if ( canCompile ) {
            String parent = "";
            if ( extendsC.isEmpty() ) {
                parent = internalObject;
            } else { // if ( extendsC.size() == 1 )
                BaseType parentType = extendsC.get(0).getBaseType();
                if ( parentType instanceof AnyType )
                    parent = fortressAny;
                else if ( parentType instanceof TraitType ) {
                    Id name = ((TraitType)parentType).getName();
                    Option<APIName> apiName = name.getApiName();
                    if ( apiName.isNone() ) parent = name.toString();
                    else { // if ( apiName.isSome() )
                        String api = apiName.unwrap().getText();
                        if ( WellKnownNames.exportsDefaultLibrary( api ) )
                            parent += fortressPackage + dot;
                        parent += api + dollar + name.getText();
                    }
                } else
                    sayWhat( parentType, "Invalid type in an extends clause." );
            }
            String classFile = makeClassName(x);
            cw = new ClassWriter(0);
            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                      classFile, null, internalObject, new String[] { parent });
            dumpClass( classFile );
        } else sayWhat( x );
    }

    public void forFnDecl(FnDecl x) {
        Id unambiguousName = x.getUnambiguousName();
        FnHeader header = x.getHeader();
        boolean canCompile =
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty();           // no modifiers

        if ( canCompile ) {
            Option<Expr> body = x.getBody();
            IdOrOpOrAnonymousName name = header.getName();
            Option<com.sun.fortress.nodes.Type> returnType = header.getReturnType();
            if ( body.isNone() ) {
                sayWhat(x, "Abstract function declarations are not supported.");
            } else if ( returnType.isNone() ) {
                sayWhat(x, "Return type is not inferred.");
            } else if ( name instanceof Id ) {
                mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
                                    ((Id)name).getText(),
                                    emitFnDeclDesc(NodeUtil.getParamType(x),
                                                   returnType.unwrap()),
                                    null, null);
                // For now assuming one parameter.
                mv.visitVarInsn(Opcodes.ALOAD, 0);                
                mv.visitCode();
                body.unwrap().accept(this);
                mv.visitMaxs(2, 1);
                mv.visitEnd();
            } else sayWhat(x, "Operator declarations are not supported.");
        } else sayWhat( x );
    }

    public void forBlock(Block x) {
        for ( Expr e : x.getExprs() ) {
            e.accept(this);
        }
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "com/sun/fortress/interpreter/evaluator/values/FVoid",
                          "V", "Lcom/sun/fortress/interpreter/evaluator/values/FVoid;");
    }

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        String name = x.getOriginalName().getText();
        if ( aliasTable.containsKey(name) ) {
            String n = aliasTable.get(name);
            // Cheating by assuming class is everything before the dot.
            int lastDot = n.lastIndexOf(dot);
            String internal_class = n.substring(0, lastDot).replace(dot, slash);
            String _method = n.substring(lastDot+1);
            Debug.debug( Debug.Type.COMPILER, 1,
                         "class = " + internal_class + " method = " + _method );
            Option<com.sun.fortress.nodes.Type> type = x.getInfo().getExprType();
            if ( type.isNone() )
                sayWhat( x, "The type of this expression is not inferred." );
            else {
                com.sun.fortress.nodes.Type arrow = type.unwrap();
                if ( arrow instanceof ArrowType )
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, internal_class,
                                       _method, emitDesc(arrow));
                else // if ( ! arrow instanceof ArrowType )
                    sayWhat( x, "The type of a function reference " +
                             "is not an arrow type." );
            }
        } else {
            List<IdOrOp> names = x.getNames();
            // For now assuming only 1
            for (IdOrOp stupidName : names) {
                String nameString = stupidName.getText();
                Option<APIName> apiName = stupidName.getApiName();
                if (apiName.isSome()) {
                    APIName foo = apiName.unwrap();
                    String apiString = foo.getText();
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                       "fortress/" + apiString, nameString,
                                       "(Lcom/sun/fortress/interpreter/evaluator/values/FString;)Lcom/sun/fortress/interpreter/evaluator/values/FVoid;");
                }
            }
        }
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        x.getArgument().accept(this);
        x.getFunction().accept(this);
        mv.visitInsn(Opcodes.RETURN);
    }

    public void forFloatLiteralExpr(FloatLiteralExpr x) {
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFloat, "parseFloat",
                           makeMethodDesc(descString, descFloat));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFortressFloat, "make",
                           makeMethodDesc(descFloat, descFortressFloat));

    }

    public void forIntLiteralExpr(IntLiteralExpr x) {
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalInt, "parseInt",
                           makeMethodDesc(descString, descInt));
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFortressInt, "make",
                           makeMethodDesc(descInt, descFortressInt));
    }

    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FString and push it on the stack.
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFortressString, "make",
                           makeMethodDesc(descString, descFortressString));
    }
}
