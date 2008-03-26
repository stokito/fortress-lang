PROJECT FORTRESS SUBVERSION REPOSITORY
------------------------------------
This is the top-level directory for all Fortress projects.  If you have
Subversion installed, it can be checked out by going to the directory
in which you want to check it out and issuing the following command:

svn checkout https://projectfortress.sun.com/svn/Community/trunk PFC

(The name "PFC" merely specifies the name of the directory you want
to check the code into. Feel free to substitute another directory
name if you prefer.)

You'll now have a subdirectory named 'PFC'.  Go into that
directory and you'll see several subdirectories:

Fortify: The Fortify tool for converting Fortress code into LaTeX,
both interactively and in batch mode.

ProjectFortress: The Fortress interpreter.  You'll need to build the
interpreter by following the instructions below for setting up your 
environment in order to have a complete Fortress installation.

SpecData: Machine-readable files used by the Fortress Language
Specification (e.g., a list of all reserved words).  Editors and 
other tools can make use of these files too.

Library: The home for all of the Fortress standard libraries.

bin: Shell scripts for our various projects. These are bash scripts; 
you will need an installation of bash on your system to run them.

You will also see the following files:

ant: A small bash script used for invoking the build script with 
specific Ant options. (This script defers to the script with the
same name in directory ProjectFortress.)

build.xml: The interpreter build script, written in Ant. (This
script defers to the script with the same name in directory
ProjectFortress.)

fortress.properties: This file defines several environment variables
used by the internals of the Fortress interpreter. (Normally, there is 
no reason to override the settings in this file.)

Setting up your environment
---------------------------
We assume you are using an operating system with a Unix-style shell
(for example, Solaris, Linux, Mac OS X, or Cygwin on Windows).

In your shell startup script, define environment variable
FORTRESS_HOME to point to the Fortress subdirectory you just checked
out. It is very important to set this  environment variable; it is 
used by several scripts and build files.

In your shell startup script, add $FORTRESS_HOME/bin to your path. 
The shell scripts in this directory are bash scripts. To run them, 
you must have bash accessible in /bin/bash. 

If you have Java 5 and Ant 1.6.5 or greater, make sure the following
environment variables are set in your startup script:

JAVA_HOME
ANT_HOME

(Although our scripts are sometimes able to guess the locations of 
JAVA_HOME and ANT_HOME, it is preferred that you set them manually.)

Once all of these environment variables are set, build the
interpreter by going to directory $FORTRESS_HOME and typing the command:

	./ant clean test

If that doesn't work, there's a bug in the interpreter; please issue a
bug report.

Once you do that, you can call the interpreter from any directory, on
any Fortress file, simply by typing one of the following commands at a
command line:

fortress compile somefile.fs{s,i} 
fortress [run] [-test] [-debug] somefile.fss arg...
fortress help

A command of the form "fortress compile somefile.fss" or 
"fortress compile somefile.fsi" calls the static checker on the given 
file and stores the result in a hidden "cache" directory. No visible 
object file is generated. (At present, the static checker has limited
functionality. Static type errors, for example, are not signaled.)
A File with suffix .fsi should contain a single API definition. The name
of the API should match the name of the file. Similarly, a file with the
suffix .fss should contain a single component definition. The name
of the component should match the name of the file.

A command of the form "fortress run somefile.fss" checks whether a cached
and up to date result of compiling the given file exists. If so, it runs 
the cached file. Otherwise, it compiles the given file and runs the result. 
This command can be abbreviated as "fortress somefile.fss". If the optional 
flag -test is given, the "run" function is not executed; instead,
all tests defined the given file are run. If the optional flag -debug is
given, stack traces from the underlying interpreter are displayed when 
errors are signaled. 

