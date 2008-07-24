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

public class FortressAstToConcrete extends NodeDepthFirstVisitor<String> {

    @Override public String forComponentOnly(Component that, String name_result, List<String> imports_result, List<String> exports_result, List<String> decls_result) {
        StringBuilder s = new StringBuilder();
        s.append( "component " ).append( name_result ).append( "\n" );
        for ( String import_ : imports_result ){
            s.append( import_ ).append( "\n" );
        }
        for ( String export_ : exports_result ){
            s.append( export_ ).append( "\n" );
        }
        for ( String decls : decls_result ){
            s.append( decls ).append( "\n" );
        }
        s.append( "end" );
        return s.toString();
    }
    
    @Override public String forIdOnly(Id that, Option<String> api_result) {
        return that.getText();
    }
    
    @Override public String forAPINameOnly(APIName that, List<String> ids_result) {
        StringBuilder s = new StringBuilder();

        for ( String id : ids_result ){
            s.append( id );
        }

        return s.toString();
    }
    
    @Override public String forExportOnly(Export that, List<String> apis_result) {
        StringBuilder s = new StringBuilder();
        s.append( "export ");

        for ( String id : apis_result ){
            s.append( id );
        }

        return s.toString();

    }
    
    @Override public String forVarTypeOnly(VarType that, String name_result) {
        return name_result;
    }
    
    @Override public String forVarargsParamOnly(VarargsParam that, List<String> mods_result, String name_result, String type_result) {
        StringBuilder s = new StringBuilder();

        for ( String mod : mods_result ){
            s.append( mod );
        }

        s.append( name_result );
        s.append( ":" );
        s.append( type_result );
        s.append( "..." );

        return s.toString();
    }
    
    @Override public String forWhereClauseOnly(WhereClause that, List<String> bindings_result, List<String> constraints_result) {
        StringBuilder s = new StringBuilder();

        s.append( "where" );

        return s.toString();
    }
    
    @Override public String forContractOnly(Contract that, Option<List<String>> requires_result, Option<List<String>> ensures_result, Option<List<String>> invariants_result) {
        StringBuilder s = new StringBuilder();

        s.append( "contract" );
        
        return s.toString();
    }
    
    @Override public String forNormalParamOnly(NormalParam that, List<String> mods_result, String name_result, Option<String> type_result, Option<String> defaultExpr_result) {
        StringBuilder s = new StringBuilder();

        s.append( name_result );

        return s.toString();
    }
    
    @Override public String forMultiFixityOnly(MultiFixity that) {
        return "";
    }
    
    @Override public String forOpOnly(Op that, Option<String> api_result, Option<String> fixity_result) {
        StringBuilder s = new StringBuilder();

        if ( api_result.isSome() ){
            s.append( api_result.unwrap() );
        }

        return s.toString();
    }
    
    @Override public String forOpRefOnly(OpRef that, String originalName_result, List<String> ops_result, List<String> staticArgs_result) {
        StringBuilder s = new StringBuilder();

        s.append( originalName_result );

        return s.toString();
    }
    
    @Override public String forInFixityOnly(InFixity that) {
        return "";
    }
    
    @Override public String forVarRefOnly(VarRef that, String var_result) {
        return var_result;
    }
    
    @Override public String forStringLiteralExprOnly(StringLiteralExpr that) {
        return "\"" + that.getText() + "\"";
    }
    
    @Override public String forLooseJuxtOnly(LooseJuxt that, String multiJuxt_result, String infixJuxt_result, List<String> exprs_result) {
        StringBuilder s = new StringBuilder();

        for ( String expr : exprs_result ){
            s.append( expr );
            s.append( " " );
        }

        return s.toString();
    }
    
    @Override public String forFnDefOnly(FnDef that, List<String> mods_result, String name_result, List<String> staticParams_result, List<String> params_result, Option<String> returnType_result, Option<List<String>> throwsClause_result, String where_result, String contract_result, String body_result) {
        StringBuilder s = new StringBuilder();

        s.append( name_result );
        if ( ! staticParams_result.isEmpty() ){
            s.append("\\[");
            for ( String staticParam : staticParams_result ){
                s.append( staticParam ).append( "," );
            }
            s.append("\\]");
        }
        s.append( "(" );
        s.append( join(params_result, ",") );
        s.append( ")" );
        s.append( " = " );
        s.append( body_result );

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
}
