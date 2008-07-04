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

package com.sun.fortress.astgen;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types;
import edu.rice.cs.astgen.Types.PrimitiveName;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.Pair;

public class TemplateGapClass extends NodeClass {
    private TypeName _superClass;

    public TemplateGapClass(String name, List<Field> fields, TypeName superClass, List<TypeName> interfaces) {
        super(name, false, fields, superClass, interfaces);
    }

    public void output(ASTModel ast, Iterable<CodeGenerator> gens) {
        TabPrintWriter writer = ast.options().createJavaSourceInOutDir(_name);

        // Class header
        writer.startLine("/**");
        writer.startLine(" * Class " + _name + ", a component of the ");
        writer.print("ASTGen-generated composite hierarchy.");

        if (!ast.options().allowNulls) {
            writer.startLine(" * Note: null is not allowed as a value for any field.");
        }

        writer.startLine(" * @version  Generated automatically by ASTGen at ");
        writer.print(new Date());
        writer.startLine(" */");

        writer.startLine("public class " + _name + " extends " + _superClass.name());
        if (_interfaces.size() > 0) {
            writer.print(" implements ");
            writer.print(IterUtil.toString(IterUtil.map(_interfaces, Types.GET_NAME), "", ", ", ""));
        }
        writer.print(" {");
        writer.indent();

        Option<NodeType> parent = ast.parent(this);
        Iterable<Field> allFields = allFields(ast);
        Iterable<Field> declaredFields = declaredFields(ast);

        // Fields for this class
        for (Field f : declaredFields(ast)) {
            writer.startLine(f.getFieldDefinition());
        }

        writer.println(); // skip line after fields

        // Constructors
        Set<List<String>> constructorTypes = new HashSet<List<String>>();
        List<String> mainConstructorType = new LinkedList<String>();
        for (Field f : allFields) { mainConstructorType.add(f.type().erasedName()); }
        constructorTypes.add(mainConstructorType);
        _outputMainConstructor(writer, allFields, declaredFields, ast.options().allowNulls);

        //        List<Iterable<Pair<Field, Boolean>>> constructorParams = new LinkedList<Iterable<Pair<Field, Boolean>>>();
        //        for (Field f : allFields) {
        //          if (f.defaultValue().isNone()) { constructorParams.add(IterUtil.singleton(Pair.make(f, true))); }
        //          else { constructorParams.add(IterUtil.make(Pair.make(f, true), Pair.make(f, false))); }
        //        }
        //        
        //        for (Iterable<Pair<Field, Boolean>> params : IterUtil.cross(constructorParams)) {
        //          List<String> ts = _erasedTypesOfFields(params);
        //          if (!constructorTypes.contains(ts)) {
        //            constructorTypes.add(ts);
        //            _outputDelegatingConstructor(writer, params);
        //          }
        //        }


        // Getters (only for fields defined in this class or that subtype superclass fields)
        for (Field f : declaredFields(ast)) {
            writer.startLine(f.getGetterMethod(!this.isAbstract(), false));
        }

        if (parent.isSome()) {
            NodeType parentType = parent.unwrap();
            for (Field f : _fields) {
                Option<Field> supF = parentType.fieldForName(f.name(), ast);
                if (supF.isSome() && !f.matchesNameAndType(supF.unwrap())) {
                    writer.startLine(f.getGetterMethod(!this.isAbstract(), true));
                }
            }
        }
        writer.println();

        for (CodeGenerator g : gens) { g.generateClassMembers(writer, this); }

        writer.unindent();
        writer.startLine("}");
        writer.println();
        writer.close();
    }

    private void _outputMainConstructor(TabPrintWriter writer, Iterable<Field> allFields,
            Iterable<Field> declaredFields, boolean allowNulls) {
        writer.startLine("/**");
        writer.startLine(" * Constructs a " + _name + ".");

        if (!allowNulls) {
            writer.startLine(" * @throws java.lang.IllegalArgumentException");
            writer.print("  If any parameter to the constructor is null.");
        }
        writer.startLine(" */");

        writer.startLine("public " + _name + IterUtil.toString(allFields, "(", ", ", ")") + " {");
        writer.indent();

        writer.startLine("super();");

        for (Field curField : declaredFields) {
            // Each class is only responsible for checking its own fields (not the super fields) for null
            if (!curField.allowNull() && !(curField.type() instanceof PrimitiveName)) {
                writer.startLine("if (" + curField.getConstructorArgName() + " == null) {");
                writer.indent();
                writer.startLine("throw new java.lang.IllegalArgumentException(");
                writer.print("\"Parameter '" + curField.name());
                writer.print("' to the " + _name + " constructor was null\"");
                writer.print(");");
                writer.unindent();
                writer.startLine("}");
            }
            writer.startLine(curField.getFieldInitialization());
        }

        writer.unindent();
        writer.startLine("}");
        writer.println(); // skip line after constructor
    }
}
