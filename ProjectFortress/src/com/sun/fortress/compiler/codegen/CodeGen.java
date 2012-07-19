/*******************************************************************************
    Copyright 2009,2011, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

******************************************************************************/
package com.sun.fortress.compiler.codegen;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import scala.Tuple2;

import com.sun.fortress.compiler.GlobalEnvironment;
import com.sun.fortress.compiler.NamingCzar;
import com.sun.fortress.compiler.OverloadSet;
import com.sun.fortress.compiler.WellKnownNames;
import com.sun.fortress.compiler.index.ApiIndex;
import com.sun.fortress.compiler.index.ComponentIndex;
import com.sun.fortress.compiler.index.DeclaredFunction;
import com.sun.fortress.compiler.index.DeclaredMethod;
import com.sun.fortress.compiler.index.FieldGetterMethod;
import com.sun.fortress.compiler.index.FieldSetterMethod;
import com.sun.fortress.compiler.index.Function;
import com.sun.fortress.compiler.index.Functional;
import com.sun.fortress.compiler.index.FunctionalMethod;
import com.sun.fortress.compiler.index.HasSelfType;
import com.sun.fortress.compiler.index.Method;
import com.sun.fortress.compiler.index.ParametricOperator;
import com.sun.fortress.compiler.index.TraitIndex;
import com.sun.fortress.compiler.index.TypeConsIndex;
import com.sun.fortress.compiler.nativeInterface.SignatureParser;
import com.sun.fortress.compiler.typechecker.StaticTypeReplacer;
import com.sun.fortress.compiler.typechecker.TypeNormalizer;
import com.sun.fortress.exceptions.CompilerError;
import com.sun.fortress.nodes.APIName;
import com.sun.fortress.nodes.Assignment;
import com.sun.fortress.nodes.ASTNode;
import com.sun.fortress.nodes.AbbreviatedType;
import com.sun.fortress.nodes.AnyType;
import com.sun.fortress.nodes.ArrowType;
import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Block;
import com.sun.fortress.nodes.BoolArg;
import com.sun.fortress.nodes.BooleanLiteralExpr;
import com.sun.fortress.nodes.BottomType;
import com.sun.fortress.nodes.CaseClause;
import com.sun.fortress.nodes.CaseExpr;
import com.sun.fortress.nodes.Catch;
import com.sun.fortress.nodes.CatchClause;
import com.sun.fortress.nodes.ChainExpr;
import com.sun.fortress.nodes.CharLiteralExpr;
import com.sun.fortress.nodes.Component;
import com.sun.fortress.nodes.CompoundAssignmentInfo;
import com.sun.fortress.nodes.Decl;
import com.sun.fortress.nodes.DimArg;
import com.sun.fortress.nodes.Do;
import com.sun.fortress.nodes.Expr;
import com.sun.fortress.nodes.Fixity;
import com.sun.fortress.nodes.FloatLiteralExpr;
import com.sun.fortress.nodes.FnDecl;
import com.sun.fortress.nodes.FnExpr;
import com.sun.fortress.nodes.FnHeader;
import com.sun.fortress.nodes.FnRef;
import com.sun.fortress.nodes.FunctionalRef;
import com.sun.fortress.nodes.GeneratorClause;
import com.sun.fortress.nodes.Id;
import com.sun.fortress.nodes.IdOrOp;
import com.sun.fortress.nodes.IdOrOpOrAnonymousName;
import com.sun.fortress.nodes.If;
import com.sun.fortress.nodes.IfClause;
import com.sun.fortress.nodes.Import;
import com.sun.fortress.nodes.ImportNames;
import com.sun.fortress.nodes.ImportStar;
import com.sun.fortress.nodes.IntArg;
import com.sun.fortress.nodes.IntLiteralExpr;
import com.sun.fortress.nodes.IntersectionType;
import com.sun.fortress.nodes.KindOp;
import com.sun.fortress.nodes.Lhs;
import com.sun.fortress.nodes.LValue;
import com.sun.fortress.nodes.Link;
import com.sun.fortress.nodes.LocalVarDecl;
import com.sun.fortress.nodes.MethodInfo;
import com.sun.fortress.nodes.MethodInvocation;
import com.sun.fortress.nodes.NamedType;
import com.sun.fortress.nodes.Node;
import com.sun.fortress.nodes.NodeAbstractVisitor_void;
import com.sun.fortress.nodes.ObjectDecl;
import com.sun.fortress.nodes.Op;
import com.sun.fortress.nodes.OpArg;
import com.sun.fortress.nodes.OpExpr;
import com.sun.fortress.nodes.OpRef;
import com.sun.fortress.nodes.Param;
import com.sun.fortress.nodes.Pattern;
import com.sun.fortress.nodes.PatternBinding;
import com.sun.fortress.nodes.PlainPattern;
import com.sun.fortress.nodes.SelfType;
import com.sun.fortress.nodes.StaticArg;
import com.sun.fortress.nodes.StaticParam;
import com.sun.fortress.nodes.StaticParamKind;
import com.sun.fortress.nodes.StringLiteralExpr;
import com.sun.fortress.nodes.SubscriptExpr;
import com.sun.fortress.nodes.Throw;
import com.sun.fortress.nodes.TraitDecl;
import com.sun.fortress.nodes.TraitObjectDecl;
import com.sun.fortress.nodes.TraitSelfType;
import com.sun.fortress.nodes.TraitType;
import com.sun.fortress.nodes.TraitTypeHeader;
import com.sun.fortress.nodes.TraitTypeWhere;
import com.sun.fortress.nodes.Try;
import com.sun.fortress.nodes.TupleExpr;
import com.sun.fortress.nodes.TupleType;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.TypeArg;
import com.sun.fortress.nodes.TypeOrPattern;
import com.sun.fortress.nodes.TypePattern;
import com.sun.fortress.nodes.Typecase;
import com.sun.fortress.nodes.TypecaseClause;
import com.sun.fortress.nodes.UnitArg;
import com.sun.fortress.nodes.VarDecl;
import com.sun.fortress.nodes.VarRef;
import com.sun.fortress.nodes.VarType;
import com.sun.fortress.nodes.VoidLiteralExpr;
import com.sun.fortress.nodes.While;
import com.sun.fortress.nodes._RewriteFnApp;
import com.sun.fortress.nodes._RewriteFnOverloadDecl;
import com.sun.fortress.nodes_util.ExprFactory;
import com.sun.fortress.nodes_util.Modifiers;
import com.sun.fortress.nodes_util.NodeComparator;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.SourceLoc;
import com.sun.fortress.nodes_util.Span;
import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.runtimeSystem.BAlongTree;
import com.sun.fortress.runtimeSystem.InitializedInstanceField;
import com.sun.fortress.runtimeSystem.InitializedStaticField;
import com.sun.fortress.runtimeSystem.InstantiatingClassloader;
import com.sun.fortress.runtimeSystem.Naming;
import com.sun.fortress.runtimeSystem.RTHelpers;
import com.sun.fortress.scala_src.overloading.OverloadingOracle;
import com.sun.fortress.scala_src.typechecker.CFormula;
import com.sun.fortress.scala_src.typechecker.Formula;
import com.sun.fortress.scala_src.types.TypeAnalyzer;
import com.sun.fortress.scala_src.useful.STypesUtil;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.ConcatenatedList;
import com.sun.fortress.useful.Debug;
import com.sun.fortress.useful.DefaultComparator;
import com.sun.fortress.useful.DeletedList;
import com.sun.fortress.useful.F;
import com.sun.fortress.useful.Fn;
import com.sun.fortress.useful.InsertedList;
import com.sun.fortress.useful.MagicNumbers;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.Pair;
import com.sun.fortress.useful.StringHashComparer;
import com.sun.fortress.useful.SwappedList;
import com.sun.fortress.useful.TopSort;
import com.sun.fortress.useful.TopSortItemImpl;
import com.sun.fortress.useful.Useful;

import edu.rice.cs.plt.collect.IndexedRelation;
import edu.rice.cs.plt.collect.PredicateSet;
import edu.rice.cs.plt.collect.Relation;
import edu.rice.cs.plt.tuple.Null;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Wrapper;

// Note we have a name clash with org.objectweb.asm.Type
// and com.sun.fortress.nodes.Type.  If anyone has a better
// solution than writing out their entire types, please
// shout out.
public class CodeGen extends NodeAbstractVisitor_void implements Opcodes {

    private static final boolean DEBUG_OVERLOADED_METHOD_CHAINING =
        ProjectProperties.getBoolean("fortress.debug.overloaded.methods", false);
    
    private static final boolean EMIT_ERASED_GENERICS = false;
    private static final boolean OVERLOADED_METHODS = true;

    private static final F<TraitTypeWhere, String> TTWtoString = new F<TraitTypeWhere, String>() {

        @Override
        public String apply(TraitTypeWhere x) {
            if (x.getWhereClause().isSome()) {
                return x.getBaseType().toString() + " where " + x.getWhereClause().unwrap();
            }
            return x.getBaseType().toString();
        }
        
    };

    private final MultiMap<IdOrOp, FunctionalMethod> orphanedFunctionalMethods;
    
    private CodeGenClassWriter cw;
    CodeGenMethodVisitor mv; // Is this a mistake?  We seem to use it to pass state to methods/visitors.
    final String packageAndClassName;
    public String traitOrObjectName; // set to name of current trait or object, as necessary.
    private String springBoardClass; // set to name of trait default methods class, if we are emitting it.

    // traitsAndObjects appears to be dead code.
    // private final Map<String, ClassWriter> traitsAndObjects =
    //     new BATree<String, ClassWriter>(DefaultComparator.normal());
    private final TypeAnalyzer typeAnalyzer;
    private final ParallelismAnalyzer pa;
    private final FreeVariables fv;
    private final FreeVarTypes fvt;
    private final Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> topLevelOverloads;
    private final MultiMap<String, Function> exportedToUnambiguous;
    /**
     * Name+sigs that have been "taken" by overloaded functions already emitted.
     * 
     */
    private  Set<String> topLevelOverloadedNamesAndSigs;
    /**
     * Names+sigs that have been "taken" by overloaded methods already emitted.
     * This is separate because in the case of functional methods, both may be
     * relevant.
     */
    private  Set<String> typeLevelOverloadedNamesAndSigs;

    // lexEnv does not include the top level or object right now, just
    // args and local vars.  Object fields should have been translated
    // to dotted notation at this point, right?  Right?  (No, not.)
    // Actually, you can't; fields are supposed to be in scope in the
    // initializers of later fields, but it is not correct to translate
    // references to them to `self.name` because `self` is not in scope
    // within such initializers.
    private BATree<String, VarCodeGen> lexEnv;
    boolean inATrait = false;
    boolean inAnObject = false;
    boolean inABlock = false;
    private boolean emittingFunctionalMethodWrappers = false;
    public TraitObjectDecl currentTraitObjectDecl = null;

    private boolean fnRefIsApply = false; // FnRef is either apply or closure

    final Component component;
    private final ComponentIndex ci;
    private GlobalEnvironment env;

    /**
     * Some traits and objects end up with initialized static fields as part of
     * their implementation.  They accumulate here, and their initialization
     * is packed into the clinit method.
     * 
     * Null if not in a trait or object scope.
     */
    private List<InitializedStaticField> initializedStaticFields_TO;
    
    
    /**
     * Collections object instance fields. Their initialization
     * is packed into the init method.
     * 
     * Null if not in an object scope.
     */
    private List<InitializedInstanceField> initializedInstanceFields_O;
    private InstanceFields instanceFields;
    
    private class InstanceFields {
    	private List<VarCodeGen> fields;
    	private List<Type> fieldTypes;
    	private List<Expr> fieldInitializers;
    	
    	public InstanceFields() {
    		fields = new ArrayList<VarCodeGen>();
    		fieldTypes = new ArrayList<Type>();
    		fieldInitializers = new ArrayList<Expr>();
    	}
    	
    	public int size() {
    		return fields.size();
    	}
    	
    	public void put(VarCodeGen field, Expr init) {
    		fields.add(field);
    		fieldTypes.add(field.fortressType);
    		fieldInitializers.add(init);
    	}

		public List<VarCodeGen> getFields() {
			return Collections.unmodifiableList(fields);
		}

		public List<Type> getFieldTypes() {
			return Collections.unmodifiableList(fieldTypes);
		}

		public List<Expr> getFieldInitializers() {
			return Collections.unmodifiableList(fieldInitializers);
		}
    }
    
    /**
     * 
     * creates a ClassNameBundle containing java file and name
     * information for the entity represented by id with 
     * the given static type parameters.  Translation
     * data will be harvested if xldata is supplied.
     *  
     * @param id - id of the entity
     * @param original_static_params - list of static type parameters including bounds
     * @param xldata - if not null, static parameters translation data is stored here
     * @return
     */
    public ClassNameBundle new_ClassNameBundle(Id id,
            List<StaticParam> original_static_params,
            Naming.XlationData xldata) {
        String sparams_part =
            NamingCzar.genericDecoration(original_static_params,
                xldata, thisApi());
        return new ClassNameBundle(stemFromId(id, packageAndClassName), sparams_part);
    }


    // Create a fresh codegen object for a nested scope.  Technically,
    // we ought to be able to get by with a single lexEnv, because
    // variables ought to be unique by location etc.  But in practice
    // I'm not assuming we have a unique handle for any variable,
    // so we get a fresh CodeGen for each scope to avoid collisions.
    private CodeGen(CodeGen c) {
        this(c, c.typeAnalyzer);
    }

    private CodeGen(CodeGen c, TypeAnalyzer new_ta) {
        this.cw = c.cw;
        this.mv = c.mv;
        this.packageAndClassName = c.packageAndClassName;
        this.traitOrObjectName = c.traitOrObjectName;
        this.springBoardClass = c.springBoardClass;

        this.typeAnalyzer = new_ta;
        this.pa = c.pa;
        this.fv = c.fv;
        this.fvt = c.fvt;
        this.topLevelOverloads = c.topLevelOverloads;
        this.typeLevelOverloadedNamesAndSigs = c.typeLevelOverloadedNamesAndSigs;
        this.exportedToUnambiguous = c.exportedToUnambiguous;
        this.topLevelOverloadedNamesAndSigs = c.topLevelOverloadedNamesAndSigs;
        this.typeLevelOverloadedNamesAndSigs = c.typeLevelOverloadedNamesAndSigs;

        this.lexEnv = new BATree<String,VarCodeGen>(c.lexEnv);

        this.inATrait = c.inATrait;
        this.inAnObject = c.inAnObject;
        this.inABlock = c.inABlock;
        this.emittingFunctionalMethodWrappers = c.emittingFunctionalMethodWrappers;
        this.currentTraitObjectDecl = c.currentTraitObjectDecl;
        
        this.initializedStaticFields_TO = c.initializedStaticFields_TO;
        this.initializedInstanceFields_O = c.initializedInstanceFields_O;
      
        this.component = c.component;
        this.ci = c.ci;
        this.env = c.env;
        this.orphanedFunctionalMethods = c.orphanedFunctionalMethods;
    }

