#!/usr/bin/python
#
# Calibrates the expected output for the given test files. Run this script
# with a list of .test files to calibrate. For each one, it will run the
# source file and get the REAL output, compare it against what was expected
# in the .test file, and -- if they differ -- write back into the .test file
# the actual output as expected.

import sys
import os, os.path
import re
import popen2

LINE_DEF_REGEX = re.compile(r"^(\w+)(?:=(.*))?$")
VALUE_SPACES_REGEX = re.compile(r"^ +", re.M)
UNIX_PATH_REGEX = re.compile(r"[ ^](/[^/ ]+)+")
FORTRESS_HOME = os.path.realpath(os.getenv("FORTRESS_HOME"))
FORTRESS_PATH = "%s/bin/fortress" % FORTRESS_HOME
COPYRIGHT = \
"""#    Copyright 2009, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#

"""

 # Get the value for ${FORTRESS_SOURCE_PATH}
(tmp, _) = popen2.popen2("%s expand FORTRESS_SOURCE_PATH" % FORTRESS_PATH)
FORTRESS_SOURCE_PATH = tmp.read().rstrip('\n')

def main():
    try:
        if not os.path.isfile(FORTRESS_PATH):
            raise RuntimeError("no fortress executable at %s" % FORTRESS_PATH)
        
        test_files = sys.argv[1:]
        confirm(test_files)
        count = sum(calibrate_test_file(f) for f in test_files)
        print "* updated %d test%s" % (count, count != 1 and "s" or "")

    except KeyboardInterrupt:
        print "\nstopped"
        sys.exit(1)
        
    except RuntimeError, e:
        print str(e)
        sys.exit(1)


# Confirm that the user wants to do this!
def confirm(test_files):
    n = len(test_files)
    prompt = "Are you sure you want to calibrate %d test%s? [y/n] " % (n, n != 1 and "s" or "")
    choice = "X"
    while choice not in "yn":
        choice = raw_input(prompt).lower()
    if choice != "y":
        print "quitting..."
        sys.exit(0)


# Calibrate the given test file.
def calibrate_test_file(test_file):
    print "calibrating %s..." % test_file
    try:
    
        # Get all the config info.
        config = parse_test_file(test_file)
    
        # Get the source file.
        source = get_source_file(config)
        if not source: raise RuntimeError("error getting source file from %s" % test_file)
    
        # Which mode to run?
        mode = get_mode(config)
    
        # Run fortress to get output.
        (out, err) = run_fortress(mode, source, get_tests_dir(config))
    
        # Compare the outputs
        (out_okay, err_okay) = compare_outputs(mode, config, out, err)
        if out_okay and err_okay: return 0
    
        # If stdout different, add in the new one.
        if not out_okay:
            #print "* %s: different stdout!" % test_file
            key = "%s_out_equals" % mode
            replace_config_value(config, key, format_value(out))
    
        # If stderr different, add in the new one.
        if not err_okay:
            #print "* %s: different stderr!" % test_file
            key = "%s_err_equals" % mode
            replace_config_value(config, key, format_value(err))
    
        # Write back to file!
        output_config(config, test_file)
        print "* wrote out new %s" % test_file
        return 1
        
    except RuntimeError, e:
        print e
        return 0

# Output the given config into the given test file.
def output_config(config, test_file):
    f = open(test_file, 'w')
    f.write(COPYRIGHT)
    for (k, v) in config:
        if v is None: line = k
        else: line = "%s=%s" % (k, v)
        f.write(line+"\n")
    f.close()


# Substitute in the config the new value for `name` for the old one.
def replace_config_value(config, name, value):
    for (i, (k, _)) in enumerate(config):
        if k == name:
            config[i] = (name, value)
            
            # Check if there were any possible file system paths.
            m = UNIX_PATH_REGEX.search(value)
            if m: print "* WARNING! possible unix path detected in output: %s" % m.group(0)
            return


# Format a value for use in the config.
def format_value(value):
    value = VALUE_SPACES_REGEX.sub("\\ ", value)
    value = value.replace("\n", "\\n\\\n")
    if value.endswith("\\\n"): value = value[:-2]
    if "\n" in value: value = "\\\n" + value
    return value


# Figure out which mode to run fortress in. Works on compile and typecheck
# (preferring compile).
def get_mode(config):
    compile = False
    typecheck = False
    for (k, _) in config:
        if k == "compile": compile = True
        if k == "typecheck": typecheck = True
    if compile: return "compile"
    elif typecheck: return "typecheck"
    else: return None


