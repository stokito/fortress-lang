LATEX = lualatex
# LATEX = xelatex
# xelatex makes a smaller pdf, but only runs on Macs.

all: FortressEncodings.pdf

clean:
	rm *.pdf *.log *.aux

# keep this for anyone who wants to look, who lacks luatex/xelatex and/or the fonts
svn: FortressEncodings-rendered.pdf

FortressEncodings-rendered.pdf: FortressEncodings.pdf
	cp FortressEncodings.pdf FortressEncodings-rendered.pdf

FortressEncodings.pdf: FortressEncodings.tex
	$(LATEX) FortressEncodings.tex
