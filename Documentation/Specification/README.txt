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

FORTRESS_HOME/Documentation/Specification/ is the root directory for the sources
for the Fortress Language Specification.  Hereafter we call it "SPEC_HOME".

It has six subdirectories: Root, Support, Prose, Code, Data, and Generated.
The first five contain source files only, and are never modified by the build
process; Generated contans no source files, and is used as a working area
for temporary files during the build process.  The build process leaves a file
"fortress.pdf" within this root directory.

Here are descrtiptions of the six subdirectories:

"Root" contains files used to build the specification.  In particular, it
includes a top-level LaTeX file, a build script, and README.txt instructions on
how to build the specification.

"Support" contains macros and scripts that are used to build the specification.

"Prose" contains ".tick" files from which ".tex" files are generated, suitable
for processing by LaTeX.  Within a ".tick" file, backquotes may be used to
surround snippets of ASCII Fortress code, and these are processed into LaTeX
source for typesetting.

"Code" contains Fortress code for examples in the specification.  This code is
checked by running it through the Fortress compiler and then executing it; then
LaTeX files containing marked excerpts automatically generated from these code
files for inclusion by prose files.

"Data" contains other language-related data (such as list of keywords) from
which certain tables in the specification are generated.

"Generated" has five subsubdirectories: Root, Support, Prose, Code, and Data.
The substructure of these five subsubdirectories matched the substructure of the
correspondingly named five subdirectories of SPEC_HOME.  As a general rule,
source files are copied from the five subdirectories of SPEC_HOME into the
corresponding subsubdirectories of "Generated" and then processed there.