    public CodeGen(Component c,
                   TypeAnalyzer ta, ParallelismAnalyzer pa, FreeVariables fv,
                   FreeVarTypes fvt, ComponentIndex ci, GlobalEnvironment env) {
        component = c;
        packageAndClassName = NamingCzar.javaPackageClassForApi(c.getName());
        String dotted = NamingCzar.javaPackageClassForApi(c.getName(), ".");
        JarOutputStream jos;
        try {
            jos = new JarOutputStream(new BufferedOutputStream( new FileOutputStream(NamingCzar.cache + dotted + ".jar")));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.typeAnalyzer = ta;
        this.pa = pa;
        this.fv = fv;
        this.fvt = fvt;
        this.ci = ci;
        this.exportedToUnambiguous = new MultiMap<String, Function> ();
        this.orphanedFunctionalMethods = new MultiMap<IdOrOp, FunctionalMethod>(NodeComparator.idOrOpComparer);
        this.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, jos);
        cw.visitSource(NodeUtil.getSpan(c).begin.getFileName(), null);
        boolean exportsExecutable = false;
        boolean exportsDefaultLibrary = false;
        

        /*
         * Find every exported name, and make an entry mapping name to
         * API declarations, so that unambiguous names from APIs can
         * also be emitted.
         * 
         * However, it looks like this information is currently unused.
         */
        List<APIName> exports = c.getExports();
        for (APIName apiname:exports) {
            if ( WellKnownNames.exportsMain(apiname.getText()) )
                exportsExecutable = true;
            if ( WellKnownNames.exportsDefaultLibrary(apiname.getText()) )
                exportsDefaultLibrary = true;
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

        
        Relation<IdOrOpOrAnonymousName, Function> augmentedFunctions =
            new IndexedRelation<IdOrOpOrAnonymousName, Function>(false);
        
        augmentedFunctions.addAll(ci.functions());
        
        /*
         * Need to spot defined (not abstract) functional methods 
         * from supertraits where opr-parameterization changes
         * the name of the method.  The parameter-named method
         * should NOT appear in top-level functions; the translated
         * method SHOULD.  As an overload, it only needs to be typed
         * with the subtypes that have no opr parameters. 
         */
        Map<Id, TypeConsIndex> some_traits = ci.typeConses();
        /* Use this to avoid creating duplicate overloaded methods.
         * Note that we have to be careful about Java representations
         * of types; when self-types are involved, the naively computed
         * Java representations of two "Fortress-equal" types may not
         * match, which can lead to verify errors.
         * 
         * Ideally, we want to use the more-Java-generic static signature
         * (which is more likely to match and not fail verification)
         * but use the tighter for dynamic tests.
         */
        OverloadingOracle oa = new OverloadingOracle(ta);
        for (Map.Entry<Id,TypeConsIndex> ent : some_traits.entrySet()) {
            TypeConsIndex tci = ent.getValue();
            Id id = ent.getKey();
            List <StaticParam>  tci_sps = tci.staticParameters();
            if (! staticParamsIncludeOpr(tci_sps)) {
                TraitObjectDecl tci_decl = (TraitObjectDecl) tci.ast();
                Relation<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
		    toConsider = STypesUtil.properlyInheritedMethodsNotIdenticallyCovered(id, typeAnalyzer);
                /* This tci has no opr params; we need 
                 * to look at all the functional methods
                 * that it inherits, and if any of them come
                 * from traits with opr params, and if
                 * their name is one of those opr params,
                 * and if this trait/object does not define
                 * that same method itself, then we need
                 * to note the need for such a functional
                 * method, both for its declaration and for
                 * purposes of overloading.
                 */
                for (edu.rice.cs.plt.tuple.Pair<IdOrOpOrAnonymousName,
                        scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
                assoc : toConsider) {
                    IdOrOpOrAnonymousName n = assoc.first();
                    scala.Tuple3<Functional, StaticTypeReplacer, TraitType> tup = assoc.second();
                    
                    Functional fnl = tup._1();
                    if (! (fnl instanceof FunctionalMethod))
                        continue;
                    StaticTypeReplacer inst = tup._2();
                    TraitType tupTrait = tup._3();
                    
                    IdOrOp nn = (IdOrOp) (inst.replaceIn(n));
                    
                    if (! n.equals(nn)) {
                        FunctionalMethod fm = (FunctionalMethod) fnl;
                        FnDecl new_fm_ast = (FnDecl) fm.ast().accept(inst);
                        FunctionalMethod new_fm = new FunctionalMethod(new_fm_ast,
                                tci_decl, tci_sps);
                        // Filter out functions already defined in the overload.
                        boolean duped = false;
                        Set<? extends Functional> defs = augmentedFunctions.matchFirst(nn);
                        for (Functional af : defs) {
                            if (oa.lteq(af, new_fm) && oa.lteq(new_fm, af)) {
                                duped = true;
                                break;
                            }
                        }

                        if (! duped) {
                            // System.err.println("Found orphaned functional method " + nn);
                            augmentedFunctions.add(nn, new_fm);
                            orphanedFunctionalMethods.putItem(id, new_fm);
                        }
                    }
                }
               
            }
        }
        
        this.topLevelOverloads =
            sizePartitionedOverloads(augmentedFunctions);

        this.topLevelOverloadedNamesAndSigs = new HashSet<String>();
        this.lexEnv = new BATree<String,VarCodeGen>(StringHashComparer.V);
        this.env = env;


        String extendedJavaClass =
            exportsExecutable ? "com/sun/fortress/runtimeSystem/BaseTask" :
                                NamingCzar.fortressComponent ;

        cw.visit(InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER,
                 packageAndClassName, null, extendedJavaClass,
                 null);

        debug( "Compile: Compiling ", packageAndClassName );

        // Always generate the init method
        basicInitMethod(extendedJavaClass);
        // If this component exports an executable API,
        // generate a main method.
        if ( exportsExecutable ) {
            generateMainMethod();
        }
    }
    


    private boolean staticParamsIncludeOpr(List<StaticParam> sps) {
        for (StaticParam sp : sps) {
            if (sp.getKind() instanceof KindOp)
                return true;
        }
        return false;
    }


    // We need to expose this because nobody else helping CodeGen can
    // understand unqualified names (esp types) without it!
    public APIName thisApi() {
        return ci.ast().getName();
    }

    /** Factor out method call path so that we do it right
        everywhere we invoke a dotted method of any kind. */
    private void methodCall(IdOrOp method,
                            NamedType receiverType,
                            Type domainType, Type rangeType) {       
        String sig = NamingCzar.jvmSignatureFor(domainType, rangeType, thisApi());
        String methodName = method.getText();
        methodCall(methodName, receiverType, sig);
    }
    
    /** Factor out method call path so that we do it right
        everywhere we invoke a dotted method of any kind.
        Here, we either know the Strings already for name and signature,
        or they could not easily be encoded into AST anyway.
    */
    private void methodCall(String methodName,
            NamedType receiverType,
            String sig) {
        int opcode;
        // TODO BUG
        /*
         * BUG HERE.  When receiverType is a VarType, we are forced to guess
         * what the receiverType will be.  I think we have an opportunity
         * to figure this out at template rewriting time.
         * 
         * FOR NOW, guess INVOKEINTERFACE.
         */
        if (receiverType instanceof VarType || typeAnalyzer.typeCons(((TraitType)receiverType).getName()).ast() instanceof TraitDecl &&
                !NamingCzar.fortressTypeIsSpecial(receiverType)) {
            opcode = INVOKEINTERFACE;
        } else {
            opcode = INVOKEVIRTUAL;
        }
        String methodClass = NamingCzar.jvmTypeDesc(receiverType, thisApi(), false);
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

        mv = cw.visitCGMethod(ACC_PUBLIC + ACC_STATIC, "main",
                            "()V", null, null);
        mv.visitCode();

        // new packageAndClassName()
        mv.visitTypeInsn(NEW, "com/sun/fortress/runtimeSystem/FortressTaskRunnerGroup");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/BaseTask", "getNumThreads", "()I");
        mv.visitMethodInsn(INVOKESPECIAL, "com/sun/fortress/runtimeSystem/FortressTaskRunnerGroup",
                           "<init>",
                           "(I)V");

        mv.visitTypeInsn(NEW, packageAndClassName);
        mv.visitInsn(DUP);        
        mv.visitMethodInsn(INVOKESPECIAL, packageAndClassName,
                           "<init>",
                           "()V");
        mv.visitMethodInsn(INVOKEVIRTUAL, 
                           "com/sun/fortress/runtimeSystem/FortressTaskRunnerGroup",                           
                           "invoke",
                           "(Ljsr166y/ForkJoinTask;)Ljava/lang/Object;");
        mv.visitInsn(POP);
        
        voidEpilogue();

        mv = cw.visitCGMethod(ACC_PUBLIC, "compute",
                             Naming.voidToVoid, null, null);
         mv.visitCode();

         mv.visitVarInsn(ALOAD, mv.getThis());
         mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/BaseTask", "setTask", "(Lcom/sun/fortress/runtimeSystem/BaseTask;)V");

         mv.visitVarInsn(ALOAD, mv.getThis());
         // Call through to static run method in this component.
         mv.visitMethodInsn(INVOKESTATIC, packageAndClassName, "run",
                            NamingCzar.voidToFortressVoid);
         // Discard the FVoid that results
         mv.visitInsn(POP);
         mv.visitVarInsn(ALOAD, mv.getThis());
         mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/fortress/runtimeSystem/BaseTask", "printStatistics", "()V");
         voidEpilogue();
    }

    private void cgWithNestedScope(ASTNode n) {
        CodeGen cg = new CodeGen(this);
        n.accept(cg);
    }

    private void addLocalVar( VarCodeGen v ) {
        debug("addLocalVar ", v);
        lexEnv.put(v.getName(), v);
    }

    private void addStaticVar( VarCodeGen v ) {
        debug("addStaticVar ", v);
        lexEnv.put(v.getName(), v);
    }

    private VarCodeGen addParam(Param p) {
        VarCodeGen v =
            new VarCodeGen.ParamVar(p.getName(), (Type)p.getIdType().unwrap(), this);
        addLocalVar(v);
        return v;
    }

    private VarCodeGen addSelf() {
        Id tid = (Id)currentTraitObjectDecl.getHeader().getName();
        Id id = NodeFactory.makeId(NodeUtil.getSpan(tid), "self");
        Type t = STypesUtil.declToTraitType(currentTraitObjectDecl);
        VarCodeGen v = new VarCodeGen.ParamVar(id, t, this);
        addLocalVar(v);
        return v;
    }

    // Always needs context-sensitive null handling anyway.  TO FIX.
    // private VarCodeGen getLocalVar( ASTNode ctxt, IdOrOp nm ) {
    //     VarCodeGen r = getLocalVarOrNull(nm);
    //     if (r==null) return throw sayWhat(ctxt, "Can't find lexEnv mapping for local var");
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

    private VarCodeGen getLocalVarOrNull( IdOrOp nm, List<StaticArg> lsargs) {
        debug("getLocalVar: ", nm);
        String s = VarCodeGen.varCGName(nm, lsargs);
        VarCodeGen r = lexEnv.get(s);

        if (r != null)
            debug("getLocalVar:", nm, " VarCodeGen = ", r, " of class ", r.getClass());
        else
            debug("getLocalVar:", nm, " VarCodeGen = null");
        return r;
    }


    /**
     * @param nm
     * @param lsargs
     * @return
     */

    public static void popAll(MethodVisitor mv, int onStack) {
        if (onStack == 0) return;
        for (; onStack > 1; onStack -= 2) {
            mv.visitInsn(POP2);
        }
        if (onStack==1) {
            mv.visitInsn(POP);
        }
    }

    private void dumpTraitDecls(List<Decl> decls) {
        debug("dumpDecls", decls);
        for (Decl d : decls) {
            if (d instanceof FnDecl) d.accept(this);
            else if (!(d instanceof VarDecl))
                throw sayWhat(d);
        }
    }

    /**
     * 
     * Generate a an instance method forwarding calls from fromTrait.fnl to
     * the static method toTrait.fnl.
     * 
     * @param from_fnl
     * @param inst
     * @param toTrait
     * @param fromTrait
     */
    private void generateForwardingFor(Functional from_fnl, StaticTypeReplacer inst,
            TraitType fromTrait, TraitType toTrait) {
        generateForwardingFor(from_fnl, from_fnl, false, inst, fromTrait, toTrait, false);
    }
    private void generateForwardingFor(Functional from_fnl, Functional to_fnl, boolean forward_to_non_overload,
            StaticTypeReplacer inst, TraitType fromTrait, TraitType toTrait, boolean narrowing) {
        debug("generateForwarding: from ", from_fnl, " to ", to_fnl);
        IdOrOp name = from_fnl.name();
        if (!(from_fnl instanceof HasSelfType))
            throw sayWhat(name, " method "+from_fnl+" doesn't appear to have self type.");
        HasSelfType st = (HasSelfType)from_fnl;
        int selfIndex = st.selfPosition();
        List<Param> from_params = from_fnl.parameters();
        List<Param> to_params = to_fnl.parameters();
        int arity = from_params.size();
        
        
        String receiverClass = NamingCzar.jvmTypeDescAsTrait(toTrait, component.getName()) +
        NamingCzar.springBoard;
        
        boolean isObject = springBoardClass == null;
        
        if (typeAnalyzer.equiv(toTrait,fromTrait)) {
            receiverClass = springBoardClass;                
            if (narrowing) {
                // Might be wrong for traits
                receiverClass = NamingCzar.jvmTypeDescAsTrait(toTrait, component.getName());
            }
        }

        List<StaticParam> static_parameters = from_fnl.staticParameters();
        String mname;
        if (static_parameters.size() > 0) {
            // TODO must check this name for the narrowing case
            mname = genericMethodName(from_fnl, selfIndex);
            String sig = genericMethodClosureFinderSig;
            arity = 3; // Magic number
            // TODO This could be bogus, but for now, let's try it.
            String from_name = forward_to_non_overload ? NamingCzar.mangleAwayFromOverload(mname): mname;
            InstantiatingClassloader.forwardingMethod(cw, from_name, ACC_PUBLIC,
						      0,   // ZERO is also a magic number
                    receiverClass, mname + Naming.GENERIC_METHOD_FINDER_SUFFIX_IN_TRAIT, INVOKESTATIC,
                    sig, sig, arity, false, null);
        } else {
            Type fromReturnType = (from_fnl.getReturnType().unwrap());
            Type toReturnType =   (to_fnl.getReturnType().unwrap());
            Type fromParamType =  (NodeUtil.getParamType(from_params, NodeUtil.getSpan(name)));
            Type toParamType =    (NodeUtil.getParamType(to_params, NodeUtil.getSpan(name)));
            
            // Think inst is unnecessary for self-forwarding
            if (inst != null) {
                fromReturnType = inst.replaceIn(fromReturnType);
                toReturnType = inst.replaceIn(toReturnType);
                fromParamType = inst.replaceIn(fromParamType);
                toParamType = inst.replaceIn(toParamType);
            }
            
            String from_sig = NamingCzar.jvmSignatureFor(
                    fromParamType,
                    NamingCzar.jvmBoxedTypeDesc(fromReturnType, component.getName()),
                    narrowing ? -1 : 0,
                    toTrait, // TODO should this be fromTrait?  It worked when it was not.
                    component.getName());
            
            String to_sig = NamingCzar.jvmSignatureFor(
                    toParamType,
                    NamingCzar.jvmBoxedTypeDesc(toReturnType, component.getName()),
                    narrowing ? -1 : 0,
                    toTrait,
                    component.getName());
            
            if (selfIndex != Naming.NO_SELF) {
                // TODO Buggy if narrowing self and not-self
                from_sig = Naming.removeNthSigParameter(from_sig, narrowing ? selfIndex : selfIndex+1);
                to_sig = Naming.removeNthSigParameter(to_sig, narrowing ? selfIndex : selfIndex+1);
                mname = Naming.fmDottedName(singleName(name), selfIndex);
            } else {
                mname = singleName(name); // What about static params?
                arity++;
            }
            String from_name = forward_to_non_overload ? NamingCzar.mangleAwayFromOverload(mname): mname;
            String actual_from_sig =
                Naming.removeNthSigParameter(from_sig, st instanceof DeclaredMethod ? 0 : selfIndex);
            boolean already_emitted_as_overload = typeLevelOverloadedNamesAndSigs != null &&
                typeLevelOverloadedNamesAndSigs.contains(from_name+actual_from_sig+"" ) ;
            
            if (already_emitted_as_overload) {
                from_name = NamingCzar.mangleAwayFromOverload(from_name);
            }
            else
                InstantiatingClassloader.forwardingMethod(cw, from_name, ACC_PUBLIC, 0,
                    receiverClass, mname, narrowing ? (isObject ? INVOKEVIRTUAL : INVOKEINTERFACE ): INVOKESTATIC,
                            from_sig, to_sig, narrowing ? null : from_sig, arity, true, null,
                                    toReturnType instanceof BottomType);

        }
        
    }


    /**
      Fortress objects, compiled into classes, will class-inherit default methods
      defined in the first trait they extend (they are compiled to extend its
      trait default methods, which are in a class), but not default methods
      defined in subsequent extended traits.

      To fix this forwarding methods are added to the object to call the
      non-inherited default methods (where appropriate, meaning not defined
      in either the object itself or an earlier trait).

      Forwarding methods are also added to the compiled default-trait-methods
      classes, so that if they are inherited, they will supply all the necessary
      methods, and to ensure that the forwarding analysis is entirely local
      (that is, this trait/object, its first extended trait, and subsequent
      extended traits).

      includeCurrent is true for traits.  default-trait-method bodies are static,
      but instance methods are required for inheritance and dispatch.  For traits,
      in order to inherit the own (current) default methods, instance-to-static
      forwarding methods must be also be added.

      The conditions for adding a forwarding method are:

          1) Inherited through the second and subsequent supertraits -OR-
             includeCurrent and defined in the current trait.

          2) Are not also inherited through the first supertrait
             (due to joins in type hierarchy)

          3) Are not overridden by a method in the present trait or object
          
     Note the assumption that trait inheritance is normalized.
     If "A extends {B,C}" has been normalized,
     then B does not extend C and C does not extend B.
     That is, the extends clause is minimal.
     (There may be some issues with normalized inheritance and
      comprises clauses.)
    */
    private void dumpMethodChaining(String [] superInterfaces, boolean includeCurrent) {
        debug("Dump method chaining");
        /*
         * If the number of supertraits is 0, there is nothing to inherit.
         * If it is one, inheritance comes via the class hierarchy.
         * If not includeCurrent, then there are no own-trait methods to forward.
         *
         * In that case, there is nothing to do.
         */
        if (!includeCurrent && superInterfaces.length <= 1) return;

        TraitType currentTraitObjectType = STypesUtil.declToTraitType(currentTraitObjectDecl);
        List<TraitTypeWhere> extendsClause = NodeUtil.getExtendsClause(currentTraitObjectDecl);
        Relation<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
            alreadyIncluded;
        /*
         * Initialize alreadyIncluded to empty, or to the inherited methods
         * from the first t
         */
        if (extendsClause.size() == 0 || !(extendsClause.get(0).getBaseType() instanceof TraitType)) {
            alreadyIncluded =
                new IndexedRelation<IdOrOpOrAnonymousName,
                             scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>();
        } else {
//          alreadyIncluded = STypesUtil.inheritedMethods(extendsClause.subList(0,1), typeAnalyzer);
            alreadyIncluded = STypesUtil.allMethods((TraitType)(extendsClause.get(0).getBaseType()), typeAnalyzer);
	    debug("Already included: ", alreadyIncluded);
        }
	//        System.err.println("For " + currentTraitObjectType + " the already included methods are " + alreadyIncluded);
        /*
         * Apparently allMethods returns the transitive closure of all methods
         * declared in a particular trait or object and the types it extends.
         * Iterate over all of them, noting the ones with bodies, that are not
         * already defined in this type or the first extending type (those
         * defined in this type are conditional on includeCurrent).
         *
         * Note that extends clauses should be minimal by this point, or at
         * least as-if minimal; we don't want to be dealing with duplicated
         * methods that would not trigger overriding by the meet rule (if the
         * extends clause is minimal, and a method is defined twice in the
         * extends clause, then it needs to be disambiguated in this type).
         */
        Relation<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
            toConsider = STypesUtil.allMethodsNotIdenticallyCovered(currentTraitObjectType, typeAnalyzer);
        debug("Considering chains for ", currentTraitObjectType, ": ", toConsider);
        for (edu.rice.cs.plt.tuple.Pair<IdOrOpOrAnonymousName,scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
                 assoc : toConsider) {
            scala.Tuple3<Functional, StaticTypeReplacer, TraitType> tup = assoc.second();
            TraitType tupTrait = tup._3();
            StaticTypeReplacer inst = tup._2();
            Functional fnl = (tup._1()); // better be true
            //System.err.println("  "+assoc.first()+" "+tupTrait+" "+fnl);
            /* Skip non-definitions. */
            if (!fnl.body().isSome()) {
                // need to worry about tighter definitions in implementing types.
                continue;
            }

            List<StaticParam> static_parameters = fnl.staticParameters();
            
	    //            if (tupTrait.equals(currentTraitObjectType)) {
            if (typeAnalyzer.equiv(tupTrait, currentTraitObjectType)) {
                /* If defined in the current trait. */
                if (includeCurrent) {
                    // Trait, not object
                    generateForwardingFor(fnl, inst, currentTraitObjectType, tupTrait); // swapped
                    generateForwardingFor(fnl, fnl, true, inst, currentTraitObjectType, tupTrait, false); // swapped
                }
            } else {
                /* If not defined in the current trait. */
		boolean alreadyThere = false;

		/* Iterate over tuples for
		 * already-defined methods
		 * whose names match
		 * that of the method being considered (assoc.first()).
		 *
		 * If the trait of the method being considered,
		 * and the trait of any name-matching already included method
		 * match, then don't generate a wrapper.
		 *
		 * DOES THIS HAVE A BUG IN IT?  WHAT ABOUT OVERLOADED METHODS?
		 * Their names will match, but the parameter types need not.
		 */
		debug("For method ", assoc.first(), ", does any of these match: ", alreadyIncluded.matchFirst(assoc.first()));
		for (scala.Tuple3<Functional, StaticTypeReplacer, TraitType> tupAlready :
			 alreadyIncluded.matchFirst(assoc.first())) {
		    if (tupAlready._3().equals(tupTrait)) {
		        alreadyThere = true;
	            break;
		    }
		    
		    StaticTypeReplacer str = tupAlready._2();
		    Type alreadyType = str.replaceIn(tupAlready._3());
		    if (typeAnalyzer.equiv(alreadyType,tupTrait)) {
			//System.err.println("    " + fnl + " already imported by first supertrait.");
			alreadyThere = true;
			break;
		    }
		}
		if (!alreadyThere) {
		    generateForwardingFor(fnl, inst, currentTraitObjectType, tupTrait); // swapped
		    generateForwardingFor(fnl, fnl, true, inst, currentTraitObjectType, tupTrait, false); // swapped
		}
	    }
        }
    }

    /**
     * 
     * Deals with overloading and narrowed signatures in the mapping
     * from Fortress methods to Java methods.
     * 
     * The problem comes, e.g., if
     * trait T 
     *   m(T):T
     * end
     * 
     * object O extends T
     *   m(T):O
     * end
     * 
     * In the translation to Java, O's m has a different return type from T's m,
     * thus it has a different signature, thus it does not properly override it
     * in the generated bytecodes.  This has to be detected, and a forwarding 
     * method for O.(T):T has to be created so that calls to T.m(T):T will
     * arrive at O.(T):O.
     * 
     * There are variants of this problem created when T defines a non-abstract
     * m(T), and O defines m(O) -- in that case, there needs to be O.m(x:T) that
     * performs the overloading.
     * 
     *   if (x instanceof O)
     *   then O.m((O)x)
     *   else T.m(x)
     *   Note that because the overloading is only valid in the case
     *   that T defined a method with a body, T.m(x) is expressed as
     *   T$defaultTraitMethods(self, x).
     * 
     * 
     * @param superInterfaces
     * @param includeCurrent
     * @return 
     */
    
    private Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> dumpOverloadedMethodChaining(String [] superInterfaces, boolean includeCurrent) {
        Map<IdOrOpOrAnonymousName, MultiMap<Integer,Functional>> overloadedMethods =
            new HashMap<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>>();

        /*
         * If the number of supertraits is 0, there is nothing to override.
         */
        if (superInterfaces.length < 1)
            return overloadedMethods;
        
        TraitType currentTraitObjectType = STypesUtil.declToTraitTypeEnhanced(currentTraitObjectDecl);
        // TraitType currentTraitObjectType = STypesUtil.declToTraitType(currentTraitObjectDecl);
        List<TraitTypeWhere> extendsClause = NodeUtil.getExtendsClause(currentTraitObjectDecl);
        
        OverloadingOracle oa =  new OverloadingOracle(typeAnalyzer);
        
        /*
         * properlyInheritedMethods is supposed to return methods that are
         * (potentially) inherited by this trait.  The declaration from the
         * most specific trait declaring a given method signature should be
         * what is returned; HOWEVER, there is a bug, in that return types
         * are apparently ignored, so two methods with same domain but different
         * return types will only result in a single method.  This can cause
         * a problem in the case that a method is available from parent and
         * from great-grandparent, AND if the parent-inheritance is not first
         * position (otherwise, a default implementation appears from the
         * parent, though this might not be the correct one).
         * 
         * Note that extends clauses should be minimal by this point, or at
         * least as-if minimal; we don't want to be dealing with duplicated
         * methods that would not trigger overriding by the meet rule (if the
         * extends clause is minimal, and a method is defined twice in the
         * extends clause, then it needs to be disambiguated in this type).
         */
        if (DEBUG_OVERLOADED_METHOD_CHAINING)
            System.err.println("Considering overloads for "+currentTraitObjectType +
                    ", extended=" + Useful.listTranslatedInDelimiters("[", extendsClause, "]", ",", TTWtoString));
            
        Relation<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
            toConsider = STypesUtil.properlyInheritedMethodsNotIdenticallyCovered(currentTraitObjectType.getName(), typeAnalyzer);
        
        MultiMap<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>> toConsiderFixed =
            new MultiMap<IdOrOpOrAnonymousName, scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>();
        
        for (edu.rice.cs.plt.tuple.Pair<IdOrOpOrAnonymousName,scala.Tuple3<Functional, StaticTypeReplacer, TraitType>>
        assoc : toConsider) {
            IdOrOpOrAnonymousName n = assoc.first();
            scala.Tuple3<Functional, StaticTypeReplacer, TraitType> tup = assoc.second();
            
            Functional fnl = tup._1();
            StaticTypeReplacer inst = tup._2();
            TraitType tupTrait = tup._3();
            
            IdOrOpOrAnonymousName nn = inst.replaceIn(n);
            
            toConsiderFixed.putItem(nn, tup);
            
        }
        
        
        if (DEBUG_OVERLOADED_METHOD_CHAINING) {
            System.err.println("properlyInheritedMethods  (orig)=" + toConsider);
            System.err.println("properlyInheritedMethods (fixed)=" + toConsiderFixed);
        }
        
        TraitIndex ti = (TraitIndex) typeAnalyzer.traits().typeCons(currentTraitObjectType.getName()).unwrap();
        
        // This is used to fix the type annotation in type_info; it lacks
        // constraints on static parameters.
        StaticTypeReplacer local_inst =
            new StaticTypeReplacer(ti.staticParameters(),
                    STypesUtil.staticParamsToArgsEnhanced(ti.staticParameters()));
        
        MultiMap<IdOrOpOrAnonymousName, Functional> nameToFSets =
            new MultiMap<IdOrOpOrAnonymousName, Functional>();
        
        // Sort defined methods into map from single names to sets of defs
        for (Map.Entry<Id, FieldGetterMethod> ent: ti.getters().entrySet()) {
            nameToFSets.putItem(ent.getKey(), ent.getValue());
        }
        for (Map.Entry<Id, FieldSetterMethod> ent: ti.setters().entrySet()) {
            nameToFSets.putItem(ent.getKey(), ent.getValue());
        }
        /*
         *  There's a glitch here, with the names of functional methods,
         *  vs their names as implemented methods.  Static analyzer works with
         *  names-as-written; code generation wishes to mangle them.
         */
        for (edu.rice.cs.plt.tuple.Pair<IdOrOpOrAnonymousName, FunctionalMethod> ent : ti.functionalMethods()) {
            nameToFSets.putItem(ent.first(), ent.second());
        }
        for (edu.rice.cs.plt.tuple.Pair<IdOrOpOrAnonymousName, DeclaredMethod> ent : ti.dottedMethods()) {
            nameToFSets.putItem(ent.first(), ent.second());
        }
        
        /*
         * The subtype-defined methods can overlap in several ways.
         * Classified by D->R (Domain, Range), the following relationships are
         * possible:
         * 
         * EQ -> EQ means Java dispatch works. (shadowed)
         * 
         * EQ -> LT means that the name is wrong;
         *          the (Java) EQ->EQ method must be included.
         *          (EQ->EQ cannot exist in (Fortress) subtype)
         *          
         * LT -> EQ means that there is an overloading.
         *          (EQ -> LT cannot exist in (Fortress) subtype; EQ->EQ can)
         *          
         * LT -> LT means that there is an overloading.
         *          EQ -> LT or EQ -> EQ can exist in subtype;
         *          LT -> EQ can exist in super, if it does,
         *          then it is covered in a diff relationship by EQ -> LT above.
         *          
         * EX -> ... means there is an excluding overloading
         *          
         * 
         * There may also be overloadings present in
         * the methods defined by this trait or object.
         * 
         * Forwarded methods are those that are in EQ -> LT relationship
         * with some super method.
         * 
         * Overload sets contain all methods in the subtype plus all the 
         * unshadowed, unforwarded methods from super.
         * 
         * As overloads, these are similar to function overloads, except that
         * the parameters come in 1-n instead of 0-(n-1), and the invocation
         * varies -- supertype invocation dispatches to the default-methods
         * class, subtype invocation goes, where? (to the static, if subtype
         * is a trait, otherwise, we need to dance the name out of the way).
         * 
         */
        

        for(Map.Entry<IdOrOpOrAnonymousName, Set<Functional>> ent : nameToFSets.entrySet())  {
            IdOrOpOrAnonymousName name = ent.getKey();
            Set<Functional> funcs = ent.getValue();
            
            // initial set of potential overloads is this trait/object's methods
            Set<Functional> perhapsOverloaded = new HashSet<Functional>(funcs);
            
            for (scala.Tuple3<Functional, StaticTypeReplacer, TraitType> overridden :
                toConsiderFixed.getEmptyIfMissing(name)) {
                // toConsider.matchFirst(name)) {
                Functional super_func = overridden._1();
                StaticTypeReplacer super_inst = overridden._2();
                
                if (super_func instanceof ParametricOperator) {
                    // TODO -- substitute and compare, I think.
                    continue;
                }
                
                TraitType traitDeclaringMethod = overridden._3();
                if (typeAnalyzer.equiv(traitDeclaringMethod,currentTraitObjectType))
                    continue;
                
                boolean shadowed = false;  // EQ -> EQ seen
                boolean narrowed = false;  // EQ -> LT seen
                Functional narrowed_func = null;
                
                Type raw_super_ret = oa.getRangeType(super_func);
                
                int super_self_index = NodeUtil.selfParameterIndex(super_func.parameters());
                Type raw_super_noself_domain = oa.getNoSelfDomainType(super_func);
                Type super_noself_domain_possibly_no_params = super_inst.replaceInEverything(raw_super_noself_domain);
                List<StaticParam> better_super_params = new ArrayList<StaticParam>();
                
                /*
                 * Combine all the static params for super, preferring the definitions from the
                 * subtype when names match (those may be narrower).
                 */
                for(StaticParam bsp : super_noself_domain_possibly_no_params.getInfo().getStaticParams()) {
                    boolean use_bsp = true;
                    for (StaticParam sp : currentTraitObjectDecl.getHeader().getStaticParams()) {
                        if (sp.getName().equals(bsp.getName())) {
                            better_super_params.add(sp);
                            use_bsp = false;
                            break;
                        }
                    }
                    if (use_bsp)
                        better_super_params.add(bsp);
                }
                for (StaticParam sp : currentTraitObjectDecl.getHeader().getStaticParams()) {
                    boolean use_sp = true;
                    for(StaticParam bsp : super_noself_domain_possibly_no_params.getInfo().getStaticParams()) {
                        if (sp.getName().equals(bsp.getName())) {
                            use_sp = false;
                            break;
                        }
                    }
                    if (use_sp)
                        better_super_params.add(sp);
                }

                Type super_noself_domain = STypesUtil.insertStaticParams(STypesUtil.clearStaticParams(super_noself_domain_possibly_no_params),
                        better_super_params);

                Type pre_super_ret = super_inst.replaceInEverything(raw_super_ret);
                
                Type super_ret = STypesUtil.insertStaticParams(STypesUtil.clearStaticParams(pre_super_ret),
                        better_super_params);
                //		    super_noself_domain_possibly_no_params.getInfo().getStaticParams().isEmpty() ?
//		    STypesUtil.insertStaticParams(super_noself_domain_possibly_no_params,     // Aha!  GLS 6/12/12
//						  currentTraitObjectDecl.getHeader().getStaticParams()) :
//		    super_noself_domain_possibly_no_params;

                for (Functional func : funcs) {
                    Type ret = local_inst.replaceInEverything(oa.getRangeType(func));
                    int self_index = NodeUtil.selfParameterIndex(func.parameters());
                    if (self_index != super_self_index) {
                        /*
                         * Not sure we see this ever; it is a bit of a mistake,
                         * and will require further tinkering when we sort out
                         * the overloads.  We DON'T want to attempt a type
                         * comparison of dissimilar-selfed functions.
                         */
                        continue; 
                    }
                    
                    /*
                     * See above comment -- I think this is the wrong instantiater,
                     * but it is not far wrong.
                     */
                    Type raw_noself_domain = oa.getNoSelfDomainType(func);
                    Type noself_domain = local_inst.replaceInEverything(raw_noself_domain);

                    // Classify potential override

                    /*
                     * Funny business with "self_index" --
                     *   the overloading oracle / type analyzer believes that
                     *   subtype_SELF more specific than supertype_SELF.
                     *   
                     *   This is not what we want for purposes of spotting
                     *   shadowing and collisions.
                     *  
                     */
                    if (DEBUG_OVERLOADED_METHOD_CHAINING && name.toString().contains("seq")) {
                      System.out.println("******* " + noself_domain.toStringReadable() + " { " + noself_domain.getInfo().getStaticParams() + " } "
                      + "\nvs super " + super_noself_domain.toStringReadable() + " { " + super_noself_domain.getInfo().getStaticParams() + " }");
                      System.out.println("** raw super is " + raw_super_noself_domain.toStringReadable() +
                      " { " + raw_super_noself_domain.getInfo().getStaticParams() + " }");

                      System.out.println("******* " + ret.toStringReadable() + " { " + ret.getInfo().getStaticParams() + " } "
                              + "\nvs super " + super_ret.toStringReadable() + " { " + super_ret.getInfo().getStaticParams() + " }");
                              System.out.println("** raw super is " + raw_super_ret.toStringReadable() +
                              " { " + raw_super_ret.getInfo().getStaticParams() + " }");
                      // super_noself_domain_possibly_no_params
                      System.out.println("** super pnp is " + super_noself_domain_possibly_no_params.toStringReadable() +
                              " { " + super_noself_domain_possibly_no_params.getInfo().getStaticParams() + " }");
                      System.out.println("** replacer is " + super_inst);
                      System.out.println("** better super params is { " + better_super_params + " }");
                        boolean d_a_le_b = oa.lteq(noself_domain, super_noself_domain) ;
                        boolean d_b_le_a = oa.lteq(super_noself_domain, noself_domain) ;
                        boolean r_a_le_b = oa.lteq(ret, super_ret);
                        boolean r_b_le_a = oa.lteq(super_ret, ret);
                        System.err.println("" + func + " ?? " + super_func + " " + d_a_le_b + d_b_le_a + r_a_le_b + r_b_le_a);
                      System.out.println("**** END");
                    }
                    boolean d_a_le_b = oa.lteq(noself_domain, super_noself_domain) ;
                    boolean d_b_le_a = oa.lteq(super_noself_domain, noself_domain) ;

                    boolean r_a_le_b = oa.lteq(ret, super_ret);
                    boolean r_b_le_a = oa.lteq(super_ret, ret);
                    
                    if (DEBUG_OVERLOADED_METHOD_CHAINING) {
                        System.err.println("" + func + " ?? " + super_func + " " + d_a_le_b + d_b_le_a + r_a_le_b + r_b_le_a);
                    }
                    
                    boolean schema_narrowed = false;
                    if (func.staticParameters().size() > 0) {
                        int selfIndex = ((HasSelfType)super_func).selfPosition();
                        String from_name = genericMethodName(super_func, selfIndex);
                        String to_name = genericMethodName(func, selfIndex);
                        schema_narrowed = ! from_name.equals(to_name);
                    } 

                    if (d_a_le_b && d_b_le_a) {
                        // equal domains
                        if (r_a_le_b) { // sub is LE
                            if (r_b_le_a) {
                                // eq
                                // note that GENERIC methods all have same Java return type,
                                // hence "equal".
                                if (schema_narrowed) {
                                    narrowed = true; // could "continue" here
                                    narrowed_func = func;
                                    if (DEBUG_OVERLOADED_METHOD_CHAINING)
                                        System.err.println("  "+ func + " schema-narrows " + super_func);
                                } else {
                                    shadowed = true; // could "continue" here
                                    if (DEBUG_OVERLOADED_METHOD_CHAINING)
                                        System.err.println("  "+ func + " shadows " + super_func);
                                }

                            } else {
                                // lt
                                narrowed = true;
                                narrowed_func = func;
                                if (DEBUG_OVERLOADED_METHOD_CHAINING)
                                    System.err.println("  "+ func + " narrows " + super_func);

                            }
                        }
                    }
                }

                if (shadowed)
                    continue;
                if (narrowed) {
//		    System.out.println("generateForwardingFor:");
//		    System.out.println("  super_func = " + super_func);
//		    System.out.println("  narrowed_func = " + narrowed_func);
//		    System.out.println("  super_inst = " + super_inst);
//		    System.out.println("  currentTraitObjectType = " + currentTraitObjectType);
                    if (narrowed_func.staticParameters().size() > 0) {
                        if (DEBUG_OVERLOADED_METHOD_CHAINING)
                            System.err.println("Generic narrowing, " + narrowed_func + " narrows " + super_func);
                        int selfIndex = ((HasSelfType)super_func).selfPosition();
                        String from_name = genericMethodName(super_func, selfIndex);
                        String to_name = genericMethodName(narrowed_func, selfIndex);
                        String sig = genericMethodClosureFinderSig;
                        int arity = 3; // Magic number, number of parameters to generic method closure finder
                        // TODO This could be bogus, but for now, let's try it.
                        String receiverClass = NamingCzar.jvmTypeDescAsTrait(currentTraitObjectType, component.getName());
                        
                        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, from_name, sig, null, null);
                        mv.visitCode();
                        
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitVarInsn(LLOAD, 1);
                        mv.visitVarInsn(ALOAD, 3);
                        mv.visitMethodInsn(inATrait ? INVOKEINTERFACE : INVOKEVIRTUAL, receiverClass, to_name, genericMethodClosureFinderSig);
                        
                        mv.visitInsn(ARETURN);
                        mv.visitMaxs(1,1);
                        mv.visitEnd();
                        
                    } else {
                        generateForwardingFor( super_func, narrowed_func, false, super_inst, currentTraitObjectType, currentTraitObjectType, true); // swapped
                    }
                    continue;
                }
                perhapsOverloaded.add((Functional)(((HasSelfType)super_func).instantiateTraitStaticParameters(ti.staticParameters(), super_inst)));  // GLS 3/12/2012
            }

            // need to refine the overloaded methods check because of exclusion
            MultiMap<Integer, Functional> partitionedByArgCount = partitionByMethodArgCount(oa, perhapsOverloaded);

            if (partitionedByArgCount.size() > 0 ) {
                
                overloadedMethods.put(name, partitionedByArgCount);
                if (DEBUG_OVERLOADED_METHOD_CHAINING)
                    System.err.println(" Method "+ name + " has overloads " + perhapsOverloaded);
            }
            // TODO now emit necessary overloads, if any.
            
        }
        return overloadedMethods;
    }

    /**
     * Returns the "Type" for the domain of a method, with self removed from
     * the parameter list, and the static parameters of the method itself
     * propagated onto the type (necessary for meaningful generic type
     * queries).
     * 
     * @param super_func
     * @param super_self_index
     * @return
     */
//    public Type selfEditedDomainType(Functional super_func, int super_self_index) {
//        return STypesUtil.insertStaticParams(fndeclToType(super_func, super_self_index).getDomain(), super_func.staticParameters());
//    }

    
    /* Large chunk of code removed here that was not being tested;
     * intent was to create erased methods for reference from functional
     * methods.  It may have been wrong-headed, and it was certainly
     * large and untested.
     */

    /**
     * Returns a list of parameter types, doing appropriate detupling of the
     * parameter type.
     * 
     * @param paramType
     * @return
     */
    private List<Type> normalizeParamsToList(Type paramType) {
        return paramType instanceof TupleType ? ((TupleType)paramType).getElements() : Useful.list(paramType);
    }

    private void addLineNumberInfo(ASTNode x) {
        addLineNumberInfo(mv, x);
    }

