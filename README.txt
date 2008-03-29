PROJECT FORTRESS SUBVERSION REPOSITORY

This README exists in the top-level directory of the Fortress project.
Information about Fortress can be found at the following website:

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
                "/Fortify/fortify.el"))

SpecData: Machine-readable files used by the Fortress Language
Specification (e.g., a list of all reserved words).  Editors and other
tools may also benefit from using these files.  Moreover, all examples
included in the language specification are included in the directory
SpecData/examples.

Library: The home for all of the Fortress standard libraries.

bin: Shell scripts for our various projects.  These are bash scripts;
you will need an installation of Bash on your system to run them.

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

We assume you are using an operating system with a Unix-style shell
(for example, Solaris, Linux, Mac OS X, or Cygwin on Windows).  You
will need to have access to the following:

* J2SDK 1.5 or later.  See http://java.sun.com/javase/downloads/index.jsp
* Ant 1.6.5 or later.  See http://ant.apache.org/bindownload.cgi
* JUnit 3.8.1 or later.  See http://prdownloads.sourceforge.net/junit
* Bash version 2.5 or later, installed at /bin/bash.
  See http://www.gnu.org/software/bash/

In your shell startup script, define environment variable
FORTRESS_HOME to point to the PFC directory you checked out.
It is very important to set this environment variable correctly;
it is used by several scripts and build files.

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
by going to the directory $FORTRESS_HOME and typing the command:

  ./ant clean test

If that doesn't work, there's a bug in the interpreter; please issue a
bug report.

Once you have built the interpreter, you can call it from any directory,
on any Fortress file, simply by typing one of the following commands at a
command line:

  fortress compile somefile.fs{s,i}
  fortress [run] [-test] [-debug] somefile.fss arg...
  fortress help

A command of the form "fortress compile somefile.fss" or
"fortress compile somefile.fsi" calls the static checker on the given
file and stores the result in a hidden "cache" directory.  No user-visible
object file is generated.  (At present, the static checker has limited
functionality.  Most significantly, static type errors are not yet
signaled.)  A file with suffix .fsi should contain a single API definition.
The name of the API should match the name of the file.  Similarly, a file
with the suffix .fss should contain a single component definition.  The name
of the component should match the name of the file.

A command of the form "fortress run somefile.fss" checks whether a cached
and up to date result of compiling the given file exists.  If so, it runs
the cached file.  Otherwise, it compiles the given file and runs the result.
This command can be abbreviated as "fortress somefile.fss".  If the optional
flag -test is given, all test functions defined in the given file are run.
If the optional flag -debug is given, stack traces from the underlying
interpreter are displayed when errors are signaled.

If all else fails, look at the script bin/fortress to see if your system
has peculiarities (for example cygwin requires ; separators in the
classpath).


DEMO PROGRAMS

The directory ProjectFortress/demos/ contains some demonstration Fortress
programs.  Among them are:

buffons.fss: Buffon's needle.  Estimates pi using a Monte Carlo
simulation.

lutx.fss: Naive dense LU decomposition.  Demonstrates how to define
new subclasses of Array2.

mm.fss, mm64.fss, mm64x.fss: Matrix multiplication by recursive
decomposition.  The library routine for matrix multiplication uses a
similar cache-oblivious multiplication routine.

sudoku.fss: Solve a simple sudoku by elimination.  Includes a
tree-based set implementation.


COMPONENTS

Fortress currently lacks a full-blown component system.  All the code
in your Fortress program should reside in API and compponent file pairs.
If you take a look at the Fortress programs in ProjectFortress/tests/
or ProjectFortress/demos/ SpecData/examples, you'll see that they have
the same overall structure:


component MyComponent
  exports Executable

  ...  Your program here ...

  run(args:String...):() = ...

end


LANGUAGE FEATURES THAT ARE IMPLEMENTED

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