# Look in the given config for the source file.
def get_source_file(config):
    for (k, v) in config:
        if k == "tests":
            if v.endswith(".fss"): return v
            elif v.endswith(".fsi"): return v
            else: return "%s.fss" % v
    return None


# Get the directory for these tests. Returns (name, path) where name is the
# name of the variable holding the value, and path is the value itself.
def get_tests_dir(config):
    for (k, v) in config:
        if k.endswith("_TESTS_DIR"):
            tests_dir = v.replace("${FORTRESS_AUTOHOME}", FORTRESS_HOME)
            return (k, tests_dir)
    raise RuntimeError("error locating tests dir")


# Run fortress and return the stdout and stderr.
def run_fortress(mode, source, tests_dir):
    (out, _, err) = popen2.popen3("%s %s %s" % (FORTRESS_PATH, mode, source))
    out, err = out.read(), err.read()

    # Escape the slashes and quotes.
    out = out.replace('\\', '\\\\').replace('"', '\\"')
    err = err.replace('\\', '\\\\').replace('"', '\\"')
    
    # Replace the name of the tests directory.
    tests_dir_name, tests_dir_path = tests_dir
    out = out.replace(tests_dir_path, "${%s}" % tests_dir_name)
    err = err.replace(tests_dir_path, "${%s}" % tests_dir_name)
    
    # Replace location of source files from repository.
    out = out.replace(FORTRESS_SOURCE_PATH, "${FORTRESS_SOURCE_PATH}")
    err = err.replace(FORTRESS_SOURCE_PATH, "${FORTRESS_SOURCE_PATH}")
             
    # Replace FORTRESS_HOME after everything else.
    out = out.replace(FORTRESS_HOME, "${FORTRESS_AUTOHOME}")
    err = err.replace(FORTRESS_HOME, "${FORTRESS_AUTOHOME}")
    return (out, err)


# Compare the stdout and stderr of the program with those expected.
def compare_outputs(mode, config, out, err):
    try:
        # Get the expected outputs out of the config.
        expected_err = None
        expected_out = None
        for (k, v) in config:
            if k == "%s_err_equals" % mode:
                expected_err = v.replace("\\n", "\n")
            elif k == "%s_out_equals" % mode:
                expected_out = v.replace("\\n", "\n")
        if expected_err is None or expected_out is None:
            raise RuntimeError
        
        # Turn the expected output into a regex to match against. Here we must
        # perform magic to catch the "\ " spacing thing.
        out_regex = re.compile(re.escape(expected_out).replace("\\\\\\ ", r" +"))
        err_regex = re.compile(re.escape(expected_err).replace("\\\\\\ ", r" +"))
        
        # Compare!
        out_okay = out_regex.match(out) is not None
        err_okay = err_regex.match(err) is not None
        return (out_okay, err_okay)

    except RuntimeError:
        raise RuntimeError("error comparing output")

# Parse the test file, returning a dictionary of the config values.
# `test_file` must be a file object opened for reading
def parse_test_file(test_file):
    # Try to open the file
    f = open(test_file, 'r')
    if not f: raise RuntimeError("error reading test file %s" % test_file)
    
    try:
        config = []
        name = None
        for line in f:
            line = line.strip()
            
            # If the line is empty, keep going
            if (not name and not line) or line.startswith("#"): continue

            # If not already in a value definition, match a definition.
            if not name:
                m = LINE_DEF_REGEX.match(line)
                
                if not m: raise RuntimeError()
                name, value = m.group(1), m.group(2)
                #print "found", name, value

                # If this is not a key=value pair or one that is closed.
                if value is None or not line.endswith("\\"):
                    config.append((name, value))
                    #print "closing", name
                    name = None

                # If this is a key=value pair that is open.
                else: 
                    content = value[:-1]
                    #print "eating", name

            elif not line.endswith("\\"):
                content += line
                config.append((name, content))
                #print "closing", name
                name = None

            # If we're in a def and it's still open, keep reading.
            else:
                content += line[:-1]
                #print "eating", name

        # We should be done now.
        if name: raise RuntimeError()

        return config

    except RuntimeError:
        raise RuntimeError("error parsing test file %s" % test_file)


if __name__ == '__main__':
    main()