    private void addLineNumberInfo(CodeGenMethodVisitor m, ASTNode x) {
        org.objectweb.asm.Label bogus_label = new org.objectweb.asm.Label();
        m.visitLabel(bogus_label);
        Span span = NodeUtil.getSpan(x);
        SourceLoc begin = span.getBegin();
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

        mv.visitMethodInsn(INVOKESTATIC, pkgAndClassName,
                method_and_signature.first(), method_and_signature.second());

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

            signature = OverloadSet.getSignature(it, paramCount, typeAnalyzer);

        } else {
                throw sayWhat( x, "Neither arrow nor intersection type: " + arrow );
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
            mv.visitInsn(ICONST_0);
            break;
        case 1:
            mv.visitInsn(ICONST_1);
            break;
        case 2:
            mv.visitInsn(ICONST_2);
            break;
        case 3:
            mv.visitInsn(ICONST_3);
            break;
        case 4:
            mv.visitInsn(ICONST_4);
            break;
        case 5:
            mv.visitInsn(ICONST_5);
            break;
        default:
            mv.visitLdcInsn(y);
            break;
        }

    }

    private void allSayWhats() {
        return; // This is a great place for a breakpoint!
    }

    private CompilerError sayWhat(ASTNode x) {
        allSayWhats();
        return new CompilerError(x, "Can't compile " + x);
    }

    private CompilerError sayWhat(Node x) {
        if (x instanceof ASTNode)
            throw sayWhat((ASTNode) x);
        allSayWhats();
        return new CompilerError("Can't compile " + x);
    }

    private CompilerError sayWhat(ASTNode x, String message) {
        allSayWhats();
        return new CompilerError(x, message + " node = " + x);
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
            popAll(mv, onStack);
            e.accept(this);
            onStack = 1;
            // TODO: can we have multiple values on stack in future?
            // Whither primitive types?
            // May require some tracking of abstract stack state.
            // For now we always have 1 pointer on stack and this doesn't
            // matter.
        }
    }

    private void genParallelExprs(List<? extends Expr> args, Type domain_type, List<VarCodeGen> vcgs, boolean clearStackAtEnd) {
        final int n = args.size();
        if (n <= 0) return;
        
        List<Type> domain_types;
        if (NodeUtil.isVoidType(domain_type)) {
            domain_types = new ArrayList<Type>();
            for (int i = 0; i < n; i++)
                domain_types.add(domain_type);
        } else if (args.size() != 1 && domain_type instanceof TupleType) {
            TupleType tdt = (TupleType) domain_type;
            domain_types = tdt.getElements();
        } else {
            domain_types = Collections.singletonList(domain_type);
        }
        
        String [] tasks = new String[n];
        String [] results = new String[n];
        int [] taskVars = new int[n];

        if (vcgs != null && vcgs.size() != n) {
            System.out.println("vcgs size = " + vcgs.size() + " n = " + n);
            throw sayWhat(args.get(0), "Internal error: number of args does not match number of consumers.");
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
                throw sayWhat(arg, "Missing type information for argument " + arg);
            Type t = ot.unwrap();
            String tDesc = NamingCzar.jvmBoxedTypeDesc(t, component.getName());
            // Find free vars of arg
            List<VarCodeGen> freeVars = getFreeVars(arg);
            BASet<VarType> fvts = fvt.freeVarTypes(arg);
            // TO DO if fvts non-empty, will need to make a generic task

            // Generate descriptor for init method of task
            String init = taskConstructorDesc(freeVars);

            String task = delegate(arg, tDesc, init, freeVars);
            tasks[i] = task;
            results[i] = tDesc;
            constructWithFreeVars(task, freeVars, init);

            mv.visitInsn(DUP);
            int taskVar = mv.createCompilerLocal(task, // Naming.mangleIdentifier(task),
                                                 Naming.internalToDesc(task));
            taskVars[i] = taskVar;
            mv.visitVarInsn(ASTORE, taskVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, task, "forkIfProfitable", "()V");
        }

        // arg 0 gets compiled in place, rather than turned into work.
        args.get(0).accept(this);
        conditionallyCastParameter(args.get(0), domain_types.get(0));
        if (vcgs != null) vcgs.get(0).assignValue(mv);
        if (clearStackAtEnd) popAll(mv, 0); 

        // join / perform work locally left to right, leaving results on stack.
        for (int i = 1; i < n; i++) {
            int taskVar = taskVars[i];
            mv.visitVarInsn(ALOAD, taskVar);
            mv.disposeCompilerLocal(taskVar);
            mv.visitInsn(DUP);
            String task = tasks[i];
            mv.visitMethodInsn(INVOKEVIRTUAL, task, "joinOrRun", "()V");
            mv.visitFieldInsn(GETFIELD, task, "result", results[i]);
            conditionallyCastParameter(args.get(i), domain_types.get(i));
            if (vcgs != null) vcgs.get(i).assignValue(mv);
            if (clearStackAtEnd) popAll(mv, 1);  // URGH!!!  look into better stack management...            
        }
    }

    public void defaultCase(Node x) {
        throw sayWhat(x);
    }

    public void forImportStar(ImportStar x) {
        // do nothing, don't think there is any code go generate
    }

    public void forAssignment(Assignment x) {
        List<Lhs> lhs_list = x.getLhs();
        if (lhs_list.size() != 1)
            throw new RuntimeException(" Can't do multiple assignments yet");
        Lhs lhs = lhs_list.get(0);
        if (! (lhs instanceof VarRef))
            throw new RuntimeException(" Can't do anything other than VarRefs");
        Option<FunctionalRef> assignOp = x.getAssignOp();
        if (assignOp.isSome())
            throw new RuntimeException(" Can't handle special assignments");

        List<CompoundAssignmentInfo> assignmentInfos = x.getAssignmentInfos();
        if (assignmentInfos.size() > 0)
            throw new RuntimeException(" Can't handle compound assigments");

        Expr rhs = x.getRhs();

        rhs.accept(this);
        VarRef vr = (VarRef) lhs;
        Id var = vr.getVarId();
        VarCodeGen r = getLocalVarOrNull(var);

        if (r != null)
            r.assignValue(this.mv);
        // All statements need to leave a value on the stack.
        pushVoid();
    }


    public void forBlockHelper(Block x) {
        boolean oldInABlock = inABlock;
        inABlock = true;
        debug("forBlock ", x);
        doStatements(x.getExprs());
        inABlock=oldInABlock;
    }

    public void forAtomicBlockHelper(Block x) {
        mv.visitMethodInsn(INVOKESTATIC, 
                           "com/sun/fortress/runtimeSystem/BaseTask", 
                           "startTransaction", "()V");     
        forBlockHelper(x);
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/BaseTask", 
                           "endTransaction", "()V");
    }

    // For debugging        
    public void printString(CodeGenMethodVisitor mv, String s) {
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitLdcInsn(s);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }

    public void forAtomicBlock(Block x) {
        Label start = new Label();
        Label end = new Label();
        Label exit = new Label();
        Label handler = new Label();
        Label startOver = new Label();
        String type = "com/sun/fortress/runtimeSystem/TransactionAbortException";

        mv.visitLabel(startOver);
        mv.visitTryCatchBlock(start, end, handler, type);

        mv.visitLabel(start);
        forAtomicBlockHelper(x);
        mv.visitLabel(end);
        mv.visitJumpInsn(GOTO, exit);

        // Exception Handler
        mv.visitLabel(handler);
        mv.visitInsn(POP); // Pop the exception
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/BaseTask", 
                           "cleanupTransaction", "()V");
        // Wait a randomized exponential amount of time after an error
        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/BaseTask",
                           "delayTransaction", "()V");
        mv.visitJumpInsn(GOTO, startOver);

        // Exit
        mv.visitLabel(exit);
    }


    public void forBlock(Block x) {
        if (x.isAtomicBlock()) {
            forAtomicBlock(x);
        } else {
            forBlockHelper(x);
        }
    }

// I took a stab at this, but got stuck on the comparison operator.
//   public void forCaseExpr(CaseExpr x) {
//         debug( " forCaseExpr:", x);
//         System.out.println("ForCaseExpr: x = " + x + 
//                            " param = " + x.getParam() + 
//                            " compare = " + x.getCompare() + 
//                            " _equalsOp = " + x.getEqualsOp() +
//                            " _inOp = " + x.getInOp() + 
//                            " clauses = " + x.getClauses() +
//                            " else = " + x.getElseClause());

//         if (x.getParam().isNone()) {
//             throw new RuntimeException("NYI: forCaseExpr with null param");
//         } else if (x.getCompare().isSome()) {
//             throw new RuntimeException("Only default CMP for now ");
//         }

//         Label done = new Label();
//         for (CaseClause c : x.getClauses()) {
//             System.out.println("CaseClause: " + c +
//                                " Match Clause = " + c.getMatchClause() +
//                                " Body = " + c.getBody() + 
//                                " Op = " + c.getOp());

//             Label next = new Label();
//             Label end = new Label();
//             x.getParam().unwrap().accept(this);
//             c.getMatchClause().accept(this);
            
            
//             mv.visitJumpInsn(IF_EQ, next);
//             mv.visitJumpInsn(GOTO, end);
//             mv.visitLabel(next);
//             c.getBody().accept(this);
//             mv.visitJumpInsn(GOTO, done);
//             mv.visitLabel(end);
//         }
//         if (x.getElseClause().isSome())
//             x.getElseClause().unwrap().accept(this);

//         mv.visitMethodInsn(INVOKESTATIC,
//                             NamingCzar.internalFortressVoid, NamingCzar.make,
//                             NamingCzar.voidToFortressVoid);

//         mv.visitLabel(done);
//     }

    public void forChainExpr(ChainExpr x) {
        throw sayWhat(x, "ChainExpr should have been desugared earlier");
//         debug( "forChainExpr", x);
//         Expr first = x.getFirst();
//         List<Link> links = x.getLinks();
//         debug( "forChainExpr", x, " about to call accept on ",
//                first, " of class ", first.getClass());
//         first.accept(this);
//         Iterator<Link> i = links.iterator();
//         if (links.size() != 1)
//             throw new CompilerError(x, x + "links.size != 1");
//         Link link = i.next();
//         link.getExpr().accept(this);
//         debug( "forChainExpr", x, " about to call accept on ",
//                link.getOp(), " of class ", link.getOp().getClass());
//         int savedParamCount = paramCount;
//         try {
//             // TODO is this the general formula?
//             paramCount = links.size() + 1;
//             link.getOp().accept(this);
//         } finally {
//             paramCount = savedParamCount;
//         }

//         debug( "We've got a link ", link, " an op ", link.getOp(),
//                " and an expr ", link.getExpr(), " What do we do now");
    }

    public void forComponent(Component x) {
        debug("forComponent ",x.getName(),NodeUtil.getSpan(x));

        for ( Import i : x.getImports() ) {
            i.accept(this);
        }

        // determineOverloadedNames(x.getDecls() );

        // Must do this first, to get local decls right.
        topLevelOverloadedNamesAndSigs = generateTopLevelOverloads(thisApi(), topLevelOverloads, typeAnalyzer, cw, this, OverloadSet.localFactory);

        /* Need wrappers for the API, too. */

        // Must process top-level values next to make sure fields end up in scope.
        for (Decl d : x.getDecls()) {
            if (d instanceof ObjectDecl) {
                ObjectDecl od = (ObjectDecl) d;
                TraitTypeHeader tth = od.getHeader();
                CodeGen newcg = new CodeGen(this,
                        typeAnalyzer.extendJ(tth.getStaticParams(), tth.getWhereClause()));
                newcg.forObjectDeclPrePass(od);
            } else if (d instanceof VarDecl) {
                this.forVarDeclPrePass((VarDecl)d);
            }
        }

        // Static initializer for this class.
        // Since all top-level fields and singleton objects are singleton inner classes,
        // this does nothing for these.
        mv = cw.visitCGMethod(ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);

        /*
         * Dynamic inference caching tables
         * 
         * Any top-level overloaded function that is generic (either from its own
         * definition or its defining trait in the case of functional methods)
         * may be dynamically instantiated.  
         * 
         * In the declaring component, we keep tables with instances of the
         * instantiated generic function to avoid always invoking the
         * classloader.  Because of variance, we cannot
         * necessarily make assumptions based on the static types, even
         * from instantiated traits/objects, which is why even tables for functional
         * methods are put at the Component level
         */
        
        // tables to cache dynamic instantiations of generic functions
        for(MultiMap<Integer,Functional> overloadDefns : this.topLevelOverloads.values()) {
            for (Set<Functional> paramDefns : overloadDefns.values()) {
                for (Functional def : paramDefns) {
                    if (def.staticParameters().size() > 0 ||                              //if method generic
                            ( def instanceof FunctionalMethod &&                          //or if it is a functional method
                              ((FunctionalMethod) def).traitStaticParameters().size() > 0 //and the defining trait is generic
                            )                                                             //then need a cache for instantiations
                       ) {
                        
                        //table name built off the unambiguous name for the function definition (includes line numbers)
                        String tableName = Naming.cacheTableName(def.unambiguousName().getText());
                        
                        //owning class is the component class
                        String className =  Naming.dotToSep(x.getName().getText());
                        
                        //A) declare static field
                        //B) in static initializer:
                        //   1) new table
                        //   2) call constructor
                        //   3) store into static field
                        
                        cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,   //A)
                                      tableName, 
                                      Naming.CACHE_TABLE_DESC,
                                      null, null);
                        mv.visitTypeInsn(NEW, Naming.CACHE_TABLE_TYPE); //B.1)
                        mv.visitInsn(DUP);
                        mv.visitMethodInsn(INVOKESPECIAL, Naming.CACHE_TABLE_TYPE, "<init>", "()V"); //B.2)
                        mv.visitFieldInsn(PUTSTATIC, className, tableName, Naming.CACHE_TABLE_DESC); //B.3)
                    }
                }
            }
        }
        
        voidEpilogue();
        
        for ( Decl d : x.getDecls() ) {
            d.accept(this);
        }

        //        System.out.println("About to print parallelism table for " + packageAndClassName);
        //        pa.printTable();
        cw.dumpClass( packageAndClassName );
        
    }

    public void forDecl(Decl x) {
        throw sayWhat(x, "Can't handle decl class "+x.getClass().getName());
    }

    // ForDoParallel is just like forExprsParallel except we need to clear the stack
    // of those pesky FVoids for the arms of the DO.
    public void forDoParallel(List<? extends Expr> args, Type domain_type, List<VarCodeGen> vcgs) {
        boolean clearStackAtEnd = true;
        genParallelExprs(args, domain_type, vcgs, clearStackAtEnd);
    }

    public void forDo(Do x) {
        debug("forDo ", x);
        int n = x.getFronts().size();
        popAll(mv, 0);

        if (n > 1) {
            forDoParallel(x.getFronts(), NodeFactory.makeVoidType(x.getInfo().getSpan()), null);
        } else { 
            x.getFronts().get(0).accept(this);
        }
    }


//     public void forDo(Do x) {
//         // TODO: these ought to occur in parallel!
//         debug("forDo ", x);
//         int onStack = 0;
//         for ( Block b : x.getFronts() ) {
//             popAll(onStack);
//             b.accept(this);
//             onStack = 1;
//         }
//     }

    // TODO: arbitrary-precision version of FloatLiteralExpr, correct
    // handling of types other than double (float should probably just
    // truncate, but we want to warn if we lose bits I expect).
    public void forFloatLiteralExpr(FloatLiteralExpr x) {
        debug("forFloatLiteral ", x);
        double val = x.getIntPart().doubleValue() +
            (x.getNumerator().doubleValue() /
             Math.pow(x.getDenomBase(), x.getDenomPower()));
        mv.visitLdcInsn(val);
        mv.visitMethodInsn(INVOKESTATIC,
                           NamingCzar.internalFortressFloatLiteral, NamingCzar.make,
                           Naming.makeMethodDesc(NamingCzar.descDouble,
                                                     NamingCzar.descFortressFloatLiteral));
    }

    public void forThrow(Throw x) {
        debug("forThrow ", x);
        Expr e = x.getExpr();

        mv.visitTypeInsn(NEW, "com/sun/fortress/compiler/runtimeValues/FException");
        mv.visitInsn(DUP);
        e.accept(this);
        mv.visitMethodInsn(INVOKESPECIAL, "com/sun/fortress/compiler/runtimeValues/FException",
                           "<init>",
                           "(Lcom/sun/fortress/compiler/runtimeValues/FValue;)V");
        mv.visitInsn(ATHROW);
    }

    public void forTry(Try x) {
         Label l0 = new Label();
         Label l1 = new Label();
         Label l2 = new Label();
         Label lfinally = new Label();

         Block body = x.getBody();
         Option<Catch> catchClauses = x.getCatchClause();
         List<BaseType> forbid = x.getForbidClause();
         Option<Block> finallyClause = x.getFinallyClause();
         if (!forbid.isEmpty())
             throw new RuntimeException("NYI: Forbid clauses are not yet implemented");
         
         mv.visitTryCatchBlock(l0,l1,l2, "com/sun/fortress/compiler/runtimeValues/FException");
         mv.visitLabel(l0);
         body.accept(this);
         mv.visitLabel(l1);
         mv.visitJumpInsn(GOTO, lfinally);
         mv.visitLabel(l2);
         // We really should have desugared this into typecase, but for now...
         
         mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"com/sun/fortress/compiler/runtimeValues/FException"});

         
         if (catchClauses.isSome()) {
             Catch _catch = catchClauses.unwrap();
             Id name = _catch.getName();

             Label done = new Label(); // first statement after the whole try-catch statement
             List<CatchClause> clauses = _catch.getClauses();
             for (CatchClause clause : clauses) {
                 Label end = new Label(); // if catch clause does not match, skip clause body
                 BaseType match = clause.getMatchType();
                 
                 mv.visitInsn(DUP); // Save Java exception while we fiddle with Fortress data
                 mv.visitMethodInsn(INVOKEVIRTUAL, "com/sun/fortress/compiler/runtimeValues/FException", 
                                    "getValue","()Lcom/sun/fortress/compiler/runtimeValues/FValue;");

                 InstantiatingClassloader.generalizedInstanceOf(mv, NamingCzar.jvmBoxedTypeName(match, thisApi()));
                 mv.visitJumpInsn(IFEQ, end);
                 mv.visitInsn(POP);
                 clause.getBody().accept(this);
                 mv.visitJumpInsn(GOTO, done);
                 mv.visitLabel(end);
             }
             mv.visitInsn(ATHROW); // Rethrow if no match
             mv.visitLabel(done);
         }
         mv.visitLabel(lfinally);
         if (finallyClause.isSome()) {
             finallyClause.unwrap().accept(this);
             popAll(mv, 1);
         }
    }

    private Type getType(TypeOrPattern t) {
        if (t instanceof Type) return (Type)t;
        else {
            Pattern p = (Pattern)t;
            if (p.getName().isSome()) return p.getName().unwrap();
            else {
                List<Type> types = new ArrayList<Type>();
                for (PatternBinding pb : p.getPatterns().getPatterns()) {
                    if (pb instanceof PlainPattern &&
                        ((PlainPattern)pb).getIdType().isSome()) {
                        types.add(getType(((PlainPattern)pb).getIdType().unwrap()));
                    } else if (pb instanceof TypePattern) {
                        types.add(((TypePattern)pb).getTyp());
                    } else 
                        //                        return error("typecase match failure!");
                        throw new RuntimeException("typcase match failure!");
                }
                return NodeFactory.makeTupleType(types);
            }
        }
    }

    // May be useful for debugging expr types.  chf
    private void printExprAndClassInfo(Expr expr) {
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V");
        mv.visitLdcInsn("Object = ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        expr.accept(this);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitLdcInsn(" of class ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
        expr.accept(this);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    }

    public void forTypecase(Typecase x) {
        Expr expr = x.getBindExpr();
        expr.accept(this);
        
        Label done = new Label();
        for (TypecaseClause c : x.getClauses()) {
            Label next = new Label();
            Label end = new Label();
            TypeOrPattern match = c.getMatchType();
            Type typ = getType(match);
            mv.visitInsn(DUP);
            InstantiatingClassloader.generalizedInstanceOf(mv, NamingCzar.jvmBoxedTypeName(typ, thisApi()));
            mv.visitJumpInsn(IFNE, next);
            mv.visitJumpInsn(GOTO, end);
            mv.visitLabel(next);
            mv.visitInsn(POP);
            c.getBody().accept(this);
            mv.visitJumpInsn(GOTO, done);
            mv.visitLabel(end);
         }

        if (x.getElseClause().isSome()) {
            mv.visitInsn(POP);
        	x.getElseClause().unwrap().accept(this);
            mv.visitJumpInsn(GOTO, done);
        }

         mv.visitMethodInsn(INVOKESTATIC,
                            NamingCzar.internalFortressVoid, NamingCzar.make,
                            NamingCzar.voidToFortressVoid);

         mv.visitLabel(done);


    }


    /**
     * Generate the closure class for a generic function.  Also used to create
     * a closure implementing a generic method (in which case a downcast is
     * required in the closure's apply method).
     * 
     * 
     * @param x
     * @param name
     * @param selfIndex
     * @param savedInATrait 
     * @param forceCastParam0InApply
     */
    public String generateGenericFunctionClass(FnNameInfo x, GenericMethodBodyMaker body_maker, IdOrOp name,
                                            int selfIndex, String forceCastParam0InApply) {
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


        List<StaticParam> static_params = x.static_params;
        
        String sig =
            NamingCzar.jvmSignatureFor(x.functionArrowType(), component.getName());

        /* TODO Really, the canonicalization of the type names should occur
         * in static analysis.  This has to use names that will be known
         * at the reference site, so for now we are using the declared
         * names.  In rare cases, this might lead to a problem.
         */
        // String generic_arrow_type = NamingCzar.jvmTypeDesc(fndeclToType(x), thisApi(), false);

        // TODO different collision rules for top-level and for
        // methods. (choice of mname)

        if (selfIndex != Naming.NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex);
        } 
        
        // TODO refactor, this is computed in another place.
        
        /* 
         * This may be a mistake, but class name and the file name do not match.
         * The reason for this is that the class is not really a class; it is 
         * a template to be filled in.  The class name needs parameter slots
         * embedded in it so that they can be replaced at instantiation time,
         * but it is more convenient (but perhaps not 100% necessary at this
         * point, given the use of metadata) to omit the parameters from the
         * container's file name.
         */
//        String PCN_for_class =
//            Naming.genericFunctionPkgClass(packageAndClassName, mname,
//                                               sparams_part, generic_arrow_type);
//        
//        String PCN_for_file =
//            Naming.genericFunctionPkgClass(packageAndClassName, mname,
//                        Naming.makeTemplateSParams(sparams_part) , generic_arrow_type);
        PCNforClosure pair = nonCollidingClosureName(x, selfIndex, name,
                static_params, this.packageAndClassName);
        
        String PCN_for_class = pair.PCN;
        String PCN_for_file = pair.PCNOuter;
        Naming.XlationData xldata = pair.xldata;

        // System.err.println(PCN);

        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);

        ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();

        // This creates the closure bits
        String applied_method = InstantiatingClassloader.closureClassPrefix(PCN_for_class, cg.cw, PCN_for_class, sig, forceCastParam0InApply, isf_list);

        // Code below cribbed from top-level/functional/ordinary method

        String modified_sig = sig;
        if (forceCastParam0InApply != null) {
            modified_sig = Naming.replaceNthSigParameter(sig, 0, Naming.internalToDesc(forceCastParam0InApply));
            // Ought to rewrite params for better debugging info, but yuck, it is hard.
        }

        body_maker.generateGenericMethodBody(cg, selfIndex, applied_method,
                modified_sig);

        InstantiatingClassloader.optionalStaticsAndClassInitForTO(isf_list, cg.cw);

        cg.cw.dumpClass(PCN_for_file, xldata);
        
        //used when we need to infer static parameters at runtime
//        CodeGen cg2 = new CodeGen(this);
//        cg2.generateRuntimeInstantiationClass(PCN_for_file, header.getStaticParams().size());
        
        return PCN_for_file;
    }

    public abstract static class GenericMethodBodyMaker {
        public abstract void generateGenericMethodBody(CodeGen cg, int selfIndex,
                String applied_method, String modified_sig);
    }
    
    static class GenericMethodBodyMakerFnDecl extends GenericMethodBodyMaker {
        FnDecl fndecl;
        GenericMethodBodyMakerFnDecl(FnDecl fndecl) {
            super();
            this.fndecl = fndecl;
        }
        
        public void generateGenericMethodBody(CodeGen cg, int selfIndex,
                String applied_method, String modified_sig) {
            Expr body = fndecl.getBody().unwrap();
            List<Param> params = fndecl.getHeader().getParams();
            int modifiers = ACC_PUBLIC | ACC_STATIC ;
            cg.generateActualMethodCode(modifiers, applied_method, modified_sig, params, selfIndex,
                                        selfIndex != Naming.NO_SELF, body);
        }
     }
    
  
