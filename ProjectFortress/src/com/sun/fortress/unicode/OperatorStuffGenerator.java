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

package com.sun.fortress.unicode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.fortress.interpreter.drivers.ProjectProperties;
import com.sun.fortress.useful.BASet;
import com.sun.fortress.useful.BATree;
import com.sun.fortress.useful.MultiMap;
import com.sun.fortress.useful.StringComparer;
import com.sun.fortress.useful.StringEncodedAggregate;


/**
 * Given the following inputs:
 *
 * 1) A com.sun.fortress.unicode data file
 *
 * 2) A list of optional abbreviation strings (e.g., "LETTER ") This is encoded
 * into class Element.
 *
 * 3) A list of operator groups, defined as GroupName indented Unicode name,
 * comma-separated extra aliases. Note that spaces are NOT separators; com.sun.fortress.unicode
 * names often contain several words separated by spaces.
 *
 * Generate several files, depending on the com.sun.fortress.parser implementation technology.
 * For OCaml/Elkhound, generate input files to the grammar, scanner, and token
 * list.
 */
public class OperatorStuffGenerator {

    static boolean shortOnly = false;

    static int max = 12;

    static NamedXForm doofusUser = new NamedXForm("Doofus User");

    static String fortressName(String s) {
        return s.trim().replaceAll("\\s+|-", "_");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // com.sun.fortress.interpreter.unicode/UnicodeData.410.txt java-interpreter/src/com.sun.fortress.interpreter.unicode/ operators.txt  Syntax.java
        String unicodeFile = ProjectProperties.BASEDIR + "third_party/unicode/UnicodeData.500.txt";
        String dir = ProjectProperties.BASEDIR + "src/com/sun/fortress/parser_util/precedence_resolver/";
        String operatorFile = dir + "operators.txt";
        String pkg = "com.sun.fortress.parser_util.precedence_resolver";
        String cls = "Operators";
        String theJavaFile = dir + cls + ".java"; // The Java File

        // unicodeFile = args[0];
        // dir = args[1];
        // operatorFile = dir + args[2];
        // theJavaFile = dir + args[3]; // The Java File

        MultiMap<String, Element> groups = new MultiMap<String, Element>();

        ArrayList<Element> chars = null;
        try {
            chars = Element.readUnicodeFile(unicodeFile);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        HashMap<String, Element> namesToElements = Element
                .generateAbbreviated(chars);
        HashSet<Element> allElements = new HashSet<Element>();
        try {

            readOperators(operatorFile, groups, namesToElements, allElements);
            generateJavaFile(theJavaFile, groups, namesToElements, allElements, pkg, cls);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // for (String k : groups.keySet()) {
        // System.out.println("Group " + k);
        // Set<Element> els = groups.get(k);
        // for (Element e : els) {
        // System.out.println(e.spaceFreeAliases.toString());
        // }
        // }
        // System.out.println();
        System.out.println("Groups:  " + groups.keySet());

    }

    /**
     * @param operatorFile
     * @param groups
     * @param namesToElements
     * @param allElements
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static void readOperators(String operatorFile,
            MultiMap<String, Element> groups,
            HashMap<String, Element> namesToElements,
            HashSet<Element> allElements) throws FileNotFoundException,
            IOException {
        BufferedReader br = Element.filenameToBufferedReader(operatorFile);
        String line = br.readLine();
        String currentGroup = null;
        while (line != null) {
            // If the line has no leading whitespace, it names a group.
            // otherwise, tokenize by commas, normalize all names found, add
            // element to group, add aliases to aliases for the element.
            if (line.trim().length() > 0) {

                char ch = line.charAt(0);
                if (Character.isWhitespace(ch)) {
                    // Tokenize, get an element.
                    line = line.trim().toUpperCase();
                    String[] tokens = line.split(",");
                    String name = tokens[0].trim().replaceAll("\\s+", " ");
                    Element e = namesToElements.get(name);
                    if (e == null) {
                        System.err.println("NOT SEEN IN UNICODE: " + name);
                    } else {
                        allElements.add(e);
                        e.addAlias(e.name(), namesToElements, doofusUser,
                                fortressName(tokens[0]), true);
                        for (int i = 1; i < tokens.length; i++) {
                            e.addAlias(e.name(), namesToElements, doofusUser,
                                    tokens[i].trim(), true);
                        }
                        groups.putItem(currentGroup, e);
                    }
                } else if (ch != '#') {
                    currentGroup = line.trim();
                    // System.err.println("Group = " + currentGroup);
                }

            } else {
                // Do nothing
            }
            line = br.readLine();
        }
    }

    private static void generateJavaFile(String theJavaFile,
            MultiMap<String, Element> groups,
            HashMap<String, Element> namesToElements,
            HashSet<Element> allElements,
            String pkg, String cls) throws IOException {
        BufferedWriter tjf = Element.filenameToBufferedWriter(theJavaFile);

        Set<Element> enclosing = groups.get("enclosing");
        Set<Element> enclosing_left = groups.get("enclosing_left");
        Set<Element> enclosing_right = groups.get("enclosing_right");

        tjf.write("/* THIS FILE WAS AUTOMATICALLY GENERATED BY com.sun.fortress.unicode.OperatorStuffGenerator.java FROM operators.txt */");
        tjf.newLine();
        tjf.write("package "+pkg+";");
        tjf.newLine();
        tjf.write("import com.sun.fortress.useful.*;");
        tjf.newLine();
        tjf.write("import java.util.Set;");
        tjf.newLine();
        tjf.write("import java.util.Map;");
        tjf.newLine();
        tjf.write("class "+ cls +" {");
        tjf.newLine();

        Set<String> s_enclosing = new BASet<String>(StringComparer.V);
        Set<String> s_left = new BASet<String>(StringComparer.V);
        Set<String> s_right = new BASet<String>(StringComparer.V);
        Set<String> s_ops = new BASet<String>(StringComparer.V);
        Map<String, String> l2r = new BATree<String, String>(StringComparer.V);
        Map<String, String> aliases = new BATree<String, String>(StringComparer.V);

        for (Element e : allElements) {
            String ename = fortressName(e.name());
            String sname = e.escapedShortName();
            // PARSER

            Set<String> which_set = enclosing.contains(e) ? s_enclosing
                    : enclosing_left.contains(e) ? s_left : enclosing_right
                            .contains(e) ? s_right : s_ops;

            which_set.add(sname);

            // If we just parsed a left or an enclosing,
            // we need to also emit some match code.
            if (which_set == s_left) {
                l2r.put(sname,
                        otherEnd(ename, namesToElements, enclosing_right));

            } else if (which_set == s_enclosing) {
                l2r.put(sname, sname);
            }

            for (String a : e.spaceFreeAliases) {
                String s = a.replace("\\", "\\\\");
                s = s.replace("\"", "\\\"");
                if (! s.equals(sname))
                    aliases.put(s, sname);
            }


        }

        tjf.write("   final private static String encodedEnclosing ="
                + StringEncodedAggregate.setToFormattedString(s_enclosing, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedLeft ="
                + StringEncodedAggregate.setToFormattedString(s_left, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedRight ="
                + StringEncodedAggregate.setToFormattedString(s_right, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedOps ="
                + StringEncodedAggregate.setToFormattedString(s_ops, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedL2R ="
                + StringEncodedAggregate.mapToFormattedString(l2r, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedAliases ="
                + StringEncodedAggregate.mapToFormattedString(aliases, ';',
                        new StringBuffer()) + ";");
        tjf.newLine();

        for (String k : groups.keySet()) {
            Set<Element> els = groups.get(k);
            Set<String> s_e = new BASet<String>(StringComparer.V);

            for (Element e : els) {
                String ename = e.escapedShortName();
                s_e.add(ename);
            }

            String enc_group = "encoded_" + k;

            tjf.write("   final private static String "
                    + enc_group
                    + " ="
                    + StringEncodedAggregate.setToFormattedString(s_e, ';',
                            new StringBuffer()) + ";");
            tjf.newLine();
        }

        tjf.write("   static Set<String> enclosing = StringEncodedAggregate.stringToSet(encodedEnclosing,';',new BASet<String>(StringComparer.V));");
        tjf.newLine();
        tjf.write("   static Set<String> left = StringEncodedAggregate.stringToSet(encodedLeft,';',new BASet<String>(StringComparer.V));");
        tjf.newLine();
        tjf.write("   static Set<String> right = StringEncodedAggregate.stringToSet(encodedRight,';',new BASet<String>(StringComparer.V));");
        tjf.newLine();
        tjf.write("   static Set<String> ops = StringEncodedAggregate.stringToSet(encodedOps,';',new BASet<String>(StringComparer.V));");
        tjf.newLine();
        tjf.write("   static Map<String, String> l2r = StringEncodedAggregate.stringToMap(encodedL2R,';',new BATree<String, String>(StringComparer.V));");
        tjf.newLine();
        tjf.write("   static Map<String, String> aliases = StringEncodedAggregate.stringToMap(encodedAliases,';',new BATree<String, String>(StringComparer.V));");
        tjf.newLine();

        for (String k : groups.keySet()) {

            String enc_group = "encoded_" + k;

            tjf.write("   static Set<String> p_" + k
                    + " = StringEncodedAggregate.stringToSet(" + enc_group
                    + ",';',new BASet<String>(StringComparer.V));");
            tjf.newLine();

        }

        tjf.write("}");
        tjf.newLine();
        tjf.close();

    }

    /**
     * @param args
     * @param dir
     * @param groups
     * @param namesToElements
     * @param allElements
     * @throws IOException
     */
    private static void generateOCamlFiles(String[] args, String dir,
            MultiMap<String, Element> groups,
            HashMap<String, Element> namesToElements,
            HashSet<Element> allElements) throws IOException {
        String parserOpsFile = dir + args[3];
        String parserLeftFile = dir + args[4];
        String parserRightFile = dir + args[5];
        String parserEncFile = dir + args[6];
        String parserMatchFile = dir + args[7];

        String lexerFile = dir + args[8];
        String tokensFile = dir + args[9];
        String precedenceFile = dir + args[10];

        BufferedWriter par_ops = Element
                .filenameToBufferedWriter(parserOpsFile);
        BufferedWriter par_left = Element
                .filenameToBufferedWriter(parserLeftFile);
        BufferedWriter par_right = Element
                .filenameToBufferedWriter(parserRightFile);
        BufferedWriter par_enc = Element
                .filenameToBufferedWriter(parserEncFile);
        BufferedWriter par_match = Element
                .filenameToBufferedWriter(parserMatchFile);

        BufferedWriter lex = Element.filenameToBufferedWriter(lexerFile);
        BufferedWriter tok = Element.filenameToBufferedWriter(tokensFile);
        BufferedWriter prc = Element.filenameToBufferedWriter(precedenceFile);

        Set<Element> enclosing = groups.get("enclosing");
        Set<Element> enclosing_left = groups.get("enclosing_left");
        Set<Element> enclosing_right = groups.get("enclosing_right");

        // OCaml, Parser file contains one line for each element, of the
        // form
        // -> "ENAME" {"ENAME"}

        // OCaml, Tokens file conains one line for each element, of the form
        // | (* ENAME *) ENAME

        // OCaml, Lexer file contains one line for each element, of the form
        // | alias1 | alias2 | 0xUnicode -> ENAME

        // OCaml, Precedence file contains one line for each group, of the
        // form
        // let GNAME = set ["ENAME1"; "ENAME2"; "ENAME3"]

        for (Element e : allElements) {
            String ename = fortressName(e.name());
            String sname = e.escapedShortName();
            // PARSER

            if (shortOnly && e.shortName().length() > max) {
                continue;
            }

            BufferedWriter par_something = enclosing.contains(e) ? par_enc
                    : enclosing_left.contains(e) ? par_left : enclosing_right
                            .contains(e) ? par_right : par_ops;

            par_something.write(" -> \"" + sname + "\" {\"" + sname + "\"}");
            par_something.newLine();

            // If we just parsed a left or an enclosing,
            // we need to also emit some match code.
            if (par_something == par_left) {
                par_match.write("| \"" + sname + "\",\""
                        + otherEnd(ename, namesToElements, enclosing_right)
                        + "\"");
                par_match.newLine();
            } else if (par_something == par_enc) {
                par_match.write("| \"" + sname + "\",\"" + sname + "\"");
                par_match.newLine();
            }

            // TOKENS
            tok.write(" | (* " + sname + " *) " + ename);
            tok.newLine();

            // LEXER
            for (String a : e.spaceFreeAliases) {
                String s = a.replace("\\", "\\\\");
                s = s.replace("\"", "\\\"");
                lex.write(" | \"" + s + "\"");
            }
            lex.write(" | " + e.quotedShortNameOrOXUnicode() + " -> " + ename);
            lex.newLine();
        }

        for (String k : groups.keySet()) {
            prc.write("let " + k + " = set ");
            Set<Element> els = groups.get(k);
            boolean sawOne = false;
            String sep = "[";
            for (Element e : els) {
                String ename = e.escapedShortName();
                if (shortOnly && e.shortName().length() > max)
                    continue;
                prc.write(sep);
                prc.write("\"" + ename + "\"");
                sep = "; ";
                sawOne = true;
            }
            if (!sawOne)
                prc.write(sep);
            prc.write("]");
            prc.newLine();
        }

        lex.close();
        par_ops.close();
        par_left.close();
        par_right.close();
        par_enc.close();
        par_match.close();
        tok.close();
        prc.close();
    }

    private static String otherEnd(String ename, HashMap<String, Element> h,
            Set<Element> enclosing_right) {
        String formulaOther = ename.replaceAll("LEFT", "RIGHT");
        Element otherE = h.get(formulaOther);
        if (otherE != null && enclosing_right.contains(otherE)) {
            return otherE.escapedShortName();
        }
        // HACK for two exceptions in Unicode where s/LEFT/RIGHT/ is not good
        // enough. Sigh.
        if (ename.equals("LEFT_ARC_LESS_THAN_BRACKET"))
            return h.get("RIGHT_ARC_GREATER_THAN_BRACKET").escapedShortName();
        if (ename.equals("DOUBLE_LEFT_ARC_GREATER_THAN_BRACKET"))
            return h.get("DOUBLE_RIGHT_ARC_LESS_THAN_BRACKET")
                    .escapedShortName();
        throw new Error("Unmatched left end " + ename + " formulaOther="
                + formulaOther + " otherE=" + otherE);

    }

}
