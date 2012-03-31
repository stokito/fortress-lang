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

HOW TO BUILD THE SPECIFICATION

Build the specification using ./ant (mind the ./).
Options:

bib: build the bibliography
tex : build the complete specification
onlyText: build the specification without the bibliography
genSource: build the fortress examples
clean: clean the build 

Building the specification requires:
- the Fortress compiler (in FORTRESS_HOME/ProjectFortress)
- pdflatex/bibtex
- ocaml/ocamlc
- perl


FILES IN THIS DIRECTORY (FORTRESS_HOME/Documentation/Specification/Root)

Basic files:

fortress.tex: The base LaTeX file.
build-options: Adjustable options for building the specification.
build.xml: 
ant: 

Generated files: 

fortress.(log/bbl/etc): Files created by LaTeX/BiBTeX
fortress-keywords.tex
fortress-specialReservedWords.tex
fortress-unitOperators.tex
build-options.tex: LaTeX macros from build-options (* not yet generated *)

% Scripts used to generate files specifically for the specification should be in ../Support.
