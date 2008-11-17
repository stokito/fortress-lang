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

package com.sun.fortress.compiler.typechecker;

import java.util.Iterator;
import edu.rice.cs.plt.lambda.Lambda;
import edu.rice.cs.plt.iter.SequenceIterator;
import edu.rice.cs.plt.collect.OneToOneRelation;
import edu.rice.cs.plt.collect.IndexedOneToOneRelation;
import com.sun.fortress.nodes.*;
import com.sun.fortress.nodes_util.NodeFactory;
import com.sun.fortress.nodes_util.NodeUtil;
import com.sun.fortress.nodes_util.Span;

/**
 * Renames inference variables to a canonical form, in which each unique variable encountered
 * by an instance of the translator is given a unique (greek-letter) name.  The mapping to
 * canonical names is recorded, so that it can later be reversed.  This allows assertions
 * about relationships between types to be treated as assertions about type <em>patterns</em>,
 * where any renaming of inference variables can be treated as equivalent.
 */
public class InferenceVarTranslator {

    private final Iterator<Character> _canonicalNames;
    private final OneToOneRelation<_InferenceVarType, _InferenceVarType> _replacements;
    private final NodeVisitor<Node> _canonicalUpdater;
    private final NodeVisitor<Node> _reverseUpdater;

    public InferenceVarTranslator() {
        // sequence starts with alpha = 03b1
        _canonicalNames = new SequenceIterator<Character>('\u03b1', CHAR_SEQUENCE);
        _replacements = new IndexedOneToOneRelation<_InferenceVarType, _InferenceVarType>();

        _canonicalUpdater = new NodeUpdateVisitor() {
            @Override public Node for_InferenceVarType(_InferenceVarType var) {
                if (!_replacements.containsFirst(var)) {
                    _replacements.add(var, nextCanonicalVar());
                }
                return _replacements.value(var);
            }
        };

        _reverseUpdater = new NodeUpdateVisitor() {
            @Override public Node for_InferenceVarType(_InferenceVarType var) {
                if (!_replacements.containsSecond(var)) {
                    _replacements.add(NodeFactory.make_InferenceVarType(var.getSpan()), var);
                }
                return _replacements.antecedent(var);
            }

        };
    }

    private static final Lambda<Character, Character> CHAR_SEQUENCE =
        new Lambda<Character, Character>() {
        public Character value(Character prev) {
            switch (prev) {
                case '\u03c1': return '\u03c3'; // rho -> sigma
                case '\u03c9': return '\u0391'; // omega -> Alpha
                case '\u03a1': return '\u03a3'; // Rho -> Sigma
                case '\u03a9': return 'a'; // Omega -> a
                case 'z': return 'A'; // z -> A
                case 'Z': return '\u24d0'; // Z -> circled a
                case '\u24e9': return '\u24b6'; // circled z -> circled A
                case '\u24cf':
                    throw new IllegalStateException("Set of names is exhausted");
                default: return (char) (prev+1);
            }
        }
    };

    private _InferenceVarType nextCanonicalVar() {
        // If it becomes a concern, we can replace the use of Characters as identifiers
        // with instances of a custom class, thus preventing name clashes with variables
        // created elsewhere that wrap Characters (currently, other instances just wrap
        // raw Objects)
        return new _InferenceVarType(NodeFactory.makeSpan("blame InferenceVarTranslator.nextCanonicalVar"), _canonicalNames.next());
    }

    public Type canonicalizeVars(Type t) {
        return (Type) t.accept(_canonicalUpdater);
    }

    public Type revertVars(Type t) {
        return (Type) t.accept(_reverseUpdater);
    }

    public Lambda<Type, Type> canonicalSubstitution() {
        return new Lambda<Type, Type>() {
            public Type value(Type t) { return canonicalizeVars(t); }
        };
    }

    public Lambda<Type, Type> revertingSubstitution() {
        return new Lambda<Type, Type>() {
            public Type value(Type t) { return revertVars(t); }
        };
    }

}
