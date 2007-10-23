PROJECT FORTRESS SUBVERSION REPOSITORY
------------------------------------
This is the top-level directory for all Fortress projects.  If you have
Subversion installed, it can be checked out by going to the directory
in which you want to check it out and issuing the following command:

svn checkout https://projectfortress.sun.com/svn/Community/trunk Fortress

You'll now have a subdirectory named 'Fortress'.  Go into that
subdirectory and you'll see several subprojects:

Fortify: The Fortify tool for converting Fortress code into LaTeX,
both interactively and in batch mode.

ProjectFortress: The Fortress interpreter.  You need to build the
interpreter in order to have a complete Fortress installation.
See the README in this directory for instructions on building the
interpreter.

SpecData: Machine-readable files used by the Fortress Language
Specification (e.g., a list of all reserved words).  Eventually,
editors and other tools can make use of these files too.

StandardLibrary: The home for all of the Fortress standard libraries.

bin: Shell scripts for our various projects.

Setting up your environment
-----------------------
In your shell startup script, define environment variable
FORTRESS_HOME to point to the Fortress subdirectory you just checked
out.

In your shell startup script, add $FORTRESS_HOME/bin to your path.

(This step is necessary if you want to build the interpreter): If you
have Java 5 and Ant 1.6.5 or greater, make sure the following
environment variables are set in your startup script:

JAVA_HOME
ANT_HOME

Once all of these environment variables are set, you can build the
interpreter by going to directory $FORTRESS_HOME/ProjectFortress and
typing the command:

	./ant clean test

If that doesn't work, there's a bug in the interpreter; please issue a
bug report.

Once you do that, you can call the interpreter from any directory, on
any Fortress file, simply by typing "fortress <YOUR FILE's NAME>".
