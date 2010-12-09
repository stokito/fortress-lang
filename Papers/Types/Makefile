# ################################################################################
#    Copyright 2010 Sun Microsystems, Inc.,
#    4150 Network Circle, Santa Clara, California 95054, U.S.A.
#    All rights reserved.
#
#    U.S. Government Rights - Commercial software.
#    Government users are subject to the Sun Microsystems, Inc. standard
#    license agreement and applicable provisions of the FAR and its supplements.
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
#    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
#    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
################################################################################
# very basic makefile

TICKS = paper.tick introduction.tick abstract.tick related-work.tick setup.tick rules.tick exclusion.tick haskell.tick conclusion.tick exc-preamble.tick exc-spec.tick exc-poly.tick exc-reduction.tick exc-constraints.tick problems.tick
TEXS = $(TICKS:.tick=.tex)

# path to fortify.sty
FORTIFY_PATH = $(FORTRESS_HOME)/Fortify

# run the entire command chain necessary for clean compile
all: $(TICKS)
	make tick
	-make texbatch
	make bib
	-make texbatch
	make tex

# run fortick
tick: $(TICKS)
	$(foreach var, $(TICKS), fortick $(var);)

# make the pdf from the tex files and barf output
tex: $(TEXS)
	TEXINPUTS="$(FORTIFY_PATH):$(TEXINPUTS)" pdflatex -interaction nonstopmode paper.tex

# make the pdf from the tex files and be quiet about it
texbatch: $(TEXS)
	TEXINPUTS="$(FORTIFY_PATH):$(TEXINPUTS)" pdflatex -interaction batchmode paper.tex

# run bibtex
bib: paper.bib
	bibtex paper

open:
	open paper.pdf
    
clean:
	-rm -f $(TEXS)
	-rm -f $(TEXS:.tex=.tex~)
	-rm -f paper.aux paper.log paper.pdf paper.bbl paper.blg
