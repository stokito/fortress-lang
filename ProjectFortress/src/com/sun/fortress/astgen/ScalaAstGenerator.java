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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rice.cs.astgen.ASTModel;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeInterface;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Types.ClassName;
import edu.rice.cs.astgen.Types.OptionClassName;
import edu.rice.cs.astgen.Types.PrimitiveArrayName;
import edu.rice.cs.astgen.Types.PrimitiveName;
import edu.rice.cs.astgen.Types.ReferenceArrayName;
import edu.rice.cs.astgen.Types.SequenceClassName;
import edu.rice.cs.astgen.Types.TupleClassName;
import edu.rice.cs.astgen.Types.TypeArgumentName;
import edu.rice.cs.astgen.Types.TypeName;
import edu.rice.cs.astgen.Types.TypeNameVisitor;
import edu.rice.cs.plt.iter.IterUtil;
import edu.rice.cs.plt.tuple.Option;
import edu.rice.cs.plt.tuple.OptionUnwrapException;

/* Generates FortressAst.scala, a Scala representation
 * of the fortress ast( Fortress.ast )
 */
public class ScalaAstGenerator extends CodeGenerator {
    public ScalaAstGenerator(ASTModel ast) {
        super(ast);
    }

    public Iterable<Class<? extends CodeGenerator>> dependencies(){
        return new ArrayList<Class<? extends CodeGenerator>>();
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i) {
    }

    @Override
    public void generateClassMembers(TabPrintWriter writer, NodeClass c) {
    }

    private List<? extends NodeInterface> getInterfaces(){
        return mkList(ast.interfaces());
    }

