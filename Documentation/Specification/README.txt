%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   Copyright 2012, Oracle and/or its affiliates.
%   All rights reserved.
%
%
%   Use is subject to license terms.
%
%   This distribution may include materials developed by third parties.
%
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

This is the root directory for the sources for the Fortress Language Specification.
It has several subdirectories corresponding to parts of the specification, 
each of which has several files or subdirectories corresponding to chapters.
In addition, it has the following subdirectories:

Root: This directory contains files used to build the specification.
In particular, it includes a top-level LaTeX file, a build script, and
instructions on how to build the specification.

Support: This directory contains macros and scripts that are used to
build the specification.

Code: This directory contains Fortress code for examples in the
specification.  This code is checked by the compiler, and LaTeX files
with the appropriate excerpts are automatically generated from these
files and added to the appropriate Specification subdirectory.

Data: This directory contains other language related data from which
tables in the specification are generated.

In addition, the specification document (pdf) itself should appear in
this directory.
