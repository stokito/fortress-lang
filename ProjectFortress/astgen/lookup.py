#!/usr/bin/env python
"""
*******************************************************************************
Copyright 2008, Oracle and/or its affiliates.
All rights reserved.


Use is subject to license terms.

This distribution may include materials developed by third parties.

*******************************************************************************

This script simply looks up the definitions and comments for some Fortress AST nodes.
"""
import sys, re, os, optparse

# The relative path to the file containing node definitions.
FORTRESS_AST_PATH = './Fortress.ast'

def node_definition(names, options):
    """Return the given node's full definition, or None if it could not be found."""
    
    # Make the names unique.
    names = set([name.replace('*', '[a-zA-Z]*') for name in names])
    must_find = [name for name in names if name.isalpha()]
    
    # Keep a history of lines so we can backtrack to get comments.
    lines = []
    index = 0
    
    # Regex to check for node definitions.
    regex_flags = 0
    if options.I: regex_flags |= re.I
    regex = re.compile(r"^(\s+)(?:abstract )?(%s)\(" % '|'.join(names), regex_flags)
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
            yield ("".join(s[num_spaces-2:] for s in lines[read_from:]), True)
            start_def = None
            
            # Remove it from the list of names to find.
            if name in must_find: must_find.remove(name)
        
        index += 1
    
    # Yield the ones not found.
    for name in must_find:
        yield ("error: Node %s was not found in %s." % (name, FORTRESS_AST_PATH), False)


if __name__ == '__main__':
    # Change into the script's dir.
    os.chdir(os.path.dirname(sys.argv[0]))
    
    # Setup the options parser and help text.
    usage = "usage: %prog [options] node [...]"
    description = "Prints the definition and comments for each of the given nodes. Each node is " \
                  "a simple string. Asterisk (*) may be used as a wildcard, e.g. *Id* *Name Trait*"
    optparser = optparse.OptionParser(usage=usage, description=description)
    optparser.add_option("-i",
                         action="store_true", dest="I", default=False,
                         help="case insensitive search")
    (options, args) = optparser.parse_args()
    if not args:
        optparser.print_help()
        sys.exit()
    
    # Print the definition for each node given.
    print ""
    count = 0
    for definition, found in node_definition(args, options):
        print definition
        if found: count += 1
    print "notice: found %d match%s" % (count, count != 1 and 'es' or '')  
