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

package com.sun.fortress.tools;

import java.util.List;
import com.sun.fortress.nodes.*;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;
import edu.rice.cs.plt.iter.IterUtil;

import static com.sun.fortress.exceptions.InterpreterBug.bug;

public class FortressAstToConcrete extends NodeDepthFirstVisitor<String> {

    /* Possible improvements **************************************************/
    /* 1. Add newlines and indentation
       2. Handle "parenthesized" field
       3. Handle syntax abstraction nodes.
     */

    /* utility methods ********************************************************/
    /* returns a string beginning with 'kind' followed by a sequence of elements
       in 'list' separated by commas and  enclosed by '{' and '}'
     */
    private String inCurlyBraces(String kind, List<String> list) {
        StringBuilder s = new StringBuilder();
        s.append( kind );
        s.append( "{ " );
        for ( String elem : IterUtil.skipLast(list) ){
            s.append( elem ).append( ", " );
        }
        s.append( IterUtil.last(list) );
        s.append( " }" );
        return s.toString();
    }

    /* returns a string beginning with 'kind' followed by a sequence of elements
       in 'list' separated by commas and  enclosed by '[\' and '\]'
     */
    private String inOxfordBrackets(String kind, List<String> list) {
        StringBuilder s = new StringBuilder();
        s.append( kind );
        s.append( "[\\ " );
        for ( String elem : IterUtil.skipLast(list) ){
            s.append( elem ).append( ", " );
        }
        s.append( IterUtil.last(list) );
        s.append( " \\]" );
        return s.toString();
    }

    /* returns a string of a sequence of elements
       in 'list' separated by commas and  enclosed by '(' and ')'
     */
    private String inParentheses(List<String> list) {
        StringBuilder s = new StringBuilder();
        s.append( "(" );
        for ( String elem : IterUtil.skipLast(list) ){
            s.append( elem ).append( ", " );
        }
        s.append( IterUtil.last(list) );
        s.append( ")" );
        return s.toString();
    }

    private String join( List<String> all, String sep ){
        StringBuilder s = new StringBuilder();

        int index = 0;
        for ( String element : all ){
            s.append( element );
            if ( index < all.size() - 1 ){
                s.append( sep );
            }
            index += 1;
        }

        return s.toString();
    }

    /* visit nodes ************************************************************/
    @Override public String forComponentOnly(Component that, String name_result,
                                             List<String> imports_result,
                                             List<String> exports_result,
                                             List<String> decls_result) {
        StringBuilder s = new StringBuilder();
        if (that.is_native())
            s.append( "native " );
        s.append( "component " ).append( name_result ).append( "\n" );
        for ( String import_ : imports_result ){
            s.append( import_ ).append( "\n" );
        }
        for ( String export_ : exports_result ){
            s.append( export_ ).append( "\n" );
        }
        for ( String decl_ : decls_result ){
            s.append( decl_ ).append( "\n" );
        }
        s.append( "end" );
        return s.toString();
    }

//    @Override public String forApiOnly(Api that,
//    @Override public String forImportStarOnly(ImportStar that,
//    @Override public String forImportNamesOnly(ImportNames that,
//    @Override public String forImportApiOnly(ImportApi that,
//    @Override public String forAliasedSimpleNameOnly(AliasedSimpleName that,
//    @Override public String forAliasedAPINameOnly(AliasedAPIName that,

    @Override public String forExportOnly(Export that, List<String> apis_result) {
        StringBuilder s = new StringBuilder();
        if (apis_result.size() == 0)
            return bug(that, "An export statement should have at least one API name.");
        else
            return inCurlyBraces("export ", apis_result);
    }

//    @Override public String forAbsTraitDeclOnly( that,
//    @Override public String forTraitDeclOnly( that,
//    @Override public String forAbsObjectDeclOnly( that,
//    @Override public String forObjectDeclOnly( that,
//    @Override public String forAbsVarDeclOnly( that,
//    @Override public String forVarDeclOnly( that,
//    @Override public String forLValueBindOnly( that,
//    @Override public String forUnpastingBindOnly( that,
//    @Override public String forUnpastingSplitOnly( that,
//    @Override public String forAbsFnDeclOnly( that,