    private String extendsClause( NodeInterface box ){
        List<TypeName> interfaces = box.interfaces();
        if ( interfaces.isEmpty() ){
            return "";
        }
        if ( interfaces.size() == 1 ){
            return "extends " + fieldType(interfaces.get(0));
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append( "extends " );
        int i = 0;
        for ( TypeName name : interfaces ){
            if ( i != 0 ){
                buffer.append( " with " );
            }
            i += 1;
            buffer.append( fieldType(name) );
        }

        return buffer.toString();
    }

    private String extendsClause( NodeClass box ){

        Option<NodeType> parent = ast.parent(box);
        Iterable<Field> superFields = IterUtil.empty();
        if (parent.isSome() && parent.unwrap() instanceof NodeClass) {
            superFields = parent.unwrap().allFields(ast);
        }
        Iterable<Field> allFields = box.allFields(ast);
        Iterable<Field> declaredFields = box.declaredFields(ast);

        TypeName superName = box.superClass();
        // System.err.println(box.name() + " <: " + superName.name());

        StringBuffer buffer = new StringBuffer();

        buffer.append("extends ");
        buffer.append(superName.name());

        // Classes defined outside ASTGen and extended by an ASTGen class
        // are required to have a zeroary constructor.
        buffer.append("(");
        boolean first = true;
        for (Field f : superFields) {
            if (first) { first = false; }
            else { buffer.append(", "); }
            buffer.append(f.getGetterName());
        }
        buffer.append(")");


        List<String> names = new ArrayList<String>();

        for ( TypeName name : box.interfaces() ){
            names.add(fieldType(name));
        }

        for ( String name : names ){
            buffer.append( " with " );
            buffer.append( name );
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
                if ( name.equals("int") ){
                    return "Int";
                }
                if ( name.equals("boolean") ){
                    return "Boolean";
                }
                throw new RuntimeException("Unknown primitive " + name);
            }

            /** A {@code java.lang.String}.  Has 0 type arguments. */
            public String forString(ClassName t){
                return "String";
            }

            /** An array of primitives. */
            public String forPrimitiveArray(PrimitiveArrayName t){
                throw new RuntimeException("Can't handle primitive array");
            }

            /** An array of reference types (non-primitives). */
            public String forReferenceArray(ReferenceArrayName t){
                throw new RuntimeException("Can't handle reference array");
            }

            /** A list, set, or other subtype of {@code java.lang.Iterable}. */
            public String forSequenceClass(SequenceClassName t){
                return sub("List[@type]", "@type", t.elementType().accept(this));
            }

            /** A {@code edu.rice.cs.plt.tuple.Option}.  Has 1 type argument. */
            public String forOptionClass(OptionClassName t){
                return sub("Option[@type]", "@type", t.elementType().accept(this));
            }

            /** A tuple (see definition in {@link TupleName} documentation). */
            public String forTupleClass(TupleClassName t){
                throw new RuntimeException("Can't handle tuple");
            }

            /** A type for which none of the other cases apply. */
            public String forGeneralClass(ClassName t){
                StringBuffer name = new StringBuffer();

                // Handle types for which ASTGen provides no hooks,
                // but that we still want to treat specially.
                if (t.className().equals("java.util.Map")) { 
                    name.append("Map");
                } else { 
                    name.append(t.className());
                }

                boolean first = true;
                for (TypeArgumentName arg : t.typeArguments()) {
                    if (first) {
                        name.append("["); 
                        first = false;
                    } else {
                        name.append(", ");
                    }
                    // Note: This will not work with nested generic types. 
                    // ASTGen does not provide a facility for recursive deconstruction 
                    // of nested generic type arguments. 
                    name.append(arg.name());
                }
                if (! first) {
                    name.append("]");
                }
                return name.toString();
            }

        });
    }

    /* return true if some fields should be ignored.
     * List:
     *  span
     */
    private boolean ignoreField( Field field ){
//         if ( field.name().equals( "span" ) ){
//             return true;
//         }
//         if ( field.name().equals( "parenthesized" ) ){
//             return true;
//         }

        return false;
    }

    /* remove ignored fields */
    private Iterable<Field> filterFields(Iterable<Field> fields){
        List<Field> ok = new ArrayList<Field>();

        for ( Field field : fields ){
            if ( ! ignoreField( field ) ){
                ok.add( field );
            }
        }

        return ok;
    }

    private String capitalize(String s) {
        if (s.length() == 0) { return s; }
        else { return s.substring(0,1).toUpperCase() + s.substring(1); }
    }

    private String traitFields( List<Field> fields ) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for ( Field field : filterFields(fields) ){
            if ( first ){
                buffer.append( "\n" );
                first = false;
            }
            buffer.append(sub("  def @name:@type\n", "@name", field.getGetterName(), "@type", fieldType(field.type()) ));
        }
        return buffer.toString();
    }

    private String fields( NodeInterface box ) {
        return traitFields(box.fields());
    }

    private String fields( NodeClass box ) {
        if ( mkList(box.allFields(ast)).isEmpty() ){
            return "";
        } else {
            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            int i = 0;
            for ( Field field : box.allFields(ast) ){
                if ( i != 0 ){
                    buffer.append( ", " );
                }
                i += 1;
                buffer.append(sub("@name:@type", "@name", field.getGetterName(), "@type", fieldType(field.type()) ));
            }
            buffer.append(")");
            return buffer.toString();
        }
    }

    private String fieldsNoParens( NodeClass box, boolean firstPass ) {
        if ( mkList(filterFields(box.allFields(ast))).isEmpty() ){
            return "";
        } else {
            StringBuffer buffer = new StringBuffer();

            for ( Field field : filterFields(box.allFields(ast)) ){
                if ( ! firstPass ){
                    buffer.append( ", " );
                }
                firstPass = false;
                buffer.append(sub("@name:@type", "@name", field.getGetterName(), "@type", fieldType(field.type()) ));
            }
            return buffer.toString();
        }
    }

    /** 
     * Return a string including all field declarations, including inherited declarations. 
     */
    public String allFields(NodeClass box) {
        return "(" + allFieldsNoParens(box, true) + ")";
    }

    private StringBuffer allFieldsNoParens(NodeClass box, boolean firstPass) {
            StringBuffer buffer = new StringBuffer();

            if (ast.isTopClass(box)) { return new StringBuffer(""); }

            for ( Field field : filterFields(box.allFields(ast)) ){
                if (! firstPass) {
                    buffer.append( ", " );
                }
                firstPass = false;
                buffer.append(sub("@name:@type", "@name", field.getGetterName(), "@type", fieldType(field.type()) ));
            }
            // If the supertype has no definition, there is a bug in Fortress.ast or ASTGen.
            // If the defined supertype is not a class, there is a bug in Fortress.ast or ASTGen.
            try {
                buffer.append(fieldsNoParens((NodeClass)ast.typeForName(box.superClass()).unwrap(), firstPass));
            } catch (OptionUnwrapException e) {
                throw new RuntimeException("Missing supertype definition in Fortress.ast: " + box.superClass());
            }
            return buffer;
    }


    private String fieldsNoTypes( NodeType box ) {
        if ( mkList(filterFields(box.allFields(ast))).isEmpty() ){
            return "";
        } else {
            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            int i = 0;
            for ( Field field : filterFields(box.allFields(ast)) ){
                if ( i != 0 ){
                    buffer.append( ", " );
                }
                i += 1;
                buffer.append(sub("@name", "@name", field.getGetterName()));
            }
            buffer.append(")");
            return buffer.toString();
        }
    }

    private String wrappedFieldCalls(String wrapper, NodeType box) {
        if ( mkList(filterFields(box.allFields(ast))).isEmpty() ){
            return "";
        } else {
            StringBuffer buffer = new StringBuffer();
            buffer.append("(");
            int i = 0;
            for ( Field field : filterFields(box.allFields(ast)) ){
                if ( i != 0 ){
                    buffer.append( ", " );
                }
                i += 1;
                buffer.append(sub("@wrapper(@name).asInstanceOf", 
                                  "@wrapper", wrapper, "@name", field.getGetterName()));
            }
            buffer.append(")");
            return buffer.toString();
        }
    }


    /* a nice function for string replacement.
     * sub( "foo @bar @baz", "@bar", "1", "@baz", "2" ) ->
     * "foo 1 2"
     */
    private String sub( String s, String... args ){
        if ( args.length == 0 ){
            return s;
        } else {
            Map<String,String> map = new HashMap<String,String>();
            return sub(String.format(s.replaceAll( args[ 0 ], "%1\\$s" ), args[ 1 ]), Arrays.asList( args ).subList( 2, args.length ).toArray(new String[0]) );
            // return sub( s.replaceAll( args[ 0 ], args[ 1 ] ), Arrays.asList( args ).subList( 2, args.length ).toArray(new String[0]) );
        }
    }

    /* Iterable -> List */
    private <T> List<T> mkList(Iterable<T> iter){
        List<T> list = new ArrayList<T>();
        for ( T i : iter ){
            list.add(i);
        }

        return list;
    }

    private <T extends NodeType> Iterable<T> sort(Iterable<T> boxes){
        List<T> list = mkList(boxes);
        Collections.sort(list, new Comparator<NodeType>(){
            public int compare( NodeType b1, NodeType b2 ){
                return b1.name().compareTo( b2.name() );
            }
        });
        return list;
    }

    private boolean ignoreClass( String name ){
        if (name.startsWith( "_Rewrite" ) ) {
            return false;
        }
        if (name.startsWith( "_SyntaxTransformation" ) ) {
            return true; 
        }
        if (name.startsWith( "_Ellipses" ) ) {
            return true; 
        }

        /* TODO: this won't be needed once TemplateGap's are removed from Fortress.ast */
        if ( name.startsWith( "TemplateGap" ) ){
            return true;
        }
        return false;
    }


    private void generateBody(PrintWriter writer) {
        for ( NodeInterface box : sort(getInterfaces()) ){
            writer.println(sub("trait @name @extends {@fields}", "@name", box.name(), "@extends", extendsClause(box), "@fields", fields(box) ));
        }

        for ( NodeClass c : sort(ast.classes()) ){
            if ( ignoreClass(c.name()) ){
                continue;
            }
            if ( c.isAbstract() ){
                writer.println(sub( "abstract class @name @fields @extends", "@name", c.name(), "@fields", fields(c), "@extends", extendsClause(c) ));
            } else {
                writer.println(sub( "case class @name @fields @extends", 
                                    "@name", c.name(), "@fields", fields(c), "@extends", extendsClause(c)  ));
            }
        }
        // Generate translator
        writer.println();
        writer.println("object Translator {");
        writer.println("   def toJavaAst(node:Any):Any = {");
        writer.println("       node match {");
        for ( NodeClass c : sort(ast.classes()) ) {
            if ( ignoreClass(c.name()) ){
                continue;
            }
            if ( c.isAbstract() ){
                continue;
            } else {
                writer.println(sub( "         case @name @fieldsNoTypes =>", 
                                    "@name", c.name(), "@fieldsNoTypes", fieldsNoTypes(c)));
                writer.println(sub("             new com.sun.fortress.nodes.@name @fieldsNoTypes",
                                   "@name", c.name(), "@fieldsNoTypes", wrappedFieldCalls("toJavaAst", c)));

            }
        }
        writer.println("         case xs:List[_] => Lists.toJavaList(xs)");
        writer.println("         case _ => node");
        writer.println("      }");
        writer.println("   }");
        writer.println("}");
    }

    private void generateFile( String file, String preamble) {
        FileWriter out = null;
        PrintWriter writer = null;
        try{
            out = options.createFileInOutDir( file );
            writer = new PrintWriter(out);

            writer.println( copyright() );

            writer.println( preamble );

            writer.println();

            generateBody(writer);

            writer.println();

            writer.close();
            out.close();
        } catch ( IOException ie ){
            ie.printStackTrace();
        } finally {
            try{
                if ( out != null ){
                    out.close();
                }
                if ( writer != null ){
                    writer.close();
                }
            } catch ( IOException ie ){
                ie.printStackTrace();
            }
        }
    }

    private void generateScalaFile() {
        generateFile("FortressAst.scala", 
                     "package com.sun.fortress.scalasrc.nodes\n" +
                     "import com.sun.fortress.scalasrc.useful._\n" + 
                     "import com.sun.fortress.nodes_util._\n" +
                     "import com.sun.fortress.useful.HasAt\n" +
                     "import _root_.scala.collection.mutable.ListBuffer\n" +
                     "import _root_.java.math.BigInteger\n");
    }

    public void generateAdditionalCode(){
        generateScalaFile();
    }

    private String copyright(){
        StringWriter string = new StringWriter();
        PrintWriter writer = new PrintWriter( string );
        writer.println("/* THIS FILE WAS AUTOMATICALLY GENERATED BY");
        writer.println(sub("   @class FROM Fortress.ast */", "@class", this.getClass().getName()));
        return string.toString();
    }
}
