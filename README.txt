PROJECT FORTRESS SUBVERSION REPOSITORY
--------------------------------------

This README exists in the top-level directory of the Fortress project.
Information about Fortress can be found at the following web site:

    http://projectfortress.sun.com

If you have Subversion installed, you can check out the Fortress
repository by going to the directory in which you want to check it out
and issuing the following command:

    svn checkout https://projectfortress.sun.com/svn/Community/trunk PFC

(The name "PFC" merely specifies the name of the directory you want
to check the code into.  Feel free to substitute another directory
name if you prefer.)

You'll now have a subdirectory named 'PFC'.  Go into that
directory and you'll see several subdirectories:

Fortify: The Fortify tool for converting Fortress code into LaTeX,
both interactively and in batch mode.  Scripts are provided for
conveniently producing rendered Fortress code in LaTeX documents,
for producing PDF "doc" files from Fortress source code, etc.  See
Fortify/fortify-doc.txt for more information.

ProjectFortress: The Fortress interpreter.  You'll need to build the
interpreter by following the instructions below for setting up your
environment in order to have a complete Fortress installation.

Emacs: A directory holding the Emacs Lisp file fortress-mode.el,
which defines a Fortress mode for Emacs.  To use this file, load
it from your .emacs file with the following command:

    (load (concat (getenv "FORTRESS_HOME")
                  "/contrib/Emacs/fortress-mode.el"))
    (push '("\\.fs[si]$" . fortress-mode) auto-mode-alist)

If you wish to use the Fortify package to format Fortress source code
into LaTeX, you should also add the following to your .emacs (for more
information about fortify, see Fortify/fortify-doc.txt):

    (load (concat (getenv "FORTRESS_HOME")
                  "/Fortify/fortify.el"))

Vim: A directory containing vim script files for syntax highlighting.
To enable syntax highlighting for fortress code copy the sub-directories
under Vim/ to your ~/.vim directory.
  $ mkdir ~/.vim
  $ cp -a Vim/ftdetect Vim/syntax Vim/ftplugin ~/.vim/.
If your cp command does not accept the -a option then use -r
  $ cp -r Vim/ftdetect Vim/syntax Vim/ftplugin ~/.vim/.

You should also add the following line to your ~/.vimrc file
  au BufNewFile,BufRead *.fsi,*.fss set ft=fortress

SpecData: Machine-readable files used by the Fortress Language
Specification (e.g., a list of all reserved words).  Editors and other
tools may also benefit from using these files.  Moreover, all examples
included in the language specification are included in the directory
SpecData/examples.

Specification: A directory containing a PDF of the Fortress Language
Specification, Version 1.0.

Library: The home for all of the Fortress standard libraries.

bin: Shell scripts for our various projects.  These are bash scripts;
you will need an installation of Bash on your system to run them.  To
make these scripts "auto-homing", script "forfoobar" begins with the
line
    FORTRESS_HOME=`${0%forfoobar}fortress_home`
This replaces 'forfoobar' in whatever was used to invoke the script
with 'fortress_home', runs that command, and assigns its output to
FORTRESS_HOME for the remainder of the scripts.  'fortress_home'
determines the location of fortress_home if it is not otherwise
specified.  This command can also be used in your own build files;
for example, if you include the fortify macros in a LaTeX file
    \input{$FORTRESS_HOME/Fortify/fortify-macros}
you might precede the latex command with
    FORTRESS_HOME="`fortress_home`"
It is also possible to set FORTRESS_HOME in your environment, but
if you have multiple versions of Fortress installed this can cause
confusion and build problems.

You will also see the following files:

ant: A small bash script used for invoking the build.xml with
specific Ant options.  (This script defers to the script with the
same name in directory ProjectFortress.)

build.xml: The interpreter build script, written in Ant.  (This
script defers to the script with the same name in the directory
ProjectFortress.)

fortress.properties: This file defines several environment variables
used by the internals of the Fortress interpreter.  (Normally, there is
no reason to override the settings in this file.)


SETTING UP YOUR ENVIRONMENT
---------------------------

We assume you are using an operating system with a Unix-style shell
(for example, Solaris, Linux, Mac OS X, or Cygwin on Windows).  You
will need to have access to the following:

* J2SDK 1.6 or later.  See http://java.sun.com/javase/downloads/index.jsp
* Ant 1.6.5 or later.  See http://ant.apache.org/bindownload.cgi
* Bash version 2.5 or later, installed at /bin/bash.
  See http://www.gnu.org/software/bash/

Assume FORTRESS_HOME points to the PFC directory you checked out.  On
Unix-like systems this should be a matter of using export or setenv.  If
you are using Cygwin, one user reports success with the following
command line for setting FORTRESS_HOME:
  export FORTRESS_HOME=`cygpath -am cygwin/path/to/fortress/install/directory`
e.g.:
  export FORTRESS_HOME=`cygpath -am ${HOME}/tools/fortress`


In your shell startup script, add $FORTRESS_HOME/bin to your path.
The shell scripts in this directory are Bash scripts.  To run them,
you must have Bash accessible in /bin/bash.

Make sure the following environment variables are set in your startup
script:

    JAVA_HOME
    ANT_HOME

(Although our scripts are sometimes able to guess the locations of
JAVA_HOME and ANT_HOME, it is preferred that you set them manually.)

Once all of these environment variables are set, build the interpreter
by going to the directory FORTRESS_HOME and typing the command:

    ./ant clean compile

If that doesn't work, there's a bug in the interpreter; please issue a
bug report.

Once you have built the interpreter, you can call it from any directory,
on any Fortress file, simply by typing one of the following commands at a
command line:

    fortress [walk] [-test] [-debug interpreter] somefile.fss arg...
    fortress help

The first time you run a Fortress program, the static checker is
called on the given file and the results are stored in a cache
directory (by default this cache is kept in default_repository/caches
in the root of your Fortress distribution).  No user-visible object
file is generated.  A file with suffix .fsi should contain a single API
definition.  The name of the API should match the name of the file.
Similarly, a file with the suffix .fss should contain a single
component definition.  The name of the component should match the name
of the file.

A command of the form "fortress walk somefile.fss" checks whether a
cached and up to date result of compiling the given file exists.  If
so, it runs the cached file.  Otherwise, it processes the given file
and runs the result.  This command can be abbreviated as "fortress
somefile.fss".  If the optional flag -test is given, all test
functions defined in the given file are run instead.  If the optional
flag "-debug interpreter" is given, stack traces from the underlying
interpreter are displayed when errors are signaled.

If all else fails, look at the script bin/fortress to see if your system
has peculiarities (for example cygwin requires ; separators in the
classpath).


USING ECLIPSE
-------------
There exists a .project file in the directory ${FORTRESS_HOME}.
Import this project into Eclipse.

There exists a file called ${FORTRESS_HOME}/DOTCLASSPATH in
the repository. Copy this file to ${FORTRESS_HOME}/.classpath.
If you are using the Java 5.0 jdk under Windows or Linux, you will need
to add an entry to ${JAVA_HOME}/lib/tools.jar to the classpath.

Setting up Eclipse to follow the Fortress project coding style
conventions is a two-step process. The following instructions are
known to work on Eclipse 3.4, and should work on Eclipse 3.3 as well.
These will change preferences for all your Eclipse projects.
Open up Eclipse Preferences to start configuring your global
settings. First select General --> Editors --> Text Editors
and make sure the checkbox is enabled for "Insert spaces for tabs".
Second select Java --> Code Style --> Formatter and click on the "Edit..."
button. Change the Tab policy to "Spaces only" and give the profile a new
name (recommended name: "Spaces only").  Click "OK" and you are finished.

DEMO PROGRAMS
-------------

The directory ProjectFortress/demos/ contains some demonstration Fortress
programs.  Among them are:

buffons.fss: Buffon's needle.  Estimates pi using a Monte Carlo
simulation.

lutx.fss: Naive dense LU decomposition.  Demonstrates how to define
new subclasses of Array2.

conjGrad.fss: Conjugate gradient, including the snapshot from the NAS
CG benchmark that you've seen in many Fortress talks.  Uses the Sparse
library for sparse matrices and vectors.

sudoku.fss: Solve a simple sudoku by elimination.  Includes a
tree-based set implementation.

aStar.fss: Generic A* search, accompanied by a specific instance for
solving sudoku that cannot be solved by elimination alone.

Lambda.fss: A simple interpreter for the lambda calculus that permits
top-level binding and reduces to both WNHF and NF.  If you're curious
how to parse text using the Fortress libraries, you should look here
(it's presently far more painful than we'd like).


TEST PROGRAMS
-------------

The directory ProjectFortress/tests/ contains some Fortress programs
to test the interpreter.  Test programs that are supposed to fail
(for example, storing a String into a ZZ32-typed mutable) have names
that are prefixed with XXX.

The directory ProjectFortress/static_tests/ contains some Fortress
programs to test the static end.  Test programs that are supposed to
fail have names that are prefixed with XXX.  Test programs that are
supposed to pass static disambiguation then fail have names that
are prefixed with DXX.

The directory ProjectFortress/parser_tests/ contains some Fortress
programs to test the parser.  Test programs that are supposed to
fail to be parsed have names that are prefixed with XXX.

The directory ProjectFortress/not_passing_yet/ contains some Fortress
programs that should pass, but do not.  For example, if we had a test
file containing an error that should be detected, but it isn't, that
would be contained in ProjectFortress/not_passing_yet with a name
prefixed with XXX.  Test programs in this directory should pass
the parser.


COMPONENTS
----------

Fortress currently lacks a full-blown component system.  All the code
in your Fortress program should reside in API and component file pairs.
If you take a look at the Fortress programs in ProjectFortress/tests/,
ProjectFortress/demos/, or SpecData/examples, you'll see that they have
the same overall structure:


component MyComponent
  exports Executable

  ...  Your program here ...

  run():() = ...

end


LANGUAGE FEATURES THAT ARE IMPLEMENTED
--------------------------------------

* Object and trait declarations, including polymorphic traits.
  Constructor invocations must *always* provide the static arguments
  explicitly.

* Overloaded functions and ordinary methods.  Top-level overloaded
  functions can be polymorphic.  Nested functions and methods must be
  monomorphic.

* Polymorphic top-level functions and methods, so long as the methods
  are not overloaded.

* Checking and inference of argument types to functions, methods, and
  operators.  These checks use the dynamic types of the arguments.
  Return types are NOT checked.  Inference of static parameters is not
  complete yet; it is often necessary to provide static arguments
  explicitly.  It is *always* necessary to do so in a constructor call
  and in any situation where a static parameter occurs only in the result
  and not in the arguments to a function.  For example, you must always
  provide the array element type E and size n when invoking the
  factory array1[\E,n\]().

* Arrays of up to three dimensions.  Note that there isn't yet a
  single overarching Array type.  For more details on the array types
  and operations defined see below.  In particular, note that array
  comprehensions are not yet implemented; the array types provide
  functions to work around this lack.  Another caveat: due to a bug we
  haven't fully understood, some (but not all) uses of the compact
  notation T[n,m] for an array type cause the interpreter to fail.
  Desugaring the code by hand to e.g. Array2[\T,0,n,0,m\] works around
  this bug.

* Array aggregates except singleton arrays.

* Parallel tupling and argument evaluation.

* Parallel for loops over simple ranges such as 0#n.

* Sequential for loops over simple ranges.  The functional method seq()
  and the equivalent function sequential() can be used to turn any
  Generator into a SequentialGenerator.

* While loops, typecase, if, etc.  Note that for parametric types
  typecase isn't nearly as useful as you might think, since it cannot
  bind type variables; we are working to address this shortcoming.

* The "atomic" construct uses code based on the DSTM2 library.  Nested
  transactions are flattened.  We use their obstruction free algorithm
  with a simple lowest-thread-wins contention manager.  Reduction
  variables in for loops are not yet implemented, so perform an
  explicit atomic update or just use a reduction expression instead.

* throw and catch expressions.

* Generators.

* at expressions.

* spawn

* also (multiple parallel blocks)


LANGUAGE FEATURES THAT ARE NOT IMPLEMENTED
------------------------------------------

* Numerals with radix specifiers (which implies that some numerals may be
  recognized as identifiers)

* Unicode names

* Dimensions and units

* Static arguments: nat (using minus), int, bool, dimension, and unit

* Modifiers

* Keyword arguments

* Where clauses

* Coercion

* Constraint solving for nat parameters

* Reduction variables

* Distributions

* Any of the types which classify operator properties

* Any of the bits and storage types

* Non-RR64 floats

* Integers other than ZZ32 and ZZ64

* Use of ZZ64 for indexing (the JVM uses 32-bit indices)


CHANGES SINCE FORTRESS LANGUAGE SPECIFICATION v.1.0 BETA
--------------------------------------------------------

* Fortress 1.0 is the first  release of the Fortress language
interpreter is the first to be released in tandem with the language
specification, available as open source and online at:

    http://projectfortress.sun.com

Each example in the specification is automatically generated from
a corresponding working Fortress program which is run by every test run
of the interpreter.

* To synchronize the specification with the implementation, it was
necessary to temporarily drop the following features from the specification:

 - Static checks (including static overloading checks)
 - Static type inference
 - Qualified names (including aliases of imported APIs)
 - Getters and setters
 - Array comprehensions
 - Keyword parameters and keyword expressions
 - Most modifiers
 - Dimensions and units
 - Type aliases
 - Where clauses
 - Coercions
 - Distributions
 - Parallel nested transactions
 - Abstract function declarations
 - Tests and properties
 - Syntactic abstraction

* Libraries have significantly changed.

* Syntax and semantics of the following features have changed:
 - Tuple and functional arguments
 - Operator rules: associativity, precedence, fixity, and juxtaposition
 - Operator declaration
 - Extremum expression
 - Import statement
 - Multiple variable declaration
 - Typecase expression

* The following features have been added to the language:
 - "native" modifier
 - Operator associativity
 - Explicit static arguments to big operator applications

* The following features have been eliminated from the language:
 - Identifier parameters
 - Explicit self parameters of dotted methods
 - Empty extends clauses
 - Local operator declarations
 - Shorthands for Set, List, and Map types
 - Tuple type encompassing all tuple types

* Significantly more examples have been added.


THE DEFAULT LIBRARIES
---------------------

The components ProjectFortress/LibraryBuiltin/FortressBuiltin.fsi,
ProjectFortress/LibraryBiltin/NativeSimpleTypes.fss and
Library/FortressLibrary.fss are imported implicitly whenever any
Fortress program is run.


BUILT-IN TYPES
--------------

There are a bunch of types that are defined internally by the Fortress
interpreter.  With the exception of Any, these cannot be overridden.
The built-in types are found in
ProjectFortress/LibraryBuiltin/FortressBuiltin.fsi and
NativeSimpleTypes.fsi; documentation for the released version of these
libraries can be found in the accompanying specification release.
Most built-in types do not have any methods.  Note that the types
found in FortressBuiltin do not have methods.

Tuple and arrow types are always built in, and cannot be overridden in
any way.

Note that there isn't (yet) a trait Object!  Eventually user-written
trait and object declarations will extend Object by default; right now,
they instead extend Any by default.  We plan to migrate to a new
infrastructure for primitive objects (based on the one used for
Boolean in NativeSimpleTypes) at which point we will remedy this
situation.

Meanwhile, operations on the primitive types in FortressBuiltin can be
found in Library/FortressLibrary.fsi; again these primitive are
documented in the specification as well.  Note in particular that in
the absence of coercion, you may occasionally need to make use of widen
and narrow to convert between ZZ32 and ZZ64.


LIBRARY HIGH POINTS
-------------------

Your best guide to library functionality is the library code itself;
this can be found in Library/ and in ProjectFortress/LibraryBuiltin.
The APIs for these libraries can also be found in the language
specification (note, though, that if you downloaded the latest version
of the Fortress implementation then the two may differ).  This section
provides an overview of things you may not immediately realize are
there.

Juxtaposition of strings means string append.  You may also
find the BIG STRING operation (that concatenates strings) useful.

Several functions attempt to convert data of type Any to a string.
These include print(), println(), assert(), and juxtaposition of Any
with a string.  Right now, the FortressBuiltin types are printed using
internal magic, and object types are printed using the toString
method.  The consequence of this is that you will see a runtime error
if you attempt to print an object without first defining a toString
method.

In the absence of array comprehensions, there are several ways to
create and initialize an array (in these examples a 1-D array, but the
2- and 3-D arrays work the same way):

The simplest is to use an aggregate expression (this seems to fail at
top level in your program, which is a known bug):
    z : ZZ32[3] = [1 2 3]

If you know the size statically (it is a static parameter to your
function, or is fixed at compile time):

    a : T[size] = array1[\T,size\]()  (* lower bound 0 *)
    a[i] := f(i),  i <- a.bounds()

or:
    a : T[size] = array1[\T,size\](initialValue)

or:
    a : T[size] = array1[\T,size\](fn (index:ZZ32) => ...)

If you are computing the size at run time:
    a = array[\T\](size)
    a[i] := f(i),  i <- a.bounds()

or:
    a = array[\T\](size).fill(initialValue)

or:
    a = array[\T\](size).fill(fn (index:ZZ32) => ...)

At the moment, to create a non-0-indexed array you need to create a
correctly-sized 0-indexed array as described above, then use the
shift(newlower) method to shift the lower index.  Thus, to create an
nxn 1-indexed array you can do something like this:

    a = array2[\T,n,n\]().shift(1,1)

The replicate[\T\]() method on arrays is a little unintuitive at
first.  It creates a fresh array whose element type is T but whose
bounds are the same as the bounds of the array being replicated.  When
data distribution is fully implemented, it should respect that as well.
It is a bit like saying array[\T\](a.bounds().upper()) for 0-indexed
arrays but is slightly more graceful and deals well with non-0-indexed
arrays.

You can convert any array to use 0 indexing simply by indexing it with
an empty range:
    a[:] or a[#]  ==>  a, only 0-indexed.

Any operation that yields a subarray of an underlying array shares
structure.  If you want a fresh copy of the data, use the copy() method.

To assign the contents of array a to array b, you can use:

    a.assign(b)

if a is freshly allocated.  The following should work all the time:

    a[:] := b[:]

Right now type-level ranges don't really exist, so if you want to
operate on subarrays with statically type-checked bounds, you'll need
to work with the subarray method:

    subarray[\nat b, nat s, nat o\]():Array1[\T, b, s\]

This returns a structure-sharing subarray with base b and size s
starting from offset o in the current array.

The special factory functions vector and matrix are restricted to numeric
argument types and static dimensionality:

    x' : ZZ64[1000] = vector[\ZZ64,1000\](17)

At the moment, any Array1 or Array2 whose element type extends Number
is considered to be a valid vector or matrix respectively (this will
eventually be accomplished by coercion, and vectors will be a distinct
type).  Note that the t() method on matrices is transposition, and
will eventually be replaced by opr ()^T.


GENERATORS, REDUCTIONS, and COMPREHENSIONS
------------------------------------------

Defining new generators is discussed in detail in the Fortress
language specification, but if you're trying it yourself for the first
time, you may find it instructive to browse the source code of the libraries.


DEFINING NEW PRIMITIVE FUNCTIONS
--------------------------------

It is relatively easy to add new primitive functions to Fortress.  To
do this, you simply invoke the builtinPrimitive function with the name
of a loadable Java class which extends glue.NativeApp.  Useful
subclasses are NativeFn1 and NativeFn2, and any of the classes in
glue.prim (particularly the classes in glue.prim.Util).  Here's a
sample native binding, which defines the floor operator which returns
an integer:

    opr |\a:RR64/|:ZZ64 = builtinPrimitive("glue.prim.Float$IFloor")

You should *not* mention the type parameter to builtinPrimitive when
invoking it; doing so will confuse the interpreter.  Note also that
the interpreter requires that you declare appropriate argument and
return types for your native functions as shown above.  If you give an
incorrect type declaration on the Fortress side, you'll get
non-user-friendly error messages when the Java code is run.


DEFINING NEW PRIMITIVE CLASSES
------------------------------

To define a new primitive class, you will need to write a native
component.  Examples of these can be found in Library; anything that
starts with "native component" is a native component.  Here's the
first few lines of File.fss:

native component File
import FileSupport.{...}
export File

private language="java"
private package="com.sun.fortress.interpreter.glue.prim"

object FileReadStream(filename:String)
        extends { ReadStream, FileStream}
    getter fileName():String =
      builtinPrimitive(
        "com.sun.fortress.interpreter.glue.prim.FileReadStream$fileName")
    ....


Note that we import a non-native component that defines traits
mentioned in the extends clause.  The first two bindings must be
language and package in that order; right now only language="java" is
supported, and the package is where the backing class will be found.
In com.sun.fortress.interpreter.glue.prim.FileReadStream defines the
corresponding backing data type.  Note that FileReadStream extends
Constructor, and defines an inner class that extends FOrdinaryObject
that represents the actual values that get passed around at run time.

The methods must extend NativeMethod, but are otherwise referenced
using builtinPrimitive just as for top-level functions.

A native class can contain a mix of native and non-native method
code.  Note, however, that the namespace in which a native object is
defined is slightly odd from the perspective of library name
visibility.  For this reason, some primitive classes extend a parent
trait (defined in a non-native component) that contains most of their
non-native functionality and that has full access to the libraries.
For example, FileStream provides a number of generator definitions
that are inherited by FileReadStream.