    @Override public String forFnDefOnly(FnDef that,
                                         List<String> mods_result,
                                         String name_result,
                                         List<String> staticParams_result,
                                         List<String> params_result,
                                         Option<String> returnType_result,
                                         Option<List<String>> throwsClause_result,
                                         String where_result,
                                         String contract_result,
                                         String body_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        if ( ! staticParams_result.isEmpty() )
            s.append( inOxfordBrackets("", staticParams_result) );
        s.append( inParentheses(params_result) );
        if (returnType_result.isSome())
            s.append( ": " ).append( returnType_result.unwrap() );
        if (throwsClause_result.isSome()) {
            List<String> throws_ = throwsClause_result.unwrap();
            if ( ! throws_.isEmpty() )
                s.append( inCurlyBraces("throws ", throws_) );
        }
        s.append( where_result );
        s.append( contract_result );
        s.append( " = " );
        s.append( body_result );
        return s.toString();
    }

    @Override public String forNormalParamOnly(NormalParam that,
                                               List<String> mods_result,
                                               String name_result,
                                               Option<String> type_result,
                                               Option<String> defaultExpr_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        if (type_result.isSome())
            s.append( ": " ).append( type_result.unwrap() );
        if (defaultExpr_result.isSome())
            s.append( "=").append( defaultExpr_result );
        return s.toString();
    }

    @Override public String forVarargsParamOnly(VarargsParam that,
                                                List<String> mods_result,
                                                String name_result,
                                                String type_result) {
        StringBuilder s = new StringBuilder();
        for ( String mod : mods_result ){
            s.append( mod ).append( " " );
        }
        s.append( name_result );
        s.append( ": " );
        s.append( type_result );
        s.append( "..." );
        return s.toString();
    }

//    @Override public String forDimDeclOnly( that,
//    @Override public String forUnitDeclOnly( that,
//    @Override public String forTestDeclOnly( that,
//    @Override public String forPropertyDeclOnly( that,
//    @Override public String forAsExprOnly( that,
//    @Override public String forAsIfExprOnly( that,
//    @Override public String forAssignmentOnly( that,
//    @Override public String forBlockOnly( that,
//    @Override public String forCaseExprOnly( that,
//    @Override public String forDoOnly( that,
//    @Override public String forForOnly( that,
//    @Override public String forIfOnly( that,
//    @Override public String forLabelOnly( that,
//    @Override public String forObjectExprOnly( that,
//    @Override public String for_RewriteObjectExprOnly( that,
//    @Override public String forTryOnly( that,
//    @Override public String forTupleExprOnly( that,
//    @Override public String forArgExprOnly( that,
//    @Override public String forTypecaseOnly( that,
//    @Override public String forWhileOnly( that,
//    @Override public String forAccumulatorOnly( that,
//    @Override public String forArrayComprehensionOnly( that,
//    @Override public String forAtomicExprOnly( that,
//    @Override public String forExitOnly( that,
//    @Override public String forSpawnOnly( that,
//    @Override public String forThrowOnly( that,
//    @Override public String forTryAtomicExprOnly( that,
//    @Override public String forFnExprOnly( that,
//    @Override public String forLetFnOnly( that,
//    @Override public String forLocalVarDeclOnly( that,
//    @Override public String forGeneratedExprOnly( that,
//    @Override public String forSubscriptExprOnly( that,
//    @Override public String forFloatLiteralExprOnly( that,
//    @Override public String forIntLiteralExprOnly( that,
//    @Override public String forCharLiteralExprOnly( that,

    @Override public String forStringLiteralExprOnly(StringLiteralExpr that) {
        return "\"" + that.getText() + "\"";
    }

    @Override public String forVoidLiteralExprOnly(VoidLiteralExpr that) {
        return "()";
    }

