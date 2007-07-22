/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
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

/*
 * Fortress functional header clauses.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser;
import java.util.List;

import com.sun.fortress.nodes.Contract;
import com.sun.fortress.useful.Option;
import com.sun.fortress.nodes.TypeRef;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.MagicNumbers;

public class FnHeaderClause {

    private Option<List<TypeRef>> throwsClause;
    private Option<List<WhereClause>> whereClause;
    private Option<Contract> contractClause;
    private Option<TypeRef> returnType;

    public FnHeaderClause(Option<List<TypeRef>> throwsClause,
                          Option<List<WhereClause>> whereClause,
                          Option<Contract> contractClause,
                          Option<TypeRef> returnType) {
        this.throwsClause = throwsClause;
        this.whereClause = whereClause;
        this.contractClause = contractClause;
        this.returnType = returnType;
    }

    public Option<List<TypeRef>> getThrowsClause() {
        return throwsClause;
    }

    public Option<List<WhereClause>> getWhereClause() {
        return whereClause;
    }

    public Option<Contract> getContractClause() {
        return contractClause;
    }

    public Option<TypeRef> getReturnType() {
        return returnType;
    }

    public int hashCode() {
        return throwsClause.hashCode() + whereClause.hashCode()
            * MagicNumbers.n + contractClause.hashCode() * MagicNumbers.t
            * returnType.hashCode() * MagicNumbers.d;
    }

    public boolean equals(Object o) {
        if (o.getClass().equals(this.getClass())) {
            FnHeaderClause fhc = (FnHeaderClause) o;
            return throwsClause.equals(fhc.getThrowsClause())
                && whereClause.equals(fhc.getWhereClause())
                && contractClause.equals(fhc.getContractClause())
                && returnType.equals(fhc.getReturnType());
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (throwsClause.isPresent()) {
            sb.append("throws { ");
            sb.append(throwsClause.getVal());
            sb.append(" } ");
        }
        if (whereClause.isPresent()) {
            sb.append("where { ");
            sb.append(whereClause.getVal());
            sb.append(" } ");
        }
        if (contractClause.isPresent()) {
            sb.append(contractClause.getVal());
        }
        if (returnType.isPresent()) {
            sb.append(":");
            sb.append(returnType.getVal());
        }
        return sb.toString();
    }
}
