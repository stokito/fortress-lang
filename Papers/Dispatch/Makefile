################################################################################
#    Copyright 2009 Sun Microsystems, Inc.,
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

OBJ = *.log *.aux *.toc *.dvi *.nav *.out *.snm *.pdf

all: 
	fortick body.tick
	latex main.tex
	dvipdfm	main.dvi

complete: 
	fortick body.tick
	latex main.tex
	bibtex main	
	latex main.tex
	latex main.tex
	dvipdfm	main.dvi

some:
	latex main.tex
	dvipdfm main.dvi

clean:
	rm -f ${OBJ}
	rm -f body.tex