    @Override public String forVarRefOnly(VarRef that,
                                          String var_result) {
        return var_result;
    }

//    @Override public String for_RewriteObjectRefOnly( that,
//    @Override public String forFieldRefOnly( that,
//    @Override public String for_RewriteFieldRefOnly( that,
//    @Override public String forFnRefOnly( that,
//    @Override public String for_RewriteFnRefOnly( that,

    /* TODO: BIG Op StaticArgs
             LeftEncloser StaticArgs ExprList RightEncloser
     */
    @Override public String forOpRefOnly(OpRef that,
                                         String originalName_result,
                                         List<String> ops_result,
                                         List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append( originalName_result );

        return s.toString();
    }

    @Override public String forLooseJuxtOnly(LooseJuxt that,
                                             String multiJuxt_result,
                                             String infixJuxt_result,
                                             List<String> exprs_result) {
        StringBuilder s = new StringBuilder();
        for ( String expr : exprs_result ){
            s.append( expr );
            s.append( " " );
        }
        return s.toString();
    }

    @Override public String forTightJuxtOnly(TightJuxt that,
                                             String multiJuxt_result,
                                             String infixJuxt_result,
                                             List<String> exprs_result) {
        StringBuilder s = new StringBuilder();
        if ( exprs_result.isEmpty() )
            return bug(that, "A tight juxtaposition expression should have " +
                       "at least two subexpressions.");
        s.append(IterUtil.first(exprs_result));
        for ( String expr : IterUtil.skipFirst(exprs_result) ){
            s.append( "(" );
            s.append( expr );
            s.append( ")" );
        }
        return s.toString();
    }

//    @Override public String for_RewriteFnAppOnly( that,
//    @Override public String forOpExprOnly( that,
//    @Override public String forAmbiguousMultifixOpExprOnly( that,
//    @Override public String forChainExprOnly( that,
//    @Override public String forCoercionInvocationOnly( that,
//    @Override public String forMethodInvocationOnly( that,
//    @Override public String forMathPrimaryOnly( that,
//    @Override public String forArrayElementOnly( that,
//    @Override public String forArrayElementsOnly( that,
//    @Override public String forExponentTypeOnly( that,
//    @Override public String forBaseDimOnly( that,
//    @Override public String forDimRefOnly( that,
//    @Override public String forProductDimOnly( that,
//    @Override public String forQuotientDimOnly( that,
//    @Override public String forExponentDimOnly( that,
//    @Override public String forOpDimOnly( that,
//    @Override public String forAnyTypeOnly( that,
//    @Override public String forBottomTypeOnly( that,

    @Override public String forVarTypeOnly(VarType that, String name_result) {
        return name_result;
    }

//    @Override public String forTraitTypeOnly( that,
//    @Override public String for_RewriteGenericSingletonTypeOnly( that,
//    @Override public String forArrayTypeOnly( that,
//    @Override public String forMatrixTypeOnly( that,
//    @Override public String forTaggedDimTypeOnly( that,
//    @Override public String forTaggedUnitTypeOnly( that,
//    @Override public String forTupleTypeOnly( that,
//    @Override public String forVarargTupleTypeOnly( that,

