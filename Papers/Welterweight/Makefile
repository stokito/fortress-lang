# ################################################################################
#    Copyright 2010,2012 Oracle and/or its affiliates.
#    All rights reserved.
#
#
#    Use is subject to license terms.
#
#    This distribution may include materials developed by third parties.
#
################################################################################
# very basic makefile

TICKS = paper.tick introduction.tick notation.tick grammar.tick static.tick evaluation.tick subtyping.tick \
	inference.tick dispatch.tick related-work.tick fig-altmsa.tick \
	fig-constraints.tick fig-covered.tick fig-evaluation.tick fig-exclusion.tick fig-expressiontypes.tick fig-grammar.tick fig-internalsymbols.tick \
	fig-meetrule.tick fig-metavariables.tick fig-miscjudgments.tick fig-miscsubtyping.tick fig-msa.tick fig-quantified.tick \
	fig-subtyping.tick fig-values.tick fig-wellformeddecls.tick fig-wellformedtypes.tick \
        notation-details.tick proofs.tick

TEXS = $(TICKS:.tick=.tex)

# path to fortify.sty
FORTIFY_PATH = $(FORTRESS_HOME)/Fortify
BIN = $(FORTRESS_HOME)/bin

# run the entire command chain necessary for clean compile
all: $(TICKS)
	make tick
	-make tex
	make bib
	-make tex
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