//    private void generateRuntimeInstantiationClass(String templateClassName, int numStaticParams) {
//        String tableClassName = templateClassName.substring(0, templateClassName.indexOf(Naming.LEFT_OXFORD)) + "$TABLE";
//        //tableClassName = tableClassName.replace(Naming.GEAR, "");
//        
//        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
//        // Begin with a class
//        cw.visit(InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC, tableClassName, null, NamingCzar.internalObject, null);
//        
//        int modifiers = ACC_PUBLIC | ACC_STATIC ;
//        String sig = InstantiatingClassloader.jvmSignatureForNTypes(numStaticParams, 
//                                                                    Naming.RTTI_CONTAINER_TYPE, 
//                                                                    Naming.internalToDesc(NamingCzar.internalObject));
//        mv = cw.visitCGMethod(modifiers, "getFunctionBody", sig, null, null);
//        mv.visitCode();
//        mv.visitLdcInsn(templateClassName);
//        for (int i = 0; i < numStaticParams; i++) {
//            mv.visitVarInsn(ALOAD, i);
//        }
//        
//        String ic_sig = InstantiatingClassloader.jvmSignatureForOnePlusNTypes(NamingCzar.internalString, numStaticParams, 
//                Naming.RTTI_CONTAINER_TYPE, 
//                Naming.internalToDesc(NamingCzar.internalObject));
//        mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "loadClosureClass", ic_sig);
//        
//        mv.visitInsn(ARETURN);
//        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
//        mv.visitEnd();
//        
//        cw.dumpClass(tableClassName);
//    }
    
    /**
     * Generate a generic method with a body.
     * 
     * Two methods must be generated.
     * 
     * One method is an instance method, like
     * a forwarding method, that takes a precomputed hashcode and a String
     * encoding the static parameters of the generic method (as they were
     * inferred or provided).  It returns a closure (one that has no particularly
     * interesting environment, really just a function pointer) made by specializing
     * the second method.  Obviously, these are cached.
     * 
     * The static parameters are slightly modified to include an encoding of
     * the static type of "self" at the call site; this is necessary to get
     * the type right on the returned arrow type, which makes "self" explicit.
     * 
     * The second method is a static method as if for a generic function, with
     * the signature of the original method, except that self is explicitly
     * prepended.  This is very similar to a trait default method, except that
     * it is generic.
     * 
     * @param x
     * @param name
     * @param selfIndex
     * @param savedInATrait 
     * @param returnType
     * @param inAMethod
     * @param body
     */
    private void generateGenericMethod(FnDecl x, IdOrOp name,
            int self_index,
            boolean savedInATrait,
            boolean inAMethod) {

        /*
         * First, create a modified FnDecl that looks like a top-level generic
         * method.
         * 
         * Add a new static parameter onto the front of the SP list; that will
         * be the call-site type of "self".
         * 
         * Next remove any explicit self parameter from within the parameter
         * list, and create a new explicit self parameter, with type given by
         * the new SP, and put that at the front of the parameter list (this
         * may require a minor bit of type-replumbing for self, not clear how
         * that works yet).
         * 
         */
        
        Span sp_span = x.getInfo().getSpan();
        
        FnDecl new_fndecl = convertGenericMethodToClosureDecl(x, self_index, sp_span);
        
                
        // This is not right yet -- the name is wrong.
        String TO_method_name = currentTraitObjectDecl.getHeader().getName().stringName() + Naming.UP_INDEX + name.getText();
        
        List<StaticParam> trait_static_parameters = currentTraitObjectDecl.getHeader().getStaticParams();
        
        String selfType = savedInATrait ? traitOrObjectName +  NamingCzar.springBoard : traitOrObjectName;
        // Bug here, not getting the type name right.
        Id gfid = NodeFactory.makeId(sp_span, TO_method_name);
        String template_class_name = 
            generateGenericFunctionClass(new FnNameInfo(new_fndecl, trait_static_parameters, thisApi()),
                    new GenericMethodBodyMakerFnDecl(new_fndecl), gfid,
                    Naming.NO_SELF, traitOrObjectName);
        
        String method_name = NamingCzar.genericMethodName(new FnNameInfo(x, trait_static_parameters, thisApi()), self_index, thisApi());
        String table_name = Naming.cacheTableName(method_name);  //closure table uses the unmangled method name
        if (typeLevelOverloadedNamesAndSigs.contains(method_name)) {
            method_name  = NamingCzar.mangleAwayFromOverload(method_name);
            table_name  = NamingCzar.mangleAwayFromOverload(table_name);
        }
        generateGenericMethodClosureFinder(method_name, table_name, template_class_name, selfType, savedInATrait);
        
        
    }

    /**
     * Creates a new FnDecl for the generic closure created to implement a generic method.
     * 
     * @param x
     * @param self_index
     * @param sp_span
     * @return
     */
    public FnDecl convertGenericMethodToClosureDecl(FnDecl x, int self_index,
            Span sp_span) {
        FnHeader xh = x.getHeader();
        List<StaticParam> sparams = xh.getStaticParams();
        List<Param> params = xh.getParams();
        Type returnType = xh.getReturnType().unwrap();
        Id sp_name = NodeFactory.makeId(sp_span, Naming.UP_INDEX);
        Id p_name = NamingCzar.selfName(sp_span);
        StaticParam new_sp = NodeFactory.makeTypeParam(sp_span, Naming.UP_INDEX);
        
        // also splice in trait/object sparams, if any.
        List<StaticParam> new_sparams = new ConcatenatedList<StaticParam>(
                new InsertedList<StaticParam>(sparams, 0, new_sp),
                this.currentTraitObjectDecl.getHeader().getStaticParams());
        
        Param new_param = NodeFactory.makeParam(p_name, NodeFactory.makeVarType(sp_span, sp_name));
        List<Param> new_params = new InsertedList<Param>(
                (self_index != Naming.NO_SELF ? new DeletedList<Param>(params, self_index) : params),
                0, new_param);
        Option<Expr> body = x.getBody();
        
        FnDecl new_fndecl = NodeFactory.makeFnDecl(sp_span, xh.getMods(), xh.getName(),
                new_sparams, new_params, xh.getReturnType(), xh.getThrowsClause(),
                xh.getWhereClause(), xh.getContract(), x.getUnambiguousName(), body, x.getImplementsUnambiguousName());
        return new_fndecl;
    }
    
    private static BAlongTree sampleLookupTable = new BAlongTree();


    // generateForwardingFor
    private String genericMethodName(Functional x, int selfIndex) {
        return NamingCzar.genericMethodName(new FnNameInfo(x, thisApi()), selfIndex, thisApi());    

//        IdOrOp name = x.name();
//        ArrowType at = fndeclToType(x, selfIndex);
//        String possiblyDottedName = Naming.fmDottedName(singleName(name), selfIndex);
//        
//        return genericMethodName(possiblyDottedName, at);    
    }
    
    
    
    /**
     * @param name
     * @param sparams
     */
    public String genericMethodClosureName(FnNameInfo x, int selfIndex) {
        ArrowType at = x.methodArrowType(selfIndex);
        String possiblyDottedName = Naming.fmDottedName(singleName(x.getName()), selfIndex);
        
        return genericMethodClosureName(possiblyDottedName, at);    
    }
    
    private String genericMethodClosureName(Functional x, int selfIndex) {
        return genericMethodClosureName(new FnNameInfo(x, thisApi()), selfIndex);
    }
    
    // DRC-WIP

    private String genericMethodClosureName(IdOrOp name, ArrowType at) {
        return genericMethodClosureName(name.getText(), at);
    }

    private String genericMethodClosureName(String name, ArrowType at) {
        String generic_arrow_type = NamingCzar.jvmTypeDesc(at, thisApi(),
                false);
        
        return genericMethodClosureName(name, generic_arrow_type);
    }

    private String genericMethodClosureName(String name, String generic_arrow_type) {
        /* Just append the schema.
         * TEMP FIX -- do sep w/HEAVY_X.
         * Need to stop substitution on static parameters from the method itself.
         * 
           Do not separate with HEAVY_X, because schema may depend on 
           parameters from parent trait/object.
           (HEAVY_X stops substitution in instantiator).
           */
        return name + Naming.UP_INDEX + Naming.HEAVY_CROSS + generic_arrow_type;
    }
    
    private final static String genericMethodClosureFinderSig = "(JLjava/lang/String;)Ljava/lang/Object;";
    
    /**
     * Generates the method that is called to find the closure for a generic method.
     * The method name is some minor frob on the Fortress method name, but it needs
     * the schema to help avoid overloading conflicts on the name.
     * 
     *  <pre>
     *  private static BAlongTree sampleLookupTable = new BAlongTree();
     *  public Object sampleLookup(long hashcode, String signature) {
     *  Object o = sampleLookupTable.get(hashcode);
     *  if (o == null) 
     *     o = findGenericMethodClosure(hashcode,
     *                                  sampleLookupTable,
     *                                  "template_class_name",
     *                                  signature);
     *  return o;
     *  }
     *  </pre>
     * @param method_name - may be mangled to avoid a name clash
     * @param table_name - always the unmangled version of the method_name
     * @param template_class_name
     * @param savedInATrait 
     */
    public void generateGenericMethodClosureFinder(String method_name, final String table_name, String template_class_name, final String class_file, boolean savedInATrait) {
        
        initializedStaticFields_TO.add(new InitializedStaticField() {

            @Override
            public void forClinit(MethodVisitor my_mv) {
                my_mv.visitTypeInsn(NEW, Naming.CACHE_TABLE_TYPE);
                my_mv.visitInsn(DUP);
                my_mv.visitMethodInsn(INVOKESPECIAL, Naming.CACHE_TABLE_TYPE, "<init>", "()V");
                my_mv.visitFieldInsn(PUTSTATIC, class_file, table_name, Naming.internalToDesc(Naming.CACHE_TABLE_TYPE));
            }

            @Override
            public String asmName() {
                return table_name;
            }

            @Override
            public String asmSignature() {
                return Naming.internalToDesc(Naming.CACHE_TABLE_TYPE);
            }
            
        });
        
        int hashOff = savedInATrait ? 0 : 1;
        int stringOff = hashOff + 2;
        int tmpOff = stringOff + 1;
        
        int access = savedInATrait ? ACC_PUBLIC + ACC_STATIC : ACC_PUBLIC;
        if (savedInATrait)
            method_name = method_name + Naming.GENERIC_METHOD_FINDER_SUFFIX_IN_TRAIT;
        
        List<StaticParam> to_sparams = this.currentTraitObjectDecl.getHeader().getStaticParams();
        
        CodeGenMethodVisitor mv = cw.visitCGMethod(access, method_name, genericMethodClosureFinderSig, null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        //mv.visitLineNumber(1331, l0);
        mv.visitFieldInsn(GETSTATIC, class_file, table_name, Naming.internalToDesc(Naming.CACHE_TABLE_TYPE));
        mv.visitVarInsn(LLOAD, hashOff);
        mv.visitMethodInsn(INVOKEVIRTUAL, Naming.CACHE_TABLE_TYPE, "get", "(J)Ljava/lang/Object;");
        mv.visitVarInsn(ASTORE, tmpOff);
        Label l1 = new Label();
        mv.visitLabel(l1);
        //mv.visitLineNumber(1332, l1);
        mv.visitVarInsn(ALOAD, tmpOff);
        Label l2 = new Label();
        mv.visitJumpInsn(IFNONNULL, l2);
        Label l3 = new Label();
        mv.visitLabel(l3);
        //mv.visitLineNumber(1333, l3);
        mv.visitVarInsn(LLOAD, hashOff);
        mv.visitFieldInsn(GETSTATIC, class_file, table_name,Naming.internalToDesc(Naming.CACHE_TABLE_TYPE));
        mv.visitLdcInsn(template_class_name);
        mv.visitVarInsn(ALOAD, stringOff);
        // if in a generic trait/object, need to call different method and include one more parameter.
        if (to_sparams.size() == 0) {
            mv.visitMethodInsn(INVOKESTATIC, Naming.RT_HELPERS, "findGenericMethodClosure", "(JLcom/sun/fortress/runtimeSystem/BAlongTree;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
        } else {
            // Need to use the substitute-at-load string operation.
            String string_sargs = NamingCzar.genericDecoration(null, to_sparams, null, thisApi());
            String loadString = Naming.opForString(Naming.stringMethod, string_sargs);
            mv.visitMethodInsn(INVOKESTATIC, Naming.magicInterpClass, loadString, "()Ljava/lang/String;");
            mv.visitMethodInsn(INVOKESTATIC, Naming.RT_HELPERS, "findGenericMethodClosure", "(JLcom/sun/fortress/runtimeSystem/BAlongTree;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;");
        }
        mv.visitVarInsn(ASTORE, tmpOff);
        mv.visitLabel(l2);
        //mv.visitLineNumber(1335, l2);
        mv.visitFrame(F_APPEND,1, new Object[] {"java/lang/Object"}, 0, null);
        mv.visitVarInsn(ALOAD, tmpOff);
        mv.visitInsn(ARETURN);
        Label l4 = new Label();
        mv.visitLabel(l4);
        
        if (!savedInATrait)
            mv.visitLocalVariable("this", Naming.internalToDesc(class_file), null, l0, l4, 0);
        
        mv.visitLocalVariable("hashcode", "J", null, l0, l4, hashOff);
        mv.visitLocalVariable("signature", "Ljava/lang/String;", null, l0, l4, stringOff);
        mv.visitLocalVariable("o", "Ljava/lang/Object;", null, l1, l4, tmpOff);
        // 6,6 if in a generic trait.
        //        mv.visitMaxs(5, 5);
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);            
        mv.visitEnd();  
    }

    private void generateTraitDefaultMethod(FnDecl x, IdOrOp name,
                                            List<Param> params,
                                            int selfIndex,
                                            Type returnType,
                                            Expr body) {
        int modifiers = ACC_PUBLIC | ACC_STATIC;

        Type traitType = STypesUtil.declToTraitType(currentTraitObjectDecl);

        /* Signature includes explicit leading self
           First version of sig includes duplicate self for
           functional methods, which is then cut out.
        */
        String sig = NamingCzar.jvmSignatureFor(
                         NodeUtil.getParamType(x),
                         NamingCzar.jvmTypeDesc(returnType,
                                                component.getName(), true, true),
                         0,
                         traitType,
                         component.getName());

        // TODO different collision rules for top-level and for
        // methods.
        String mname;
        int n = params.size();
        if (selfIndex != Naming.NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex+1);
            mname = Naming.fmDottedName(singleName(name), selfIndex);
        } else {
            mname = singleName(name); // static params?
            n++;
        }

        
        
        CodeGen cg = new CodeGen(this);
/*
 * Don't forward in the default methods after all -- do all duplication
 * in the inherited trait default class spine and in the object.
 */
        // IF NO OVERLOAD, EMIT FORWARDING METHOD from mname -> disambig_mname
        // Test omitted for now.
        // context -- trait defaultmethod.
        // need a static type replacer?  See if we can skip it for now
//        String disambiguated_mname = NamingCzar.mangleAwayFromOverload(mname);
//        InstantiatingClassloader.forwardingMethod(cg.cw, mname, ACC_PUBLIC, 0,
//                springBoardClass, disambiguated_mname, INVOKESTATIC,
//                sig, sig, null, n, true, null);
//        mname = disambiguated_mname;
        
        cg.generateActualMethodCode(modifiers, mname, sig, params, selfIndex,
                                    true, body);

        /*
         * Next emit an abstract redirecter, this makes life better
         * for our primitive type story.
         */

        // Dotted method; downcast self and
        // forward to static method in springboard class
        // with explicit self parameter.

        // InstantiatingClassloader.forwardingMethod(cw, mname, ACC_PUBLIC, 0,
        //                                           springBoardClass, mname, INVOKESTATIC,
        //                                           sig, n, true);
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
        int modifiers = ACC_PUBLIC;

        /*
         * Need to modify the
         * signature, depending on
         * circumstances.
         */
        String sig = NamingCzar.jvmSignatureFor(NodeUtil.getParamType(x),
                                                returnType, component.getName());

        String mname;
        
        int n = params.size();
        if (n == 1 && params.get(0).getIdType().unwrap() instanceof TupleType) {
        	Param p0 = params.get(0);
            TupleType tuple_type = ((TupleType) p0.getIdType().unwrap());
            if (tuple_type.getElements().size() == 0) n = 0;  //single void argument not used
        }

        // TODO different collision rules for top-level and for
        // methods. (choice of mname)

        String schema = ""; // static params?
        
        if (selfIndex != Naming.NO_SELF) {
            sig = Naming.removeNthSigParameter(sig, selfIndex);
            mname = Naming.fmDottedName(singleName(name), selfIndex);
            n--;
        } else if (inAMethod ) { // savedInAnObject) {
            mname = singleName(name);
        } else {
            mname = nonCollidingSingleName(name, sig,schema); 
        }

        CodeGen cg = new CodeGen(this);
        
        if (savedInAnObject) {
            // TODO if no overload, also emit forwarding method, mutilate "mname"
            String mangled_mname = NamingCzar.mangleAwayFromOverload(mname);

            
            if (! typeLevelOverloadedNamesAndSigs.contains(mname+sig+schema)) {
            
            InstantiatingClassloader.forwardingMethod(cg.cw, mname, ACC_PUBLIC, 0,
                    traitOrObjectName, mangled_mname, INVOKEVIRTUAL,
                    sig, sig, null, n+1, true, null);
            }
            
            mname = mangled_mname;
        } else {
            // trait default OR top level.
            // DO NOT special case run() here and make it non-static
            // (that used to happen), as that's wrong. It's
            // addressed in the executable wrapper code instead.
            modifiers |= ACC_STATIC;
        }

        cg.generateActualMethodCode(modifiers, mname, sig, params, selfIndex,
                                    inAMethod, body);

    }


    /** Generate an actual Java method and body code from an Expr.
     *  Should be done within a nested codegen as follows:
     *  new CodeGen(this).generateActualMethodCode(...);
     */
    private void generateWrapperMethodCode(int modifiers, String mname, String wname, String sig,
                                          List<Param> params) {

        // ignore virtual, now.
        if (0 == (ACC_STATIC & modifiers))
            return;

        mv = cw.visitCGMethod(modifiers, wname, sig, null, null);
        mv.visitCode();

        // Need to copy the parameter across for the wrapper call.
        // Modifiers should tell us how to do the call, maybe?
        // Invokestatic, for now.

        int i = 0;

        for (Param p : params) {
            // Type ty = p.getIdType().unwrap();
            mv.visitVarInsn(ALOAD, i);
            i++;
        }

        mv.visitMethodInsn(INVOKESTATIC, packageAndClassName, mname, sig);

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
        List<VarCodeGen> paramsGen = addParams(params, selfIndex);
        // Compile the body in the parameter environment

        body.accept(this);
        try {
            exitMethodScope(selfIndex, selfVar, paramsGen);
        } catch (Throwable t) {
            throw new Error("\n"+NodeUtil.getSpan(body)+": Error trying to close method scope.\n" + mv.getText(),t);
        }
    }

    /**
     * @param params
     * @param selfIndex
     * @return
     */
    private List<VarCodeGen> addParams(List<Param> params, int selfIndex) {
        
        /* Special case 
         * If there is just one parameter, and it is a tuple, then we need to
         * monkey with the params a wee bit.
         */
        
        List<VarCodeGen> paramsGen = new ArrayList<VarCodeGen>(params.size());
        int index = 0;
        
        if (params.size() == 1 && selfIndex == Naming.NO_SELF && 
                (params.get(0).getIdType().unwrap() instanceof TupleType) ) {
            Param p0 = params.get(0);
            TupleType tuple_type = ((TupleType) p0.getIdType().unwrap());
            List<Type> tuple_types = tuple_type.getElements();
            if (tuple_types.size() == 0) return new ArrayList<VarCodeGen>(); //actually a single void parameter - don't pass it
            Id tuple_name = p0.getName();
            String tuple_name_string = tuple_name.getText();
                        
            index = Naming.TUPLE_ORIGIN;
            for (Type t : tuple_types) {
                Id id = NodeFactory.makeId(tuple_name, tuple_name_string + index);
                Param p = NodeFactory.makeParam(id, t);
                VarCodeGen v = addParam(p);
                paramsGen.add(v);
                index++;
            }
            
            /* 
             * Next construct a local var, a tuple, from the parameters.
             * Perhaps it will go unused, in which case dead code elimination
             * in the JIT will clean it out for us.
             * 
             * First push everything, then invoke the appropriate static make
             * method from the tuple type, then add the result as a local var.
             */
            
            VarCodeGen vcg = new VarCodeGen.LocalVar(tuple_name, tuple_type, this);
            
            addLocalVar(vcg);
            paramsGen.add(vcg);
            
            index = Naming.TUPLE_ORIGIN;
            for (Type t : tuple_types) {
                Id id = NodeFactory.makeId(tuple_name, tuple_name_string + index);
                VarCodeGen vcge = getLocalVarOrNull(id);
                vcge.pushValue(mv);
                index++;
            }
            
            makeTupleOfSpecifiedType(tuple_type);
            
            vcg.assignValue(mv);
            
        } else {
            for (Param p : params) {
                if (index != selfIndex) {
                    VarCodeGen v = addParam(p);
                    paramsGen.add(v);
                }
                index++;
            }
        }
       
        return paramsGen;
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
        int selfIndex = NodeUtil.selfParameterIndex(params);

        // Someone had better get rid of anonymous names before they get to CG.
        IdOrOp name = (IdOrOp) header.getName();
        //IdOrOp uaname = (IdOrOp) x.getUnambiguousName();

        if (emittingFunctionalMethodWrappers) {
            if (selfIndex==Naming.NO_SELF)
                return; // Not functional = no wrapper needed.
        }

        Option<com.sun.fortress.nodes.Type> optReturnType = header.getReturnType();

        if (optReturnType.isNone())
            throw sayWhat(x, "Return type is not inferred.");

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
            throw sayWhat(x, "Unhandled function name.");
        }

        List<StaticParam> sparams = header.getStaticParams();

        boolean canCompile =
	    header.getWhereClause().isNone() && // no where clause
	    // 14 Jul 2011: I think we can now handle throw properly, and throws clauses as such,
	    // once type-checked, do not require any special handling at the low (Java) level.  --Guy
	    //	    header.getThrowsClause().isNone() && // no throws clause
	    header.getContract().isNone() && // no contract
	    fnDeclCompilableModifiers.containsAll(header.getMods()) && // no unhandled modifiers
	    !inABlock; // no local functions

        if (!canCompile)
            throw sayWhat(x, "Don't know how to compile this kind of FnDecl.");

        boolean inAMethod = inAnObject || inATrait;
        boolean savedInAnObject = inAnObject;
        boolean savedInATrait = inATrait;
        boolean savedInAMethod = inAMethod;
        boolean savedEmittingFunctionalMethodWrappers = emittingFunctionalMethodWrappers;

        try {
            inAnObject = false;
            inATrait = false;

            if (emittingFunctionalMethodWrappers) {
                if (! sparams.isEmpty()) {
                    functionalMethodWrapper(x, (IdOrOp)name,  selfIndex, savedInATrait, sparams);
                } else {
                    functionalMethodWrapper(x, (IdOrOp)name,  selfIndex, savedInATrait, sparams);
                }
            } else {

                Option<Expr> optBody = x.getBody();
                if (optBody.isNone()) {
                    if (savedInATrait) return; // Nothing concrete to do; dumpSigs already generated abstract signature.
                    throw sayWhat(x, "Abstract function declarations are only supported in traits.");
                }
                Expr body = optBody.unwrap();

                // For now every Fortress entity is made public, with
                // namespace management happening in Fortress-land. Right?
                // [JWM:] we'll want to clamp down on this long-term, but
                // we have to get nesting right---we generate a pile of
                // class files for one Fortress component

                if (! sparams.isEmpty()) {
                    if (inAMethod) {
                        // A generic method in a trait or object.
                        // TODO no overload-disambiguation yet
                        generateGenericMethod(x, (IdOrOp)name,
                                selfIndex, savedInATrait, inAMethod);
                        // throw sayWhat(x, "Generic methods not yet implemented.");

                    } else {
                        List<StaticParam> tsp = null;
                        generateGenericFunctionClass(new FnNameInfo(x, tsp, thisApi()),
                                new GenericMethodBodyMakerFnDecl(x), (IdOrOp)name, selfIndex, null);
                    }
                 } else if (savedInATrait) {
                    generateTraitDefaultMethod(x, (IdOrOp)name,
                            params, selfIndex, returnType, body);
                 } else {
                    generateFunctionalBody(x, (IdOrOp)name,
                            params, selfIndex, returnType, savedInAMethod,
                            savedInAnObject, body);
                 }
            }

        } finally {
            inAnObject = savedInAnObject;
            inATrait = savedInATrait;
            emittingFunctionalMethodWrappers = savedEmittingFunctionalMethodWrappers;
        }

    }

    private String genericDecoration(FnDecl x, Naming.XlationData xldata) {
        List<StaticParam> sparams = x.getHeader().getStaticParams();
        return NamingCzar.genericDecoration(sparams, xldata, thisApi());
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
            int selfIndex,
            boolean savedInATrait,
            List<StaticParam> f_method_static_params) {
        functionalMethodWrapper(x, name, selfIndex, savedInATrait, f_method_static_params, currentTraitObjectDecl);
    }
    
    private void functionalMethodWrapper(FnDecl x, IdOrOp name,
                                         int selfIndex,
                                         boolean savedInATrait,
                                         List<StaticParam> f_method_static_params,
                                         TraitObjectDecl ctod) {
        
        // FnHeader header = x.getHeader();
        TraitTypeHeader trait_header = ctod.getHeader();
        List<StaticParam> trait_sparams = trait_header.getStaticParams();
        //String uaname = NamingCzar.idOrOpToString(x.getUnambiguousName());

        FnHeader header = x.getHeader();
        List<Param> params = header.getParams();
        com.sun.fortress.nodes.Type returnType = header.getReturnType()
                .unwrap();
        com.sun.fortress.nodes.Type paramType = NodeUtil.getParamType(x);
        
        String sig = NamingCzar.jvmSignatureFor(paramType,
                returnType, component.getName());
        
        int modifiers = ACC_PUBLIC + ACC_STATIC;

        int invocation = savedInATrait ? INVOKEINTERFACE : INVOKEVIRTUAL;

        
        if (f_method_static_params.size() > 0) {
            /*
             * This is the generic method case, could also be of a generic class.
             * Note that the caller does not know the difference between this
             * generic function, and any other; however, that is apparent here
             * in the wrapper, and so the generic invocation must be open-coded.
             * 
             * The name has to look right for a generic function invocation.
             * 
             * Step 1: mangle name to generic form.
             * 
             * Step 2: emit a generic forwarding method with a body that
             * invokes the appropriate dotted method.  This is not a standard
             * forwarding wrapper, at least not yet.
             * 
             * Step 3: call the appropriate, returned, closure.
             */
            
            PCNforClosure pair = nonCollidingClosureName(new FnNameInfo(x, trait_sparams, thisApi()), Naming.NO_SELF, name,
                    f_method_static_params, this.packageAndClassName);
            
            String PCN = pair.PCN;
            String PCNOuter = pair.PCNOuter;
            Naming.XlationData xldata = pair.xldata;
                        
            // System.err.println(PCN);

            CodeGen cg = new CodeGen(this);
            cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
            
            ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();

            // This creates the closure bits
            String forwarding_method_name =
                InstantiatingClassloader.closureClassPrefix(PCN, cg.cw, PCN, sig, null, isf_list);

            // Need to invoke a generic method
            cg.mv = cg.cw.visitCGMethod(modifiers, forwarding_method_name, sig, null, null);
            cg.mv.visitCode();
            
            // DRC WIP
            
            // Create a modified fndecl (same as done for the generic method
            // itself) in order to obtain the proper schema for the call.
            FnDecl new_fndecl = convertGenericMethodToClosureDecl(x, selfIndex,
                    x.getInfo().getSpan());
            
            // Next need to perform a generic method invocation.

//            FnNameInfo fnni = new FnNameInfo(new_fndecl, thisApi());
//            String dottedName = Naming.fmDottedName(singleName(name), selfIndex);
//            ArrowType invoked_at = fndeclToType(new FnNameInfo(new_fndecl, thisApi()), selfIndex);
//            String method_name2 = genericMethodName(dottedName, invoked_at);
            String method_name = NamingCzar.genericMethodName(new FnNameInfo(new_fndecl, trait_sparams, thisApi()), selfIndex, thisApi());
//            String alt_method_name = genericMethodName(fnni, selfIndex);
//            if (! method_name.equals(method_name2)) {
//                throw new Error("mismatched names:\n" + method_name + "\n" + method_name2+ "\n" + alt_method_name);
//            }
            // don't need String method_closure_name = genericMethodClosureName(dottedName, invoked_at);

            Param self_param = params.get(selfIndex);
            Type self_type = (Type) self_param.getIdType().unwrap(); // better not be a pattern
            
            String string_sargs = NamingCzar.genericDecoration(self_type, f_method_static_params, null, thisApi());
            
            // Obtain self
            cg.mv.visitVarInsn(ALOAD, selfIndex);

            Type prepended_domain = null;
            if (! (paramType instanceof TupleType) || selfIndex == 0) {
                prepended_domain = paramType;
            } else {
                TupleType tpt = (TupleType) paramType;
                List<Type> lt = tpt.getElements();
                // need to fix the list call
                prepended_domain = NodeFactory.makeTupleType(paramType.getInfo().getSpan(), new SwappedList<Type>(lt, selfIndex));
            }
            
            // find closure
            String castToArrowType = cg.emitFindGenericMethodClosure(
                    method_name, self_type, prepended_domain, returnType,
                    string_sargs, true);
            
            // generic method closure (gmc) signature
            String gmc_sig = NamingCzar.jvmSignatureFor(prepended_domain, returnType, thisApi());
            
            SignatureParser sp = new SignatureParser(gmc_sig);

            int parsed_arg_cursor = 1;
            
            // Push params
            InstantiatingClassloader.pushParamsNotSelf(selfIndex, params.size(), null, cg.mv,
                    sp, parsed_arg_cursor);
            
            // invoke closure
            cg.emitInvokeGenericMethodClosure(gmc_sig,
                    castToArrowType);            
            
            cg.mv.visitInsn(ARETURN);
            cg.mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);            
        //            cg.mv.visitMaxs(2, 3);
            cg.mv.visitEnd();
            
            InstantiatingClassloader.optionalStaticsAndClassInitForTO(isf_list, cg.cw);
            
            cg.cw.dumpClass(PCNOuter, xldata);



        } else {
        
            String dottedName = Naming.fmDottedName(singleName(name), selfIndex);
        if (trait_sparams.size() == 0) {

            // Check for sparams on the function itself; if so, emit a generic
            // function performing a generic dispatch.
            
            // TODO different collision rules for top-level and for methods.
            String mname = nonCollidingSingleName(name, sig, "");

            functionalForwardingMethod(cw, mname, sig, traitOrObjectName,
                    dottedName, selfIndex, params.size(), savedInATrait);
            
        } else {

            functionalMethodOfGenericTraitObjectWrapper(x, trait_sparams,
                    sig, invocation, dottedName, selfIndex,
                    params, modifiers);

        }
        }

    }

    /**
     * Newspeak for forwarding; the old forwarding method code is too atrocious for words.
     * 
     * @param cw
     * @param generated_method_name
     * @param generated_method_sig
     * @param invoked_method_trait_object
     * @param invoked_method_name
     * @param self_index
     * @param n_params_including_self
     * @param is_a_trait
     */
    static void functionalForwardingMethod(
            ClassWriter cw,
            String generated_method_name,
            String generated_method_sig,
            String invoked_method_trait_object,
            String invoked_method_name,
            int self_index,
            int n_params_including_self,
            boolean is_a_trait
            ) {
        String invoked_method_sig = Naming.removeNthSigParameter(generated_method_sig, self_index);
        String self_sig = Naming.nthSigParameter(generated_method_sig, self_index);
        self_sig.substring(1, self_sig.length()-1); // Strip off L;
        
        String ret_type = Naming.sigRet(invoked_method_sig);
        
        MethodVisitor mv = cw.visitMethod(ACC_STATIC + ACC_PUBLIC, generated_method_name, generated_method_sig, null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, self_index);
        // If the self_sig does not match the type that we are about to invoke,
        // cast it.
        if (! self_sig.equals(invoked_method_trait_object)) {
            InstantiatingClassloader.generalizedCastTo(mv, invoked_method_trait_object);
        }
        // push parameters, except for self
        for (int i = 0; i < n_params_including_self; i++)
            if (i != self_index) {
                mv.visitVarInsn(ALOAD, i);
            }
        
        // invoke
        mv.visitMethodInsn(is_a_trait ? INVOKEINTERFACE : INVOKEVIRTUAL, invoked_method_trait_object, invoked_method_name, invoked_method_sig);

        
        // This will need to generalize for covariant types, I expect
        if (ret_type.startsWith(Naming.TUPLE_OX) ||
                ret_type.startsWith(Naming.ARROW_OX) ) {
            InstantiatingClassloader.generalizedCastTo(mv, ret_type);
        }
        mv.visitInsn(ARETURN);

        mv.visitMaxs(2, 3);
        mv.visitEnd();

    }

    /**
     * @param x
     * @param name
     * @param f_method_static_params
     * @return
     */
    public PCNforClosure nonCollidingClosureName(FnNameInfo x, int self_index, IdOrOp name,
            List<StaticParam> f_method_static_params, String packageAndClassName) {
        ArrowType at = x.functionArrowType(); // type schema from old
        String generic_arrow_type = NamingCzar.jvmTypeDesc(at, thisApi(),
                   false);
        return nonCollidingClosureName(generic_arrow_type, self_index, name, f_method_static_params,
                packageAndClassName);
    }
    /**
     * @param generic_arrow_type
     * @param name
     * @param f_method_static_params
     * @return
     */
    public PCNforClosure nonCollidingClosureName(String generic_arrow_type, int self_index, IdOrOp name,
                List<StaticParam> f_method_static_params, String packageAndClassName) {
        PCNforClosure pair = new PCNforClosure();
        
        pair.xldata = 
            xlationData(Naming.FUNCTION_GENERIC_TAG);
        
        String sparams_part = NamingCzar.genericDecoration(f_method_static_params,
                pair.xldata, thisApi());


        /* What's going on here, is that the method name needs to ignore the
           self parameter (it has to override from trait to object, etc.)
           But the class name for the closure has to be qualified to avoid
           collisions. */
         
        String mname = self_index == Naming.NO_SELF ?
                singleName(name) :
                Naming.fmDottedName(singleName(name), self_index);
        
        // This is not quite general enough, yet -- need to be clear on
        // where the static parameters go in the generic-generic case.
        
        
        pair.PCN =
            Naming.genericFunctionPkgClass(packageAndClassName, mname,
                                               sparams_part, generic_arrow_type);
        pair.PCNOuter =
            Naming.genericFunctionPkgClass(packageAndClassName, mname,
                        Naming.makeTemplateSParams(sparams_part) , generic_arrow_type);
        
        if (topLevelOverloadedNamesAndSigs.contains(pair.PCN) //KBN - think we mean typeLevelOverloadedNamesAndSigs when method (always?)
                                                              //   Even those appear to be generated incorrectly right now though
                // topLevelOverloadedNamesAndSigs.contains(pair.PCNOuter)
                ) { // Clash on file name, else we create a bad class file.
            mname = NamingCzar.mangleAwayFromOverload(mname);
            pair.PCN =
                Naming.genericFunctionPkgClass(packageAndClassName, mname,
                                                   sparams_part, generic_arrow_type);
            pair.PCNOuter =
                Naming.genericFunctionPkgClass(packageAndClassName, mname,
                            Naming.makeTemplateSParams(sparams_part) , generic_arrow_type);
        }
        return pair;
    }


    /**
     * @param mname
     * @param sparams_part
     * @param sig
     * @param generic_arrow_type
     * @param invocation
     * @param dottedName
     * @param selfIndex
     * @param params
     * @param modifiers
     */
    private void functionalMethodOfGenericTraitObjectWrapper(
            FnDecl x,
            List<StaticParam> static_params, String sig, 
            int invocation, String dottedName, int selfIndex,
            List<Param> params, int modifiers) {
                
        List<StaticParam> tsp = currentTraitObjectDecl.getHeader().getStaticParams();

        PCNforClosure pair = nonCollidingClosureName(new FnNameInfo(x, tsp, thisApi()), Naming.NO_SELF,
                (IdOrOp) x.getHeader().getName(),
                static_params, this.packageAndClassName);
        
        String PCN = pair.PCN;
        String PCNOuter = pair.PCNOuter;
        Naming.XlationData xldata = pair.xldata;

        // System.err.println(PCN);

        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        
        ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();

        // This creates the closure bits
        String forwarding_method_name =
            InstantiatingClassloader.closureClassPrefix(PCN, cg.cw, PCN, sig, null, isf_list);

        InstantiatingClassloader.forwardingMethod(cg.cw, forwarding_method_name, modifiers,
                selfIndex,
                //traitOrObjectName+sparams_part,
                traitOrObjectName,
                dottedName, invocation, sig, sig,
                params.size(), true, null);

        InstantiatingClassloader.optionalStaticsAndClassInitForTO(isf_list, cg.cw);
        
        cg.cw.dumpClass(PCNOuter, xldata);
        
        //used when we need to infer static parameters at runtime
//        CodeGen cg2 = new CodeGen(this);
//        cg2.generateRuntimeInstantiationClass(PCNOuter, sparams_part.split(",").length);
    }


    /**
     * @param cg
     */

    private void methodReturnAndFinish() {
        mv.visitInsn(ARETURN);
        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }

    public void forTupleExpr(TupleExpr x) {
        List<Expr> exprs = x.getExprs();
        Type t = x.getInfo().getExprType().unwrap();
        evaluateSubExprsAppropriately(x, t, exprs);
        // Invoke Tuple[\ whatever \].make(Object;Object;Object;etc)
        makeTupleOfSpecifiedType(t);

    }

    /**
     * @param t
     */
    private void makeTupleOfSpecifiedType(Type t) {
        String tcn =  NamingCzar.jvmTypeDesc(t, thisApi(), false, true);
        String arg_sig = NamingCzar.jvmTypeDesc(t, thisApi(), true, false);
        String sig = Naming.makeMethodDesc(arg_sig,Naming.internalToDesc(tcn));
        tcn = "Concrete" + tcn;
        mv.visitMethodInsn(INVOKESTATIC,
                tcn,
                "make",
                sig);
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
        BASet<VarType> fvts = fvt.freeVarTypes(x);
        // This below catches references to the "self" type in a generic context.
        // It appears as the up-pointing-finger VarType of "self".
        // This may not catch all references to the type; that needs to be
        // figured out later, as tests go blooie.  The signature of this error
        // would be unreplaced occurrences of the up-arrow in executed code,
        // leading to verify errors of various sorts.
        List<VarCodeGen> freeVars = getFreeVars(body, fvts);

        //      Create the Class
        String desc = NamingCzar.makeAbstractArrowDescriptor(params, rt, thisApi());
        String idesc = NamingCzar.makeArrowDescriptor(params, rt, thisApi());
        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        cg.cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        String className = NamingCzar.gensymArrowClassName(Naming.deDot(thisApi().getText()), x.getInfo().getSpan());
        String classFileName = className;
        Naming.XlationData xldata = null;
        if (fvts.size() > 0) {
            // TODO Need to use proper conventions for this.
            className += Useful.listInDelimiters(Naming.LEFT_OXFORD, fvts, Naming.RIGHT_OXFORD);
            classFileName += Naming.LEFT_OXFORD + Naming.RIGHT_OXFORD;
            xldata = xlationData(Naming.FUNCTION_GENERIC_TAG);
            for (VarType v : fvts)
                xldata.addKindAndNameToStaticParams(Naming.XL_TYPE, v.getName().getText());
        }
        debug("forFnExpr className = ", className, " desc = ", desc);
        cg.lexEnv = cg.createTaskLexEnvVariables(className, freeVars);
        cg.cw.visit(InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER,
                    className, null, desc, new String[] {idesc});

        // Generate the constructor (initializes captured free vars from param list)
        String init = taskConstructorDesc(freeVars);
        cg.generateFnExprInit(desc, init, freeVars);


        String applyDesc = NamingCzar.jvmSignatureFor(params, NamingCzar.jvmBoxedTypeDesc(rt, thisApi()),
                                                      thisApi());

        // Generate the apply method
        // System.err.println(idesc+".apply"+applyDesc+" gen in "+className);
        cg.mv = cg.cw.visitCGMethod(ACC_PUBLIC, Naming.APPLY_METHOD, applyDesc, null, null);
        cg.mv.visitCode();

        // Since we call this virtually we need a slot for the arrow implementation of this object.
        cg.mv.reserveSlot0();
        
        cg.addParams(params, Naming.NO_SELF);
        
        body.accept(cg);

        cg.methodReturnAndFinish();
        cg.cw.dumpClass(classFileName, xldata);
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
    private String nonCollidingSingleName(IdOrOpOrAnonymousName name, String sig, String schema) {
        String mname = singleName(name);
        if (topLevelOverloadedNamesAndSigs.contains(mname+sig+schema)) {
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
    static private String singleName(IdOrOpOrAnonymousName name) {
        String nameString = NamingCzar.idOrOpToString((IdOrOp)name);
        String mname = nameString; // Naming.mangleIdentifier(nameString);
        return mname;
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
        StringBuilder b = new StringBuilder();

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
        if (fn != null)
            throw sayWhat(x, "Haven't figured out references to local/parameter functions yet");

        Pair<String, String> pc_and_m= functionalRefToPackageClassAndMethod(x);

        // need to deal with generics.
        // TODO refactor against else-arm and against forFunctionalRef
        List<StaticArg> sargs = x.getStaticArgs();
        String decoration = NamingCzar.instantiatedGenericDecoration(sargs, thisApi());
        if (decoration.length() > 0) {
            // com.sun.fortress.nodes.Type arrow = exprType(x);

            // debugging reexecute
            decoration = NamingCzar.instantiatedGenericDecoration(sargs, thisApi());
            /*
             * TODO, BUG, need to use arrow type of uninstantiated generic!
             * This is necessary because otherwise it is difficult (impossible?)
             * to figure out the name of the template class that will be
             * expanded later.
             */

            Type oschema = x.getOverloadingSchema().unwrap();
            Type otype = x.getOverloadingType().unwrap();

            String arrow_desc = NamingCzar.jvmTypeDesc(otype, thisApi(), true);
            String arrow_type = NamingCzar.jvmTypeDesc(oschema, thisApi(), false);

            String PCN =
                Naming.genericFunctionPkgClass(pc_and_m.first(), pc_and_m.second(),
                                                   decoration, arrow_type);
            
            mv.visitFieldInsn(GETSTATIC, PCN, Naming.CLOSURE_FIELD_NAME, arrow_desc);

        } else { // not generic reference.

            // If it's an overloaded type, oy.
            com.sun.fortress.nodes.Type arrow = exprType(x);
            // Capture the overloading foo, mutilate that into the name of the thing that we want.
            Pair<String, String> method_and_signature = resolveMethodAndSignature(
                    x, arrow, pc_and_m.second());
            /* we now have package+class, method name, and signature.
             * Emit a static reference to a field in package/class/method+envelope+mangled_sig.
             * Classloader will see this, and it will trigger demangling of the name, to figure
             * out the contents of the class to be loaded.
             */
            String arrow_desc = NamingCzar.jvmTypeDesc(arrow, thisApi(), true);
            String arrow_type = NamingCzar.jvmTypeDesc(arrow, thisApi(), false);
            String PCN = pc_and_m.first() + "$" +

            method_and_signature.first() +
            Naming.ENVELOPE + "$"+ // "ENVELOPE"
            arrow_type;
            /* The suffix will be
             * (potentially mangled)
             * functionName<ENVELOPE>closureType (which is an arrow)
             *
             * must generate code for the class with a method apply, that
             * INVOKE_STATICs prefix.functionName .
             */
            mv.visitFieldInsn(GETSTATIC, PCN, Naming.CLOSURE_FIELD_NAME, arrow_desc);
        }
    }

    /**
     * @param x
     */
    public void forFunctionalRef(FunctionalRef x) {
        debug("forFunctionalRef ", x);

        /* Arrow, or perhaps an intersection if it is an overloaded function. */
        com.sun.fortress.nodes.ArrowType arrow = (ArrowType) exprType(x);

        List<StaticArg> sargs = x.getStaticArgs();
        TraitType trait_self_t = null;
        if (arrow instanceof ArrowType) {
            /*
             *  Note this does not yet deal with functional, generic methods
             *  because it stomps on "sargs".
             *  
             *  This is also hacked because of uncertain results
             *  with opr parameters; the args were repeated (why?)
             */
            Option<MethodInfo> omi = ((ArrowType) arrow).getMethodInfo();
            
            // sargs.size() == 0 is a hack to avoid doubled parameters.
            if (omi.isSome() && sargs.size() == 0) {
                MethodInfo mi = omi.unwrap();
                // int self_i = mi.getSelfPosition();
                Type self_t = mi.getSelfType();
                if (self_t instanceof TraitSelfType)
                    self_t = ((TraitSelfType)self_t).getNamed();
                if (self_t instanceof TraitType) {
                    trait_self_t = (TraitType) self_t;
                    /* Trait static args, followed by method static args */
                    List<StaticArg> new_sargs = Useful.concat(trait_self_t.getArgs(), sargs);
                    if (sargs.size() > 0 && new_sargs.size() != sargs.size())
                        throw new Error("Interesting case found");
                    sargs = new_sargs;
                }
            }
        } else {
            System.err.println("non-arrowtype "+arrow);
        }

        String decoration = NamingCzar.instantiatedGenericDecoration(sargs, thisApi());

        debug("forFunctionalRef ", x, " arrow = ", arrow);

        Pair<String, String> calleeInfo = functionalRefToPackageClassAndMethod(x);

        String pkgClass = calleeInfo.first();
        String theFunction = calleeInfo.second();
        
        /*
         * BEGIN AMAZING HACK.
         * If this is a reference to an Op,
         * and the Op has the same name as an OpArg in sargs,
         * figure out the original 
         */
        
        if (x instanceof OpRef && sargs.size() > 0 && trait_self_t != null) {
            int arg_index = 0;
            for (StaticArg sarg : sargs) {
                if (sarg instanceof OpArg) {
                    OpArg op_sarg = (OpArg) sarg;
                    if (op_sarg.getId().getText().equals(theFunction)) {
                        TypeConsIndex tst_tci = ci.typeConses().get(trait_self_t.getName());
                        List<StaticParam> tst_tci_sp = tst_tci.staticParameters();
                        StaticParam sps = tst_tci_sp.get(arg_index);
                        theFunction = sps.getName().getText();
                        break;
                    }
                }
                arg_index++;
            }
        }

        /*
         * END AMAZING HACK.
         */
        
        if (decoration.length() > 0) {
            // debugging reexecute
            decoration = NamingCzar.instantiatedGenericDecoration(sargs, thisApi());
            /*
             * TODO, BUG, need to use arrow type of uninstantiated generic!
             * This is necessary because otherwise it is difficult (impossible?)
             * to figure out the name of the template class that will be
             * expanded later.
             */

            Option<Type> oschema = x.getOverloadingSchema();
            Type arrowToUse = arrow;

            if (oschema.isSome()) {
                // bug??
                // Schema should NOT be AnyType, but in at least one case it is
                if (!( oschema.unwrap() instanceof AnyType ))
                    arrowToUse = oschema.unwrap();
            } else {
                System.err.println(NodeUtil.getSpan(x) + ": FunctionalRef " + x + " lacks overloading schema; using "+arrowToUse+" sargs "+sargs);
            }

            String arrow_type = NamingCzar.jvmTypeDesc(arrowToUse, thisApi(), false);

            pkgClass =
                Naming.genericFunctionPkgClass(pkgClass, theFunction,
                                                   decoration, arrow_type);
            theFunction = Naming.APPLIED_METHOD;

            // pkgClass = pkgClass.replace(".", "/");
            // DEBUG, for looking at the schema append to a reference.
            // System.err.println("At " + x.getInfo().getSpan() + ", " + pkgClass);
        }

        callStaticSingleOrOverloaded(x, arrow, pkgClass, theFunction);
        if (arrow.getRange() instanceof BottomType) {
            infiniteLoop(); // prevent bottoms from reaching other code and generating verify errors.
        }
    }

    private void infiniteLoop() {
        castToBottom(mv);
    }
    
    static public void castToBottom(MethodVisitor mv) {
        // better to push null, then throw it.
        org.objectweb.asm.Label loop = new org.objectweb.asm.Label();
        mv.visitLabel(loop);
        mv.visitJumpInsn(GOTO, loop);        
    }
    /**
     * @param x
     * @return
     */
    private Pair<String, String> functionalRefToPackageClassAndMethod(
            FunctionalRef x) {
        List<IdOrOp> names = x.getNames();

        if ( names.size() != 1) {
            throw sayWhat(x,"Non-unique overloading after rewrite " + x);
        }

        IdOrOp n = names.get(0);

        if (n.getText().contains(Naming.FOREIGN_TAG))
            return idToPackageClassAndName(n);

        Option<APIName> oapi = n.getApiName();
        if (oapi.isSome()) {
            APIName a = oapi.unwrap();
            ApiIndex ai = env.apis().get(a);
            Relation<IdOrOpOrAnonymousName, Function> fns = ai.functions();
            Set<Function> s = fns.matchFirst(NodeFactory.makeLocalIdOrOp(n));
            if (s.size() != 0) {
                IdOrOp nn = NodeFactory.makeIdOrOp(a, x.getOriginalName());
                Pair<String, String> trial = idToPackageClassAndName(nn);
                //Pair<String, String> rval = idToPackageClassAndName(n);
//                if (! (trial.first().equals(rval.first()) &&
//                       trial.second().equals(rval.second()))) {
//                    System.err.println("Substitute " +
//                            trial.first()+"."+trial.second()+" for "+
//                            rval.first()+"."+rval.second());
//                }
                return trial;

            } else {
                Pair<String, String> rval = idToPackageClassAndName(n);
                return rval;

            }

        } else {
            Relation<IdOrOpOrAnonymousName, Function> fns = ci.functions();
            Set<Function> s = fns.matchFirst(n);
            if (s.size() == 1) {
                IdOrOp nn = x.getOriginalName();
                Pair<String, String> trial = idToPackageClassAndName(nn);
                //Pair<String, String> rval = idToPackageClassAndName(n);
//                if (!(trial.first().equals(rval.first()) && trial.second()
//                        .equals(rval.second()))) {
//                    System.err.println("Substitute " + trial.first() + "."
//                            + trial.second() + " for " + rval.first() + "."
//                            + rval.second());
//                }
                return trial;
            } else {
                //IdOrOp nn = x.getOriginalName();
                //Pair<String, String> trial = idToPackageClassAndName(nn);
                Pair<String, String> rval = idToPackageClassAndName(n);
                return rval;
            }

        }
        // return idToPackageClassAndName(n); // names.get(0));
    }

    private Pair<String, String> idToPackageClassAndName(IdOrOp fnName) {
        return NamingCzar.idToPackageClassAndName(fnName, thisApi());
    }
    public void forIf(If x) {
        Debug.debug( Debug.Type.CODEGEN, 1,"forIf ", x);
        List<IfClause> clauses = x.getClauses();
        //Option<Block> elseClause = x.getElseClause();

        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        org.objectweb.asm.Label falseBranch = new org.objectweb.asm.Label();
        for (IfClause ifclause : clauses) {

            GeneratorClause cond = ifclause.getTestClause();

            if (!cond.getBind().isEmpty())
                throw sayWhat(x, "Undesugared generalized if expression.");

            // emit code for condition and to check resulting boolean
            Expr testExpr = cond.getInit();
            debug( "about to accept ", testExpr, " of class ", testExpr.getClass());
            testExpr.accept(this);
            addLineNumberInfo(x);
            mv.visitMethodInsn(INVOKEVIRTUAL,
                               NamingCzar.internalFortressBoolean, "getValue",
                               Naming.makeMethodDesc("", NamingCzar.descBoolean));
            mv.visitJumpInsn(IFEQ, falseBranch);

            // emit code for condition true
            ifclause.getBody().accept(this);
            mv.visitJumpInsn(GOTO, done);

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
        if (l <= 32) {
            int y = bi.intValue();
            addLineNumberInfo(x);
            pushInteger(y);
            addLineNumberInfo(x);

            mv.visitMethodInsn(INVOKESTATIC,
                    NamingCzar.internalFortressIntLiteral, NamingCzar.make,
                    Naming.makeMethodDesc(NamingCzar.descInt,
                                              NamingCzar.descFortressIntLiteral));
        } else if (l <= 64) {
            long yy = bi.longValue();
            addLineNumberInfo(x);
            mv.visitLdcInsn(yy);
            addLineNumberInfo(x);
            mv.visitMethodInsn(INVOKESTATIC,
                    NamingCzar.internalFortressIntLiteral, NamingCzar.make,
                    Naming.makeMethodDesc(NamingCzar.descLong,
                                              NamingCzar.descFortressIntLiteral));
        } else {
            String s = bi.toString();
            addLineNumberInfo(x);
            mv.visitLdcInsn(s);
            addLineNumberInfo(x);
            mv.visitMethodInsn(INVOKESTATIC,
                    NamingCzar.internalFortressIntLiteral, NamingCzar.make,
                    Naming.makeMethodDesc(NamingCzar.descString,
                                              NamingCzar.descFortressIntLiteral));
        }
    }

    public void forBooleanLiteralExpr(BooleanLiteralExpr x) {
    	debug("forBooleanLiteralExpr ",x);
    	int b = x.getBooleanVal();
    	addLineNumberInfo(x);
    	if (b == 0) 
    		mv.visitInsn(ICONST_0);
    	else
    		mv.visitInsn(ICONST_1);	
    	addLineNumberInfo(x);
        mv.visitMethodInsn(INVOKESTATIC,
                NamingCzar.internalFortressBoolean, NamingCzar.make,
                Naming.makeMethodDesc(NamingCzar.descBoolean,
                                          NamingCzar.descFortressBoolean));    	
    }
    	
    public void forLocalVarDecl(LocalVarDecl d) {
        debug("forLocalVarDecl", d);

        List<LValue> lhs = d.getLhs();
        int n = lhs.size();
        List<VarCodeGen> vcgs = new ArrayList(n);
        List<Type> lhs_types = new ArrayList(n);
        for (LValue v : lhs) {
            if (!v.getIdType().isSome()) {
                throw sayWhat(d, "Variable being bound lacks type information!");
            }

            // Introduce variable
            Type ty = (Type)v.getIdType().unwrap();
            VarCodeGen vcg;
            if (v.isMutable()) {
                vcg = new VarCodeGen.LocalMutableVar(v.getName(), ty, this, thisApi());
            } else {
                vcg = new VarCodeGen.LocalVar(v.getName(), ty, this);
            }
            vcgs.add(vcg);
            addLocalVar(vcg);
            lhs_types.add(ty);
        }
        
        Type lhs_type = lhs_types.size() == 1 ? lhs_types.get(0) : NodeFactory.makeTupleType(lhs_types);

        // Compute rhs
        List<Expr> rhss = null;
        Expr rhs = d.getRhs().unwrap();
        if (n==1) {
            rhss = Collections.singletonList(rhs);
        } else if (rhs instanceof TupleExpr) {
            if (!((TupleExpr)rhs).getVarargs().isSome() &&
                    ((TupleExpr)rhs).getKeywords().isEmpty() ) {
                if (((TupleExpr)rhs).getExprs().size() == n)
                    rhss = ((TupleExpr)rhs).getExprs();
                else
                    rhss = null;
            } else {
                throw sayWhat(d, "Can't yet generate multiple-variable bindings if rhs is varargs or has keywords.");
            }
        } else {
            // Binding one (assumed to be tuple type) to many
            rhss = null;
        }

        if (rhss == null) {
            TupleType rhs_type = (TupleType) rhs.getInfo().getExprType().unwrap();
            String rhs_type_desc = NamingCzar.makeTupleDescriptor(rhs_type, thisApi(), false);
            String[] rhs_element_type_descs = NamingCzar.makeTupleElementDescriptors(rhs_type, thisApi());
            // Assignment of tuple, to multiple left hand side varibles.
            // Serially pick the pieces out of the tuple.
            rhs.accept(this);
            // Tuple is TOS
            for (int i = 0; i < n; i++) {
                // dup tuple (always, ensure swap will work).
                mv.visitInsn(DUP);

                VarCodeGen vcg = vcgs.get(i);
                /* swap dup'd tuple to TOS -- note assumption that prepareAssignValue pushes 
                 * either zero or one item on stack.
                 */
                mv.visitInsn(SWAP);
                // extract i'th member from tuple.
                mv.visitMethodInsn(INVOKEINTERFACE, rhs_type_desc, InstantiatingClassloader.TUPLE_TYPED_ELT_PFX+(Naming.TUPLE_ORIGIN+i), "()" + Naming.internalToDesc(rhs_element_type_descs[i]));
                
                vcg.assignValue(mv);
            }
            // discard tuple from TOS
            mv.visitInsn(POP);

        } else if (pa.worthParallelizing(rhs)) {
            forExprsParallel(rhss, lhs_type, vcgs);
        } else {
            forExprsSerial(rhss, lhs_type, vcgs);
        }

        // Evaluate rest of block with binding in scope
        CodeGen cg = new CodeGen(this);

        cg.doStatements(d.getBody().getExprs());
        // Dispose of bindings now that we're done
        // Do this in reverse order of creation.
        for (int i = n-1; i >= 0; i--) {
            vcgs.get(i).outOfScope(mv);
        }
    }

    // forObjectDecl just generates top-level bindings for functional
    // methods of object decls.  All the remaining work (of actually
    // generating the object class) is done by forObjectDeclPrePass.
    public void forObjectDecl(ObjectDecl x) {
        TraitTypeHeader header = x.getHeader();
        emittingFunctionalMethodWrappers = true;

        // TODO trim and/or consolidate this boilerplate around sparams_part
        List<StaticParam> original_static_params = header.getStaticParams();
        
        String sparams_part = NamingCzar.genericDecoration(original_static_params, null, thisApi());

        Id classId = NodeUtil.getName(x);
        String classFile =
            NamingCzar.jvmClassForToplevelTypeDecl(classId,
                    sparams_part,
                    packageAndClassName);

//        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
//                                                         NamingCzar.idToString(NodeUtil.getName(x)));
        debug("forObjectDecl ",x," classFile = ", classFile);
        traitOrObjectName = classFile;
        currentTraitObjectDecl = x;
        dumpTraitDecls(header.getDecls());
        emitOrphanedFunctionalMethods(classId);
        currentTraitObjectDecl = null;
        emittingFunctionalMethodWrappers = false;
        traitOrObjectName = null;
    }


    /**
     * @param for_class_id
     */
    private void emitOrphanedFunctionalMethods(Id for_class_id) {
        for (FunctionalMethod ofm : orphanedFunctionalMethods.getEmptyIfMissing(for_class_id)) {
            System.err.println("Orphaned functional method, trait " + for_class_id + ", fmethod " + ofm);
            FnDecl x = ofm.ast();
            int selfIndex = ofm.selfPosition();
            List<StaticParam> sparams = ofm.staticParameters(); // what about trait static parameters??
            IdOrOp name = ofm.name();
            BaseType bt = ((TraitSelfType)(x.getHeader().getParams().get(selfIndex).getIdType().unwrap())).getNamed();
            
            FnHeader header = x.getHeader();
            List<Param> params = header.getParams();
            com.sun.fortress.nodes.Type returnType = header.getReturnType()
                    .unwrap();
            com.sun.fortress.nodes.Type paramType = NodeUtil.getParamType(x);
            String sig = NamingCzar.jvmSignatureFor(paramType,
                    returnType, component.getName());

            
            String mname = nonCollidingSingleName(name, sig, "");

            String invoked_class_name =
                NamingCzar.jvmClassForToplevelTypeDecl(for_class_id,
                        "", // no generics just yet
                        packageAndClassName);

            String invoked_method_name = Naming.fmDottedName(singleName(name), selfIndex);

            
            functionalForwardingMethod(cw, mname, sig,
                    invoked_class_name, invoked_method_name, selfIndex, params.size(), inATrait);
        }
    }

    // forObjectDeclPrePass actually generates the class corresponding
    // to the given ObjectDecl.
    public void forObjectDeclPrePass(ObjectDecl x) {
        debug("Begin forObjectDeclPrePass for ", x);
        TraitTypeHeader header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();

        boolean canCompile =
            // x.getParams().isNone() &&             // no parameters
            // header.getStaticParams().isEmpty() && // no static parameter
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            //            header.getDecls().isEmpty() &&        // no members
            Modifiers.ObjectMod.containsAll(header.getMods())
            // ( extendsC.size() <= 1 ); // 0 or 1 super trait
            ;

        if ( !canCompile ) throw sayWhat(x);

        // Map<String, String> xlation = new HashMap<String, String>();
        List<String> splist = new ArrayList<String>();
        final List<StaticParam> original_static_params = header.getStaticParams();
        Option<List<Param>> original_params = NodeUtil.getParams(x);
        
        Naming.XlationData xldata = 
            xlationData(Naming.OBJECT_GENERIC_TAG);
        
        boolean savedInAnObject = inAnObject;
        inAnObject = true;
        Id classId = NodeUtil.getName(x);

        final ClassNameBundle cnb = new_ClassNameBundle(classId, original_static_params, xldata);

        String erasedSuperI = (EMIT_ERASED_GENERICS && cnb.isGeneric) ?
                cnb.stemClassName : "";
        String [] superInterfaces =
            NamingCzar.extendsClauseToInterfaces(extendsC, component.getName(), erasedSuperI);

        if (EMIT_ERASED_GENERICS && cnb.isGeneric) {
            emitErasedClassFor(cnb, (TraitObjectDecl) x);
        }

        String abstractSuperclass;
        if (superInterfaces.length > 0) {
            abstractSuperclass = superInterfaces[0] + NamingCzar.springBoard;
        } else {
            abstractSuperclass = NamingCzar.internalObject;
        }
        
        traitOrObjectName = cnb.className;
        debug("forObjectDeclPrePass ",x," classFile = ", traitOrObjectName);

        boolean isSingletonObject = NodeUtil.getParams(x).isNone();
        List<Param> params;
        if (!isSingletonObject) {
            params = NodeUtil.getParams(x).unwrap();				//TUPLE CONSTRUCTOR PROBLEM
            String init_sig = NamingCzar.jvmSignatureFor(params, "V", thisApi());

             // Generate the factory method
            String sig = NamingCzar.jvmSignatureFor(params, cnb.classDesc, thisApi());

            String mname ;

            CodeGen cg = this;
            String PCN = null;
            String PCNOuter = null;
            
            ArrayList<InitializedStaticField> isf_list = new ArrayList<InitializedStaticField>();           

            if (cnb.isGeneric) {
                ArrowType at =
                    FnNameInfo.typeAndParamsToArrow(NodeUtil.getSpan(x),
                            NodeFactory.makeTraitType(classId,
                                    STypesUtil.staticParamsToArgs(original_static_params)),
                                    original_params.unwrap());
                String generic_arrow_type = NamingCzar.jvmTypeDesc(at, thisApi(), false);
                

                PCNforClosure pair = nonCollidingClosureName(generic_arrow_type, Naming.NO_SELF, (IdOrOp) x.getHeader().getName(),
                        original_static_params, this.packageAndClassName);
                
                PCN = pair.PCN;
                PCNOuter = pair.PCNOuter;
                xldata = pair.xldata;

                cg = new CodeGen(this);
                cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);

                // This creates the closure bits
                // The name is disambiguated by the class in which it appears.
                mname = InstantiatingClassloader.closureClassPrefix(PCN, cg.cw, PCN, sig, null, isf_list);
            } else {
                mname = nonCollidingSingleName(x.getHeader().getName(), sig, "");
            }

            CodeGenClassWriter cw = cg.cw;
            CodeGenMethodVisitor mv = cw.visitCGMethod(ACC_STATIC + ACC_PUBLIC,
                    mname,
                    sig,
                    null,
                    null);

            mv.visitTypeInsn(NEW, cnb.className);
            mv.visitInsn(DUP);

            // iterate, pushing parameters, beginning at zero.
           // TODO actually handle N>0 parameters.

            int numParams = params.size();
            //if only a single parameter which is a tuple, signature will give them individually
            if (numParams == 1 && (params.get(0).getIdType().unwrap() instanceof TupleType)) {
            	Param p0 = params.get(0);
                TupleType tuple_type = ((TupleType) p0.getIdType().unwrap());
                List<Type> tuple_types = tuple_type.getElements();
                numParams = tuple_types.size();
            }
            for (int stack_offset = 0; stack_offset < numParams; stack_offset++) {
                // when we unbox, this will be type-dependent
                mv.visitVarInsn(ALOAD, stack_offset);
            }

            mv.visitMethodInsn(INVOKESPECIAL, cnb.className, "<init>", init_sig);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
            mv.visitEnd();

            if (cnb.isGeneric) {
                InstantiatingClassloader.optionalStaticsAndClassInitForTO(isf_list, cg.cw);

                cg.cw.dumpClass(PCNOuter, xldata);
            }


        } else { // singleton
            params = Collections.<Param>emptyList();
        }

        CodeGenClassWriter prev = cw;

        /* Yuck, ought to allocate a new codegen here. */
        
        initializedStaticFields_TO = new ArrayList<InitializedStaticField>();
        
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        // Until we resolve the directory hierarchy problem.
        //            cw.visit( V1_5, ACC_PUBLIC + ACC_SUPER+ ACC_FINAL,
        //                      classFile, null, NamingCzar.internalObject, new String[] { parent });
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER + ACC_FINAL,
                cnb.className, null, abstractSuperclass, superInterfaces);

        if (isSingletonObject) {
            
            initializedStaticFields_TO.add(new InitializedStaticField() {

                @Override
                public void forClinit(MethodVisitor imv) {
                    imv.visitTypeInsn(NEW, cnb.className);
                    imv.visitInsn(DUP);
                    imv.visitMethodInsn(INVOKESPECIAL, cnb.className,
                            "<init>", Naming.voidToVoid);
                    imv.visitFieldInsn(PUTSTATIC, cnb.className,
                            NamingCzar.SINGLETON_FIELD_NAME, cnb.classDesc);                    
                }

                @Override
                public String asmName() {
                    return NamingCzar.SINGLETON_FIELD_NAME;
                }

                @Override
                public String asmSignature() {
                    return cnb.classDesc;
                }
           
            });
            
            
            /* Used to pass splist to the static-parametered form
             * but it was always empty.  Tests work like that.
             * Bit of a WTF, keep an eye on this.
             * Repurpose splist (non-null) for the computation and
             * caching of RTTI, which also goes in a static.
             */
            addStaticVar(new VarCodeGen.StaticBinding(
                    classId, NodeFactory.makeTraitType(classId),
                    cnb.stemClassName,
                    NamingCzar.SINGLETON_FIELD_NAME, cnb.classDesc));
            
//            // Singleton; generate field in class to hold sole instance.
//            cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
//                          NamingCzar.SINGLETON_FIELD_NAME, cnb.classDesc,
//                          null /* for non-generic */, null /* instance has no value */);
        }
    	
        currentTraitObjectDecl = x;
        //create CodeGen for init method
        // has different set of parameter variables in scope
        CodeGen initCodeGen = initializeInitMethod(params);
        
        initCodeGen.initializedInstanceFields_O = new ArrayList<InitializedInstanceField>();
//        List<Binding> fieldsForEnv = new ArrayList<Binding>();
        initCodeGen.instanceFields = new InstanceFields();

        
        BATree<String, VarCodeGen> savedLexEnv = lexEnv.copy();
        
        // for each parameter
        // 1) add field to the class
        // 2) add field along with initializer to list of instance fields to put in the constructor
        // 3) add parameter to local scope of init method
        // 4) add field to scope for use in methods
        for (int i = 0; i < params.size(); i++) {
        	Param p = params.get(i);
        	
        	String paramName = p.getName().getText();
   		   	Type paramType = (Type)p.getIdType().unwrap();
   		   	String typeDesc = NamingCzar.jvmBoxedTypeDesc(paramType, thisApi());
   		   	Id id = NodeFactory.makeId(NodeUtil.getSpan(p.getName()), paramName);
   		   	VarCodeGen vcg = new VarCodeGen.FieldVar(id,
               paramType,
               cnb.className,
               paramName,
               typeDesc);
   		   	
   		   	//1) add field to class
   		   	cw.visitField(
               ACC_PUBLIC + ACC_FINAL,
               paramName, typeDesc,
               null /* for non-generic */, null /* instance has no value */);
   		   	
   		   	//2) add field with initializer as a refernce to the parameter
   		   	initCodeGen.instanceFields.put(vcg, ExprFactory.makeVarRef(id));
   		   	
   		   	//3) add param to scope for init method only
   		   	initCodeGen.addStaticVar(new VarCodeGen.ParamVar(id, paramType, initCodeGen));
   		   	
   		   	//4) add field to scope for method codegen
   		   	addStaticVar(vcg);
        }   
        
        // find declared fields in the list of decls and
        // 1) add field to the class
        // 2) add field along with initializer to list of instance fields to put in the constructor
        // 3) add field to scope for use in methods
        for (Decl d : header.getDecls()) {
            if (d instanceof VarDecl) {
         	   // TODO need to spot for "final" fields.  Right now we assume mutable, not final.
               final VarDecl vd = (VarDecl) d;
               final CodeGen cg = this;
         	   int numDecls = vd.getLhs().size();
               for(int i = 0; i < numDecls; i++) {
         		   LValue l = vd.getLhs().get(i);
         		   String fieldName = l.getName().getText();
         		   Type fieldType = (Type)l.getIdType().unwrap();
         		   String typeDesc = NamingCzar.jvmBoxedTypeDesc(fieldType, thisApi());
         		   Id id = NodeFactory.makeId(NodeUtil.getSpan(l.getName()), fieldName);
         		   VarCodeGen fieldVcg = new VarCodeGen.MutableFieldVar(id,
                         fieldType,
                         cnb.className,
                         fieldName,
                         typeDesc);
        		   
         		   //1) add field to class
         		   cw.visitField(
         	             ACC_PUBLIC,
         	             fieldName, typeDesc,
         	             null /* for non-generic */, null /* instance has no value */);
         		   
         		   //2) set up initializer
         		   if (vd.getInit().isNone()) sayWhat(vd, "no initializer for declared field(s)");
         		   Expr init = vd.getInit().unwrap();
         		   
         		   if (numDecls != 1 && init instanceof TupleExpr) {
         			   List<Expr> tupleExprs = ((TupleExpr)init).getExprs();
         			   if ( tupleExprs.size() != numDecls)
         				   sayWhat(vd, "incorrect initialization for declared fields tuple");
         			   init = tupleExprs.get(i);
         		   }
         		   initCodeGen.instanceFields.put(fieldVcg, init);
         		   
         		   //3) add field to scope for method codegen
         		   addStaticVar(fieldVcg);
                }
            }
         }
        
            
        debug("Dump overloaded method chaining for ", x);
        Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> overloads = dumpOverloadedMethodChaining(superInterfaces, false);
        if (OVERLOADED_METHODS)
            typeLevelOverloadedNamesAndSigs =
                generateTopLevelOverloads(thisApi(), overloads, typeAnalyzer, cw,
                        this, new OverloadSet.TraitOrObjectFactory(Opcodes.INVOKEVIRTUAL, cnb, this));
        debug("End of dump overloaded method chaining for ", x);
       
        debug("Process declarations for ", x);
        for (Decl d : header.getDecls()) {
            // This does not work yet.
            d.accept(this);
        }
        debug("End of processing declarations for ", x);
        
        initCodeGen.instanceInitForObject(abstractSuperclass);
        
	debug("Dump method chaining for ", x);
        dumpMethodChaining(superInterfaces, false);
        // dumpErasedMethodChaining(superInterfaces, false);
	debug("End of dump method chaining for ", x);
        
        /* RTTI stuff */
        mv = cw.visitCGMethod(Opcodes.ACC_PUBLIC, // acccess
                                          Naming.RTTI_GETTER, // name
                                          Naming.STATIC_PARAMETER_GETTER_SIG, // sig
                                          null, // generics sig?
                                          null); // exceptions
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, cnb.className, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);

        areturnEpilogue();
        
        emitRttiField(cnb);
        /* end RTTI stuff */
        
        lexEnv = savedLexEnv;

        optionalStaticsAndClassInitForTO(classId, cnb, isSingletonObject);
        
        if (cnb.isGeneric) {
            cw.dumpClass( cnb.fileName, xldata.setTraitObjectTag(Naming.OBJECT_GENERIC_TAG) );
        } else {
            cw.dumpClass( cnb.className );
        }
        cw = prev;
        initializedInstanceFields_O = null;
        initializedStaticFields_TO = null;
        currentTraitObjectDecl = null;
        
        traitOrObjectName = null;

        inAnObject = savedInAnObject;
        
        // Needed (above) to embed a reference to the Rtti information for this type.
        RttiClassAndInterface(x,cnb, xldata);
        debug("End forObjectDeclPrePass for ", x);
    }

    /**
     * @param cnb
     */
    public void emitRttiField(final ClassNameBundle cnb) {
        initializedStaticFields_TO.add(new InitializedStaticField() {

            @Override
            public void forClinit(MethodVisitor mv) {
                /*
                 * Need to initialize RTTI.
                 * If there are no static parameters, it is just
                 * the fortress RTTI type, .only.
                 * 
                 * If there ARE static parameters, need to generate a
                 * factory call.
                 * 
                 * NEW PLAN: do it in the rewriting phase during classloading.
                 * If this object is generic (has static args) it will be
                 * rewritten.  The reference, rewritten, will be to
                 * Someclassname LOX static args ROX $ RTTIc
                 * Therefore, look for:
                 * FieldInsn
                 * GETSTATIC
                 * rttiClassname ends in ROX $ RTTIc
                 * Extract stem
                 * Extract parameters
                 * Emit factory calls.
                 * 
                 */
                    String rttiClassName = Naming.stemClassToRTTIclass(cnb.className);
                    mv.visitFieldInsn(GETSTATIC, rttiClassName, Naming.RTTI_SINGLETON, Naming.RTTI_CONTAINER_DESC);
                    mv.visitFieldInsn(PUTSTATIC, cnb.className, Naming.RTTI_FIELD, Naming.RTTI_CONTAINER_DESC);
            }

            @Override
            public String asmName() {
                return Naming.RTTI_FIELD;
            }

            @Override
            public String asmSignature() {
                return Naming.RTTI_CONTAINER_DESC;
            }
            
        });
    }

    /**
     * @param classId
     * @param cnb
     * @param isSingletonObject
     */
   private void optionalStaticsAndClassInitForTO(Id classId, ClassNameBundle cnb, boolean isSingletonObject) {
        if (initializedStaticFields_TO.size() ==  0 && !isSingletonObject)
            return;

        MethodVisitor imv = cw.visitMethod(ACC_STATIC,
                                           "<clinit>",
                                           Naming.voidToVoid,
                                           null,
                                           null);
        for (InitializedStaticField isf : initializedStaticFields_TO) {
            isf.forClinit(imv);
            cw.visitField(
                    ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
                    isf.asmName(), isf.asmSignature(),
                    null /* for non-generic */, null /* instance has no value */);
            // DRC-WIP
        }
        
        imv.visitInsn(RETURN);
        imv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        imv.visitEnd();
    }
   
   /**
    * Creates a basic initialization method that just calls the superclass constructor
    * with no parameters
    * 
    * @param extendedJavaClass - superclass constructor to be called
    */
   private void basicInitMethod(String extendedJavaClass) {
	   CodeGen initMethodCg = initializeInitMethod(Collections.<Param>emptyList());
       initMethodCg.instanceInitForObject( extendedJavaClass );
   }

   /**
    * sets up a CodeGen in which to create a initialization method for an object
    * 
    * @param params - input parameters to init function.  If empty, don't need to
    * 					add a self parameter either (might not be able to if not for an object/trait)
    * 					Note that this is needed to put stack parameters into the method scope
    * 					for the inputs.
    * @return
    */
   private CodeGen initializeInitMethod(List<Param> params) {
	   CodeGen initCodeGen = new CodeGen(this);
	   String init_sig = NamingCzar.jvmSignatureFor(params, "V", thisApi());

	   initCodeGen.mv = initCodeGen.cw.visitCGMethod(ACC_PUBLIC,
	                            "<init>",
	                            init_sig,
	                            null,
	                            null);
	   if (params.size() > 0 ) initCodeGen.addSelf();
	   return initCodeGen;
   }
   
   /**
    * Writes the code for the init method for a fortress object.  Pulls
    * from instanceFields to find VarCodeGen for fields and associated
    * initialization expressions which it might run in parallel 
    * 
    * @param classId
    * @param cnb
    * @param isSingletonObject
    */
  private void instanceInitForObject(String superClass) {
  
      mv.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, superClass, "<init>", Naming.voidToVoid);
       
      	if (instanceFields != null && instanceFields.size() > 0) {
      		
      		List<VarCodeGen> vcgs = instanceFields.getFields();
      		List<Type> types = instanceFields.getFieldTypes();
      		List<Expr> initializers = instanceFields.getFieldInitializers();
      		Type type = types.size() == 1 ? types.get(0) : NodeFactory.makeTupleType(types);
      		
      		
      		if (pa.worthParallelizing(
      				ExprFactory.makeTupleExpr(
      						NodeFactory.internalSpan,
      						initializers))) {
                forExprsParallel(initializers, type, vcgs);
            } else {
                forExprsSerial(initializers, type, vcgs);
            }
      		
      	}
      
      	voidEpilogue();
   }

   private void emitErasedClassFor(ClassNameBundle cnb, TraitObjectDecl x) {
        String classFile = cnb.stemClassName;
        String classFileOuter = cnb.stemClassName;

        traitOrObjectName = classFile;
        //String classDesc = NamingCzar.internalToDesc(classFile);

        // need to adapt this code

        // need to include erased superinterfaces
        // need to

        String[] superInterfaces = new String[0];

        CodeGenClassWriter prev = cw;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION,
                  ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE,
                  classFile, null, NamingCzar.internalObject, superInterfaces);

        cw.dumpClass( classFileOuter );

        cw = prev;
    }

    // This returns a list rather than a set because the order matters;
    // we should guarantee that we choose a consistent order every time.
    private List<VarCodeGen> getFreeVars(Node n) {
       return getFreeVars(n, null);
    }
    private List<VarCodeGen> getFreeVars(Node n, BASet<VarType> free_type_vars) {
        BASet<IdOrOp> allFvs = fv.freeVars(n);
        List<VarCodeGen> vcgs = new ArrayList<VarCodeGen>();
        if (allFvs == null)
            throw sayWhat((ASTNode)n," null free variable information!");
        else {
            for (IdOrOp v : allFvs) {
                VarCodeGen vcg = getLocalVarOrNull(v);
                if (vcg != null) {
                    vcgs.add(vcg);
                    Type t = vcg.fortressType;
                    if (free_type_vars != null && t instanceof VarType) {
                        free_type_vars.add((VarType) t);
                    }
                }
            }
            return vcgs;
        }
    }

    // This returns a list of free vars plus the current BaseTask.
    private List<VarCodeGen> getTaskFreeVars(Node n) {
        List<VarCodeGen> vcgs = new ArrayList<VarCodeGen>();
        vcgs.add(new VarCodeGen.BaseTaskVar(this));
        vcgs.addAll(getFreeVars(n));
        return vcgs;
    }

    private BATree<String, VarCodeGen>
            createTaskLexEnvVariables(String taskClass, List<VarCodeGen> freeVars) {

        BATree<String, VarCodeGen> result =
            new BATree<String, VarCodeGen>(StringHashComparer.V);

        for (VarCodeGen v : freeVars) {
            String name = v.getName();
            
            if (v.isAMutableLocalVar())
                result.put(name, new VarCodeGen.MutableTaskVarCodeGen((VarCodeGen.LocalMutableVar) v,
                                                                      taskClass,
                                                                      thisApi(), cw, mv));
            else if (v.isAMutableTaskVar())
                result.put(name, new VarCodeGen.MutableTaskVarCodeGen((VarCodeGen.MutableTaskVarCodeGen) v,
                                                                      taskClass,
                                                                      thisApi(), cw, mv));
            else result.put(name, new VarCodeGen.TaskVarCodeGen(v, taskClass, thisApi(), cw));
        }
        return result;
    }

    private void generateTaskInit(String baseClass,
                                  String initDesc,
                                  List<VarCodeGen> freeVars) {

        mv = cw.visitCGMethod(ACC_PUBLIC, "<init>", initDesc, null, null);
        mv.visitCode();

        // Call task superclass constructor
        mv.visitVarInsn(ALOAD, mv.getThis());

        mv.visitMethodInsn(INVOKESTATIC, baseClass, "getCurrentTask", "()Lcom/sun/fortress/runtimeSystem/BaseTask;");
        mv.visitMethodInsn(INVOKESPECIAL, baseClass,
                           "<init>", "(Lcom/sun/fortress/runtimeSystem/BaseTask;)V");

        // mv.visitVarInsn(ALOAD, mv.getThis());

        // Stash away free variables Warning: freeVars contains
        // VarCodeGen objects from the parent context, we must look
        // these up again in the child context or we'll get incorrect
        // code (or more usually the compiler will complain).
        int varIndex = 1;
        for (VarCodeGen v0 : freeVars) {
            VarCodeGen v = lexEnv.get(v0.getName());
            mv.visitVarInsn(ALOAD, varIndex++);
            v.assignHandle(mv);
        }

        voidEpilogue();
    }

    private void generateFnExprInit(String baseClass,
                                  String initDesc,
                                  List<VarCodeGen> freeVars) {

        mv = cw.visitCGMethod(ACC_PUBLIC, "<init>", initDesc, null, null);
        mv.visitCode();

        // Call task superclass constructor
        mv.visitVarInsn(ALOAD, mv.getThis());

        //        mv.visitMethodInsn(INVOKESTATIC, baseClass, "getCurrentTask", "()Lcom/sun/fortress/runtimeSystem/BaseTask;");
        //        mv.visitMethodInsn(INVOKESPECIAL, baseClass,
        //                           "<init>", "(Lcom/sun/fortress/runtimeSystem/BaseTask;)V");

        mv.visitMethodInsn(INVOKESPECIAL, baseClass, "<init>", "()V");
        // mv.visitVarInsn(ALOAD, mv.getThis());

        // Stash away free variables Warning: freeVars contains
        // VarCodeGen objects from the parent context, we must look
        // these up again in the child context or we'll get incorrect
        // code (or more usually the compiler will complain).
        int varIndex = 1;
        for (VarCodeGen v0 : freeVars) {
            VarCodeGen v = lexEnv.get(v0.getName());
            mv.visitVarInsn(ALOAD, varIndex++);
            v.assignHandle(mv);
        }

        voidEpilogue();
    }



    private void generateTaskCompute(String className, Expr x, String result) {
        mv = cw.visitCGMethod(ACC_PUBLIC + ACC_FINAL,
                                  "compute", "()V", null, null);
        mv.visitCode();

        // Debugging CHF
        //        mv.visitLdcInsn("Look Here: " + className + ":" + x);
        //
        //        mv.visitMethodInsn(INVOKESTATIC, NamingCzar.internalFortressString, NamingCzar.make,
        //                     NamingCzar.makeMethodDesc(NamingCzar.descString, NamingCzar.descFortressString));
        //        mv.visitMethodInsn(INVOKESTATIC, "native/com/sun/fortress/nativeHelpers/simplePrintln", "nativePrintln",
        //                   NamingCzar.makeMethodDesc(NamingCzar.descFortressString, NamingCzar.descFortressVoid));
        //

        mv.visitVarInsn(ALOAD, mv.getThis());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, 
                           "com/sun/fortress/runtimeSystem/BaseTask",
                           "setTask", 
                           "(Lcom/sun/fortress/runtimeSystem/BaseTask;)V");

        x.accept(this);
        mv.visitFieldInsn(PUTFIELD, className, "result", result);
        voidEpilogue();
    }

    // I'm just a stub.  Someday I'll have a body that updates the changed local variables.
    private BATree<String, VarCodeGen> restoreFromTaskLexEnv(BATree<String,VarCodeGen> old, BATree<String,VarCodeGen> task) {
        return task;
    }


    public String taskConstructorDesc(List<VarCodeGen> freeVars) {
        // And their types
        String result = "(";

        for (VarCodeGen v : freeVars) {
            if (v.isAMutableLocalVar()  || v.isAMutableTaskVar())
                result = result + NamingCzar.descFortressMutableFValueInternal;
            else result = result + NamingCzar.jvmBoxedTypeDesc(v.fortressType, thisApi());
        }
        return result + ")V";
    }

    // This sets up the parallel task construct.
    // Caveat: We create separate taskClasses for every task
    public String delegate(Expr x, String result, String init, List<VarCodeGen> freeVars) {

        String className = NamingCzar.gensymTaskName(packageAndClassName);

        debug("delegate creating class ", className, " node = ", x,
              " constructor type = ", init, " result type = ", result);

        // Create a new environment, and codegen task class in it.
        CodeGen cg = new CodeGen(this);
        cg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        cg.cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);

        cg.lexEnv = cg.createTaskLexEnvVariables(className, freeVars);
        // WARNING: result may need mangling / NamingCzar-ing.
        cg.cw.visitField(ACC_PUBLIC, "result", result, null, null);

        cg.cw.visit(InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER + ACC_FINAL,
                    className, null, NamingCzar.fortressBaseTask, null);

        cg.generateTaskInit(NamingCzar.fortressBaseTask, init, freeVars);

        cg.generateTaskCompute(className, x, result);

        cg.cw.dumpClass(className);

        this.lexEnv = restoreFromTaskLexEnv(cg.lexEnv, this.lexEnv);
        return className;
    }

