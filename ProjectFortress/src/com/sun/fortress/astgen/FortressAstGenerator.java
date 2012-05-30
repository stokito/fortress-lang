/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.astgen;

import edu.rice.cs.astgen.*;
import edu.rice.cs.astgen.Types.*;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/* Generates FortressAst.fsi and FortressAst.fss, fortress representations
 * of the fortress ast( Fortress.ast )
 */
public class FortressAstGenerator extends CodeGenerator {
    public FortressAstGenerator(ASTModel ast) {
        super(ast);
    }

    public Iterable<Class<? extends CodeGenerator>> dependencies() {
        return new ArrayList<Class<? extends CodeGenerator>>();
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    }

    @Override
    public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    }

    private List<? extends NodeInterface> getInterfaces() {
        return mkList(ast.interfaces());
    }

    /* Ignore some interfaces.
     * List:
     *  UIDObject
     *  HasAt
     *  Applicable
     */
    private boolean ignoreInterface(String name) {
        if (name.equals("UIDObject")) {
            return true;
        }
        if (name.equals("HasAt")) {
            return true;
        }
        if (name.equals("Applicable")) {
            return true;
        }
        return false;
    }

    private List<TypeName> filterInterfaces(List<TypeName> interfaces) {
        List<TypeName> ok = new ArrayList<TypeName>();

        for (TypeName name : interfaces) {
            if (!ignoreInterface(fieldType(name))) {
                ok.add(name);
            }
        }

        return ok;
    }

    private String extendsClause(NodeInterface box) {
        List<TypeName> interfaces = filterInterfaces(box.interfaces());
        if (interfaces.isEmpty()) {
            return "";
        }
        if (interfaces.size() == 1) {
            return "extends " + fieldType(interfaces.get(0));
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append("extends { ");
        int i = 0;
        for (TypeName name : interfaces) {
            if (i != 0) {
                buffer.append(", ");
            }
            i += 1;
            buffer.append(fieldType(name));
        }
        buffer.append(" }");

        return buffer.toString();
    }

    private String extendsClause(NodeClass box) {
        if (ast.isRoot(box)) {
            return "";
        }
        List<String> names = new ArrayList<String>();
        if (!ignoreInterface(fieldType(box.superClass()))) {
            names.add(fieldType(box.superClass()));
        }
        for (TypeName name : box.interfaces()) {
            if (!ignoreInterface(fieldType(name))) {
                names.add(fieldType(name));
            }
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append("extends ");
        if (names.size() == 1) {
            buffer.append(names.get(0));
        } else {
            int i = 0;
            buffer.append("{ ");
            for (String name : names) {
                if (i != 0) {
                    buffer.append(", ");
                }
                i += 1;
                buffer.append(name);
            }
            buffer.append(" }");
        }
        return buffer.toString();
    }

    private String fieldType(TypeName type) {
        return type.accept(new TypeNameVisitor<String>() {
            /** A type declared in the AST.  Has 0 type arguments. */
            public String forTreeNode(ClassName t) {
                return t.name();
            }

            /** A primitive type. */
            public String forPrimitive(PrimitiveName t) {
                String name = t.name();
                String method;
                if (name.equals("int")) {
                    return "ZZ32";
                }
                if (name.equals("boolean")) {
                    return "Boolean";
                }
                /*
                if (eltName.equals("boolean")){ method = "readBoolean()"; }
                else if (eltName.equals("char")) { method = "readChar()"; }
                else if (eltName.equals("byte")) { method = "readByte()"; }
                else if (eltName.equals("short")) { method = "readShort()"; }
                else if (eltName.equals("int")) { method = "readInt()"; }
                else if (eltName.equals("long")) { method = "readLong()"; }
                else if (eltName.equals("float")) { method = "readFloat()"; }
                else if (eltName.equals("double")) { method = "readDouble()"; }
                else { throw new RuntimeException("Unrecognized primitive: " + eltName); }
                return Pair.make(method, false);
                */
                throw new RuntimeException("Unknown primitive " + name);
            }

            /** A {@code java.lang.String}.  Has 0 type arguments. */
            public String forString(ClassName t) {
                return "String";
            }

            /** An array of primitives. */
            public String forPrimitiveArray(PrimitiveArrayName t) {
                throw new RuntimeException("Can't handle primitive array");
            }

            /** An array of reference types (non-primitives). */
            public String forReferenceArray(ReferenceArrayName t) {
                throw new RuntimeException("Can't handle reference array");
            }

            /** A list, set, or other subtype of {@code java.lang.Iterable}. */
            public String forSequenceClass(SequenceClassName t) {
                return sub("List[\\@type\\]", "@type", t.elementType().accept(this));
            }

            /** A {@code edu.rice.cs.plt.tuple.Option}.  Has 1 type argument. */
            public String forOptionClass(OptionClassName t) {
                return sub("Maybe[\\@type\\]", "@type", t.elementType().accept(this));
            }

            /** A tuple (see definition in {@link TupleName} documentation). */
            public String forTupleClass(TupleClassName t) {
                throw new RuntimeException("Can't handle tuple");
            }

            /** A type for which none of the other cases apply. */
            public String forGeneralClass(ClassName t) {
                String name = t.className();
                if (name.equals("BigInteger")) {
                    return "ZZ64";
                }
                return t.className();
                // throw new RuntimeException("General class " + t.className() + " should have been eliminated");
            }
        });
    }

    /* return true if some fields should be ignored.
     * List:
     *  span
     */
    private boolean ignoreField(Field field) {
        if (field.name().equals("span")) {
            return true;
        }
        if (field.name().equals("parenthesized")) {
            return true;
        }

        return false;
    }

    /* remove ignored fields */
    private Iterable<Field> filterFields(Iterable<Field> fields) {
        List<Field> ok = new ArrayList<Field>();

        for (Field field : fields) {
            if (!ignoreField(field)) {
                ok.add(field);
            }
        }

        return ok;
    }

    private String traitFields(List<Field> fields) {
        /* RMC: Temporary hack to eliminate shadowing. After all, we don't actually
         * use the AST types at all any longer.
         */
        return "";
        /*
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for ( Field field : filterFields(fields) ){
            if ( first ){
                buffer.append( "\n" );
                first = false;
            }
            buffer.append(sub("in_@name:@type\n", "@name", field.name(), "@type", fieldType(field.type()) ));
        }
        return buffer.toString();
        */
    }

    private String fields(NodeInterface box) {
        return traitFields(box.fields());
    }

    private String fields(NodeClass box) {
        /* RMC: Temporary hack to eliminate shadowing. After all, we don't actually
         * use the AST types at all any longer.
         */
        return "";
        /*
        if ( box.isAbstract() ){
            return traitFields(box.fields());
        } else {
            if ( mkList(filterFields(box.allFields(ast))).isEmpty() ){
                return "";
            } else {
                StringBuilder buffer = new StringBuilder();
                buffer.append("(");
                int i = 0;
                for ( Field field : filterFields(box.allFields(ast)) ){
                    if ( i != 0 ){
                        buffer.append( ", " );
                    }
                    i += 1;
                    buffer.append(sub("in_@name:@type", "@name", field.name(), "@type", fieldType(field.type()) ));
                }
                buffer.append(")");
                return buffer.toString();
            }
        }
        */
    }

    /* a nice function for string replacement.
     * sub( "foo @bar @baz", "@bar", "1", "@baz", "2" ) ->
     * "foo 1 2"
     */
    private String sub(String s, String... args) {
        if (args.length == 0) {
            return s;
        } else {
            return sub(String.format(s.replaceAll(args[0], "%1\\$s"), args[1]), Arrays.asList(args).subList(2, args.length).toArray(new String[0]));
            // return sub( s.replaceAll( args[ 0 ], args[ 1 ] ), Arrays.asList( args ).subList( 2, args.length ).toArray(new String[0]) );
        }
    }

    /* Iterable -> List */
    private <T> List<T> mkList(Iterable<T> iter) {
        List<T> list = new ArrayList<T>();
        for (T i : iter) {
            list.add(i);
        }

        return list;
    }

    private <T extends NodeType> Iterable<T> sort(Iterable<T> boxes) {
        List<T> list = mkList(boxes);
        Collections.sort(list, new Comparator<NodeType>() {
            public int compare(NodeType b1, NodeType b2) {
                return b1.name().compareTo(b2.name());
            }
        });
        return list;
    }

    private boolean ignoreClass(String name) {
        if (name.startsWith("_")) {
            return true;
        }

        /* TODO: this won't be needed once TemplateGap's are removed from Fortress.ast */
        if (name.startsWith("TemplateGap")) {
            return true;
        }
        return false;
    }

    private void generateBody(PrintWriter writer, boolean isApi) {
        for (NodeInterface box : sort(getInterfaces())) {
            if (box.name().startsWith("_")) {
                continue;
            }
            writer.println(sub("trait @name @extends @fields end", "@name", box.name(), "@extends", extendsClause(box), "@fields", fields(box)));
        }

        for (NodeClass c : sort(ast.classes())) {
            if (ignoreClass(c.name())) {
                continue;
            }
            if (c.isAbstract()) {
                writer.println(sub("trait @name @extends @fields end", "@name", c.name(), "@extends", extendsClause(c), "@fields", fields(c)));
            } else {
                String asString;
                if (isApi) {
                    asString = "getter asString():String";
                } else {
                    asString = sub("asString():String = \"@name\"", "@name", c.name());
                }
                writer.println(sub("object @name @fields @extends\n @asString\n end", "@name", c.name(), "@extends", extendsClause(c), "@fields", fields(c), "@asString", asString));
            }
        }
    }

    private void generateFile(String file, String preamble, boolean isApi) {
        FileWriter out = null;
        PrintWriter writer = null;
        try {
            out = options.createFileInOutDir(file);
            writer = new PrintWriter(out);

            writer.println(copyright());

            writer.println(preamble);

            writer.println();

            generateBody(writer, isApi);

            writer.println();
            writer.println("end");

            writer.close();
            out.close();
        }
        catch (IOException ie) {
            ie.printStackTrace();
        }
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (writer != null) {
                    writer.close();
                }
            }
            catch (IOException ie) {
                ie.printStackTrace();
            }
        }
    }

    private void generateApi() {
        generateFile("FortressAst.fsi", "api FortressAst\nimport List.{...}\nimport FortressLibrary.{...} except ExtentRange", true);
    }

    private void generateComponent() {
        generateFile("FortressAst.fss", "component FortressAst\nimport List.{...}\nimport FortressLibrary.{...} except ExtentRange\nexport FortressAst", false);
    }

    public void generateAdditionalCode() {
        generateApi();
        generateComponent();
    }

    private String copyright() {
        StringWriter string = new StringWriter();
        PrintWriter writer = new PrintWriter(string);
        writer.println("(* THIS FILE WAS AUTOMATICALLY GENERATED BY");
        writer.println(sub("   @class FROM Fortress.ast *)", "@class", this.getClass().getName()));
        return string.toString();
    }
    // names.addAll(Arrays.asList(box.getInterfaceNames()));
}
