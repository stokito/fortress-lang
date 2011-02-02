/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

/*
 * Fortress functional header clauses.
 * Fortress AST node local to the Rats! com.sun.fortress.interpreter.parser.
 */
package com.sun.fortress.parser_util;

import com.sun.fortress.nodes.BaseType;
import com.sun.fortress.nodes.Contract;
import com.sun.fortress.nodes.Type;
import com.sun.fortress.nodes.WhereClause;
import com.sun.fortress.useful.MagicNumbers;
import edu.rice.cs.plt.tuple.Option;

import java.util.List;

public class FnHeaderClause {

    private Option<List<Type>> throwsClause;
    private Option<WhereClause> whereClause;
    private Option<Contract> contractClause;
    private Option<Type> returnType;

    public FnHeaderClause(Option<List<Type>> throwsClause,
                          Option<WhereClause> whereClause,
                          Option<Contract> contractClause,
                          Option<Type> returnType) {
        this.throwsClause = throwsClause;
        this.whereClause = whereClause;
        this.contractClause = contractClause;
        this.returnType = returnType;
    }

    public Option<List<Type>> getThrowsClause() {
        return throwsClause;
    }

    public Option<WhereClause> getWhereClause() {
        return whereClause;
    }

    public Option<Contract> getContractClause() {
        return contractClause;
    }

    public Option<Type> getReturnType() {
        return returnType;
    }

    public int hashCode() {
        return throwsClause.hashCode() + whereClause.hashCode() * MagicNumbers.n +
               contractClause.hashCode() * MagicNumbers.t * returnType.hashCode() * MagicNumbers.d;
    }

    public boolean equals(Object o) {
        if (o == null) return false;
        if (o.getClass().equals(this.getClass())) {
            FnHeaderClause fhc = (FnHeaderClause) o;
            return throwsClause.equals(fhc.getThrowsClause()) && whereClause.equals(fhc.getWhereClause()) &&
                   contractClause.equals(fhc.getContractClause()) && returnType.equals(fhc.getReturnType());
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (throwsClause.isSome()) {
            sb.append("throws { ");
            sb.append(throwsClause.unwrap());
            sb.append(" } ");
        }
        sb.append("where...");
        if (contractClause.isSome()) {
            sb.append(contractClause.unwrap());
        }
        if (returnType.isSome()) {
            sb.append(":");
            sb.append(returnType.unwrap());
        }
        return sb.toString();
    }
}
