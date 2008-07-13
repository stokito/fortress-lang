#!/usr/bin/env python
"""
*******************************************************************************
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
*******************************************************************************

This script simply looks up the definitions and comments for some Fortress AST nodes.
"""
import sys, re, os, optparse

# The relative path to the file containing node definitions.
FORTRESS_AST_PATH = './Fortress.ast'

def node_definition(names):
    """Return the given node's full definition, or None if it could not be found."""
    # Make the names unique.
    names = set(names)
    
    # Keep a history of lines so we can backtrack to get comments.
    lines = []
    index = 0
    
    # Regex to check for node definitions.
    regex = re.compile(r"^(\s+)(?:abstract )?(%s)\(" % '|'.join(names), re.I)
    match = None
    
    # Index of the line that starts a definition.
    start_def = None
    
    # Index of the line that starts the most recent comment.
    start_comment = None
    
    # Open file and loop over it line by line.
    f = open(FORTRESS_AST_PATH, 'r+')
    for line in f:
        lines.append(line)
        
        # If beginning a comment, save this index.
        if line.lstrip().startswith('/*'):
            start_comment = index
        
        # If looking for a definition.
        if start_def is None:
            match = regex.match(line)
            if match: start_def = index # Found node; save the index.
        
        # If reached end of definition.
        if start_def is not None and ';' in line:
            num_spaces = len(match.group(1))
            name = match.group(2)
            
            # Start reading from the definition's line, or its comment's line.
            read_from = start_def
            if lines[start_def-1].strip() == '*/':
                read_from = start_comment
                
            # Yield and reset for next node.
            yield "".join(s[num_spaces-2:] for s in lines[read_from:])
            start_def = None
            names.remove(name)
        
        index += 1
    
    # Yield the ones not found.
    for name in names:
        yield "error: Node %s was not found in %s.\n" % (name, FORTRESS_AST_PATH) 


if __name__ == '__main__':
    # Change into the script's dir.
    os.chdir(os.path.dirname(sys.argv[0]))
    
    # Setup the options parser and help text.
    usage = "usage: %prog node [...]"
    description = "Prints the definition and comments for each of the given nodes."
    optparser = optparse.OptionParser(usage=usage, description=description)
    (options, args) = optparser.parse_args()
    if not args: optparser.print_help()
    
    # Print the definition for each node given.
    print ""
    for definition in node_definition(args):
        print definition