* Parallel for loops over simple ranges such as 0#n.  Only values of
  type ZZ32Range can currently be used as for loop generators; true
  generators do not yet exist due to shortcomings in the type system
  and the absence of loop desugaring.

* Sequential for loops over simple ranges.  The seq() and sequential()
  functions (which are identical) take a ZZ32Range and return an
  equivalent sequential ZZ32Range.  Every use of the resulting range
  as a loop generator will cause the loop to run sequentially.

* While loops, typecase, if, etc.  Note that for parametric types
  typecase isn't nearly as useful as you might think, since it cannot
  bind type variables; we are working to address this shortcoming.

* The "atomic" construct uses code from the DSTM2 transactional memory
  library.  Nested transactions are flattened.  We use their obstruction
  free algorithm with a simple backoff contention manager.  Reductions
  are not yet implemented, so perform an explicit atomic update instead.

* throw and catch.  Because of this lack we do not yet perform
  arithmetic range checks.

* Generators

* at and other data placement

* spawn

* also (multiple parallel blocks)

LANGUAGE FEATURES THAT ARE NOT IMPLEMENTED

* Numerals with radix specifiers (which implies that some numerals may be
  recognized as identifiers)

* Unicode names

* Dimensions and units

* Static arguments: nat (using minus), int, bool, dimension, and unit

* Modifiers

* Keyword arguments

* True type inference

* Any checking of return types at all

* Where clauses

* Coercion

* Constraint solving for nat parameters

* Reduction variables

* Any of the types which classify operator properties

* Non-println I/O

* Any of the bits and storage types

* Non-RR64 floats

* Integers other than ZZ32 and ZZ64

* Use of ZZ64 for indexing


CHANGES SINCE FORTRESS LANGUAGE SPECIFICATION v.1.0 BETA

* This release of the Fortress language specification is the first to be
released in tandem with a compliant interpreter, available as open source
and online at:

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


BUILT-IN TYPES

There are a bunch of types that are defined internally by the
Fortress interpreter.  With the exception of Any these cannot be
overridden.  Most built-in types do not have any methods.  The
built-in types are:

trait  Number        extends { Any }         excludes { String, Boolean }
trait  Integral      extends { Number }      excludes { String, Boolean, RR64, FloatLiteral }
object ZZ32          extends { Integral }    excludes { String, Boolean, RR64, FloatLiteral }
object ZZ32Range     extends { Any }
object ZZ64          extends { Integral }    excludes { String, Boolean, RR64, FloatLiteral }
object RR64          extends { Number }      excludes { String, Boolean }
object String        extends { Any }         excludes { IntLiteral, FloatLiteral, Boolean }
object Char          extends { Any }
object IntLiteral    extends { ZZ32, ZZ64, RR64 }
object FloatLiteral  extends { RR64 }
object Boolean       extends { Any }

Tuple and arrow types (that are always built-in)

object FlatStorageMaker[\T, n\]
  built-in flat indexed storage of size n containing objects of type
  T.  This type defines get and put methods, but only checks bounds at
  the java level.  It is not intended for programmer consumption, but
  is used to bootstrap support for arrays.

trait  Any           extends {}
  Note that everything is considered to extend the type Any.

Note also that there isn't (yet) a trait Object!  Eventually
user-written trait and object declarations will extend Object by
default; right now they instead extend Any by default.

The library defines primitive functions on the primitive Numbers:
+ -(unary and binary) *(juxtaposition) DOT = <= ^
MIN MAX |x| > < >= =/= are derived from these

For integral types:
DIV REM MOD GCD LCM CHOOSE BITAND BITOR BITXOR LSHIFT RSHIFT BITNOT
widen for ZZ32
narrow for ZZ64

For ZZ64:
> < >= =/= MIN MAX |\x/| |/x\| truncate
sqrt sin cos tanasin acos atan atan2 floor ceiling random |x|
Plus the constants pi and infinity.

For String:
= =/= < <= > >=
juxtaposition means string append, and can include non-string left or
right arguments.  This is presently the only way to convert numbers to
strings for output.

