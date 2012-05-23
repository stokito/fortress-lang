# ################################################################################
#    Copyright 2010, Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################
# very basic makefile

TICKS = paper.tick introduction.tick abstract.tick related-work.tick setup.tick rules.tick \
        exclusion.tick conclusion.tick exc-spec.tick exc-reduction.tick \
        exc-constraints.tick examples.tick safety.tick discussion.tick fig-exclusion.tick fig-subtype.tick \
        overloading-check.tick fig-existential.tick fig-meet.tick fig-application.tick fig-constraints.tick \
        fig-judgements.tick proofs.tick

TEXS = $(TICKS:.tick=.tex)

# path to fortify.sty
FORTIFY_PATH = $(FORTRESS_HOME)/Fortify
BIN = $(FORTRESS_HOME)/bin

# run the entire command chain necessary for clean compile
all: $(TICKS)
	make tick
	-make texbatch
	make bib
	-make texbatch
	make tex

# run fortick
tick: $(TICKS)
	$(foreach var, $(TICKS), $(BIN)/fortick $(var);)

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
