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

package com.sun.fortress.useful;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.fortress.shell.Files;

public class MacPortsHelper {

    static Memo1<String, TopSortItemImpl<String>> table = new Memo1<String, TopSortItemImpl<String>>(
            new Factory1<String, TopSortItemImpl<String>>() {

                public TopSortItemImpl<String> make(String part1) {
                    return new TopSortItemImpl<String>(part1);
                }

            });

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err
                    .println("MacPortsHelper takes a directory expected to contain files named <portname>.");
            System.err
                    .println("Each file contains the output of 'port deps <portname>");
            System.err
                    .println("Such a directory could be produced by executing the (bash) commands: ");
            System.err.println("  mkdir portdeps");
            System.err
                    .println("  for i in `port list installed | awk '{print $1'}` ; do ");
            System.err.println("    port deps $i > portdeps/$i");
            System.err.println("  done");
            System.err
                    .println("The result is a topologically sorted list of ports, most dependent first.");
            System.err
                    .println("The list may be uninstalled in order, or installed in reverse order.");
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
                    if (ports.contains(b))
                        node.edgeTo(table.make(b));
                    b = br.readLine();
                }
            }
            List<TopSortItemImpl<String>> ordered = TopSort.breadthFirst(table
                    .values());
            for (TopSortItemImpl i : ordered) {
                System.out.println(i.x);
            }
        }
    }

}
