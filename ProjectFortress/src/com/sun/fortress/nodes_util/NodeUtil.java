/*******************************************************************************
    Copyright 2008 Sun Microsystems, Inc.,
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

package com.sun.fortress.nodes_util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionVisitor;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.lambda.Lambda2;

import com.sun.fortress.nodes.*;
import com.sun.fortress.useful.*;
import com.sun.fortress.compiler.Parser;
import com.sun.fortress.interpreter.glue.WellKnownNames;
import com.sun.fortress.exceptions.StaticError;

import static com.sun.fortress.exceptions.InterpreterBug.bug;
import static com.sun.fortress.parser_util.FortressUtil.syntaxError;

public class NodeUtil {

    /* get the declared name of a component or api */
    public static APIName apiName(APIName name, File f) throws StaticError {
        CompilationUnit cu = Parser.preparseFileConvertExn(name, f);
        return cu.getName();
    }

    public static Iterable<Id> getIds(final Id qName) {
        return qName.getApi().apply(new OptionVisitor<APIName, Iterable<Id>>() {
            public Iterable<Id> forSome(APIName apiName) {
                return IterUtil.compose(apiName.getIds(), qName);
            }
            public Iterable<Id> forNone() {
                return IterUtil.singleton(qName);
            }
        });
    }

    /* get a list of imported apis from a component/api */
    public static List<APIName> getImportedApis(APIName name, File f) throws StaticError {
        CompilationUnit cu = Parser.preparseFileConvertExn(name, f);
        if (cu instanceof Component) {
            return collectComponentImports((Component)cu);
        } else if (cu instanceof Api) {
            return collectApiImports((Api)cu);
        } else {
            throw StaticError.make("Neither a component nor an api", cu);
        }
    }

    public static List<APIName> collectApiImports(Api api){
        List<APIName> all = new ArrayList<APIName>();
        for (Import i : api.getImports()){
            if (i instanceof ImportedNames) {
                ImportedNames names = (ImportedNames) i;
                all.add( names.getApi() );
            } else { // i instanceof ImportApi
                ImportApi apis = (ImportApi) i;
                for (AliasedAPIName a : apis.getApis()) {
                    all.add(a.getApi());
                }
            }
        }
        return removeExecutableApi(all);
    }

    public static List<APIName> collectComponentImports(Component comp){
        final List<APIName> all = new ArrayList<APIName>();
        comp.accept(new NodeDepthFirstVisitor_void(){
                @Override
                public void forImportedNamesDoFirst(ImportedNames that) {
                    Debug.debug(Debug.Type.SYNTAX, 2, "Add import api ", that.getApi());
                    all.add(that.getApi());
                }

                @Override
                public void forImportApi(ImportApi that){
                    for (AliasedAPIName api : that.getApis()){
                        Debug.debug(Debug.Type.SYNTAX, 2, "Add aliased api ", api.getApi());
                        all.add(api.getApi());
                    }
                }
            });
        for (APIName api : comp.getExports()) {
            all.add(api);
        }
        return removeExecutableApi(all);
    }

    private static List<APIName> removeExecutableApi(List<APIName> all){
        APIName executable = NodeFactory.makeAPIName("Executable");
        List<APIName> fixed = new ArrayList<APIName>();
        for (APIName name : all){
            if (! name.equals(executable)){
                fixed.add(name);
            }
        }
        return fixed;
    }

    /* for HasAt ***********************************************************/
    /**
     * Returns the index of the 'self' parameter in the parameter list,
     * or -1 if it does not appear.
     * Only meaningful for method declarations.
     */
    public static int selfParameterIndex(Applicable d) {
        int i = 0;
        for (Param p : d.getParams()) {
            Id name = p.getName();
                if (WellKnownNames.defaultSelfName.equals(nameString(name))) {
                return i;
            }
            i++;
        }
        return -1;
    }

    /* for Applicable ******************************************************/
    public static String nameAsMethod(Applicable app) {
        String name = nameString(app.getName());
            int spi = selfParameterIndex(app);
            if (spi >= 0)
                return "rm$" + spi + "$" + name;
            else
                return name;

    }

    public static Option<Expr> getBody(Applicable def) {
        if (def instanceof FnDecl) { return Option.some(((FnDecl)def).getBody()); }
        else if (def instanceof FnExpr) { return Option.some(((FnExpr)def).getBody()); }
        else { return Option.none(); }
    }

    /* for Param ***********************************************************/
    public static boolean isMultifix(List<Param> params) {
        for (Param p : params) {
            if (p instanceof VarargsParam) return true;
        }
        return (params.size() > 2);
    }

    public static boolean isSetterOrGetter(List<Modifier> mods) {
        NodeDepthFirstVisitor<Boolean> mod_visitor =
            new NodeDepthFirstVisitor<Boolean>() {
            @Override public Boolean defaultCase(Node n) { return false; }
            @Override public Boolean forModifierGetter(ModifierGetter that) { return true; }
            @Override public Boolean forModifierSetter(ModifierSetter that) { return true; }
        };

        return
            IterUtil.fold(mod_visitor.recurOnListOfModifier(mods), false, new Lambda2<Boolean,Boolean,Boolean>(){
                    public Boolean value(Boolean arg0, Boolean arg1) { return arg0 | arg1; }});
    }

    public static boolean isVar(List<Modifier> mods) {
        for (Modifier mod : mods) {
            if ( mod instanceof ModifierVar )
                return true;
        }
        return false;
    }

    public static boolean isMutable(Param p) {
        for (Modifier m : p.getMods()) {
            if (m instanceof ModifierVar || m instanceof ModifierSettable) {
                return true;
            }
        }
        return false;
    }

    public final static NodeVisitor<String> nameSuffixGetter =
        new NodeAbstractVisitor<String>() {
        @Override public String forAPIName(APIName n) {
            return n.getText();
            }
        public String forId(Id n) {
            return n.getText();
        }
        public String forOp(Op n) {
            return OprUtil.fixityDecorator(n.getFixity(), n.getText());
        }
        public String forEnclosing(Enclosing n) {
            // Interior space is REQUIRED
            return n.getOpen().getText() + " " + n.getClose().getText();
        }
        public String forAnonymousFnName(AnonymousFnName n) {
            return n.getSpan().toString();
        }
    };

    public static String nameSuffixString(AbstractNode n) {
        return n.accept(nameSuffixGetter);
    }

    private final static NodeVisitor<String> nameGetter =
        new NodeAbstractVisitor<String>() {

        @Override public String forAPIName(APIName n) {
            return n.getText();
            }
        @Override public String forIdOrOpOrAnonymousName(IdOrOpOrAnonymousName n) {
            return nameString(n);
            }
        public String forId(Id n) {
            final String last = n.getText();
            Option<APIName> odn = n.getApi();
            return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
        }
        public String forOp(Op n) {
            Option<APIName> odn = n.getApi();
            String last = OprUtil.fixityDecorator(n.getFixity(), n.getText());
            return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
        }
        public String forEnclosing(Enclosing n) {
            Option<APIName> odn = n.getApi();
            // Interior space is REQUIRED
            String last = n.getOpen().getText() + " " + n.getClose().getText();
            return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;

        }
        public String forAnonymousFnName(AnonymousFnName n) {
            return n.getSpan().toString();
        }
        public String forConstructorFnName(ConstructorFnName n) {
            // TODO Auto-generated method stub
            return nameString(n);
        }
    };

    /* nameString *************************************************************/
    public static String nameString(Name n) {
        return n.accept(nameGetter);
    }

    public static String nameString(IdOrOpOrAnonymousName n) {
        return n.accept(nameGetter);
    }

    public static String nameString(APIName n) {
        return n.getText();
//        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
//      return IterUtil.toString(ns, "", ".", "");
    }

    public static String dirString(APIName n) {
        Iterable<String> ns = IterUtil.map(n.getIds(), IdToStringFn);
        // NOT File.separator -- that is unnecessary and confusing.
        return IterUtil.toString(ns, "", "/", "");
    }

    public static String shortNameString(Id n) {
        final String last = n.getText();
        Option<APIName> odn = n.getApi();
        if (odn.isSome()) {
            APIName _odn = odn.unwrap();
            if (_odn.getText().equals("FortressBuiltin")
                || _odn.getText().equals("FortressLibrary")) {
                return last;
            } else {
                return nameString(odn.unwrap()) + "." + last;
            }
        } else {
            return last;
        }
    }

    public static String nameString(Id n) {
        final String last = n.getText();
        Option<APIName> odn = n.getApi();
        return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
    }

    public static String nameString(Op n) {
        return OprUtil.fixityDecorator(n.getFixity(), n.getText());
    }

    public static String nameString(Enclosing n) {
        // Interior space is REQUIRED
        return n.getOpen().getText() + " " + n.getClose().getText();
    }
    public static String nameString(AnonymousFnName n) {
        return n.getSpan().toString();
    }
    public static String nameString(ConstructorFnName n) {
        // TODO Auto-generated method stub
        return stringName(n.getDef());
    }

    /*
    public static String nameString(IdOrOpOrAnonymousName n) {
        final String last = n.accept(nameGetter);
        Option<APIName> odn = n.getApi();
        return odn.isSome() ? nameString(odn.unwrap()) + "." + last : last;
    }
    */

    public static String namesString(Iterable<? extends Name> names) {
        return IterUtil.toString(IterUtil.map(names, NameToStringFn), "", ", ", "");
    }


    /* getName *************************************************************/
    public static String getName(StaticParam param) {
        return param.accept(new NodeAbstractVisitor<String>() {
            public String forBoolParam(BoolParam p) {
                return p.getName().getText();
            }
            public String forDimParam(DimParam p) {
                return p.getName().getText();
            }
            public String forIntParam(IntParam p) {
                return p.getName().getText();
            }
            public String forNatParam(NatParam p) {
                return p.getName().getText();
            }
            public String forOpParam(OpParam p) {
                return nameString(p.getName());
            }
            public String forTypeParam(TypeParam p) {
                return p.getName().getText();
            }
            public String forUnitParam(UnitParam p) {
                return p.getName().getText();
            }
        });
    }

    private final static NodeAbstractVisitor<String> stringNameVisitor =
        new NodeAbstractVisitor<String>() {
        @Override
            public String forId(Id that) {
                return that.getText();
            }
            @Override
            public String forOp(Op that) {
               return OprUtil.fixityDecorator(that.getFixity(), that.getText());
            }
            @Override
            public String forEnclosing(Enclosing that) {
                String o = that.getOpen().accept(this);
                String c = that.getClose().accept(this);
                return o + " " + c;
            }
            @Override
        public String forDimDecl(DimDecl node) {
            return nameString(node.getDim());
        }
            @Override
        public String forUnitDecl(UnitDecl node) {
            List<Id> ids = node.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return nameString(ids.get(0));
        }
            @Override
        public String forFnAbsDeclOrDecl(FnAbsDeclOrDecl node) {
            return nameString(node.getName());
        }
            @Override
        public String forIdOrOpOrAnonymousName(IdOrOpOrAnonymousName node) {
            return nameString(node);
        }
            @Override
        public String forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl node) {
            return node.getName().getText();
        }
            @Override
        public String for_RewriteObjectExpr(_RewriteObjectExpr node) {
            return node.getGenSymName();
        }
            @Override
        public String forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl node) {
            return node.getName().getText();
        }
            @Override
        public String forTypeAlias(TypeAlias node) {
            return node.getName().getText();
        }
            @Override
        public String forOpRef(OpRef node) {
            return node.getOriginalName().accept(this);
        }
            @Override
        public String forVarRef(VarRef node) {
            return node.getVar().accept(this);
        }

            @Override
        public String defaultCase(Node node) {
            return node.getClass().getSimpleName();
        }
    };


    /* stringName **********************************************************/
    public static String stringName(Node the_node) {
        return the_node.accept(stringNameVisitor);
    }

    /* stringNames *********************************************************/
    public static IterableOnce<String> stringNames(LValue lv) {
        return lv.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forLValueBind(LValueBind d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forUnpastingBind(UnpastingBind d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forUnpastingSplit(UnpastingSplit d) {
                return new IterableOnceForLValueList(d.getElems());
            }
        });
    }

    public static IterableOnce<String> stringNames(AbsDeclOrDecl decl) {
        return decl.accept(new NodeAbstractVisitor<IterableOnce<String>>() {
            public IterableOnce<String> forDimDecl(DimDecl d) {
                return new UnitIterable<String>(d.getDim().getText());
            }
            public IterableOnce<String> forUnitDecl(UnitDecl d) {
            List<Id> ids = d.getUnits();
            if (ids.size() < 1)
                return bug("Unit declarations should have a name.");
            else return new UnitIterable<String>(nameString(ids.get(0)));
            }
            public IterableOnce<String> forFnExpr(FnExpr d) {
                return new UnitIterable<String>(nameString(d.getName()));
            }
            public IterableOnce<String> forFnAbsDeclOrDecl(FnAbsDeclOrDecl d) {
                return new UnitIterable<String>(nameString(d.getName()));
            }
            public IterableOnce<String> forGeneratedExpr(GeneratedExpr d) {
                return new UnitIterable<String>("GeneratedExpr");
            }
            public IterableOnce<String> forLetFn(LetFn d) {
                return new UnitIterable<String>(d.getClass().getSimpleName());
            }
            public IterableOnce<String> forLocalVarDecl(LocalVarDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
            public IterableOnce<String> forObjectAbsDeclOrDecl(ObjectAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> for_RewriteObjectExpr(_RewriteObjectExpr d) {
                return new UnitIterable<String>(d.getGenSymName());
            }
            public IterableOnce<String> forPropertyDecl(PropertyDecl d) {
                return d.getName().apply(new OptionVisitor<Id, IterableOnce<String>>() {
                    public IterableOnce<String> forSome(Id name) {
                        return new UnitIterable<String>(name.getText());
                    }
                    public IterableOnce<String> forNone() {
                        return new UnitIterable<String>("_");
                    }
                });
            }
            public IterableOnce<String> forTestDecl(TestDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forTraitAbsDeclOrDecl(TraitAbsDeclOrDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forTypeAlias(TypeAlias d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> forVarAbsDeclOrDecl(VarAbsDeclOrDecl d) {
                return new IterableOnceForLValueList(d.getLhs());
            }
            public IterableOnce<String> forGrammarDecl(GrammarDecl d) {
                return new UnitIterable<String>(d.getName().getText());
            }
            public IterableOnce<String> for_RewriteFnOverloadDecl(_RewriteFnOverloadDecl d) {
                return new UnitIterable<String>(nameString(d.getName()));
            }
        });
    }

    /* dump ****************************************************************/
    public static String dump(AbstractNode n) {
        try {
            StringBuffer sb = new StringBuffer();
            dump(sb, n);
            return sb.toString();
        } catch (Throwable ex) {
            return "Exception " + ex + " during dump";
        }
    }

    /**
     * @throws IOException
     */
    public static void dump(Appendable appendable, AbstractNode n) throws IOException {
        Printer p = new Printer(true, true, true);
        p.dump(n, appendable, 0);
    }

    public static <T> T NYI(String s) {
        return (T)bug("AST." + s + " NYI");
    }

    /* function ************************************************************/
    public static final Fn<Id, String> IdToStringFn = new Fn<Id, String>() {
            public String apply(Id x) {
                return stringName(x);
            }
        };

    public static final Fn<Name, String> NameToStringFn = new Fn<Name, String>() {
        public String apply(Name n) { return nameString(n); }
    };

    public static final Fn<String, Id> StringToIdFn = new Fn<String, Id>() {
            public Id apply(String x) {
                return new Id(new Span(), x);
            }
        };


    public static boolean isGetter(FnAbsDeclOrDecl decl) {
        for (Modifier mod : decl.getMods()) {
            if (mod instanceof ModifierGetter) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSetter(FnAbsDeclOrDecl decl) {
        for (Modifier mod : decl.getMods()) {
            if (mod instanceof ModifierSetter) {
                return true;
            }
        }
        return false;
    }

    /* for APIName ******************************************************/
    public static List<String> toStrings(APIName n) {
        return Useful.applyToAll(n.getIds(), IdToStringFn);
    }

    /* for TraitTypeWhere **************************************************/
    public static List<BaseType> getTypes(List<TraitTypeWhere> l) {
        List<BaseType> t = new ArrayList<BaseType>(l.size());
        for (TraitTypeWhere tw : l) {
            t.add(tw.getType());
        }
        return t;
    }

    /* for Type and StaticExpr **********************************************/
    public static boolean isExponentiation(Type type) {
        return (type instanceof ArrayType ||
                type instanceof MatrixType ||
                type instanceof ExponentType ||
                type instanceof ExponentDim);
    }
    public static boolean isExponentiation(IntExpr staticExpr) {
        return (staticExpr instanceof ExponentConstraint);
    }

    /* for String manipulation *********************************************/
    public static void validNumericLiteral(Span span, String numeral) {
        int numberOfDots = 0;
        for (int index = 0; index < numeral.length(); index++) {
            char c = numeral.charAt(index);
            if (Character.isLetter(c))
                syntaxError(span, "Syntax Error: a numeral contains " +
                            "letters and does not have a radix specifier.");
            if (c == '.') numberOfDots++;
        }
        if (numberOfDots > 1)
            syntaxError(span, "Syntax Error: a numeral contains more " +
                        "than one `.' character.");
    }

    public static void validNumericLiteral(Span span, String numeral,
                                           String radix) {
        int radixNumber = radix2Number(radix);
        if (radixNumber == -1)
            syntaxError(span, "Syntax Error: the radix of " +
                        "a numeral should be an integer from 2 to 16.");
        boolean sawUpperCase = false;
        boolean sawLowerCase = false;
        boolean sawAb = false;
        boolean sawXe = false;
        int numberOfDots = 0;
        for (int index = 0; index < numeral.length(); index++) {
            char c = numeral.charAt(index);
            if (c == '.') numberOfDots++;
            if (Character.isUpperCase(c)) {
                if (sawLowerCase)
                    syntaxError(span, "Syntax Error: a numeral " +
                                "contains both uppercase and lowercase letters.");
                else sawUpperCase = true;
            } else if (Character.isLowerCase(c)) {
                if (sawUpperCase)
                    syntaxError(span, "Syntax Error: a numeral " +
                                "contains both uppercase and lowercase letters.");
                else sawLowerCase = true;
            }
            if (radixNumber == 12) {
                if (!validDigitOrLetterIn12(c)
                    && c != '.' && c != '\'' && c != '\u202F') {
		    syntaxError(span, "Syntax Error: a numeral " +
                                "has radix 12 and contains letters other " +
                                "than A, B, X, E, a, b, x or e.");
		}
                if (c == 'A' || c == 'a' || c == 'B' || c == 'b') {
                    if (sawXe)
                        syntaxError(span, "Syntax Error: a numeral " +
                                    "has radix 12 and contains at least one " +
                                    "A, B, a or b and at least one X, E, x or e.");
                    else sawAb = true;
                } else if (c == 'X' || c == 'x' || c == 'E' || c == 'e') {
                    if (sawAb)
                        syntaxError(span, "Syntax Error: a numeral " +
                                    "has radix 12 and contains at least one " +
                                    "A, B, a or b and at least one X, E, x or e.");
                    else sawXe = true;
                }
            }
            // The numeral has a radix other than 12.
            else if (!validDigitOrLetter(c, radixNumber)
                     && c != '.' && c != '\'' && c != '\u202F') {
                syntaxError(span, "Syntax Error: a numeral has a radix " +
                            "specifier and contains a digit or letter that " +
                            "denotes a value greater than or equal to the " +
                            "numeral's radix.");
	    }
        }
        if (numberOfDots > 1)
            syntaxError(span, "Syntax Error: a numeral contains more " +
                        "than one `.' character.");
    }

    public static int radix2Number(String radix) {
        if (radix.equals("2") || radix.equals("TWO")) {
            return 2;
        } else if (radix.equals("3") || radix.equals("THREE")) {
            return 3;
        } else if (radix.equals("4") || radix.equals("FOUR")) {
            return 4;
        } else if (radix.equals("5") || radix.equals("FIVE")) {
            return 5;
        } else if (radix.equals("6") || radix.equals("SIX")) {
            return 6;
        } else if (radix.equals("7") || radix.equals("SEVEN")) {
            return 7;
        } else if (radix.equals("8") || radix.equals("EIGHT")) {
            return 8;
        } else if (radix.equals("9") || radix.equals("NINE")) {
            return 9;
        } else if (radix.equals("10") || radix.equals("TEN")) {
            return 10;
        } else if (radix.equals("11") || radix.equals("ELEVEN")) {
            return 11;
        } else if (radix.equals("12") || radix.equals("TWELVE")) {
            return 12;
        } else if (radix.equals("13") || radix.equals("THIRTEEN")) {
            return 13;
        } else if (radix.equals("14") || radix.equals("FOURTEEN")) {
            return 14;
        } else if (radix.equals("15") || radix.equals("FIFTEEN")) {
            return 15;
        } else if (radix.equals("16") || radix.equals("SIXTEEN")) {
            return 16;
        } else {
            /* radix is not valid. */
            return -1;
        }
    }

    private static boolean validDigitOrLetterIn12(char c) {
        if (Character.isLetter(c)) {
            switch (c) {
                case 'A':
                case 'a':
                case 'B':
                case 'b':
                case 'X':
                case 'x':
                case 'E':
                case 'e': { break; }
                default: {
                    /* c is not valid in radix 12. */
                    return false;
                }
            }
        }
        return true;
    }

    // radix is not 12.
    private static boolean validDigitOrLetter(char c, int radix) {
        if ((radix < 10 && Character.digit(c, radix) > -1) ||
            (radix >= 10 && Character.isDigit(c)))
            return true;
        switch (c) {
            case 'A':
            case 'a': {
                if (radix <= 10) return false;
                break;
            }
            case 'B':
            case 'b': {
                if (radix <= 11) return false;
                break;
            }
            case 'C':
            case 'c': {
                if (radix <= 12) return false;
                break;
            }
            case 'D':
            case 'd': {
                if (radix <= 13) return false;
                break;
            }
            case 'E':
            case 'e': {
                if (radix <= 14)
                    return false;
                break;
            }
            case 'F':
            case 'f': {
                if (radix <= 15) return false;
                break;
            }
            default: {
                /* c is not valid in a numeral of the radix. */
                return false;
            }
        }
        return true;
    }

    public static String nameString(BoolRef vre) {
        return nameString(vre.getName());
    }
    public static String nameString(IntRef vre) {
        return nameString(vre.getName());
    }
}