    @Override public String forVoidTypeOnly(VoidType that) {
        return "()";
    }

//    @Override public String forArrowTypeOnly( that,
//    @Override public String for_RewriteGenericArrowTypeOnly( that,
//    @Override public String for_InferenceVarTypeOnly( that,
//    @Override public String forIntersectionTypeOnly( that,
//    @Override public String forUnionTypeOnly( that,
//    @Override public String forFixedPointTypeOnly( that,
//    @Override public String forLabelTypeOnly( that,
//    @Override public String forDomainOnly( that,
//    @Override public String forEffectOnly( that,
//    @Override public String forTypeArgOnly( that,
//    @Override public String forIntArgOnly( that,
//    @Override public String forBoolArgOnly( that,
//    @Override public String forOpArgOnly( that,
//    @Override public String forDimArgOnly( that,
//    @Override public String forUnitArgOnly( that,
//    @Override public String forNumberConstraintOnly( that,
//    @Override public String forIntRefOnly( that,
//    @Override public String forSumConstraintOnly( that,
//    @Override public String forMinusConstraintOnly( that,
//    @Override public String forProductConstraintOnly( that,
//    @Override public String forExponentConstraintOnly( that,
//    @Override public String forBoolConstantOnly( that,
//    @Override public String forBoolRefOnly( that,
//    @Override public String forNotConstraintOnly( that,
//    @Override public String forOrConstraintOnly( that,
//    @Override public String forAndConstraintOnly( that,
//    @Override public String forImpliesConstraintOnly( that,
//    @Override public String forBEConstraintOnly( that,
//    @Override public String forUnitRefOnly( that,
//    @Override public String forProductUnitOnly( that,
//    @Override public String forQuotientUnitOnly( that,
//    @Override public String forExponentUnitOnly( that,

    @Override public String forWhereClauseOnly(WhereClause that,
                                               List<String> bindings_result,
                                               List<String> constraints_result) {
        if ( bindings_result.isEmpty() && constraints_result.isEmpty() )
            return "";
        StringBuilder s = new StringBuilder();
        s.append( "where " );
        if ( ! bindings_result.isEmpty() )
            s.append( inOxfordBrackets("", bindings_result) );
        if ( ! constraints_result.isEmpty() )
            s.append( inCurlyBraces("", constraints_result) );
        return s.toString();
    }

//    @Override public String forWhereTypeOnly( that,
//    @Override public String forWhereNatOnly( that,
//    @Override public String forWhereIntOnly( that,
//    @Override public String forWhereBoolOnly( that,
//    @Override public String forWhereUnitOnly( that,
//    @Override public String forWhereExtendsOnly( that,
//    @Override public String forTypeAliasOnly( that,
//    @Override public String forWhereCoercesOnly( that,
//    @Override public String forWhereWidensOnly( that,
//    @Override public String forWhereWidensCoercesOnly( that,
//    @Override public String forWhereEqualsOnly( that,
//    @Override public String forUnitConstraintOnly( that,
//    @Override public String forLEConstraintOnly( that,
//    @Override public String forLTConstraintOnly( that,
//    @Override public String forGEConstraintOnly( that,
//    @Override public String forGTConstraintOnly( that,
//    @Override public String forIEConstraintOnly( that,
//    @Override public String forBoolConstraintExprOnly( that,

    @Override public String forContractOnly(Contract that,
                                            Option<List<String>> requires_result,
                                            Option<List<String>> ensures_result,
                                            Option<List<String>> invariants_result) {
        StringBuilder s = new StringBuilder();

        if (requires_result.isSome()) {
            List<String> requires = requires_result.unwrap();
            if ( ! requires.isEmpty() )
                s.append( inCurlyBraces("requires ", requires) );
        }

        if (ensures_result.isSome()) {
            List<String> ensures = ensures_result.unwrap();
            if ( ! ensures.isEmpty() )
                s.append( inCurlyBraces("ensures ", ensures) );
        }

        if (invariants_result.isSome()) {
            List<String> invariants = invariants_result.unwrap();
            if ( ! invariants.isEmpty() )
                s.append( inCurlyBraces("invariant ", invariants) );
        }

        return s.toString();
    }

//    @Override public String forEnsuresClauseOnly( that,

    @Override public String forModifierAbstractOnly(ModifierAbstract that) {
        return "abstract";
    }

    @Override public String forModifierAtomicOnly(ModifierAtomic that) {
        return "atomic";
    }

    @Override public String forModifierGetterOnly(ModifierGetter that) {
        return "getter";
    }

    @Override public String forModifierHiddenOnly(ModifierHidden that) {
        return "hidden";
    }

    @Override public String forModifierIOOnly(ModifierIO that) {
        return "io";
    }

