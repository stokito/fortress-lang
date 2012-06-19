/*******************************************************************************
    Copyright 2008,2010, Oracle and/or its affiliates.
    All rights reserved.


    Use is subject to license terms.

    This distribution may include materials developed by third parties.

 ******************************************************************************/

package com.sun.fortress.unicode;

import com.sun.fortress.repository.ProjectProperties;
import com.sun.fortress.useful.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


/**
 * Given the following inputs:
 * <p/>
 * 1) A com.sun.fortress.unicode data file
 * <p/>
 * 2) A list of optional abbreviation strings (e.g., "LETTER ") This is encoded
 * into class Element.
 * <p/>
 * 3) A list of operator groups, defined as GroupName indented Unicode name,
 * comma-separated extra aliases. Note that spaces are NOT separators; com.sun.fortress.unicode
 * names often contain several words separated by spaces.
 * <p/>
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

        try {
            ArrayList<Element> chars = Element.readUnicodeFile(unicodeFile);
            HashMap<String, Element> namesToElements = Element.generateAbbreviated(chars);
            HashSet<Element> allElements = new HashSet<Element>();
            readOperators(operatorFile, groups, namesToElements, allElements);
            generateJavaFile(theJavaFile, groups, namesToElements, allElements, pkg, cls);
            System.out.println("Groups:  " + groups.keySet());
        }
        catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
                                      HashSet<Element> allElements) throws FileNotFoundException, IOException {
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
                        e.addAlias(e.name(), namesToElements, doofusUser, fortressName(tokens[0]), true);
                        for (int i = 1; i < tokens.length; i++) {
                            name = tokens[i].trim();
                            if (name.startsWith("\\U")) name = name.toLowerCase();
                            e.addAlias(e.name(), namesToElements, doofusUser, name, true);
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
                                         String pkg,
                                         String cls) throws IOException {
        BufferedWriter tjf = Element.filenameToBufferedWriter(theJavaFile);

        Set<Element> enclosing = groups.get("enclosing");
        Set<Element> enclosing_left = groups.get("enclosing_left");
        Set<Element> enclosing_right = groups.get("enclosing_right");

        tjf.write(
                "/* THIS FILE WAS AUTOMATICALLY GENERATED BY com.sun.fortress.unicode.OperatorStuffGenerator.java FROM operators.txt */");
        tjf.newLine();
        tjf.write("package " + pkg + ";");
        tjf.newLine();
        tjf.write("import com.sun.fortress.useful.*;");
        tjf.newLine();
        tjf.write("import java.util.Set;");
        tjf.newLine();
        tjf.write("import java.util.Map;");
        tjf.newLine();
        tjf.write("class " + cls + " {");
        tjf.newLine();

        Set<String> s_enclosing = new BASet<String>(DefaultComparator.<String>normal());
        Set<String> s_left = new BASet<String>(DefaultComparator.<String>normal());
        Set<String> s_right = new BASet<String>(DefaultComparator.<String>normal());
        Set<String> s_ops = new BASet<String>(DefaultComparator.<String>normal());
        Map<String, String> l2r = new BATree<String, String>(DefaultComparator.<String>normal());
        Map<String, String> aliases = new BATree<String, String>(DefaultComparator.<String>normal());

        for (Element e : allElements) {
            String ename = fortressName(e.name());
            String sname = e.escapedShortName();
            // PARSER

            Set<String> which_set = enclosing.contains(e) ?
                                    s_enclosing :
                                    enclosing_left.contains(e) ? s_left : enclosing_right.contains(e) ? s_right : s_ops;

            which_set.add(sname);

            // If we just parsed a left or an enclosing,
            // we need to also emit some match code.
            if (which_set == s_left) {
                l2r.put(sname, otherEnd(ename, namesToElements, enclosing_right));

            } else if (which_set == s_enclosing) {
                l2r.put(sname, sname);
            }

            for (String a : e.spaceFreeAliases) {
                String s = a.replace("\\", "\\\\");
                s = s.replace("\"", "\\\"");
                if (!s.equals(sname)) aliases.put(s, sname);
            }


        }

        tjf.write("   final private static String encodedEnclosing =" + StringEncodedAggregate.setToFormattedString(
                s_enclosing,
                ';',
                new StringBuilder()) + ";");
        tjf.newLine();
        tjf.write("   final private static String encodedLeft =" + StringEncodedAggregate.setToFormattedString(s_left,
                                                                                                               ';',
                                                                                                               new StringBuilder()) +
                  ";");
        tjf.newLine();
        tjf.write("   final private static String encodedRight =" + StringEncodedAggregate.setToFormattedString(s_right,
                                                                                                                ';',
                                                                                                                new StringBuilder()) +
                  ";");
        tjf.newLine();
        tjf.write("   final private static String encodedOps =" + StringEncodedAggregate.setToFormattedString(s_ops,
                                                                                                              ';',
                                                                                                              new StringBuilder()) +
                  ";");
        tjf.newLine();
        tjf.write("   final private static String encodedL2R =" + StringEncodedAggregate.mapToFormattedString(l2r,
                                                                                                              ';',
                                                                                                              new StringBuilder()) +
                  ";");
        tjf.newLine();
        tjf.write("   final private static String encodedAliases =" + StringEncodedAggregate.mapToFormattedString(
                aliases,
                ';',
                new StringBuilder()) + ";");
        tjf.newLine();

        for (Map.Entry<String, Set<Element>> k : groups.entrySet()) {
            Set<Element> els = k.getValue();
            Set<String> s_e = new BASet<String>(DefaultComparator.<String>normal());

            for (Element e : els) {
                String ename = e.escapedShortName();
                s_e.add(ename);
            }

            String enc_group = "encoded_" + k.getKey();

            tjf.write(
                    "   final private static String " + enc_group + " =" + StringEncodedAggregate.setToFormattedString(
                            s_e,
                            ';',
                            new StringBuilder()) + ";");
            tjf.newLine();
        }

        tjf.write(
                "   static Set<String> enclosing = StringEncodedAggregate.stringToSet(encodedEnclosing,';',new BASet<String>(DefaultComparator.<String>normal()));");
        tjf.newLine();
        tjf.write(
                "   static Set<String> left = StringEncodedAggregate.stringToSet(encodedLeft,';',new BASet<String>(DefaultComparator.<String>normal()));");
        tjf.newLine();
        tjf.write(
                "   static Set<String> right = StringEncodedAggregate.stringToSet(encodedRight,';',new BASet<String>(DefaultComparator.<String>normal()));");
        tjf.newLine();
        tjf.write(
                "   static Set<String> ops = StringEncodedAggregate.stringToSet(encodedOps,';',new BASet<String>(DefaultComparator.<String>normal()));");
        tjf.newLine();
        tjf.write(
                "   static Map<String, String> l2r = StringEncodedAggregate.stringToMap(encodedL2R,';',new BATree<String, String>(DefaultComparator.<String>normal()));");
        tjf.newLine();
        tjf.write(
                "   static Map<String, String> aliases = StringEncodedAggregate.stringToMap(encodedAliases,';',new BATree<String, String>(DefaultComparator.<String>normal()));");
        tjf.newLine();

        for (Map.Entry<String, Set<Element>> k : groups.entrySet()) {

            String enc_group = "encoded_" + k.getKey();

            tjf.write("   static Set<String> p_" + k.getKey() + " = StringEncodedAggregate.stringToSet(" + enc_group +
                      ",';',new BASet<String>(DefaultComparator.<String>normal()));");
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
    private static void generateOCamlFiles(String[] args,
                                           String dir,
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

        BufferedWriter par_ops = Element.filenameToBufferedWriter(parserOpsFile);
        BufferedWriter par_left = Element.filenameToBufferedWriter(parserLeftFile);
        BufferedWriter par_right = Element.filenameToBufferedWriter(parserRightFile);
        BufferedWriter par_enc = Element.filenameToBufferedWriter(parserEncFile);
        BufferedWriter par_match = Element.filenameToBufferedWriter(parserMatchFile);

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

            BufferedWriter par_something = enclosing.contains(e) ?
                                           par_enc :
                                           enclosing_left.contains(e) ?
                                           par_left :
                                           enclosing_right.contains(e) ? par_right : par_ops;

            par_something.write(" -> \"" + sname + "\" {\"" + sname + "\"}");
            par_something.newLine();

            // If we just parsed a left or an enclosing,
            // we need to also emit some match code.
            if (par_something == par_left) {
                par_match.write("| \"" + sname + "\",\"" + otherEnd(ename, namesToElements, enclosing_right) + "\"");
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

        for (Map.Entry<String, Set<Element>> pair : groups.entrySet()) {
            String k = pair.getKey();
            prc.write("let " + k + " = set ");
            Set<Element> els = pair.getValue();
            boolean sawOne = false;
            String sep = "[";
            for (Element e : els) {
                String ename = e.escapedShortName();
                if (shortOnly && e.shortName().length() > max) continue;
                prc.write(sep);
                prc.write("\"" + ename + "\"");
                sep = "; ";
                sawOne = true;
            }
            if (!sawOne) prc.write(sep);
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

    private static String otherEnd(String ename, HashMap<String, Element> h, Set<Element> enclosing_right) {
        String formulaOther = ename.replace("LEFT", "RIGHT");
        Element otherE = h.get(formulaOther);
        if (otherE != null && enclosing_right.contains(otherE)) {
            return otherE.escapedShortName();
        }
        // HACK for two exceptions in Unicode where s/LEFT/RIGHT/ is not good
        // enough. Sigh.
        if (ename.equals("LEFT_ARC_LESS_THAN_BRACKET"))
            return h.get("RIGHT_ARC_GREATER_THAN_BRACKET").escapedShortName();
        if (ename.equals("DOUBLE_LEFT_ARC_GREATER_THAN_BRACKET"))
            return h.get("DOUBLE_RIGHT_ARC_LESS_THAN_BRACKET").escapedShortName();
        throw new Error("Unmatched left end " + ename + " formulaOther=" + formulaOther + " otherE=" + otherE);

    }

}
