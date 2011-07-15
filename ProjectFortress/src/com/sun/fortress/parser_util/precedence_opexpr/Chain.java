/*******************************************************************************
 Copyright 2008,2009, Oracle and/or its affiliates.
 All rights reserved.


 Use is subject to license terms.

 This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.parser_util.precedence_opexpr;

import com.sun.fortress.parser_util.precedence_resolver.ExprOpPair;
import com.sun.fortress.useful.PureList;


/**
 * Class Chain, a component of the InfixFrame composite hierarchy.
 * Note: null is not allowed as a value for any field.
 */
public abstract class Chain extends Object implements InfixFrame {
    private final PureList<ExprOpPair> _links;
    private int _hashCode;
    private boolean _hasHashCode = false;

    /**
     * Constructs a Chain.
     *
     * @throws java.lang.IllegalArgumentException
     *          if any parameter to the constructor is null.
     */
    public Chain(PureList<ExprOpPair> in_links) {
        super();

        if (in_links == null) {
            throw new java.lang.IllegalArgumentException(
                    "Parameter 'links' to the Chain constructor was null. This class may not have null field values.");
        }
        _links = in_links;
    }

    public PureList<ExprOpPair> getLinks() {
        return _links;
    }

    public abstract <RetType> RetType accept(InfixFrameVisitor<RetType> visitor);

    public abstract void accept(InfixFrameVisitor_void visitor);

    public abstract void outputHelp(TabPrintWriter writer);

    protected abstract int generateHashCode();

    public final int hashCode() {
        if (!_hasHashCode) {
            _hashCode = generateHashCode();
            _hasHashCode = true;
        }
        return _hashCode;
    }
}
