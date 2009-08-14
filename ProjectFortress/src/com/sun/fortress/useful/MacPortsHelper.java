/*******************************************************************************
 Copyright 2009 Sun Microsystems, Inc.,
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

package com.sun.fortress.useful;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class MacPortsHelper {

    static  Memo1<String, TopSortItemImpl<String>> aTable() {
        return 
        new Memo1<String, TopSortItemImpl<String>>(new Factory1<String, TopSortItemImpl<String>>() {

            public TopSortItemImpl<String> make(String part1) {
                return new TopSortItemImpl<String>(part1);
            }

        });
    }
    
    static Memo1<String, TopSortItemImpl<String>> table = aTable();

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("java ... MacPortsHelper <portdepsdir> [<outdated>]");
            System.err.println("MacPortsHelper takes a directory expected to contain files named <portname>.");
            System.err.println("Each file contains the output of 'port deps <portname>");
            System.err.println("Such a directory could be produced by executing the (bash) commands: ");
            System.err.println("  mkdir portdeps");
            System.err.println("  for i in `port list installed | awk '{print $1'}` ; do ");
            System.err.println("    port deps $i > portdeps/$i");
            System.err.println("  done");
            System.err.println("The result is a topologically sorted list of ports, most dependent first.");
            System.err.println("The list may be uninstalled in order, or installed in reverse order.");
        } else {
            String dirname = args[0];
            File[] fileArray = Files.ls(dirname);
            Set<String> ports = new HashSet<String>();
            for (int i = 0; i < fileArray.length; i++) {
                File f = fileArray[i];
                String a = f.getName();
                ports.add(a);
            }
            for (int i = 0; i < fileArray.length; i++) {
                File f = fileArray[i];
                BufferedReader br = Useful.utf8BufferedFileReader(f);
                String a = f.getName();
                TopSortItemImpl<String> node = table.make(a);
                String b = br.readLine();
                while (b != null) {
                    b = b.trim();
                    // No sense creating an edge to pseudo-port
                    if (ports.contains(b)) node.edgeTo(table.make(b));
                    b = br.readLine();
                }
            }

            // Handle rooted subsets.
            if (args.length > 1) {
                int argl = 1;
                String s = args[1];
                boolean rebuild = false;
                if ("-rebuild".equals(s)) {
                    rebuild = true;
                    argl = 2;
                } else if ("-rooted".equals(s)) {
                    argl = 2;
                }
                
                //BufferedReader br = Useful.utf8BufferedFileReader(s);
                

                Memo1<String, TopSortItemImpl<String>> newTable = aTable();
                Memo1<String, TopSortItemImpl<String>> rtable = reverse(table);
                
                if (rebuild) {
                    for (int argi = argl; argi < args.length; argi++) {
                        String root = args[argi].trim();
                        if (! newTable.known(root))
                            copyRooted(table, newTable, root);
                    }

                    Memo1<String, TopSortItemImpl<String>> buildTable = aTable();
                    for (TopSortItemImpl<String> dep : newTable.values()) {
                        String sdep = dep.x;
                        if (! buildTable.known(sdep)) 
                            copyRooted(rtable, buildTable, sdep);
                    }
                    table = reverse(buildTable);
                } else {
                    for (int argi = argl; argi < args.length; argi++) {
                        String root = args[argi].trim();
                        if (! newTable.known(root))
                            copyRooted(rtable, newTable, root);
                    }

                    table = reverse(newTable);
                }            
            }

            List<TopSortItemImpl<String>> ordered = TopSort.depthFirst(table.values());
            for (TopSortItemImpl i : ordered) {
                System.out.println(i.x);
            }
        }
    }

    private static void copyRooted(
            Memo1<String, TopSortItemImpl<String>> table,
            Memo1<String, TopSortItemImpl<String>> newTable, String root) {
            
            TopSortItemImpl<String> new_root = newTable.make(root);
            TopSortItemImpl<String> node = table.make(root);
            
            for (TopSortItemImpl<String> succ : node.succs ) {
                String s = succ.x;
                if (newTable.known(s))
                    continue;
                TopSortItemImpl<String> new_succ = newTable.make(s);
                new_root.edgeTo(new_succ);
                copyRooted(table, newTable, s);
            }
        
    }
    
    private static Memo1<String, TopSortItemImpl<String>> reverse
        (Memo1<String, TopSortItemImpl<String>> table) {
        Memo1<String, TopSortItemImpl<String>> newTable = aTable();
        for (TopSortItemImpl<String> node : table.values()) {
            TopSortItemImpl<String> new_node = newTable.make(node.x);
            for (TopSortItemImpl<String> succ : node.succs) {
                TopSortItemImpl<String> new_succ = newTable.make(succ.x);
                new_succ.edgeTo(new_node);
            }
        }
        return newTable;
    }

}