//    public void constructTaskWithFreeVars(String cname, List<VarCodeGen> freeVars, String sig) {
//            mv.visitTypeInsn(NEW, cname);
//            mv.visitInsn(DUP);
//
//            for (VarCodeGen v : freeVars) {
//                if (!(v.isAMutableLocalVar()  || v.isAMutableTaskVar()))
//                    conditionallyCastParameter((Expr)null, v.fortressType);
//                v.pushHandle(mv);
//            }
//            mv.visitMethodInsn(INVOKESPECIAL, cname, "<init>", sig);
//    }

    public void constructWithFreeVars(String cname, List<VarCodeGen> freeVars, String sig) {
            mv.visitTypeInsn(NEW, cname);
            mv.visitInsn(DUP);
            // Push the free variables in order.
            for (VarCodeGen v : freeVars) {
                v.pushHandle(mv);
                if (!(v.isAMutableLocalVar()  || v.isAMutableTaskVar()))
                    conditionallyCastParameter((Expr)null, v.fortressType);
            }
            mv.visitMethodInsn(INVOKESPECIAL, cname, "<init>", sig);
    }

    // Evaluate args in parallel.  (Why is profitability given at a
    // level where we can't ask the qustion here?)
    // Leave the results (in the order given) on the stack when vcgs==null;
    // otherwise use the provided vcgs to bind corresponding values.
    public void forExprsParallel(List<? extends Expr> args, Type domain_type, List<VarCodeGen> vcgs) {
        boolean clearStackAtEnd = false;
        genParallelExprs(args, domain_type, vcgs, clearStackAtEnd);
    }

    // Evaluate args serially, from left to right.
    // Leave the results (in the order given) on the stack when vcgs==null;
    // otherwise use the provided vcgs to bind corresponding values.
    public void forExprsSerial(List<? extends Expr> args, Type domain_type, List<VarCodeGen> vcgs) {
        
        List<Type> domain_types;
        if (args.size() != 1 && domain_type instanceof TupleType) {
            TupleType tdt = (TupleType) domain_type;
            domain_types = tdt.getElements();
        } else {
            domain_types = Collections.singletonList(domain_type);
        }
        if (vcgs == null) {
            int i = 0;
            for (Expr arg : args) {
                arg.accept(this);
                conditionallyCastParameter(arg, domain_types.get(i++));
            }
        } else {
            int n = args.size();
            if (args.size() != vcgs.size()) {
                throw sayWhat(args.get(0), "Internal error: number of args does not match number of consumers.");
            }
            for (int i = 0; i < n; i++) {
                VarCodeGen vcg = vcgs.get(i);
                args.get(i).accept(this);
                conditionallyCastParameter(Wrapper.make(vcg.fortressType), domain_types.get(i));
                vcg.assignValue(mv);
            }
        }
    }

    public void forOpExpr(OpExpr x) {
        debug("forOpExpr ", x, " op = ", x.getOp(),
                     " of class ", x.getOp().getClass(),  " args = ", x.getArgs());
        FunctionalRef op = x.getOp();
        List<Expr> args = x.getArgs();

        Type domain_type = ((ArrowType)(op.getInfo().getExprType().unwrap())).getDomain();
        
        evaluateSubExprsAppropriately(x, domain_type, args);

        op.accept(this);
    }

    /**
     * @param x
     * @param args
     */
    private void evaluateSubExprsAppropriately(ASTNode x, Type domain_type, List<Expr> args) {
        if (pa.worthParallelizing(x)) {
            forExprsParallel(args, domain_type, null);
        } else {
            List<Type> domain_types;
            if (args.size() != 1 && domain_type instanceof TupleType) {
                TupleType tdt = (TupleType) domain_type;
                domain_types = tdt.getElements();
            } else {
                domain_types = Collections.singletonList(domain_type);
            }
            int i = 0;
            for (Expr arg : args) {
                arg.accept(this);
                conditionallyCastParameter(arg, domain_types.get(i));
                i++;
            }
        }
    }

    public void forOpRef(OpRef x) {
        forFunctionalRef(x);
   }

    public void forCharLiteralExpr(CharLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FJavaString and push it on the stack.
        debug("forCharLiteral ", x);
        addLineNumberInfo(x);
        mv.visitLdcInsn(x.getCharVal());
        addLineNumberInfo(x);
        mv.visitMethodInsn(INVOKESTATIC, NamingCzar.internalFortressCharacter, NamingCzar.make,
                           Naming.makeMethodDesc(NamingCzar.descCharacter, NamingCzar.descFortressCharacter));
    }
    
    public void forStringLiteralExpr(StringLiteralExpr x) {
        // This is cheating, but the best we can do for now.
        // We make a FJavaString and push it on the stack.
        debug("forStringLiteral ", x);
        addLineNumberInfo(x);
        mv.visitLdcInsn(x.getText());
        addLineNumberInfo(x);
        mv.visitMethodInsn(INVOKESTATIC, NamingCzar.internalFortressJavaString, NamingCzar.make,
                           Naming.makeMethodDesc(NamingCzar.descString, NamingCzar.descFortressJavaString));
    }

    public void forSubscriptExpr(SubscriptExpr x) {
        // TODO: FIX!!  Only works for string subscripting.  Why does this
        // AST node still exist at all at this point in compilation??
        // It ought to be turned into a MethodInvocation.
        // JWM 9/4/09

        // This is more true than ever now that we have ZZ32Vector and StringVector,
        // but I couldn't figure out where/how to add it to the proper desugaring phase.
	if (x == x) throw new Error("Should not be a subscriptExpr here in codegen!");

        debug("forSubscriptExpr ", x);
        Expr obj = x.getObj();
        List<Expr> subs = x.getSubs();
        Option<Op> maybe_op = x.getOp();
        List<StaticArg> staticArgs = x.getStaticArgs();
        boolean canCompile = staticArgs.isEmpty() && maybe_op.isSome() && (obj instanceof VarRef);
        if (!canCompile) throw sayWhat(x);
        Op op = maybe_op.unwrap();
        VarRef var = (VarRef) obj;
        Id id = var.getVarId();

        debug("ForSubscriptExpr  ", x, "obj = ", obj,
              " subs = ", subs, " op = ", op, " static args = ", staticArgs,
              " varRef = ", NamingCzar.idOrOpToString(id));

        
         var.accept(this);

         for (Expr e : subs) {
             debug("calling accept on ", e);
             e.accept(this);
         }
         addLineNumberInfo(x);

         VarCodeGen vcg = getLocalVarOrNull(id);
         if (vcg == null)
             throw new RuntimeException("Bad VCG");

         String ZZ32Sig = "Lcom/sun/fortress/compiler/runtimeValues/FZZ32;";
         String args = "(";

         for (int i = 0; i < subs.size(); i++)
             args = args + ZZ32Sig;

         args = args + ")";

         if (vcg.fortressType.toString().equals("StringVector"))
             mv.visitMethodInsn(INVOKEVIRTUAL,
                                NamingCzar.descToInternal(NamingCzar.jvmTypeDesc(vcg.fortressType, thisApi())),
                                NamingCzar.idOrOpToString(op),
                                args + "Lcom/sun/fortress/compiler/runtimeValues/FJavaString;");
         else if (vcg.fortressType.toString().equals("ZZ32Vector"))
             mv.visitMethodInsn(INVOKEVIRTUAL,
                            NamingCzar.descToInternal(NamingCzar.jvmTypeDesc(vcg.fortressType, thisApi())),
                            NamingCzar.idOrOpToString(op),
                                args + "Lcom/sun/fortress/compiler/runtimeValues/FZZ32;");

         else throw new CompilerError("Unknow Vector type: " + vcg.fortressType);
    }

    public void forTraitDecl(TraitDecl x) {
        debug("forTraitDecl", x);
        TraitTypeHeader header = x.getHeader();
        Id traitname = (Id) header.getName();
        String traitname_string = traitname.getText();
        if (traitname_string.equals("Any") &&
                traitname.getApiName().isNone()) {
            String api_stringname = thisApi().getText();
            if (api_stringname.equals("AnyType")) 
                return;
        }
        TraitTypeHeader original_header = x.getHeader();
        List<TraitTypeWhere> extendsC = header.getExtendsClause();
        boolean canCompile =
            // NOTE: Presence of excludes or comprises clauses should not
            // affect code generation once type checking is complete.
            header.getWhereClause().isNone() &&   // no where clause
            header.getThrowsClause().isNone() &&  // no throws clause
            header.getContract().isNone() &&      // no contract
            Modifiers.TraitMod.containsAll(header.getMods());
        debug("forTraitDecl", x,
                    " decls = ", header.getDecls(), " extends = ", extendsC);
        if ( !canCompile ) throw sayWhat(x);

        // Map<String, String> xlation = new HashMap<String, String>();
        List<String> splist = new ArrayList<String>();
        List<StaticParam> original_static_params = header.getStaticParams();
        
        Naming.XlationData xldata = 
            xlationData(Naming.TRAIT_GENERIC_TAG);

//       First let's do the interface class
//        String classFile = NamingCzar.makeInnerClassName(packageAndClassName,
//                                                         NodeUtil.getName(x).getText());

        /*
         * This will want refactoring into NamingCzar sooner or later.
         * I decided that the least-confusion convention for implementing
         * classes for generic traits was to use the Generic[\parameters\]$whatever
         * convention.  This may require enhancements to the mangling code, but
         * once that is done it will cause least-confusion for everyone else
         * later.
         */
        Id classId = NodeUtil.getName(x);
        
        ClassNameBundle cnb = new_ClassNameBundle(classId, original_static_params, xldata);
        
        String erasedSuperI = EMIT_ERASED_GENERICS && cnb.isGeneric ?
                cnb.stemClassName
                : "";
                
        if (EMIT_ERASED_GENERICS && cnb.isGeneric) {
           emitErasedClassFor(cnb, (TraitObjectDecl) x);
        }

        inATrait = true;
        currentTraitObjectDecl = x;

        springBoardClass = cnb.className + NamingCzar.springBoard;
        String springBoardClassOuter = cnb.fileName + NamingCzar.springBoard;

        String abstractSuperclass;
        traitOrObjectName = cnb.className;
        String[] superInterfaces = NamingCzar.extendsClauseToInterfaces(
                extendsC, component.getName(), erasedSuperI);
        if (cnb.className.equals("fortress/AnyType$Any")) {
            superInterfaces = new String[0];
            abstractSuperclass = NamingCzar.FValueType;
        } else {
            abstractSuperclass = superInterfaces[0] + NamingCzar.springBoard;
        }
        CodeGenClassWriter prev = cw;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, prev);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION,
                  ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE,
                  cnb.className, null, NamingCzar.internalObject, superInterfaces);
        
        dumpTraitMethodSigs(header.getDecls());

        initializedStaticFields_TO = new ArrayList<InitializedStaticField>();
        //no instance fields for traits
        
        emitRttiField(cnb);
        
        optionalStaticsAndClassInitForTO(classId, cnb, false);

        if (cnb.isGeneric ) {
            cw.dumpClass( cnb.fileName, xldata );
        } else {
            cw.dumpClass( cnb.fileName );
        }

        // Doing this to get an extended type analyzer for overloaded method chaining.
        CodeGen newcg = new CodeGen(this,
                typeAnalyzer.extendJ(header.getStaticParams(), header.getWhereClause()));

        // Now let's do the springboard inner class that implements this interface.
        newcg.cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, prev);
        newcg.cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        // Springboard *must* be abstract if any methods / fields are abstract!
        // In general Springboard must not be directly instantiable.
        newcg.cw.visit(InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC | ACC_ABSTRACT, springBoardClass,
                 null, abstractSuperclass, new String[] { cnb.className } );
        
        debug("Start writing springboard class ",
              springBoardClass);

        // simple init method
        newcg.basicInitMethod(abstractSuperclass);
        
        debug("Finished init method ", springBoardClass);

        newcg.initializedStaticFields_TO = new ArrayList<InitializedStaticField>();

        // Overloads will tell us which methods need forwarding,
        // but they don't yet.
        Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> overloads =
            newcg.dumpOverloadedMethodChaining(superInterfaces, true);
        
        if (OVERLOADED_METHODS)
            newcg.typeLevelOverloadedNamesAndSigs =
                generateTopLevelOverloads(thisApi(), overloads, newcg.typeAnalyzer, newcg.cw, newcg,
                        new OverloadSet.TraitOrObjectFactory(Opcodes.INVOKEINTERFACE, cnb, newcg));
                
        newcg.dumpTraitDecls(header.getDecls());
        newcg.dumpMethodChaining(superInterfaces, true);
        // dumpErasedMethodChaining(superInterfaces, true);
                
        newcg.optionalStaticsAndClassInitForTO(classId, cnb, false);
 
        debug("Finished dumpDecls ", springBoardClass);
        if (cnb.isGeneric ) {
            /* Not dead sure this is right
             * Reasoning is that if the springboard IS ever referenced in a
             * generic context (how????) it is in fact a class, so it needs
             * INVOKEVIRTUAL.
             */
            newcg.cw.dumpClass( springBoardClassOuter, xldata.setTraitObjectTag(Naming.OBJECT_GENERIC_TAG) );
        } else {
            newcg.cw.dumpClass( springBoardClassOuter );
        }
        
        // Now lets dump out the functional methods at top level.
        cw = prev;

        emittingFunctionalMethodWrappers = true;
        // Have to use the origial header to get the signatures right.
        dumpTraitDecls(original_header.getDecls());
        
        emitOrphanedFunctionalMethods(traitname);
       
        emittingFunctionalMethodWrappers = false;

        debug("Finished dumpDecls for parent");
        inATrait = false;
        currentTraitObjectDecl = null;
        traitOrObjectName = null;
        springBoardClass = null;
        initializedStaticFields_TO = null;
        
        RttiClassAndInterface(x,cnb, xldata);
    }
    
    private void RttiClassAndInterface(TraitObjectDecl tod,
                                       ClassNameBundle cnb,
                                       Naming.XlationData xldata) {
        
        TraitTypeHeader header = tod.getHeader();

        IdOrOpOrAnonymousName name = header.getName();
        List<StaticParam> sparams = header.getStaticParams();
        
        HashMap<Id, TraitIndex> transitive_extends =
            STypesUtil.allSupertraits(tod, typeAnalyzer);

        HashMap<String, Tuple2<TraitIndex, List<StaticArg>>> transitive_extends_opr_tagged =
            oprTagSupertraitsAndArgs(STypesUtil.allSupertraitsAndStaticArgs(tod, typeAnalyzer));
        
        HashMap<String, Tuple2<TraitIndex, List<StaticArg>>> direct_extends_opr_tagged =
            oprTagSupertraitsAndArgs(STypesUtil.immediateSupertraitsAndStaticArgs(tod, typeAnalyzer));
        
        
        HashMap<Id, TraitIndex> direct_extends =
            STypesUtil.immediateSupertraits(tod, typeAnalyzer);
        
        HashMap<Id, List<StaticArg>> direct_extends_args =
            STypesUtil.immediateSupertraitsStaticArgs(tod, typeAnalyzer);
        
        int d_e_size = direct_extends.size();

        CodeGenClassWriter prev = cw;
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, prev);

        /*
         * x$RTTIi
         * extends y$RTTIi for each y in extend_s

         * for each static parameter of x, declares method "asX"#number

         * x$RTTIc
         * implements x$RTTIi

         * fields
         * for each y in extend_s, one field, initially null, type y$RttiClass
         * for each z in static parameters, one field, init in constructor

         * constructor (init method)
         * parameters for static parameters, if any.
         * 
         * if no static parameters, then a static field initialized to the
         * single value of the type.
         * if static parameters, then a static field initialized to a map of
         * some sort, plus a factory receiving static parameters, that checks
         * the map, and allocates the type as necessary.

         * lazy_init method
         * for each y in extend_s, field = new y$RTTIc(type parameters).
         * type parameters need thinking about how we put them together.
         * If extends A[B[T]], should be new A$RTTIc(new B$RTTIc(T))
         * Seems like a factory would be appropriate, to avoid senseless
         * duplication of type parameters.

         * methods
         * for each static parameter #N of each type T in transitive_extends,
         * there needs to be a method as"T"#N.  For all non-self types, the
         * method will check lazy_init, and then delegate to the first type
         * in extend_s that has T in its own transitive_extends.  For T=self,
         * return the value of the appropriate static parameter.

         */
        
        /*
         * x$RTTIi
         * extends y$RTTIi for each y in extend_s
         */
        
        Naming.XlationData original_xldata = xldata;
        List<String> opr_params = oprsFromKindParamList(xldata.staticParameterKindNamePairs());
        xldata = opr_params.size() == 0 ? null : xlationData(Naming.RTTI_GENERIC_TAG);
        for (String opr_param : opr_params)
            xldata.addKindAndNameToStaticParams(Naming.XL_OPR, opr_param);
        String stemClassName =
            Naming.oprArgAnnotatedRTTI(cnb.stemClassName,
                    opr_params);
        
        
        String rttiInterfaceName = Naming.stemInterfaceToRTTIinterface(stemClassName);
        String rttiInterfaceFileName =
            opr_params.size() == 0 ? rttiInterfaceName : 
            Naming.stemInterfaceToRTTIinterface(Naming.fileForOprTaggedGeneric(cnb.stemClassName));
        
        String[] superInterfaces = new String[d_e_size];
        
        Id[] direct_extends_keys = new Id[d_e_size]; // will use in lazyInit
        {
        int i = 0;
        for (Id extendee : direct_extends.keySet()) {
            List<StaticArg> ti_args = direct_extends_args.get(extendee);
            String extendeeIlk = oprTaggedGenericStemNameSA(extendee, ti_args);

//            String extendeeIlk =
//                NamingCzar.jvmClassForToplevelTypeDecl(extendee,"",
//                        packageAndClassName);
            direct_extends_keys[i] = extendee;
            superInterfaces[i++] = Naming.stemInterfaceToRTTIinterface(extendeeIlk);
         }
        }
        
        cw.visitSource(NodeUtil.getSpan(tod).begin.getFileName(), null);
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION,
                  ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE,
                  rttiInterfaceName, null,
                  NamingCzar.internalObject, superInterfaces);
        
        /*
         * for each static parameter of x, declares method "asX"#number
         */
        {
            int i = Naming.STATIC_PARAMETER_ORIGIN;
            for (StaticParam sp : sparams) {
                if (! (sp.getKind() instanceof KindOp) ) {
                String method_name =
                    Naming.staticParameterGetterName(stemClassName, i);
                mv = cw.visitCGMethod(
                        ACC_ABSTRACT + ACC_PUBLIC, method_name,
                        Naming.STATIC_PARAMETER_GETTER_SIG, null, null);
                mv.visitMaxs(Naming.ignoredMaxsParameter,
                        Naming.ignoredMaxsParameter);
                mv.visitEnd();
                i++;
                }
            }
        }

        cw.dumpClass( rttiInterfaceFileName, xldata);
        
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, prev);

        /*
         * x$RTTIc
         * implements x$RTTIi
         */

        superInterfaces = new String[1];
        superInterfaces[0] = rttiInterfaceName;
        String rttiClassName =  Naming.stemClassToRTTIclass(stemClassName);
        String rttiClassFileName =  opr_params.size() == 0 ? rttiClassName : 
            Naming.stemClassToRTTIclass(Naming.fileForOprTaggedGeneric(cnb.stemClassName));

        cw.visitSource(NodeUtil.getSpan(tod).begin.getFileName(), null);
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION,
                  ACC_PUBLIC, rttiClassName, null,
                  Naming.RTTI_CONTAINER_TYPE, superInterfaces);
        
        /*
         * fields
         * for each y in extend_s, one field, initially null, type y$RttiClass
         * for each z in static parameters, one field, init in constructor
         */
        
        for (Id extendee : direct_extends.keySet()) {
            
            // This yutch is repeated below in lazyInit; needs cleanup.
            List<StaticArg> ti_args = direct_extends_args.get(extendee);
            String extendeeIlk = oprTaggedGenericStemNameSA(extendee, ti_args);

            // note fields are volatile because of double-checked locking below
            cw.visitField(ACC_PRIVATE + ACC_VOLATILE,
                    extendeeIlk, Naming.RTTI_CONTAINER_DESC, null, null);
         }
 
        // Fields and Getters for static parameters
        int count_non_opr_sparams;
        {
            int i = Naming.STATIC_PARAMETER_ORIGIN;
            for (StaticParam sp : sparams) {
                if (! (sp.getKind() instanceof KindOp) ) {
                    String spn = sp.getName().getText();
                    String stem_name = stemClassName;
                    InstantiatingClassloader.fieldAndGetterForStaticParameter(cw, stem_name, spn, i);           
                    i++;
                }
            }
            count_non_opr_sparams = i - Naming.STATIC_PARAMETER_ORIGIN;
        }

        /*
         * constructor (init method)
         * parameters for static parameters, if any.
         */
        final int sparams_size = count_non_opr_sparams;
        {
            // Variant of this code in InstantiatingClassloader
            String init_sig =
                InstantiatingClassloader.jvmSignatureForOnePlusNTypes("java/lang/Class", sparams_size, Naming.RTTI_CONTAINER_TYPE, "V");
            mv = cw.visitCGMethod(ACC_PUBLIC, "<init>", init_sig, null, null);
            mv.visitCode();
            
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitVarInsn(ALOAD, 1); // class
            mv.visitMethodInsn(INVOKESPECIAL, Naming.RTTI_CONTAINER_TYPE, "<init>", "(Ljava/lang/Class;)V");
            
            int pno = 2; // 1 is java class
            for (StaticParam sp : sparams) {
                if (! (sp.getKind() instanceof KindOp) ) {
                    String spn = sp.getName().getText();
                    // not yet this;  sp.getKind();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ALOAD, pno);
                    mv.visitFieldInsn(PUTFIELD, rttiClassName, spn,
                            Naming.RTTI_CONTAINER_DESC);
                    pno++;
                }
            }
            
            voidEpilogue();
        }
        
        cw.visitField(ACC_PRIVATE + ACC_VOLATILE,
                "initFlag", "Ljava/lang/Object;", null, null);
        
        /*
         * static singleton or static map + factory.
         */
        if (sparams_size == 0) {
            // static, initialized to single instance of self
            cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
                    Naming.RTTI_SINGLETON, Naming.RTTI_CONTAINER_DESC, null, null);
            
            mv = cw.visitCGMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mv.visitCode();
            // new
            mv.visitTypeInsn(NEW, rttiClassName);
            // init
            mv.visitInsn(DUP);
            mv.visitLdcInsn(org.objectweb.asm.Type.getType(cnb.classDesc));
            mv.visitMethodInsn(INVOKESPECIAL, rttiClassName, "<init>", "(Ljava/lang/Class;)V");
            // store
            mv.visitFieldInsn(PUTSTATIC, rttiClassName,
                    Naming.RTTI_SINGLETON, Naming.RTTI_CONTAINER_DESC);                

            voidEpilogue();
        } else {
            InstantiatingClassloader.
            emitDictionaryAndFactoryForGenericRTTIclass(cw, rttiClassName,
                    sparams_size, original_xldata);           
        }
        
        /*
         * lazy_init method
         * for each y in extend_s, field = new y$RTTIc(type parameters).
         * type parameters need thinking about how we put them together.
         * If extends A[B[T]], should be new A$RTTIc(new B$RTTIc(T))
         * Seems like a factory would be appropriate, to avoid senseless
         * duplication of type parameters.
         */
        if (d_e_size > 0)
        {
            mv = cw.visitCGMethod(ACC_PUBLIC, "lazyInit", "()V", null, null);
            mv.visitCode();

            Label do_ret = new Label();
            Label do_monitor_ret = new Label();
            
            
            //debugging - catch exceptions thrown inside the monitor
            Label debugTryStart = new Label();
            Label debugTryEnd = new Label();
            Label debugHandler = new Label();
            mv.visitTryCatchBlock(debugTryStart, debugTryEnd, debugHandler, "java/lang/Throwable");
            mv.visitLabel(debugTryStart);
            //end debugging - more below    
            
            // Double-checked locking.
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, rttiClassName, "initFlag", "Ljava/lang/Object;");
            mv.visitJumpInsn(IFNONNULL, do_ret);
            
            mv.visitVarInsn(ALOAD, 0);
            mv.visitInsn(MONITORENTER);

            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, rttiClassName, "initFlag", "Ljava/lang/Object;");
            mv.visitJumpInsn(IFNONNULL, do_monitor_ret);
           
            HashSet<String> spns = new HashSet<String>();
            for (StaticParam sp : sparams) {
                IdOrOp spn = sp.getName();
                String s = spn.getText();
                spns.add(s);
            }
            
            // Do the initialization.
            // Push all the type parameters
           for (Id extendee : direct_extends_keys) {
                TraitIndex ti = direct_extends.get(extendee);
                List<StaticArg> ti_args = direct_extends_args.get(extendee);
                String extendeeIlk = oprTaggedGenericStemNameSA(extendee, ti_args);

                mv.visitVarInsn(ALOAD, 0); // this ptr for store.
                // invoke factory method for value to store.
                generateTypeReference(mv, rttiClassName, extendee, ti_args, spns);
                mv.visitFieldInsn(PUTFIELD, rttiClassName, extendeeIlk, Naming.RTTI_CONTAINER_DESC);
            }
           
           // Mark as inited.  Just store a self-pointer and be done with it.
           mv.visitVarInsn(ALOAD, 0);
           mv.visitVarInsn(ALOAD, 0);
           mv.visitFieldInsn(PUTFIELD, rttiClassName, "initFlag", "Ljava/lang/Object;");
           
           //more debugging
           mv.visitLabel(debugTryEnd);
           Label debugPassCatch = new Label();
           mv.visitJumpInsn(GOTO, debugPassCatch);
           mv.visitLabel(debugHandler);
           mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
           mv.visitVarInsn(ASTORE, 0);
           Label debugInternal = new Label();
           mv.visitLabel(debugInternal);
           mv.visitVarInsn(ALOAD, 0);
           mv.visitMethodInsn(INVOKESTATIC, "com/sun/fortress/runtimeSystem/InstantiatingClassloader", "eep", "(Ljava/lang/Throwable;)V");
           mv.visitLabel(debugPassCatch);
           mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
           //end more debugging
   
           mv.visitLabel(do_monitor_ret);

           mv.visitVarInsn(ALOAD, 0);
           mv.visitInsn(MONITOREXIT);
           
           mv.visitLabel(do_ret);
           voidEpilogue();
        }
    
        /*
         * methods
         * for each static parameter #N of each type T in transitive_extends,
         * there needs to be a method as"T"#N.  For all non-self types, the
         * method will check lazy_init, and then delegate to the first type
         * in extend_s that has T in its own transitive_extends.  For T=self,
         * return the value of the appropriate static parameter.
         */
        
        /* 
         * First map extends to what they transitively extend for delegation
         * The keys of this map are the members of the extends list;
         * the values, for each car, are those things that further
         * transitively extend that extender.  Each static-parameter-probing
         * method for the type (T) whose RTTI is being generated, will either
         * return a field (static parameter) of T, or will delegate to one
         * of the types extending T.
         */
        HashMap<String, Map<String, Tuple2<TraitIndex,List<StaticArg>>>> transitive_extends_from_extends = 
            new HashMap<String, Map<String, Tuple2<TraitIndex,List<StaticArg>>>>();
        
        for (Map.Entry <Id, TraitIndex> entry : direct_extends.entrySet()) {
            Id te_id = entry.getKey();
            TraitIndex te_ti = entry.getValue();
            HashMap<Id, Tuple2<TraitIndex,List<StaticArg> > >extends_transitive_extends_tmp =
                STypesUtil.allSupertraitsAndStaticArgs(te_ti.ast(), typeAnalyzer);
            
            Tuple2<TraitIndex,List<StaticArg>> te_pair  =
                new Tuple2<TraitIndex,List<StaticArg>>(te_ti, direct_extends_args.get(te_id));
            extends_transitive_extends_tmp.put(te_id, te_pair); // put self in set.
            
            HashMap<String, Tuple2<TraitIndex, List<StaticArg>>> extends_transitive_extends =
                oprTagSupertraitsAndArgs(extends_transitive_extends_tmp);
            
            String te_id_stem = oprTaggedGenericStemNameSA(te_id,
                    direct_extends_args.get(te_id));
            
            transitive_extends_from_extends.put(te_id_stem, extends_transitive_extends);
        }
        
        // Future opt: sort by transitive_extends, use largest as class ancestor
        
        // For each type in extends list, emit forwarding functions (delegates)
        // remove traits from transitive_extends as they are dealt with.
                
        for (Map.Entry <Id, TraitIndex> entry : direct_extends.entrySet()) {
            if (transitive_extends.size() == 0)
                break;
            Id de_id = entry.getKey();
            String de_id_stem = oprTaggedGenericStemNameSA(de_id,
                    direct_extends_args.get(de_id));
            TraitIndex de_ti = entry.getValue();
            
            // iterate over all traits transitively extended by delegate (de_)            
            Map<String, Tuple2<TraitIndex,List<StaticArg>>> extends_transitive_extends =
                transitive_extends_from_extends.get(de_id_stem);
            Set<Map.Entry<String, Tuple2<TraitIndex,List<StaticArg>>>> entryset =
                extends_transitive_extends.entrySet();
            
            for (Map.Entry<String, Tuple2<TraitIndex,List<StaticArg>>> extends_entry :
                entryset) {
                    
                // delegate for extended te_id, if not already done.
                String te_id = extends_entry.getKey();
                if (! transitive_extends_opr_tagged.containsKey(te_id))
                    continue; // already done.
                else
                    transitive_extends_opr_tagged.remove(te_id);

                Tuple2<TraitIndex,List<StaticArg>> te_ti = extends_entry.getValue();
                List<StaticParam> te_sp = te_ti._1.staticParameters();
                
                if (te_sp.size() == 0)
                    continue;  // no static parameters to delegate for.
                
                String te_stem = te_id;
                // emit delegates here
                // asX#number
                int i = Naming.STATIC_PARAMETER_ORIGIN;
                for (StaticParam a_sparam : te_sp) {
                    StaticParamKind spk = a_sparam.getKind();
                    if (spk instanceof KindOp)
                        continue;
                    String method_name =
                       Naming.staticParameterGetterName(te_stem, i);
                    
                    mv = cw.visitCGMethod(ACC_PUBLIC,
                            method_name,
                            Naming.STATIC_PARAMETER_GETTER_SIG, null, null);
                    mv.visitCode();
                    
                    // lazyInit();
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitMethodInsn(INVOKEVIRTUAL, rttiClassName, "lazyInit", "()V");
                    
                    String delegate_id = null;
                    // invoke delegate -- work in progress here
                    if(direct_extends_opr_tagged.containsKey(te_id) || te_id.equals(name)) {
                       delegate_id = te_id;
                    } else { //need to find a direct extend that extends the trait we're looking for
                        for (String direct_extend_id :
                            direct_extends_opr_tagged.keySet()) {
                            if (transitive_extends_from_extends.get(direct_extend_id).containsKey(te_id)) {
                                delegate_id = direct_extend_id;
                                break;
                            }
                        }
                    }
                    if (delegate_id == null)
                           throw new CompilerError("Could not find directly extended trait that transitively extends" + te_id);
                    
                    mv.visitVarInsn(ALOAD, 0);
                    getExtendeeField(rttiClassName, delegate_id);
                    String extendeeIlk = delegate_id; // NamingCzar.jvmClassForToplevelTypeDecl(delegate_id,"",packageAndClassName);
                    String field_type = Naming.stemClassToRTTIclass(extendeeIlk);
                    mv.visitMethodInsn(INVOKEVIRTUAL, field_type, method_name,
                            Naming.STATIC_PARAMETER_GETTER_SIG);
                    
                    areturnEpilogue();
                    i++;
                }
            }
        }
        cw.dumpClass( rttiClassFileName, xldata);
        cw = prev;
    }


    /**
     * 
     * Convert a map from Id to tuple of TraitIndex and StaticArg into a map
     * from String to tuple, where the String is the opr-tagged name of the
     * supertrait.
     * 
     * @param extends_transitive_extends_tmp
     * @return
     */
    private HashMap<String, Tuple2<TraitIndex, List<StaticArg>>> oprTagSupertraitsAndArgs(
            HashMap<Id, Tuple2<TraitIndex, List<StaticArg>>> extends_transitive_extends_tmp) {
        HashMap<String, Tuple2<TraitIndex,List<StaticArg>>> extends_transitive_extends =
            new HashMap<String, Tuple2<TraitIndex,List<StaticArg>>>();
        
        for (Map.Entry<Id, Tuple2<TraitIndex,List<StaticArg> > > x :
            extends_transitive_extends_tmp.entrySet()) {
            String ete_oper_stem =
                oprTaggedGenericStemNameSA(x.getKey(), x.getValue()._2);
            extends_transitive_extends.put(ete_oper_stem, x.getValue());
        }
        return extends_transitive_extends;
    }


    /**
     * @param stem
     * @param static_args
     * @return
     */
    private String oprTaggedGenericStemNameSA(Id stem, List<StaticArg> static_args) {
        List<String> opr_args = oprsFromStaticArgs(static_args);
        String extendeeIlk = NamingCzar.jvmClassForToplevelTypeDecl(
                                   stem,opr_args,packageAndClassName);
        return extendeeIlk;
    }
    
    private String oprTaggedGenericStemNameKP(Id stem, List<Pair<String,String>> kind_param_list) {
        List<String> opr_args = oprsFromKindParamList(kind_param_list);
        
        String extendeeIlk = NamingCzar.jvmClassForToplevelTypeDecl(
                                   stem,opr_args,packageAndClassName);
        return extendeeIlk;
    }


    /**
     * Finish up a method that returns an Object
     */
    private void areturnEpilogue() {
        InstantiatingClassloader.areturnEpilogue(mv);
    }


    /**
     * Finish up a method that returns void
     */
    private void voidEpilogue() {
        InstantiatingClassloader.voidEpilogue(mv);
    }
    
    
    /**
     * 
     * @param mv2
     * @param rttiClassName - the class within which 
     * @param extendee
     * @param ti_args
     * @param spns
     */
    private void generateTypeReference(CodeGenMethodVisitor mv2, 
    								   String rttiClassName, 
    								   Id extendee, 
    								   List<StaticArg> ti_args,
    								   HashSet<String> spns)
    {	
        String extendeeIlk = oprTaggedGenericStemNameSA(extendee, ti_args);
        String field_type = Naming.stemClassToRTTIclass(extendeeIlk);      
        List<String> opr_args = oprsFromStaticArgs(ti_args);
        
        if (ti_args.size() - opr_args.size() == 0) {
            if (spns.contains(extendee.getText())) {
                // reference to a static parameter.  Load from field of same name.
                mv2.visitVarInsn(ALOAD, 0);
                mv2.visitFieldInsn(GETFIELD, rttiClassName, extendee.getText(), Naming.RTTI_CONTAINER_DESC);
            } else {
                // reference to a non-generic type.  Load from whatever.Only
                mv2.visitFieldInsn(GETSTATIC, field_type, Naming.RTTI_SINGLETON, Naming.RTTI_CONTAINER_DESC);                
            }
        } else {
            // invoke field_type.factory(class, args)
            String fact_sig = InstantiatingClassloader.jvmSignatureForNTypes(ti_args.size() - opr_args.size(),
                    Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC);
//            String fact_sig = InstantiatingClassloader.jvmSignatureForOnePlusNTypes("java/lang/Class",ti_args.size(),
//                    Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC);
//            mv.visitLdcInsn(org.objectweb.asm.Type.getType(Naming.internalToDesc(extendeeIlk + Naming.LEFT_OXFORD + Naming.RIGHT_OXFORD)));
            
            
            for (StaticArg sta : ti_args) {
                if (sta instanceof TypeArg) {
                    TypeArg sta_ta = (TypeArg) sta;
                    Type t = sta_ta.getTypeArg();
                    if (t instanceof TupleType) {
                    	TupleType tt = (TupleType) t;
                        List<Type> tupleElts = tt.getElements();
                        
                        if (tupleElts.size() == 0) {
                            // need to get field_type right
                            String void_rttic = Naming.stemClassToRTTIclass(NamingCzar.internalFortressVoid);
                            mv2.visitFieldInsn(GETSTATIC, void_rttic, Naming.RTTI_SINGLETON, Naming.RTTI_CONTAINER_DESC);                                           
                        } else {
                            for (Type it : tupleElts) {
                                if (it instanceof TraitType) {
                                    generateTraitTypeReference(mv2,(TraitType) it, rttiClassName, spns);
                                } else if (it instanceof VarType) {
                                    generateVarTypeReference(mv2,(VarType) it, rttiClassName, spns);
                                } else {
                                    generateTypeReference(mv2,rttiClassName, null, Collections.<StaticArg>emptyList(), spns);
                                }
                            }
                            String rttiClass = Naming.tupleRTTIclass(tupleElts.size());
                            String tupleFactorySig = InstantiatingClassloader.jvmSignatureForNTypes(tupleElts.size(),
                                    Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC); 
                            mv.visitMethodInsn(INVOKESTATIC, rttiClass, "factory", tupleFactorySig);
                        }
                        continue;
                    } else if (t instanceof ArrowType) {
                        ArrowType at = (ArrowType) t;
                        Type dom = at.getDomain();
                        
                        //upack the tuple input since RTTI counts each part of the tuple as a separate input
                        List<Type> arrowTypeParts = new ArrayList<Type>();
                        if (dom instanceof TupleType)
                        	arrowTypeParts.addAll(((TupleType) dom).getElements());
                        else
                        	arrowTypeParts.add(dom);
                        arrowTypeParts.add(at.getRange());
                        
                        for (Type it : arrowTypeParts) {
                        	if (it instanceof TraitType) {
                        		generateTraitTypeReference(mv2,(TraitType) it, rttiClassName, spns);
                        	} else if (it instanceof VarType) {
                        		generateVarTypeReference(mv2,(VarType) it, rttiClassName, spns);
                        	} else {
                        		generateTypeReference(mv2,rttiClassName, null, Collections.<StaticArg>emptyList(), spns);
                        	}
                        }
                        String rttiClass = Naming.arrowRTTIclass(arrowTypeParts.size());
                        String arrowFactorySig = InstantiatingClassloader.jvmSignatureForNTypes(arrowTypeParts.size(),
                                Naming.RTTI_CONTAINER_TYPE, Naming.RTTI_CONTAINER_DESC); 
                        mv.visitMethodInsn(INVOKESTATIC, rttiClass, "factory", arrowFactorySig);
                        continue;
                        
                    } else if (t instanceof SelfType) {
                        
                    } else if (t instanceof TraitType) {
                        generateTraitTypeReference(mv2,(TraitType) t, rttiClassName, spns);
                        continue;

                    } else if (t instanceof VarType) {
                        generateVarTypeReference(mv2,(VarType) t, rttiClassName, spns);
                        continue;

                    } else if (t instanceof BottomType) {
                        
                    } else if (t instanceof AnyType) {
                        
                    } else if (t instanceof AbbreviatedType) {
                        
                    } else {
                        
                    }
                    throw new CompilerError("Only handling some static args of generic types in extends clause");

                } else if (sta instanceof BoolArg) {
                    
                } else if (sta instanceof DimArg) {
                    
                } else if (sta instanceof IntArg) {
                    
                } else if (sta instanceof OpArg) {
                    continue; // skip opr args pushing to factory
                } else if (sta instanceof UnitArg) {
                    
                } else {
                    
                }
                throw new CompilerError("Only emitting RTTI for types right now");
            }
            mv2.visitMethodInsn(INVOKESTATIC, field_type, "factory", fact_sig);

        }
        
    }
    
    // List<Pair<String,String>> kind_param_list
    
    private List<String> oprsFromStaticArgs(List<StaticArg> ti_args) {
        ArrayList<String> al = new ArrayList<String>();
        for (StaticArg sta : ti_args) {
            if (sta instanceof OpArg) {
                OpArg opa = (OpArg) sta;
                al.add(opa.getId().getText());
            } 
        }
        return al;
    }

    private List<String> oprsFromKindParamList(List<Pair<String,String>> kind_param_list) {
        ArrayList<String> al = new ArrayList<String>();
        for (Pair<String,String> kp : kind_param_list) {
            if (kp.getA().equals(Naming.XL_OPR)) {
                al.add(kp.getB());
            } 
        }
        return al;
    }


    private void generateTraitTypeReference(CodeGenMethodVisitor mv2, TraitType tt, String rttiClassName, HashSet<String> spns) {
        List<StaticArg> tt_sa = tt.getArgs();
        Id tt_id = tt.getName();
        generateTypeReference(mv2, rttiClassName, tt_id,  tt_sa, spns); 
    }
    
    private void generateVarTypeReference(CodeGenMethodVisitor mv2, VarType tt, String rttiClassName, HashSet<String> spns) {
    	Id tt_id = tt.getName();
        generateTypeReference(mv2, rttiClassName, tt_id,  Collections.<StaticArg>emptyList(), spns); 
    }


    /**
     * @param rttiClassName
     * @param extendee
     * 
     * Does NOT expect that rttiRef is on stack; returns value of field.
     */
    private void getExtendeeField(String rttiClassName, String extendeeIlk) {
        // String extendeeIlk = NamingCzar.jvmClassForToplevelTypeDecl(extendee,"",packageAndClassName);
        String field_type = Naming.stemClassToRTTIclass(extendeeIlk);
        //String tyDesc = Naming.internalToDesc(field_type);
        mv.visitVarInsn(ALOAD, 0);
        //mv.visitFieldInsn(GETFIELD, rttiClassName, extendeeIlk, tyDesc);
        mv.visitFieldInsn(GETFIELD, rttiClassName, extendeeIlk, Naming.RTTI_CONTAINER_DESC);
        mv.visitTypeInsn(CHECKCAST, field_type);
    }

    /**
     * @param rttiClassName
     * @param extendee
     * 
     * Expects rttiRef and fieldvalue are on stack already, consumes both.
     */
    private void putExtendeeField(String rttiClassName, Id extendee) {
        String extendeeIlk = NamingCzar.jvmClassForToplevelTypeDecl(extendee,"",packageAndClassName);
//        String field_type = Naming.stemClassToRTTIclass(extendeeIlk);
//        String tyDesc = Naming.internalToDesc( field_type );
//        mv.visitFieldInsn(PUTFIELD, rttiClassName, extendeeIlk, tyDesc);
        mv.visitFieldInsn(PUTFIELD, rttiClassName, extendeeIlk, Naming.RTTI_CONTAINER_DESC);
    }

    public void forVarDecl(VarDecl v) {
        // Assumption: we already dealt with this VarDecl in pre-pass.
        // Therefore we can just skip it.
        debug("forVarDecl ",v," should have been seen during pre-pass.");
    }

    /** Supposed to be called with nested codegen context. */
    private void generateVarDeclInnerClass(VarDecl x, String classFile, String tyName, Expr exp) {
        String tyDesc = Naming.internalToDesc(tyName);
        cw = new CodeGenClassWriter(ClassWriter.COMPUTE_FRAMES, cw);
        cw.visitSource(NodeUtil.getSpan(x).begin.getFileName(), null);
        cw.visit( InstantiatingClassloader.JVM_BYTECODE_VERSION, ACC_PUBLIC + ACC_SUPER + ACC_FINAL,
                  classFile, null, NamingCzar.internalSingleton, null );
        cw.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL,
                      NamingCzar.SINGLETON_FIELD_NAME, tyDesc, null, null);
        mv = cw.visitCGMethod(ACC_STATIC,
                            "<clinit>", Naming.voidToVoid, null, null);
        exp.accept(this);
        // Might condition cast-to on inequality of static types
        if (tyName.startsWith(Naming.TUPLE_OX) ||
                tyName.startsWith(Naming.ARROW_OX)   ) {
            InstantiatingClassloader.generalizedCastTo(mv, tyName);
        }
        mv.visitFieldInsn(PUTSTATIC, classFile,
                          NamingCzar.SINGLETON_FIELD_NAME, tyDesc);
        voidEpilogue();
        cw.dumpClass( classFile );
    }

    private void forVarDeclPrePass(VarDecl v) {
        List<LValue> lhs = v.getLhs();
        Option<Expr> oinit = v.getInit();
        if (lhs.size() != 1) {
            throw sayWhat(v,"VarDecl "+v+" tupled lhs not handled.");
        }
        if (!oinit.isSome()) {
            debug("VarDecl ", v, " skipping abs var decl.");
            return;
        }
        LValue lv = lhs.get(0);
        if (lv.isMutable()) {
            throw sayWhat(v,"VarDecl "+v+" mutable bindings not yet handled.");
        }
        Id var = lv.getName();
        Type ty = (Type)lv.getIdType().unwrap();
        Expr exp = oinit.unwrap();
        String classFile = NamingCzar.jvmClassForToplevelDecl(var, packageAndClassName);
        String tyName = NamingCzar.jvmBoxedTypeName(ty, thisApi());
        debug("VarDeclPrePass ", var, " : ", ty, " = ", exp);
        new CodeGen(this).generateVarDeclInnerClass(v, classFile, tyName, exp);

        addStaticVar(
            new VarCodeGen.StaticBinding(var, ty, classFile,
                                         NamingCzar.SINGLETON_FIELD_NAME, Naming.internalToDesc(tyName)));
    }

    public void forVarRef(VarRef v) {
        List<StaticArg> lsargs = v.getStaticArgs();
        Id id = v.getVarId();
        VarCodeGen vcg = getLocalVarOrNull(id, lsargs);
        if (vcg == null) {
            debug("forVarRef fresh import ", v);
            Type ty = NodeUtil.getExprType(v).unwrap();
            ty = typeAnalyzer.normalize(ty);
            /* TODO if ty is an intersection type,
             * need to be sure it really is normalized.
             * Because of bugs in static analysis, it is not.
             * For now, defer normalization till run time.
             */
            String tyDesc = NamingCzar.jvmTypeDesc(ty, thisApi());
            String className = NamingCzar.jvmClassForToplevelDecl(id, packageAndClassName);
            vcg = new VarCodeGen.StaticBinding(id, lsargs, ty,
                                               className,
                                               NamingCzar.SINGLETON_FIELD_NAME, tyDesc);
            addStaticVar(vcg);


        }
        addLineNumberInfo(v);
        debug("forVarRef ", v , " Value = ", vcg);
        String static_args = NamingCzar.instantiatedGenericDecoration(lsargs, thisApi());
        vcg.pushValue(mv, static_args);
    }

    private void pushVoid() {
        mv.visitMethodInsn(INVOKESTATIC, NamingCzar.internalFortressVoid, NamingCzar.make,
                           Naming.makeMethodDesc("", NamingCzar.descFortressVoid));
    }

    public void forVoidLiteralExpr(VoidLiteralExpr x) {
        debug("forVoidLiteral ", x);
        addLineNumberInfo(x);
        pushVoid();
    }
    
    public void forWhile(While x) {
        GeneratorClause testExpr = x.getTestExpr();
        Do body = x.getBody();

        org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        org.objectweb.asm.Label done = new org.objectweb.asm.Label();
        
        // Run the test, if it true, run the body and go back to the
        // start, if it's not, jump to done.
        mv.visitLabel(start);
        
        testExpr.getInit().accept(this);
        mv.visitMethodInsn(INVOKEVIRTUAL,
                           NamingCzar.internalFortressBoolean, "getValue",
                           Naming.makeMethodDesc("", NamingCzar.descBoolean));
        mv.visitJumpInsn(IFEQ, done);

        body.accept(this);
        mv.visitInsn(POP);
        mv.visitJumpInsn(GOTO, start);
        
        mv.visitLabel(done);
        pushVoid();
    }
    
    private static final F<StaticArg, Boolean> isSymbolic = new F<StaticArg, Boolean>() {

        @Override
        public Boolean apply(StaticArg x) {
            if (x instanceof TypeArg) {
                Type t = ((TypeArg) x).getTypeArg();
                if (t instanceof VarType) {
                    return Boolean.TRUE;
                }
                if (t instanceof TraitType) {
                    return Useful.orReduction(((TraitType)t).getArgs(), this);
                }
            }
            return Boolean.FALSE;
        }
        
    };
    
    public void forMethodInvocation(MethodInvocation x) {
        debug("forMethodInvocation ", x,
              " obj = ", x.getObj(),
              " method = ", x.getMethod(),
              " static args = ", x.getStaticArgs(),
              " args = ", x.getArg(),
              " overloading type = " + x.getOverloadingType());
        IdOrOp method = x.getMethod();
        Expr obj = x.getObj();
        List<StaticArg> method_sargs = x.getStaticArgs();
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
        receiverType = sanitizePossibleStupidIntersectionType(receiverType);
//        if (!(receiverType instanceof TraitType)) {
//            throw sayWhat(x, "receiver type "+receiverType+" is not TraitType in " + x);
//        }

        int savedParamCount = paramCount;
        try {
            
            if (method_sargs.size() > 0) {
                /* Have to emit some interesting code.
                 * 
                 * Want to call object.methodnameSCHEMA
                 * to obtain an object, which we then cast to
                 * an appropriate arrow type (with an explicit leading SELF,
                 * using our static type) which we then invoke.
                 * 
                 * Must pass in a long constant that is the hashcode
                 * (but it could just be a random number, our "hashcode" is the
                 * one that is used everywhere)
                 * 
                 * Must pass in a string correctly describing our static args.
                 * 
                 * The convention for the string must agree with
                 * InstantiatingClassloader.findGenericMethodClosure
                 */
                
                /*
                 * We will be casting to a particular arrow type;
                 * we need to pass in the receiver type (the prepended first
                 * parameter for the arrow type) to be sure that we get back
                 * exactly the right sort of closure.  There is a variance
                 * problem here: if object extends trait, note that
                 * 
                 * object -> result is NOT a subtype of trait -> result.
                 * 
                 * Contravariance bites us here.
                 */
                StaticArg receiverStaticArg = NodeFactory.makeTypeArg(receiverType);
                List<StaticArg> prepended_method_sargs = Useful.prepend(receiverStaticArg, method_sargs);
                
                boolean anySymbolic = Useful.orReduction(prepended_method_sargs, isSymbolic);
                
                Type prepended_domain = receiverPrependedDomainType(
                        receiverType, domain_type);
                
                // DRC-WIP
                
                String string_sargs = NamingCzar.instantiatedGenericDecoration(prepended_method_sargs, thisApi());
                              
                // assumption -- Schema had better be here.
                Type overloading_schema = x.getOverloadingSchema().unwrap();
                
                TraitType rt_tt = ((TraitType)receiverType);
                List<StaticArg> rt_args = rt_tt.getArgs();
                
                if (false && rt_args.size() > 0) {
                    // patch linked to use of HEAVY_X in generic method names -- protect the tvars from rewriting.
                    /* 
                     * Must check overloading schema for enclosing trait args.
                     */
                    Map<Id, TypeConsIndex> citc = ci.typeConses();
                    TypeConsIndex tci = citc.get(rt_tt.getName());
                    TraitObjectDecl tod = (TraitObjectDecl) tci.ast();
                    List<StaticParam> rt_params = tod.getHeader().getStaticParams();
                    StaticTypeReplacer str = new StaticTypeReplacer(rt_params, rt_args);
                    overloading_schema = str.replaceIn(overloading_schema);
                }
                
                String methodName = NamingCzar.genericMethodName(method, (ArrowType) overloading_schema, thisApi());
                
                // Evaluate the object
                obj.accept(this);
                
                String castToArrowType = emitFindGenericMethodClosure(
                        methodName, receiverType, prepended_domain, range_type,
                        string_sargs, anySymbolic);

                // evaluate args
                evalArg(x, domain_type, arg);
                
                String sig = NamingCzar.jvmSignatureFor(prepended_domain, range_type, thisApi());

                emitInvokeGenericMethodClosure(sig, castToArrowType);

                
            } else {
                // put object on stack
                obj.accept(this);
                // put args on stack
                evalArg(x, domain_type, arg);
                methodCall(method, (NamedType)receiverType, domain_type, range_type);
            }
        } finally {
            paramCount = savedParamCount;
        }

    }


    /**
     * @param receiverType
     * @param domain_type
     * @return
     */
    private Type receiverPrependedDomainType(Type receiverType, Type domain_type) {
        Type prepended_domain = null;
        /* I think we start to have a problem here in the future,
         * when generics can be parametrized by tuples.
         * We need to figure out what this means.
         */
        if (domain_type instanceof TupleType) {
            TupleType tt = (TupleType) domain_type;
            prepended_domain = NodeFactory.makeTupleTypeOrType(tt, Useful.prepend(receiverType, tt.getElements()));
        } else {
            prepended_domain = NodeFactory.makeTupleType(domain_type.getInfo().getSpan(), Useful.list(receiverType, domain_type));
        }
        return prepended_domain;
    }

    private Type sanitizePossibleStupidIntersectionType(Type receiverType) {
        if (receiverType instanceof TraitSelfType) {
            receiverType = ((TraitSelfType) receiverType).getNamed();
        }
        if (receiverType instanceof NamedType)
            return receiverType;
        if (receiverType instanceof IntersectionType) {
            IntersectionType it = (IntersectionType) receiverType;
            Type at = null;
            for (Type t : it.getElements()) {
                    t = sanitizePossibleStupidIntersectionType(t);
                    if (at == null)
                        at = t;
                    else {
                        CFormula c1 = typeAnalyzer.subtype(t, at);
                        CFormula c2 = typeAnalyzer.subtype(at, t);
                        if (Formula.isTrue(c1, typeAnalyzer)) {
                            at = t;
                        } else if (Formula.isTrue(c2, typeAnalyzer)) {
                            // at is subtype, do nothing.
                        } else {
                            return receiverType;
                        }
                       
                    }
                
            }
            return at;
        }
        return receiverType;
    }


    /**
     * @param prepended_domain
     * @param range_type
     * @param castToArrowType
     */
    private void emitInvokeGenericMethodClosure(String sig, String castToArrowType) {
        // System.err.println(desc+".apply"+sig+" call");
        mv.visitMethodInsn(INVOKEINTERFACE, castToArrowType,
                           Naming.APPLY_METHOD, sig);
    }

    /**
     * @param methodName
     * @param receiverType
     * @param prepended_domain
     * @param range_type
     * @param string_sargs
     * @param anySymbolic
     * @return
     */
    private String emitFindGenericMethodClosure(String methodName,
            Type receiverType, Type prepended_domain, Type range_type,
            String string_sargs, boolean anySymbolic) {
        
        if (receiverType instanceof TraitSelfType) {
            receiverType = ((TraitSelfType) receiverType).getNamed();
        }
        
        // Need to dup this so we will not re-eval.
        mv.visitInsn(DUP);
        
        if (anySymbolic) {
            String loadHash = Naming.opForString(Naming.hashMethod, string_sargs);
            mv.visitMethodInsn(INVOKESTATIC, Naming.magicInterpClass, loadHash, "()J");
            RTHelpers.symbolicLdc(mv, string_sargs);
        } else {
            // compute hashcode statically, push constant,
            long hash_sargs = MagicNumbers.hashStringLong(string_sargs);
            mv.visitLdcInsn(Long.valueOf(hash_sargs));
            // compute String, push constant
            mv.visitLdcInsn(string_sargs);
        }
        
        // invoke the oddly named method
        methodCall(methodName, (TraitType)receiverType, genericMethodClosureFinderSig);
        
        // cast the result
        // Cast-to-type is Arrow[\ receiverType; domain; range \]
        
        
        String castToArrowType = NamingCzar.makeArrowDescriptor(thisApi(), prepended_domain, range_type); 
        //mv.visitTypeInsn(CHECKCAST, castToArrowType);
        // got to do the more heavyweight thing, just in case.
        InstantiatingClassloader.generalizedCastTo(mv, castToArrowType);

        // swap w/ TOS
        mv.visitInsn(SWAP);
        return castToArrowType;
    }


    /**
     * @param expr
     * @return
     */
    private Type exprType(Expr expr) {
        Option<Type> exprType = expr.getInfo().getExprType();

        if (!exprType.isSome()) {
            throw sayWhat(expr, "Missing type information for " + expr);
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
    private void evalArg(ASTNode x, Type domain_type, Expr arg) {
        if (arg instanceof VoidLiteralExpr) {
            paramCount = 0;
        } else if (arg instanceof TupleExpr) {
            // Why isn't this parallel???
            TupleExpr targ = (TupleExpr) arg;
            Type arg_t = domain_type; // arg.getInfo().getExprType().unwrap();
            if (arg_t instanceof TupleType) {
                TupleType arg_tt = (TupleType) arg_t;
                List<Type> arg_tts = arg_tt.getElements();
                List<Expr> exprs = targ.getExprs();
                int l = arg_tts.size();
                
                for (int i = 0; i < l; i++) {
                    Expr expr = exprs.get(i);
                    Type t = arg_tts.get(i);
                    expr.accept(this);

                    conditionallyCastParameter(expr, t);
                }
                
                paramCount = l;
            } else {
                // Passed to Any, probably.
            }
        } else {
            paramCount = 1; // for now; need to dissect tuple and do more.
            // Type arg_t = domain_type;
            // Has to be the expr type so that we extract the pieces using the right methods.
            Type arg_t = arg.getInfo().getExprType().unwrap();
            if (arg_t instanceof TupleType) {
                TupleType arg_tt = (TupleType) arg_t;
                List<Type> arg_tts = arg_tt.getElements();
                // First eval the tuple-typed expr.
                // Then dup, extract one element, swap
                // Repeat till last element,
                // for it, do not dup or swap.
                arg.accept(this);
                int l = arg_tts.size();
                String owner = NamingCzar.jvmTypeDesc(arg_tt, thisApi(), false, true);

                for (int i = 0; i < l; i++) {
                    Type t = arg_tts.get(i);
                    String m = InstantiatingClassloader.TUPLE_TYPED_ELT_PFX + (i + Naming.TUPLE_ORIGIN);
                    String sig = NamingCzar.jvmTypeDesc(t, thisApi(), true, true);

                    if (i < l-1) {
                        mv.visitInsn(DUP);
                        mv.visitMethodInsn(INVOKEINTERFACE, owner, m, "()"+sig);
                        conditionallyCastParameter((Expr)null, t); // can't quite figure out what the type is here.
                        mv.visitInsn(SWAP);
                    } else {
                        mv.visitMethodInsn(INVOKEINTERFACE, owner, m, "()"+sig);
                        conditionallyCastParameter((Expr)null, t); // can't quite figure out what the type is here.
                    }
                }
               
            } else if (arg_t instanceof VarType ) { 
                arg.accept(this);
                conditionallyCastParameter(arg, arg_t);
            } else if (domain_type instanceof ArrowType) { 
                arg.accept(this);
                conditionallyCastParameter(arg, domain_type);
            } else {
                arg.accept(this);
            }
        }
    }


    private void conditionallyCastParameter(Expr expr, Type domain_type) {
        conditionallyCastParameter(expr == null ?
                Null.<Type>make() : expr.getInfo().getExprType(),
                domain_type);
    }
        /**
     * @param domain_type
     */
    private void conditionallyCastParameter(Option<Type> apparent_type, Type domain_type) {
        if (domain_type instanceof ArrowType) {
            // insert cast
            String cast_to = NamingCzar.jvmTypeDesc(domain_type, thisApi(), false, true);
            InstantiatingClassloader.generalizedCastTo(mv, cast_to);
        } else if ((domain_type instanceof TupleType) && ((TupleType)domain_type).getElements().size() > 0) {
            // insert cast
            String cast_to = NamingCzar.jvmTypeDesc(domain_type, thisApi(), false, true);
            InstantiatingClassloader.generalizedCastTo(mv, cast_to);
        } else if ((domain_type instanceof VarType) &&
                ((VarType)domain_type).getName().getText().equals(Naming.UP_INDEX)) {
            // insert cast
            String cast_to = NamingCzar.jvmTypeDesc(domain_type, thisApi(), false, true);
            InstantiatingClassloader.generalizedCastTo(mv, cast_to);
        } else if (apparent_type.isNone() ||
                /* This will insert unnecessary casts, but the JIT should remove them
                 * It might be worthwhile to do a simple walk of supertraits of the
                 * apparent_type to see if the subtyping is a trivial instance of
                 * will-work-in-Java.
                 */
                0 != NodeComparator.compare(apparent_type.unwrap(), domain_type)) {
            String cast_to = NamingCzar.jvmTypeDesc(domain_type, thisApi(), false, true);
            InstantiatingClassloader.generalizedCastTo(mv, cast_to);
        }
    }

    private void generateHigherOrderCall(Type t) {
        if (!(t instanceof ArrowType)) {
            throw sayWhat(t,"Higher-order call to non-arrow type " + t);
        }
        ArrowType at = (ArrowType)t;
        String desc = NamingCzar.makeArrowDescriptor(at, thisApi());
        String sig = NamingCzar.jvmSignatureFor(at,thisApi());
        // System.err.println(desc+".apply"+sig+" call");
        mv.visitMethodInsn(INVOKEINTERFACE, desc,
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
            
            // need to use closure if fn is generic and instantiating with a
            // tuple type for the entire parameter list.
            boolean useClosure =
                !(fn instanceof FunctionalRef) 
                || ((FunctionalRef) fn).getStaticArgs().size() > 0
                ;
                
            if (useClosure) {
                // Higher-order call.
                fn.accept(this); // Puts the VarRef function on the stack.
            }
            fnRefIsApply = false;
            Type domain_type = ((ArrowType)(x.getFunction().getInfo().getExprType().unwrap())).getDomain();
            evalArg(x, domain_type, arg);
            fnRefIsApply = true;
            if (useClosure) {
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
        com.sun.fortress.nodes.Type ty = x.getType().unwrap();
        Relation<IdOrOpOrAnonymousName, Function> fnrl = ci.functions();

        MultiMap<Integer, OverloadSet.TaggedFunctionName> byCount =
            new MultiMap<Integer,OverloadSet.TaggedFunctionName>();

        // System.err.println(NodeUtil.getSpan(x) + ": _RewriteFnOverloadDecl " + name +
        //                    "\n  candidates " + fns);

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
                if (true // || OverloadSet.functionInstanceofType(f, ty, ta)
                        ) {
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
                                                           typeAnalyzer, fs, i);

                os.split(false);
                os.generateAnOverloadDefinition(name.stringName(), cw);

            }
        }

    }

    static String implMethodName(Functional f) {
        String s = f.name().getText();
        if (f instanceof FunctionalMethod) {
            return Naming.fmDottedName(s, ((FunctionalMethod)f).selfPosition());
        } else {
            return s;
        }
    }
    
    /**
     * Creates overloaded functions for any overloads present at the top level
     * of this component.  Top level overloads are those that might be exported;
     * Reference overloads are rewritten into _RewriteFnOverloadDecl nodes
     * and generated in the normal visits.
     */
    public static Set<String> generateTopLevelOverloads(APIName api_name,
            Map<IdOrOpOrAnonymousName,MultiMap<Integer, Functional>> size_partitioned_overloads,
            TypeAnalyzer ta,
            CodeGenClassWriter cw,
            CodeGen cg,
            OverloadSet.Factory overload_factory
            ) {

        Set<String> overloaded_names_and_sigs = new HashSet<String>();

        for (Map.Entry<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> entry1 :
                 size_partitioned_overloads.entrySet()) {
            IdOrOpOrAnonymousName  name = entry1.getKey();
            MultiMap<Integer, Functional> partitionedByArgCount = entry1.getValue();

            for (Map.Entry<Integer, Set<Functional>> entry :
                partitionedByArgCount.entrySet()) {
                int i = entry.getKey();
                Set<Functional> fs = entry.getValue();
                Functional one_f = fs.iterator().next();

                OverloadSet os = overload_factory.make(api_name, name,
                            ta, fs, i);

                /*
                 * Temporarily, do not generate code for generic
                 * overloads.  Many of them are statically resolved,
                 * and this includes the cases necessary for reduction.
                 */
                os.split(true);

                String s = name.stringName();
                String s2 = NamingCzar.apiAndMethodToMethod(api_name, s);

                try {
                    os.generateAnOverloadDefinition(s2, cw);
                } catch (Error ex) {
                    String mess = ex.getMessage();
                    throw ex; // good place for a breakpoint
                } catch (RuntimeException ex) {
                    String mess = ex.getMessage();
                    throw ex; // good place for a breakpoint
                }
                if (cg != null) {
                    /* Need to check if the overloaded function happens to match
                     * a name in an API that this component exports; if so,
                     * generate a forwarding wrapper from the
                     * 
                     * ????
                     */
                }

                for (Map.Entry<String, OverloadSet> o_entry : os.getOverloadSubsets().entrySet()) {
                    String ss = o_entry.getKey();
                    OverloadSet o_s = o_entry.getValue();
                    // Need to add Schema to the end of ss for generic overloads.
                    // System.err.println("Adding "+s+" : "+ss);
                    overloaded_names_and_sigs.add(ss);
                }
                
                overloaded_names_and_sigs.addAll(os.getOtherKeys());

           }
        }
        // StringBuilder sb = new StringBuilder("api ");
        // sb.append(api_name);
        // sb.append(" has overloads:\n");
        // for (String s : overloaded_names_and_sigs) {
        //     sb.append("  ");
        //     sb.append(s);
        //     sb.append("\n");
        // }
        // System.err.println(sb.toString());
        return overloaded_names_and_sigs;
    }

    public static Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>>
       sizePartitionedOverloads(Relation<IdOrOpOrAnonymousName, ? extends Functional> fns) {

        Map<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>> result =
            new BATree<IdOrOpOrAnonymousName,
            MultiMap<Integer, Functional>>(NodeComparator.IoooanComparer);
            // new HashMap<IdOrOpOrAnonymousName, MultiMap<Integer, Functional>>();

        for (IdOrOpOrAnonymousName name : fns.firstSet()) {
            Set<? extends Functional> defs = fns.matchFirst(name);
            if (defs.size() <= 1) continue;

            MultiMap<Integer, Functional> partitionedByArgCount = partitionByArgCount(defs);
            if (partitionedByArgCount.size() > 0)
                result.put(name, partitionedByArgCount);
        }

        return result;
    }


    /**
     * @param defs
     * @return
     */
    public static MultiMap<Integer,  Functional> partitionByArgCount(
            Set<? extends Functional> defs) {
        MultiMap<Integer, Functional> partitionedByArgCount =
            new MultiMap<Integer, Functional>(DefaultComparator.<Integer>normal());

        for (Functional d : defs) {
            partitionedByArgCount.putItem(d.parameters().size(), d);
        }

        for (Functional d : defs) {
            Set<Functional> sf = partitionedByArgCount.get(d.parameters().size());
            if (sf != null && sf.size() <= 1)
                partitionedByArgCount.remove(d.parameters().size());
        }
        return partitionedByArgCount;
    }

    public static MultiMap<Integer,  Functional> partitionByMethodArgCount(
            OverloadingOracle oa, 
            Set<? extends Functional> defs) {
        MultiMap<Integer, Functional> partitionedByArgCount =
            new MultiMap<Integer, Functional>(DefaultComparator.<Integer>normal());

        for (Functional d : defs) {
            partitionedByArgCount.putItem(d.parameters().size(), d);
        }

        for (Functional d : defs) {
            Set<Functional> sf = partitionedByArgCount.get(d.parameters().size());
            if (sf != null && sf.size() <= 1)
                partitionedByArgCount.remove(d.parameters().size());
        }
        return partitionedByArgCount;
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
                throw sayWhat(d, " can't sort non-value-creating decl by dependencies.");
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
    private void dumpTraitMethodSigs(List<Decl> decls) {
        debug("dumpSigs", decls);
        for (Decl d : decls) {
            debug("dumpSigs decl =", d);
            if (!(d instanceof FnDecl))
                throw sayWhat(d);

            FnDecl f = (FnDecl) d;
            FnHeader h = f.getHeader();
            List<StaticParam> sparams = h.getStaticParams();

            List<Param> params = h.getParams();
            int selfIndex = NodeUtil.selfParameterIndex(params);
            boolean  functionalMethod = selfIndex != Naming.NO_SELF;

            if (sparams.size() > 0) {
                // Not handling overload-based name-forwarding of generic methods yet.
                List<StaticParam> tsp = currentTraitObjectDecl.getHeader().getStaticParams();

                String method_name = NamingCzar.genericMethodName(new FnNameInfo(f, tsp, thisApi()), selfIndex, thisApi());
                CodeGenMethodVisitor mv = cw.visitCGMethod(ACC_ABSTRACT + ACC_PUBLIC, method_name, genericMethodClosureFinderSig, null, null);
                mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
                mv.visitEnd();
            } else {
                IdOrOpOrAnonymousName xname = h.getName();
                IdOrOp name = (IdOrOp) xname;

                String desc = NamingCzar.jvmSignatureFor(f,component.getName());
                if (functionalMethod) {
                    desc = Naming.removeNthSigParameter(desc, selfIndex);
                }

                // TODO what about overloading collisions in an interface?
                // it seems wrong to publicly mangle.
                String mname = functionalMethod ? Naming.fmDottedName(
                        singleName(name), selfIndex) : singleName(name); 
                abstractMethod(desc, mname);
                // provide both overloaded and single names in abstract decl.
                mname = NamingCzar.mangleAwayFromOverload(mname);
                abstractMethod(desc, mname);
            }
        }
    }


    /**
     * @param desc
     * @param mname
     */
    public void abstractMethod(String desc, String mname) {
        CodeGenMethodVisitor mv = cw.visitCGMethod(ACC_ABSTRACT + ACC_PUBLIC,
                        mname, desc, null, null);

        mv.visitMaxs(Naming.ignoredMaxsParameter, Naming.ignoredMaxsParameter);
        mv.visitEnd();
    }
    
    public static Naming.XlationData xlationData(String tag) {
        return
            new Naming.XlationData(tag);
    }

    /**
     * @param id
     * @param PCN
     * @return
     */
    public static String stemFromId(Id id, String PCN) {
        return NamingCzar.jvmClassForToplevelTypeDecl(id,"",PCN);
    }


    public void setCw(CodeGenClassWriter cw) {
        this.cw = cw;
    }


    public CodeGenClassWriter getCw() {
        return cw;
    }   
    
}
