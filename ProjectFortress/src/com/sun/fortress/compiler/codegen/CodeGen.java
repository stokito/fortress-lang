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
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.ByteCodeWriter;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.Debug;

public class CodeGen extends NodeAbstractVisitor_void {
    ClassWriter cw;
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;
    String className;
    Symbols symbols;
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
    String internalDouble    = Type.getInternalName(double.class);
    String internalLong    = Type.getInternalName(long.class);
    String internalBoolean    = Type.getInternalName(boolean.class);
    String internalChar    = Type.getInternalName(char.class);
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
    private String makeMethodDesc(List<String> params, String result) {
        String desc ="(";
        for (String param : params) {
            desc = desc + param;
        }
        desc = desc + "(" + result;
        return desc;
    }
    private String makeArrayDesc(String element) {
        return "[" + element;
    }
    String descFloat  = Type.getDescriptor(float.class);
    String descInt    = Type.getDescriptor(int.class);
    String descDouble = Type.getDescriptor(double.class);
    String descLong = Type.getDescriptor(long.class);
    String descBoolean = Type.getDescriptor(boolean.class);
    String descChar = Type.getDescriptor(char.class);
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
        return "com/sun/fortress/compiler/runtimeValues/F" + type;
    }

    String internalFortressZZ32  = makeFortressInternal("ZZ32");
    String internalFortressZZ64  = makeFortressInternal("ZZ64");
    String internalFortressRR32  = makeFortressInternal("RR32");
    String internalFortressRR64  = makeFortressInternal("RR64");
    String internalFortressBool  = makeFortressInternal("Bool");
    String internalFortressChar  = makeFortressInternal("Char");
    String internalFortressString = makeFortressInternal("String");
    String internalFortressVoid   = makeFortressInternal("Void");

    // fortress interpreter types: type descriptors
    String descFortressZZ32  = internalToDesc(internalFortressZZ32);
    String descFortressZZ64  = internalToDesc(internalFortressZZ64);
    String descFortressRR32  = internalToDesc(internalFortressRR32);
    String descFortressRR64  = internalToDesc(internalFortressRR64);
    String descFortressBool  = internalToDesc(internalFortressBool);
    String descFortressChar  = internalToDesc(internalFortressChar);
    String descFortressString = internalToDesc(internalFortressString);
    String descFortressVoid   = internalToDesc(internalFortressVoid);

    String voidToFortressVoid = makeMethodDesc("", descFortressVoid);

    private String emitFnDeclDesc(com.sun.fortress.nodes.Type domain,
                                  com.sun.fortress.nodes.Type range) {
        return makeMethodDesc(NodeUtil.isVoidType(domain) ? "" : emitDesc(domain),
                              emitDesc(range));
    }

    private String emitDesc(com.sun.fortress.nodes.Type type) {
        return type.accept(new NodeAbstractVisitor<String>() {
            public void defaultCase(ASTNode x) {
                sayWhat( x );
            }
            public String forArrowType(ArrowType t) {
                if (NodeUtil.isVoidType(t.getDomain()))
                    return makeMethodDesc("", emitDesc(t.getRange()));
                else return makeMethodDesc(emitDesc(t.getDomain()),
                                      emitDesc(t.getRange()));
            }
            public String forTupleType(TupleType t) {
                if ( NodeUtil.isVoidType(t) ) 
                    return descFortressVoid;
                else {
                    if (t.getVarargs().isSome())
                        sayWhat(t, "Can't compile VarArgs yet");
                    else if (!t.getKeywords().isEmpty()) 
                        sayWhat(t, "Can't compile Keyword args yet");
                    else {
                        List<com.sun.fortress.nodes.Type> elements = t.getElements();
                        Iterator<com.sun.fortress.nodes.Type> it = elements.iterator();
                        String res = "";
                        while (it.hasNext()) {
                            res = res + emitDesc(it.next());
                        }
                        return res;
                    }
                    return sayWhat( t );
                }
            }
            public String forTraitType(TraitType t) {
                if ( t.getName().getText().equals("String") )
                    return descFortressString;
                else if ( t.getName().getText().equals("ZZ32") )
                    return descFortressZZ32;
                else if ( t.getName().getText().equals("ZZ64") )
                    return descFortressZZ64;
                else if ( t.getName().getText().equals("RR32") )
                    return descFortressRR32;
                else if ( t.getName().getText().equals("RR64") )
                    return descFortressRR64;
                else if ( t.getName().getText().equals("Bool"))
                    return descFortressBool;
                else if ( t.getName().getText().equals("Char"))
                    return descFortressChar;
                else
                    return sayWhat( t );
            }
        });
    }

    private String makeClassName(TraitObjectDecl t) {
        return packageName + className + dollar + NodeUtil.getName(t).getText();
    }

    private void generateMainMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "main",
                            stringArrayToVoid, null, null);
        mv.visitCode();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, className, "run", voidToFortressVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitEnd();
    }

    private void generateInitMethod() {
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", voidToVoid, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, internalObject, "<init>", voidToVoid);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitEnd();
    }


    public CodeGen(String n, Symbols s) {
        className = n;
        symbols = s;
        aliasTable = new HashMap<String, String>();
        Debug.debug( Debug.Type.CODEGEN, 1, "Compile: Compiling " + className );
    }

    public void dumpClass( String file ) {
        cw.visitEnd();
        ByteCodeWriter.writeClass(cache, file, cw.toByteArray());
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
            packageName = fortressPackage + slash;
        else
            packageName = "";
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
                                       apiName + dot +
                                       NodeUtil.nameString(n.getName()));
                    }
                }
            }
        }
    }

    public void forDecl(Decl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forImportNames" + x);
        if (x instanceof TraitDecl)
            ((TraitDecl) x).accept(this);
        else if (x instanceof FnDecl)
            ((FnDecl) x).accept(this);
        else
            sayWhat(x);
    }

    public void forTraitDecl(TraitDecl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forTraitDecl" + x);
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
            ClassWriter prev = cw;
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit( Opcodes.V1_5,
                      Opcodes.ACC_PUBLIC + Opcodes.ACC_ABSTRACT + Opcodes.ACC_INTERFACE,
                      classFile, null, internalObject, new String[] { parent });
            dumpClass( classFile );
            cw = prev;
        } else sayWhat( x );
    }

    public void forObjectDecl(ObjectDecl x) {
        Debug.debug(Debug.Type.CODEGEN, 1, "forObjectDecl" + x);
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
            ClassWriter prev = cw;
            cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
            cw.visit( Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                      classFile, null, internalObject, new String[] { parent });
            generateInitMethod();
            dumpClass( classFile );
            cw = prev;
        } else sayWhat( x );
    }

    public void forFnDecl(FnDecl x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forFnDecl " + x);
        FnHeader header = x.getHeader();
        int paramsSize = header.getParams().size();
        boolean canCompile =
            header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            header.getMods().isEmpty() &&         // no modifiers
            ( paramsSize < 2 );

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
                // For now, only support zero or one parameter.
                if ( paramsSize == 1 )
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitCode();
                body.unwrap().accept(this);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(2, 1);
                mv.visitEnd();
            } else sayWhat(x, "Operator declarations are not supported.");
        } else sayWhat( x );
    }

    public void forDo(Do x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forDo " + x);
        for ( Block b : x.getFronts() ) {
            b.accept(this);
        }
    }

    public void forBlock(Block x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forBlock " + x);
        for ( Expr e : x.getExprs() ) {
            e.accept(this);
        }
    }

    // Setting up the alias table which we will refer to at runtime.
    public void forFnRef(FnRef x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forFnRef " + x);
        String name = x.getOriginalName().getText();
        if ( aliasTable.containsKey(name) ) {
            String n = aliasTable.get(name);
            // Cheating by assuming class is everything before the dot.
            int lastDot = n.lastIndexOf(dot);
            String internal_class = n.substring(0, lastDot).replace(dot, slash);
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
                                       _method, emitDesc(arrow));
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
                                       fortressPackage + slash + apiName.unwrap().getText(),
                                       fnName.getText(),
                                       makeMethodDesc(descFortressString,
                                                      descFortressVoid));
                } else // if ( apiName.isNone() )
                    sayWhat( x, "API name is not disambiguated." );
            } else // if ( names.size() != 1 )
                sayWhat( x, "Overloaded function references are not supported." );
        }
    }

    public void for_RewriteFnApp(_RewriteFnApp x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"for_RewriteFnApp " + x);
        x.getArgument().accept(this);
        x.getFunction().accept(this);
    }


    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FString and push it on the stack.
        Debug.debug( Debug.Type.CODEGEN, 1,"forStringLiteral " + x);
        mv.visitLdcInsn(x.getText());
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFortressString, "make",
                           makeMethodDesc(descString, descFortressString));
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forVoidLiteral " + x);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, internalFortressVoid, "make",
                           makeMethodDesc("", descFortressVoid));
    }
}