For Boolean (all derived):
AND OR NOT = =/=

For output:
print(Any)
println(Any)

THE LIBRARY

The components FortressBuiltin.fss and FortressLibrary.fss are imported
implicitly, whenever any Fortress program is run.

Note that portions of the library code are commented out; these are
opened and closed by tear lines (***********  and **********).  Much
of this is code transcribed from the language specification for
prototyping and testing purposes.  We intend to make it work one day.


LIBRARY TYPES

Your best guide to library functionality is the library code itself.
This section provides an overview and describes the much of the
non-trivial functionality.

trait Maybe[\T\] comprises { Nothing[\T\], Just[\T\] }
object Nothing[\T\]() extends { Maybe[\T\] }
object Just[\T\](x:T) extends { Maybe[\T\] }

Note that the type Nothing should actually be a singleton without a
type parameter; the absence of where clauses prevents us from writing
it monomorphically, and the absence of polymorphic singletons forces
us to construct a fresh one.

trait Exception comprises { UncheckedException, CheckedException }
trait UncheckedException extends Exception excludes { CheckedException }
trait CheckedException extends Exception excludes { UncheckedException }

These are stubs for a time when exceptions are implemented.

trait Rank[\ nat n \]

There are separate types Rank1, Rank2, and Rank3 which give
appropriate exclusions (since the absence of where clauses prevents us
from giving these exclusions directly).

trait Indexed1[\ nat n \] end
trait Indexed2[\ nat n \] end
trait Indexed3[\ nat n \] end

These indicate that an object has an i^th dimension of size n.

trait Indexed[\T extends Indexed[\T, E, I\], E, I\]
  opr[i:I] : E
  opr[i:I]:=(v:E) : ()
  assign(v:T):T = fill(fn (i:I):E => v[i])
  fill(f:I->E):T
  fill(v:E):T = fill(fn (i:I):E => v)
  copy():T
  mapReduce[\R\](f:(I,E)->R, j:(R,R)->R, z:R):R
  reduce(j:(E,E)->E, z:E):E = mapReduce[\E\](fn (i:I,e:E)=>e, j, z)
end

This defines most of the core array functionality; due to
implementation shortcomings it is not yet fully implemented for the
entire array type hierarchy, though the corresponding methods exist
for every array type.  We read Indexed[\T,E,I\] as "objects of type T
have elements of type E indexed by type I."  This contains indexing
operations.  It also contains functions which compensate for the
absence of array comprehensions and reductions:
    fill fills an array either using a function from index to value, or
        with a fixed value.
    A.reduce(j,z) is equivalent to BIG j [i <- A.indices] A[i], where j
        has zero z.  But note that j is a function, not an operator.
    A.mapReduce(f,j,z) is equivalent to BIG j [i <- A.indices] f(i,A[i])
        with the same caveats as above.

Note that these functions actually use generator-style iteration
internally, so it is possible to define new array layouts and
experiment with generators by using these functions rather than
looping.

trait Array1[\T, nat b0, nat s0\]
    extends { Indexed1 [\s0\], Rank1, Indexed[\Array1[\T,b0,s0\],T,ZZ32\] }
    excludes { Number, String }