    @Override public String forModifierOverrideOnly(ModifierOverride that) {
        return "override";
    }

    @Override public String forModifierPrivateOnly(ModifierPrivate that) {
        return "private";
    }

    @Override public String forModifierSettableOnly(ModifierSettable that) {
        return "settable";
    }

    @Override public String forModifierSetterOnly(ModifierSetter that) {
        return "setter";
    }

    @Override public String forModifierTestOnly(ModifierTest that) {
        return "test";
    }

    @Override public String forModifierTransientOnly(ModifierTransient that) {
        return "transient";
    }

    @Override public String forModifierValueOnly(ModifierValue that) {
        return "value";
    }

    @Override public String forModifierVarOnly(ModifierVar that) {
        return "var";
    }

    @Override public String forModifierWidensOnly(ModifierWidens that) {
        return "widens";
    }

    @Override public String forModifierWrappedOnly(ModifierWrapped that) {
        return "wrapped";
    }

//    @Override public String forOpParamOnly( that,
//    @Override public String forBoolParamOnly( that,
//    @Override public String forDimParamOnly( that,
//    @Override public String forIntParamOnly( that,
//    @Override public String forNatParamOnly( that,
//    @Override public String forTypeParamOnly( that,
//    @Override public String forUnitParamOnly( that,

    @Override public String forAPINameOnly(APIName that, List<String> ids_result) {
        StringBuilder s = new StringBuilder();
        if (IterUtil.isEmpty(ids_result))
            return s.toString();
        else {
            for ( String id : IterUtil.skipLast(ids_result) ){
                s.append( id ).append( "." );
            }
            s.append( IterUtil.last(ids_result) );
            return s.toString();
        }
    }

    @Override public String forIdOnly(Id that, Option<String> api_result) {
        StringBuilder s = new StringBuilder();
        if ( api_result.isSome() )
            s.append( api_result.unwrap() ).append( "." );
        s.append( that.getText() );
        return s.toString();
    }

    @Override public String forOpOnly(Op that,
                                      Option<String> api_result,
                                      Option<String> fixity_result) {
        return that.getText();
    }

//    @Override public String forEnclosingOnly( that,
//    @Override public String forAnonymousFnNameOnly( that,
//    @Override public String forConstructorFnNameOnly( that,
//    @Override public String forArrayComprehensionClauseOnly( that,
//    @Override public String forKeywordExprOnly( that,
//    @Override public String forCaseClauseOnly( that,
//    @Override public String forCatchOnly( that,
//    @Override public String forCatchClauseOnly( that,
//    @Override public String forDoFrontOnly( that,
//    @Override public String forIfClauseOnly( that,
//    @Override public String forTypecaseClauseOnly( that,
//    @Override public String forExtentRangeOnly( that,
//    @Override public String forGeneratorClauseOnly( that,
//    @Override public String forVarargsExprOnly( that,
//    @Override public String forKeywordTypeOnly( that,
//    @Override public String forTraitTypeWhereOnly( that,
//    @Override public String forIndicesOnly( that,
//    @Override public String forParenthesisDelimitedMIOnly( that,
//    @Override public String forNonParenthesisDelimitedMIOnly( that,
//    @Override public String forExponentiationMIOnly( that,
//    @Override public String forSubscriptingMIOnly( that,

    @Override public String forInFixityOnly(InFixity that) {
        return "";
    }

    @Override public String forPreFixityOnly(PreFixity that) {
        return "";
    }

    @Override public String forPostFixityOnly(PostFixity that) {
        return "";
    }

    @Override public String forNoFixityOnly(NoFixity that) {
        return "";
    }

    @Override public String forMultiFixityOnly(MultiFixity that) {
        return "";
    }

    @Override public String forEnclosingFixityOnly(EnclosingFixity that) {
        return "";
    }

    @Override public String forBigFixityOnly(BigFixity that) {
        return "";
    }

//    @Override public String forLinkOnly( that,
}
