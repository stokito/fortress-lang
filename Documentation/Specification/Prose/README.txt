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

Build the specification using ./ant (be sure to use the ./).

Options:

bib: build the bibliography
tex: build the complete specification
onlyText: build the specification without the bibliography
genSource: build the fortress examples
clean: clean the build 

Building the specification requires:
- the Fortress compiler (FORTRESS_HOME/bin/fortress)
  (supported by FORTRESS_HOME/ProjectFortress/src/com/sun/fortress/ant_tasks/FortressTask)
- the "fortick" processor (FORTRESS_HOME/bin/fortick)
  (supported by FORTRESS_HOME/ProjectFortress/src/com/sun/fortress/ant_tasks/FortickTask)
- the "fortex" processor (FORTRESS_HOME/bin/fortex)
  (supported by FORTRESS_HOME/ProjectFortress/src/com/sun/fortress/ant_tasks/FortexTask)
- the "foreg" processor (FORTRESS_HOME/bin/foreg)
  (supported by FORTRESS_HOME/ProjectFortress/src/com/sun/fortress/ant_tasks/ForegTask)
- pdflatex/bibtex
- ocaml/ocamlc
- perl
Scripts used to generate files specifically for the specification should be in ../Support.

SOURCE FILES IN THIS DIRECTORY (SPEC_HOME/Root)

fortress.tex: The base LaTeX file.
fortress.bib: The bibliographic database file for BibTeX.
build-options: Adjustable options for building the specification.  (* not yet used *)
build.xml: The build script for ant.
ant: The correct version of ant for processing the build script.

GENERATED FILES (in SPEC_HOME/Generated/Root)

fortress.tex is copied over
fortress.bib is copied over
build-options.tex: LaTeX macros from build-options  (* not yet generated *)
fortress.{log,bbl,blg,toc,aux,pdf,dvi}: Files created by LaTeX/BiBTeX


