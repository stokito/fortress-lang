package com.sun.fortress.astgen;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Comparator;
import java.util.Collections;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.IOException;

import edu.rice.cs.astgen.Types;
import edu.rice.cs.astgen.Types.*;
import edu.rice.cs.astgen.NodeInterface;
import edu.rice.cs.astgen.NodeClass;
import edu.rice.cs.astgen.NodeType;
import edu.rice.cs.astgen.CodeGenerator;
import edu.rice.cs.astgen.TabPrintWriter;
import edu.rice.cs.astgen.Field;
import edu.rice.cs.astgen.ASTModel;

/* Generates FortressAst.fsi and FortressAst.fss, fortress representations
 * of the fortress ast( Fortress.ast )
 */
public class FortressAstGenerator extends CodeGenerator {
    public FortressAstGenerator(ASTModel ast) {
        super(ast);
    }

    public Iterable<Class<? extends CodeGenerator>> dependencies(){
        return new ArrayList();
    }

    @Override
    public void generateInterfaceMembers(TabPrintWriter writer, NodeInterface i){
    }

    @Override
    public void generateClassMembers(TabPrintWriter writer, NodeClass c){
    }

    private List<? extends NodeInterface> getInterfaces(){
        return mkList(ast.interfaces());
    }

    private String extendsClause( NodeInterface box ){
        if ( box.interfaces().isEmpty() ){
            return "";
        }
        if ( box.interfaces().size() == 1 ){
            return "extends " + box.interfaces().get(0);
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append( "extends { " );
        int i = 0;
        for ( TypeName name : box.interfaces() ){
            if ( i != 0 ){
                buffer.append( ", " );
            }
            i += 1;
            buffer.append( fieldType(name) );
        }
        buffer.append( " }" );

        return buffer.toString();
    }

    private String extendsClause( NodeClass box ){
        if ( ast.isRoot(box) ){
            return "";
        }
        List<String> names = new ArrayList<String>();
        names.add(fieldType(box.superClass()));
        for ( TypeName name : box.interfaces() ){
            names.add(fieldType(name));
        }
        // names.addAll(Arrays.asList(box.getInterfaceNames()));
        StringBuffer buffer = new StringBuffer();
        buffer.append("extends ");
        if ( names.size() == 1 ){
            buffer.append( names.get( 0 ) );
        } else {
            int i = 0;
            buffer.append( "{ " );
            for ( String name : names ){
                if ( i != 0 ){
                    buffer.append( ", " );
                }
                i += 1;
                buffer.append( name );
            }
            buffer.append( " }" );
        }
        return buffer.toString();
    }

    private String fieldType(TypeName type){
        return type.accept(new TypeNameVisitor<String>(){
            /** A type declared in the AST.  Has 0 type arguments. */
            public String forTreeNode(ClassName t){
                return t.className();
            }

            /** A primitive type. */
            public String forPrimitive(PrimitiveName t){
                String name = t.name();
                String method;
                if ( name.equals("int") ){
                    return "ZZ32";
                }
                if ( name.equals("boolean") ){
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
                return sub("List[\\@type\\]", "@type", t.elementType().accept(this));
            }

            /** A {@code edu.rice.cs.plt.tuple.Option}.  Has 1 type argument. */
            public String forOptionClass(OptionClassName t){
                return sub("Maybe[\\@type\\]", "@type", t.elementType().accept(this));
            }

            /** A tuple (see definition in {@link TupleName} documentation). */
            public String forTupleClass(TupleClassName t){
                throw new RuntimeException("Can't handle tuple");
            }

            /** A type for which none of the other cases apply. */
            public String forGeneralClass(ClassName t){
                String name = t.className();
                if ( name.equals("BigInteger") ){
                    return "ZZ64";
                } 
                return t.className();
                // throw new RuntimeException("General class " + t.className() + " should have been eliminated");
            }
        });
    }

    private String traitFields( List<Field> fields ){
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for ( Field field : fields ){
            if ( first ){
                buffer.append( "\n" );
                first = false;
            }
            buffer.append(sub("in_@name:@type\n", "@name", field.name(), "@type", fieldType(field.type()) ));
        }
        return buffer.toString();

    }

    private String fields( NodeInterface box ){
        return traitFields(mkList(box.allFields(ast)));
    }

    private String fields( NodeClass box ){
        if ( box.isAbstract() ){
            return traitFields(mkList(box.allFields(ast)));
        } else {
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
                    buffer.append(sub("in_@name:@type", "@name", field.name(), "@type", fieldType(field.type()) ));
                }
                buffer.append(")");
                return buffer.toString();
            }
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
            return sub(String.format(s.replaceAll( args[ 0 ], "%s" ), args[ 1 ]), Arrays.asList( args ).subList( 2, args.length ).toArray(new String[0]) );
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

    private void generateBody( PrintWriter writer ){
        for ( NodeInterface box : sort(getInterfaces()) ){
            if ( box.name().startsWith("_") ){
                continue;
            }
            writer.println(sub("trait @name @extends @fields end", "@name", box.name(), "@extends", extendsClause(box), "@fields", fields(box) ));
        }

        for ( NodeClass c : sort(ast.classes()) ){
            if ( c.name().startsWith("_") ){
                continue;
            }
            if ( c.isAbstract() ){
                writer.println(sub( "trait @name @extends @fields end", "@name", c.name(), "@extends", extendsClause(c), "@fields", fields(c) ));
            } else {
                writer.println(sub( "object @name @fields @extends end", "@name", c.name(), "@extends", extendsClause(c), "@fields", fields(c) ));
            }
        }
    }

    private void generateFile( String file, String type ){
        FileWriter out = null;
        PrintWriter writer = null;
        try{
            out = options.createFileInOutDir( file );
            writer = new PrintWriter(out);
            
            writer.println( copyright() );

            writer.println( type );
            writer.println();

            writer.println("import List.{...}");
            writer.println();

            generateBody(writer);
            
            writer.println();
            writer.println( "end" );

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

    private void generateApi(){
        generateFile( "FortressAst.fsi", "api FortressAst" );
    }

    private void generateComponent(){
        generateFile( "FortressAst.fss", "component FortressAst" );
    }

    public void generateAdditionalCode(){
        generateApi();
        generateComponent();
    }

    private String copyright(){
        StringWriter string = new StringWriter();
        PrintWriter writer = new PrintWriter( string );
        writer.println("(* THIS FILE WAS AUTOMATICALLY GENERATED BY");
        writer.println(sub("   @class FROM Fortress.ast *)", "@class", this.getClass().getName()));
        return string.toString();
    }
}