1-D arrays.  Note the use of nat types for base b0 and size s0.
Note also that indices are ZZ32 rather than ZZ64; this is because
we're running inside java, which uses 32-bit array indices.  Internal
methods (which you shouldn't use) include get, put, and offset.  The
most interesting methods beyond those in Indexed are:

  subarray[\nat b, nat s, nat o\]():Array1[\T, b, s\]

This returns a structure-sharing subarray with base b and size s
starting from offset o in the current array.  Structure sharing means
updates to one array will be reflected in the other.  To avoid the
structure sharing just call the copy() method.

  replica[\U\]():Array1[\U,b0,s0\]

This returns a "replica" of the array with a different element type.
By "replica" we intend "an array similar in structure to this one, but
with a different element type and fresh storage."

Note that Array1 is a trait; its subclasses are unimportant (unless
you want to define your own, in which case they are instructive) and
they're subject to change anyway.

To create an Array1 you must either write a 1-D aggregate in your
program:

z : ZZ32[3] = [1 2 3]

Or you must replicate an existing array:

v : RR64[3] = z.replica[\RR64\]()

Or you must call a factory function:

w : ZZ64[1000] = array1[\ZZ64,1000\]()
x : ZZ64[1000] = array1[\ZZ64,1000\](17)
y : ZZ64[1000] = array1[\ZZ64,1000\](fn i => 2 i + 1)

The special factory function vector is restricted to numeric argument
types:

x' : ZZ64[1000] = vector[\ZZ64,1000\](17)

At the moment, any Array1 whose element type extends Number is
considered to be a valid vector (this will eventually be accomplished
by coercion, and vectors will be a distinct type).  The pmul
operation is elementwise multiplication; DOT is dot product, as is
juxtaposition; DOT, CROSS, or juxtaposition with a scalar is scalar
multiplication.  ||v|| returns the 2-norm (pythagorean length) of a
vector.

trait Array2[\T, nat b0, nat s0, nat b1, nat s1\]
    extends { Indexed1 [\ s0 \], Indexed2 [\ s1 \] , Rank2 (* ,
              Indexed[\Array2[\T,b0,s0,b1,s1\],T, (ZZ32,ZZ32)\] *) }
    excludes { Number, String }

This trait is structured much like Array1, and also provides:
  replica[\U\]():Array2[\U,b0,s0,b1,s1\]
  t():Array2[\T,b1,s1,b0,s0\]

The latter operation is transposition, and should properly be opr ()^T
when functional methods exist.  Subarray operations aren't defined yet
for two-dimensional arrays.

The factories are also similar to the 1-D case:

array2[\T, nat s0, nat s1\]():Array2[\T,0,s0,0,s1\]
array2[\T, nat s0, nat s1\](v:T):Array2[\T,0,s0,0,s1\]
array2[\T, nat s0, nat s1\](f:(ZZ32,ZZ32)->T):Array2[\T,0,s0,0,s1\]

We consider any Array2 whose element type extends Number to be a
matrix (again this will eventually use coercion).  Matrix arithmetic
defines much the same operators as vector arithmetic; all
multiplication operators are treated the same way.  When both
arguments are matrices, this is matrix multiplication.  When one
argument is a vector, it's matrix/vector or vector/matrix
multiplication.  When one argument is a scalar, it's scalar
multiplication.

Finally Array3 is similar to Array1 and Array2.  It does not yet offer
factories with arguments, nor subarrays, and we do not treat numeric
3-D arrays specially.


OTHER FUNCTIONS

A number of simple functions from the spec are provided:

cast[\T\](x:Any):T
instanceOf[\T\](x:Any):Boolean
ignore(x:Any):() = ()
identity[\T\](x:T):T = x
tuple[\T\](x:T):T = x


SOME POSSIBLY USEFUL UTILITY FUNCTIONS AND CLASSES

These classes aren't strictly intended for external use, but may be
handy as guides to how to write recursively-decomposed computations or
otherwise get things done in the current version of Fortress:

partition(x:ZZ32):(ZZ32,ZZ32)
  canonically partition positive number n into two pieces (a,b) such
  that 0 < a <= b, n = a+b.

trait ReductionBase[\T\]
trait Reduction1[\T, nat s\] extends ReductionBase[\T\]
trait Reduction2[\T, nat s0, nat s1\] extends ReductionBase[\T\]
trait Reduction3[\R,nat s0,nat s1,nat s2\] extends ReductionBase[\R\]

Reductions over 1-, 2-, and 3-D 0-based index spaces.  Used for
defining most of the array methods.

DEFINING NEW PRIMITIVE FUNCTIONS

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
